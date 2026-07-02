package com.honjeong.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.dto.CheckInUserResponse;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.config.HonjeongCheckInProperties;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * CheckInService 단위 테스트(순수 Mockito + 고정 Clock). 단일활성 멱등/409·종료·내 체크인의 비즈니스 규칙을 검증한다.
 * 엔티티 id가 필요한 비교는 mock 엔티티의 getId() 스텁으로 해결한다(DB 없이).
 */
class CheckInServiceTest {

    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final PlaceService placeService = mock(PlaceService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    // KST 12:00 = UTC 03:00 으로 고정. now()는 ofInstant(instant, KST) = 2026-06-15T12:00.
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T03:00:00Z"), ZoneOffset.UTC);
    private final HonjeongCheckInProperties props = new HonjeongCheckInProperties(3, 300_000L);
    private final CheckInService service =
            new CheckInService(checkInRepository, placeService, userRepository, clock, props);

    private final LocalDateTime nowKst = LocalDateTime.of(2026, 6, 15, 12, 0);

    private Place place(long id) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        return place;
    }

    private CheckInRequest request() {
        return new CheckInRequest(3L);
    }

    @Test
    @DisplayName("createCheckIn: 기존 ACTIVE 없으면 새 체크인을 저장하고 응답을 반환한다")
    void create_new() {
        // given: placeId=3 장소 조회 결과, 기존 ACTIVE 없음, save는 인자를 그대로 반환
        Place place = place(3L);
        when(placeService.getById(3L)).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatus(1L, CheckInStatus.ACTIVE)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        CheckInResponse res = service.createCheckIn(1L, request());

        // then: ACTIVE·placeId·startedAt(KST now) 매핑, 저장 호출됨
        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(res.placeId()).isEqualTo(3L);
        assertThat(res.startedAt()).isEqualTo(nowKst);
        verify(checkInRepository).save(any(CheckIn.class));
    }

    @Test
    @DisplayName("createCheckIn: 같은 장소에 이미 ACTIVE면 기존을 멱등 반환하고 저장하지 않는다")
    void create_samePlace_idempotent() {
        // given: 기존 ACTIVE의 place와 새 요청의 place가 같은 id=3
        Place place = place(3L);
        when(placeService.getById(3L)).thenReturn(place);
        CheckIn existing = CheckIn.start(mock(User.class), place, nowKst);
        when(checkInRepository.findByUser_IdAndStatus(1L, CheckInStatus.ACTIVE)).thenReturn(Optional.of(existing));

        // when
        CheckInResponse res = service.createCheckIn(1L, request());

        // then: 기존 반환, 저장 없음
        assertThat(res.placeId()).isEqualTo(3L);
        assertThat(res.status()).isEqualTo("ACTIVE");
        verify(checkInRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCheckIn: 다른 장소에 이미 ACTIVE면 CHECKIN_ALREADY_ACTIVE(409)")
    void create_differentPlace_conflict() {
        // given: 기존 ACTIVE place id=4, 새 요청 place id=3
        Place requestedPlace = place(3L);
        Place existingPlace = place(4L);
        when(placeService.getById(3L)).thenReturn(requestedPlace);
        CheckIn existing = CheckIn.start(mock(User.class), existingPlace, nowKst);
        when(checkInRepository.findByUser_IdAndStatus(1L, CheckInStatus.ACTIVE)).thenReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> service.createCheckIn(1L, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECKIN_ALREADY_ACTIVE));
        verify(checkInRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCheckIn: 경쟁으로 인덱스 위반 시 DataIntegrityViolationException을 409로 변환한다")
    void create_raceConflict() {
        Place place = place(3L);
        when(placeService.getById(3L)).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatus(1L, CheckInStatus.ACTIVE)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(checkInRepository.save(any())).thenThrow(new DataIntegrityViolationException("uq violation"));

        assertThatThrownBy(() -> service.createCheckIn(1L, request()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECKIN_ALREADY_ACTIVE));
    }

    @Test
    @DisplayName("createCheckIn: 존재하지 않는 placeId면 PLACE_NOT_FOUND(404)")
    void create_placeNotFound() {
        when(placeService.getById(999L)).thenThrow(new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        assertThatThrownBy(() -> service.createCheckIn(1L, new CheckInRequest(999L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PLACE_NOT_FOUND));
        verify(checkInRepository, never()).save(any());
    }

    @Test
    @DisplayName("endCheckIn: 본인 ACTIVE 체크인을 ENDED로 종료한다")
    void end_success() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        CheckIn ci = CheckIn.start(user, place(3L), nowKst);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(ci));

        CheckInResponse res = service.endCheckIn(1L, 10L);

        assertThat(res.status()).isEqualTo("ENDED");
        assertThat(res.endedAt()).isEqualTo(nowKst);
    }

    @Test
    @DisplayName("endCheckIn: 없으면 CHECKIN_NOT_FOUND(404)")
    void end_notFound() {
        when(checkInRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.endCheckIn(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECKIN_NOT_FOUND));
    }

    @Test
    @DisplayName("endCheckIn: 본인 것이 아니면 FORBIDDEN(403)")
    void end_notOwner() {
        User other = mock(User.class);
        when(other.getId()).thenReturn(2L);
        CheckIn ci = CheckIn.start(other, place(3L), nowKst);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(ci));

        assertThatThrownBy(() -> service.endCheckIn(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("getMyActiveCheckIn: ACTIVE 있으면 응답, 없으면 null")
    void myActive() {
        CheckIn ci = CheckIn.start(mock(User.class), place(3L), nowKst);
        when(checkInRepository.findByUser_IdAndStatus(1L, CheckInStatus.ACTIVE)).thenReturn(Optional.of(ci));
        assertThat(service.getMyActiveCheckIn(1L)).isNotNull();

        when(checkInRepository.findByUser_IdAndStatus(2L, CheckInStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThat(service.getMyActiveCheckIn(2L)).isNull();
    }

    @Test
    @DisplayName("getStats: todayCount는 KST 자정 기준, activeCount는 ACTIVE 수")
    void stats() {
        // clock = 2026-06-15T12:00 KST → todayStart = 2026-06-15T00:00
        when(checkInRepository.countDistinctUsersStartedSince(LocalDateTime.of(2026, 6, 15, 0, 0)))
                .thenReturn(124L);
        when(checkInRepository.countByStatus(CheckInStatus.ACTIVE)).thenReturn(17L);

        CheckInStatsResponse res = service.getStats();

        assertThat(res.todayCount()).isEqualTo(124L);
        assertThat(res.activeCount()).isEqualTo(17L);
    }

    @Test
    @DisplayName("getMap: lat/lng 누락이면 INVALID_INPUT(400)")
    void map_missingCoords() {
        assertThatThrownBy(() -> service.getMap(null, 127.0, 1000))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("getMap: 반경 밖 마커는 Haversine 보정으로 제외하고 거리순 정렬한다")
    void map_filtersAndSorts() {
        // 중심 (37.5,127.0). near≈120m, mid≈445m, far≈2004m(반경 1000 밖)
        MapMarkerResponse near = new MapMarkerResponse(2L, "가까운집", 37.5010, 127.0005, 1);
        MapMarkerResponse mid = new MapMarkerResponse(1L, "중간집", 37.5040, 127.0000, 2);
        MapMarkerResponse far = new MapMarkerResponse(3L, "먼집", 37.5180, 127.0000, 5);
        when(checkInRepository.countActiveByPlaceWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(mid, near, far));

        var result = service.getMap(37.5, 127.0, 1000);

        // far 제외, near→mid 거리순
        assertThat(result).extracting(MapMarkerResponse::placeId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("getActiveDiners: 닉네임·경과분(now−startedAt) 매핑")
    void diners_elapsed() {
        // clock now = 2026-06-15T12:00 KST. 11:45 시작 → 경과 15분
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(user.getNickname()).thenReturn("혼밥러");
        CheckIn ci = CheckIn.start(user, place(3L), nowKst.minusMinutes(15));
        when(checkInRepository.findActiveWithUserByPlace(3L)).thenReturn(List.of(ci));

        var diners = service.getActiveDiners(3L);

        assertThat(diners).hasSize(1);
        assertThat(diners.get(0).userId()).isEqualTo(5L);
        assertThat(diners.get(0).nickname()).isEqualTo("혼밥러");
        assertThat(diners.get(0).elapsedMinutes()).isEqualTo(15L);
    }

    @Test
    @DisplayName("expireStaleCheckIns: now-ttl 이전 ACTIVE를 만료시키고 건수를 반환한다")
    void expire() {
        // now=2026-06-15T12:00 KST, ttl=3h → threshold=09:00
        when(checkInRepository.endActiveStartedBefore(
                LocalDateTime.of(2026, 6, 15, 9, 0), nowKst)).thenReturn(2);

        assertThat(service.expireStaleCheckIns()).isEqualTo(2);
    }
}
