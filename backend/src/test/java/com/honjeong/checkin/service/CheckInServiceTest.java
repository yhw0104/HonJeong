package com.honjeong.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.badge.service.BadgeService;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.service.ConversationService;
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
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.place.domain.Place;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.service.NotificationService;
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
    private final BlockRepository blockRepository = mock(BlockRepository.class);
    private final MealRequestRepository mealRequestRepository = mock(MealRequestRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final BadgeService badgeService = mock(BadgeService.class);
    private final ConversationService conversationService = mock(ConversationService.class);
    // KST 12:00 = UTC 03:00 으로 고정. now()는 ofInstant(instant, KST) = 2026-06-15T12:00.
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T03:00:00Z"), ZoneOffset.UTC);
    private final HonjeongCheckInProperties props = new HonjeongCheckInProperties(3, 300_000L, 3, 3);
    private final CheckInService service =
            new CheckInService(checkInRepository, placeService, userRepository, blockRepository,
                    mealRequestRepository, notificationService, clock, props, badgeService, conversationService);

    private final LocalDateTime nowKst = LocalDateTime.of(2026, 6, 15, 12, 0);

    private Place place(long id) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        return place;
    }

    private User userRef(long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private User userRef(long id, String nickname) {
        User user = userRef(id);
        when(user.getNickname()).thenReturn(nickname);
        return user;
    }

    private CheckInRequest request() {
        return new CheckInRequest(3L);
    }

    @Test
    @DisplayName("createCheckIn: 기존 활성(SEEKING/ACTIVE/TOGETHER) 없으면 새 SEEKING 체크인을 저장하고 응답을 반환한다")
    void create_new() {
        // given: placeId=3 장소 조회 결과, 기존 활성 없음, save는 인자를 그대로 반환
        Place place = place(3L);
        when(placeService.getById(3L)).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        CheckInResponse res = service.createCheckIn(1L, request());

        // then: SEEKING·placeId·startedAt(KST now) 매핑, 저장 호출됨
        assertThat(res.status()).isEqualTo("SEEKING");
        assertThat(res.placeId()).isEqualTo(3L);
        assertThat(res.startedAt()).isEqualTo(nowKst);
        verify(checkInRepository).save(any(CheckIn.class));
    }

    @Test
    @DisplayName("createCheckIn은 SEEKING 상태 체크인을 만든다")
    void 체크인_생성은_모집중() {
        Place place = place(3L);
        when(placeService.getById(3L)).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());
        User user = userRef(1L);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckInResponse res = service.createCheckIn(1L, new CheckInRequest(3L));

        assertThat(res.status()).isEqualTo("SEEKING");
    }

    @Test
    @DisplayName("이미 모집중이면 다른 장소 재요청은 409")
    void 활동중_다른장소_409() {
        Place place = place(3L);
        Place place2 = place(4L);
        when(placeService.getById(4L)).thenReturn(place2);   // 다른 장소
        CheckIn existing = CheckIn.startSeeking(userRef(1L), place, nowKst);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createCheckIn(1L, new CheckInRequest(4L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECKIN_ALREADY_ACTIVE));
    }

    @Test
    @DisplayName("createCheckIn: 같은 장소에 이미 SEEKING이면 기존을 멱등 반환하고 저장하지 않는다")
    void create_samePlace_idempotent() {
        // given: 기존 SEEKING의 place와 새 요청의 place가 같은 id=3
        Place place = place(3L);
        when(placeService.getById(3L)).thenReturn(place);
        CheckIn existing = CheckIn.startSeeking(mock(User.class), place, nowKst);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.of(existing));

        // when
        CheckInResponse res = service.createCheckIn(1L, request());

        // then: 기존 반환, 저장 없음
        assertThat(res.placeId()).isEqualTo(3L);
        assertThat(res.status()).isEqualTo("SEEKING");
        verify(checkInRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCheckIn: 이미 ACTIVE면 같은 장소여도 CHECKIN_ALREADY_ACTIVE(409) — 멱등 반환 아님")
    void create_samePlace_activeConflict() {
        // given: 기존이 ACTIVE(혼밥중)이고 요청 placeId도 같은 3L. SEEKING만 멱등이므로 409여야 한다.
        Place place = place(3L);
        when(placeService.getById(3L)).thenReturn(place);
        CheckIn existing = CheckIn.start(userRef(1L), place, nowKst);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> service.createCheckIn(1L, new CheckInRequest(3L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECKIN_ALREADY_ACTIVE));
        verify(checkInRepository, never()).save(any());
    }

    @Test
    @DisplayName("createCheckIn: 다른 장소에 이미 활성(SEEKING/ACTIVE/TOGETHER)이면 CHECKIN_ALREADY_ACTIVE(409)")
    void create_differentPlace_conflict() {
        // given: 기존 활성 place id=4, 새 요청 place id=3
        Place requestedPlace = place(3L);
        Place existingPlace = place(4L);
        when(placeService.getById(3L)).thenReturn(requestedPlace);
        CheckIn existing = CheckIn.startSeeking(mock(User.class), existingPlace, nowKst);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.of(existing));

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
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());
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
        verify(badgeService).checkAndAward(1L, true); // 솔로 완료 → 혼밥 뱃지 지급 체크
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
    @DisplayName("endCheckIn: TOGETHER 종료 시 같은 매칭의 파트너도 함께 ENDED")
    void endCheckIn_endsPartner() {
        CheckIn mine = CheckIn.startTogether(userRef(1L), place(10L), 50L, nowKst);
        CheckIn partner = CheckIn.startTogether(userRef(2L), place(10L), 50L, nowKst);
        when(checkInRepository.findById(3L)).thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(50L)).thenReturn(List.of(mine, partner));

        CheckInResponse res = service.endCheckIn(1L, 3L);

        assertThat(mine.getStatus()).isEqualTo(CheckInStatus.ENDED);
        assertThat(partner.getStatus()).isEqualTo(CheckInStatus.ENDED);
        assertThat(res.status()).isEqualTo("ENDED");
        assertThat(res.matchedAt()).isEqualTo(nowKst);
    }

    @Test
    @DisplayName("cancelCheckIn: 소유자의 ACTIVE를 CANCELLED로 전이한다")
    void cancelCheckIn_cancelsActive() {
        CheckIn c = CheckIn.start(userRef(1L), place(10L), nowKst);
        when(checkInRepository.findById(3L)).thenReturn(Optional.of(c));

        CheckInResponse res = service.cancelCheckIn(1L, 3L);

        assertThat(res.status()).isEqualTo("CANCELLED");
        assertThat(c.getStatus()).isEqualTo(CheckInStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelCheckIn: 소유자의 SEEKING도 CANCELLED로 전이하고, 이 체크인의 대기 신청을 자동 정리한다")
    void cancelCheckIn_cancelsSeeking() {
        CheckIn c = CheckIn.startSeeking(userRef(1L), place(10L), nowKst);
        when(checkInRepository.findById(3L)).thenReturn(Optional.of(c));

        CheckInResponse res = service.cancelCheckIn(1L, 3L);

        assertThat(res.status()).isEqualTo("CANCELLED");
        assertThat(c.getStatus()).isEqualTo(CheckInStatus.CANCELLED);
        verify(mealRequestRepository).expirePendingByToCheckIn(3L, nowKst); // 그만두면 대기 신청은 좀비 → 만료
    }

    @Test
    @DisplayName("cancelCheckIn: ACTIVE가 아니면 CHECKIN_NOT_ACTIVE")
    void cancelCheckIn_rejectsNonActive() {
        CheckIn c = CheckIn.start(userRef(1L), place(10L), nowKst);
        c.end(nowKst);
        when(checkInRepository.findById(3L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.cancelCheckIn(1L, 3L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECKIN_NOT_ACTIVE));
    }

    @Test
    @DisplayName("cancelCheckIn: 타인 체크인이면 FORBIDDEN")
    void cancelCheckIn_rejectsNonOwner() {
        CheckIn c = CheckIn.start(userRef(2L), place(10L), nowKst);
        when(checkInRepository.findById(3L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.cancelCheckIn(1L, 3L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("cancelCheckIn: 없으면 CHECKIN_NOT_FOUND(404)")
    void cancelCheckIn_notFound() {
        when(checkInRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelCheckIn(1L, 3L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CHECKIN_NOT_FOUND));
    }

    @Test
    @DisplayName("dineAlone은 SEEKING을 ACTIVE(혼밥중)로 전이하고, 이 체크인의 대기 신청을 자동 정리한다")
    void 혼자먹기_시작() {
        CheckIn seeking = CheckIn.startSeeking(userRef(1L), place(10L), nowKst);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(seeking));

        CheckInResponse res = service.dineAlone(1L, 10L);

        assertThat(res.status()).isEqualTo("ACTIVE");
        verify(mealRequestRepository).expirePendingByToCheckIn(10L, nowKst); // 혼자 먹기 시작 → 대기 신청 만료
    }

    @Test
    @DisplayName("dineAlone은 SEEKING이 아니면 CHECKIN_NOT_SEEKING")
    void 혼자먹기_비SEEKING_예외() {
        CheckIn together = CheckIn.startTogether(userRef(1L), place(10L), 7L, nowKst);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(together));

        assertThatThrownBy(() -> service.dineAlone(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CHECKIN_NOT_SEEKING);
    }

    @Test
    @DisplayName("getMyCurrentCheckIn: ACTIVE 있으면 응답(파트너 없음), 없으면 null")
    void myCurrent_active() {
        CheckIn ci = CheckIn.start(mock(User.class), place(3L), nowKst);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.of(ci));
        CheckInResponse res = service.getMyCurrentCheckIn(1L);
        assertThat(res).isNotNull();
        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(res.partnerNickname()).isNull();
        assertThat(res.conversationId()).isNull(); // 비TOGETHER는 대화 id도 항상 null

        when(checkInRepository.findByUser_IdAndStatusIn(eq(2L), anyCollection())).thenReturn(Optional.empty());
        assertThat(service.getMyCurrentCheckIn(2L)).isNull();
    }

    @Test
    @DisplayName("getMyCurrentCheckIn: TOGETHER면 파트너 닉네임을 포함한다")
    void getMyCurrent_togetherWithPartner() {
        CheckIn mine = CheckIn.startTogether(userRef(1L), place(10L), 50L, nowKst);   // 나
        CheckIn partner = CheckIn.startTogether(userRef(2L, "상대"), place(10L), 50L, nowKst); // 파트너
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(50L))
                .thenReturn(List.of(mine, partner));

        CheckInResponse res = service.getMyCurrentCheckIn(1L);

        assertThat(res.status()).isEqualTo("TOGETHER");
        assertThat(res.partnerNickname()).isEqualTo("상대");
        assertThat(res.partnerUserId()).isEqualTo(2L); // 노쇼 신고 대상 지정용
    }

    @Test
    @DisplayName("getMyCurrentCheckIn: TOGETHER면 대화방 id(conversationId)도 함께 포함한다 — 상태바→대화 진입용")
    void getMyCurrent_togetherIncludesConversationId() {
        CheckIn mine = CheckIn.startTogether(userRef(1L), place(10L), 50L, nowKst);   // 나
        CheckIn partner = CheckIn.startTogether(userRef(2L, "상대"), place(10L), 50L, nowKst); // 파트너
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(50L))
                .thenReturn(List.of(mine, partner));
        when(conversationService.findIdByMealRequestId(50L)).thenReturn(42L);

        CheckInResponse res = service.getMyCurrentCheckIn(1L);

        assertThat(res.status()).isEqualTo("TOGETHER");
        assertThat(res.conversationId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("leaveMatch(ACTIVE): 내 TOGETHER를 혼밥(ACTIVE)로, 상대는 SEEKING 복귀 + 알림 발행")
    void leaveMatch_toActive() {
        CheckIn mine = CheckIn.startTogether(userRef(1L), place(10L), 50L, nowKst);
        CheckIn partner = CheckIn.startTogether(userRef(2L, "상대"), place(10L), 50L, nowKst);
        when(checkInRepository.findById(100L)).thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(50L)).thenReturn(List.of(mine, partner));

        CheckInResponse res = service.leaveMatch(1L, 100L, "ACTIVE");

        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(mine.getStatus()).isEqualTo(CheckInStatus.ACTIVE);
        assertThat(mine.getMealRequestId()).isNull();
        assertThat(partner.getStatus()).isEqualTo(CheckInStatus.SEEKING);   // 상대는 다시 모집중으로 복귀
        assertThat(partner.getMealRequestId()).isNull();
        verify(notificationService).publish(2L, NotificationType.MEAL_MATCH_CANCELLED, 1L);
    }

    @Test
    @DisplayName("leaveMatch(CANCELLED): 내 TOGETHER를 취소(CANCELLED)로, 상대는 SEEKING 복귀")
    void leaveMatch_toCancelled() {
        CheckIn mine = CheckIn.startTogether(userRef(1L), place(10L), 50L, nowKst);
        CheckIn partner = CheckIn.startTogether(userRef(2L, "상대"), place(10L), 50L, nowKst);
        when(checkInRepository.findById(100L)).thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(50L)).thenReturn(List.of(mine, partner));

        CheckInResponse res = service.leaveMatch(1L, 100L, "CANCELLED");

        assertThat(res.status()).isEqualTo("CANCELLED");
        assertThat(partner.getStatus()).isEqualTo(CheckInStatus.SEEKING);
    }

    @Test
    @DisplayName("leaveMatch: 매칭 해체(노쇼/취소) 시 mealRequestId로 대화를 닫는다"
            + "(mealRequestId는 mine.leaveMatch가 null로 지우므로 그 전에 캡처해 넘겨야 함 — endCheckIn/TTL만료/차단정리와 동일 불변식)")
    void leaveMatch_closesConversation() {
        CheckIn mine = CheckIn.startTogether(userRef(1L), place(10L), 50L, nowKst);
        CheckIn partner = CheckIn.startTogether(userRef(2L, "상대"), place(10L), 50L, nowKst);
        when(checkInRepository.findById(100L)).thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(50L)).thenReturn(List.of(mine, partner));

        service.leaveMatch(1L, 100L, "ACTIVE");

        assertThat(mine.getMealRequestId()).isNull(); // leaveMatch 후 실제로 null로 지워짐(캡처 필요성 증명)
        verify(conversationService).close(50L);
    }

    @Test
    @DisplayName("leaveMatch: to가 ACTIVE/SEEKING/CANCELLED가 아니면 INVALID_INPUT")
    void leaveMatch_invalidTo() {
        assertThatThrownBy(() -> service.leaveMatch(1L, 100L, "ENDED"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("leaveMatch: TOGETHER가 아니면 CHECKIN_NOT_TOGETHER")
    void leaveMatch_notTogether() {
        CheckIn active = CheckIn.start(userRef(1L), place(10L), nowKst); // ACTIVE
        when(checkInRepository.findById(100L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.leaveMatch(1L, 100L, "SEEKING"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHECKIN_NOT_TOGETHER);
    }

    @Test
    @DisplayName("getMyCurrentCheckIn: 현재 체크인이 없으면 null")
    void getMyCurrent_none() {
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.empty());
        assertThat(service.getMyCurrentCheckIn(1L)).isNull();
    }

    @Test
    @DisplayName("getMyCurrentCheckIn: SEEKING도 조회 대상(SEEKING/ACTIVE/TOGETHER)에 포함해 상태를 반환한다"
            + "(재시작 후에도 진행 중 체크인을 복구할 수 있어야 함 — createCheckIn의 409 판단과 일치)")
    void getMyCurrent_seeking() {
        // given: 상태 목록을 정확히 SEEKING/ACTIVE/TOGETHER로 조회할 때만 값을 반환하도록 엄격히 스텁한다.
        // anyCollection()을 쓰면 서비스가 실제로 어떤 목록을 넘기는지와 무관하게 통과해버려
        // "/me가 SEEKING을 빠뜨린다"는 버그를 못 잡는다(응답 매핑 분기 자체는 이미 SEEKING을 처리하므로).
        User user = userRef(1L);
        Place place = place(3L);
        CheckIn seeking = CheckIn.startSeeking(user, place, nowKst);
        List<CheckInStatus> expectedStatuses =
                List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE, CheckInStatus.TOGETHER);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), eq(expectedStatuses)))
                .thenReturn(Optional.of(seeking));

        // when
        CheckInResponse res = service.getMyCurrentCheckIn(1L);

        // then
        assertThat(res).isNotNull();
        assertThat(res.status()).isEqualTo("SEEKING");
        assertThat(res.partnerNickname()).isNull();
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
    @DisplayName("stats는 activeCount와 seekingCount를 함께 반환한다")
    void 통계_모집중_포함() {
        when(checkInRepository.countDistinctUsersStartedSince(any())).thenReturn(5L);
        when(checkInRepository.countByStatus(CheckInStatus.ACTIVE)).thenReturn(2L);
        when(checkInRepository.countByStatus(CheckInStatus.SEEKING)).thenReturn(3L);

        CheckInStatsResponse res = service.getStats();

        assertThat(res.activeCount()).isEqualTo(2L);
        assertThat(res.seekingCount()).isEqualTo(3L);
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
        MapMarkerResponse near = new MapMarkerResponse(2L, "가까운집", 37.5010, 127.0005, 1, 0);
        MapMarkerResponse mid = new MapMarkerResponse(1L, "중간집", 37.5040, 127.0000, 2, 0);
        MapMarkerResponse far = new MapMarkerResponse(3L, "먼집", 37.5180, 127.0000, 5, 0);
        when(checkInRepository.countActiveByPlaceWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(mid, near, far));

        var result = service.getMap(37.5, 127.0, 1000);

        // far 제외, near→mid 거리순
        assertThat(result).extracting(MapMarkerResponse::placeId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("getSeekers: 닉네임·경과분(now−startedAt) 매핑")
    void seekers_elapsed() {
        // clock now = 2026-06-15T12:00 KST. 11:45 시작 → 경과 15분
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(user.getNickname()).thenReturn("혼밥러");
        CheckIn ci = CheckIn.startSeeking(user, place(3L), nowKst.minusMinutes(15));
        when(blockRepository.findExclusionIds(1L)).thenReturn(List.of(-1L));
        when(checkInRepository.findSeekingWithUserByPlace(3L, List.of(-1L))).thenReturn(List.of(ci));

        var seekers = service.getSeekers(1L, 3L);

        assertThat(seekers).hasSize(1);
        assertThat(seekers.get(0).userId()).isEqualTo(5L);
        assertThat(seekers.get(0).nickname()).isEqualTo("혼밥러");
        assertThat(seekers.get(0).elapsedMinutes()).isEqualTo(15L);
    }

    @Test
    @DisplayName("getSeekers: 차단 상호 은닉 — blockRepository의 제외 id를 리포지토리에 그대로 전달한다")
    void seekers_passesExclusionIdsFromBlockRepository() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(user.getNickname()).thenReturn("혼밥러");
        CheckIn ci = CheckIn.startSeeking(user, place(3L), nowKst.minusMinutes(15));
        when(blockRepository.findExclusionIds(1L)).thenReturn(List.of(9L, 10L));
        when(checkInRepository.findSeekingWithUserByPlace(3L, List.of(9L, 10L))).thenReturn(List.of(ci));

        var seekers = service.getSeekers(1L, 3L);

        assertThat(seekers).hasSize(1);
        verify(checkInRepository).findSeekingWithUserByPlace(3L, List.of(9L, 10L));
    }

    @Test
    @DisplayName("getCheckinSummary: totalDiners=전체 세션 수(중복 포함, 바 합과 동일) + summarize 조립")
    void 체크인_요약_조립() {
        Place p = place(9L);
        when(placeService.getById(9L)).thenReturn(p);
        // 같은 사람이 12:00에 두 번 온 것도 각각 카운트 — distinct였다면 5보다 작음
        when(checkInRepository.findDinerStartedAtByPlace(9L)).thenReturn(List.of(
                LocalDateTime.of(2026, 7, 19, 12, 0),
                LocalDateTime.of(2026, 7, 19, 12, 0),
                LocalDateTime.of(2026, 7, 19, 13, 0),
                LocalDateTime.of(2026, 7, 19, 18, 0),
                LocalDateTime.of(2026, 7, 19, 18, 0)));

        var res = service.getCheckinSummary(9L);

        // 전체 세션 5 = 바 합(점심 3 + 저녁 2), distinct 아님
        assertThat(res.totalDiners()).isEqualTo(5);
        assertThat(res.periods()).extracting("count").containsExactly(0L, 3L, 2L, 0L);
        assertThat(res.peakPeriodKey()).isEqualTo("LUNCH"); // 점심 3 vs 저녁 2, 총 5 >= 5(피크 최소값)
        assertThat(res.periods()).extracting("key").containsExactly("MORNING", "LUNCH", "EVENING", "NIGHT");
    }

    @Test
    @DisplayName("getCheckinSummary: 존재하지 않는 placeId면 PLACE_NOT_FOUND(404)")
    void 체크인_요약_장소없음() {
        when(placeService.getById(999L)).thenThrow(new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        assertThatThrownBy(() -> service.getCheckinSummary(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PLACE_NOT_FOUND));
        verify(checkInRepository, never()).findDinerStartedAtByPlace(any());
    }

    @Test
    @DisplayName("expireStaleCheckIns: now-ttl 이전 ACTIVE를 만료시키고 건수를 반환한다")
    void expire() {
        // now=2026-06-15T12:00 KST, ttl=3h → threshold=09:00
        when(checkInRepository.endActiveStartedBefore(
                LocalDateTime.of(2026, 6, 15, 9, 0), nowKst)).thenReturn(2);

        assertThat(service.expireStaleCheckIns()).isEqualTo(2);
    }

    @Test
    @DisplayName("expireStaleCheckIns: ACTIVE와 TOGETHER를 각각의 기준(ttlHours/togetherTtlHours)으로 만료한다"
            + "(threshold 스왑 방지 — 두 ttl을 다른 값으로 구성)")
    void expire_bothStatuses() {
        // ttlHours=3, togetherTtlHours=5로 서로 다르게 구성한다. 공용 props(둘 다 3)를 쓰면 두 threshold가
        // 같은 값(09:00)이 되어 프로덕션이 두 인자를 뒤바꿔 써도 테스트가 통과해버린다(스왑 무방비).
        // 여기서는 ACTIVE→09:00, TOGETHER→07:00로 서로 다르게 만들어 스왑 시 반드시 실패하게 한다.
        HonjeongCheckInProperties props2 = new HonjeongCheckInProperties(3, 300_000L, 5, 3);
        CheckInService service2 =
                new CheckInService(checkInRepository, placeService, userRepository, blockRepository,
                        mealRequestRepository, notificationService, clock, props2, badgeService, conversationService);
        when(checkInRepository.endActiveStartedBefore(any(), any())).thenReturn(2);
        when(checkInRepository.endTogetherMatchedBefore(any(), any())).thenReturn(1);

        int n = service2.expireStaleCheckIns();

        assertThat(n).isEqualTo(3);

        ArgumentCaptor<LocalDateTime> activeThreshold = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> togetherThreshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(checkInRepository).endActiveStartedBefore(activeThreshold.capture(), eq(nowKst));
        verify(checkInRepository).endTogetherMatchedBefore(togetherThreshold.capture(), eq(nowKst));

        // ttlHours=3 → now-3h(09:00), togetherTtlHours=5 → now-5h(07:00). 값이 다르므로 인자가
        // 뒤바뀌면(스왑) 아래 두 단언 중 하나가 실패한다.
        assertThat(activeThreshold.getValue()).isEqualTo(nowKst.minusHours(3));
        assertThat(togetherThreshold.getValue()).isEqualTo(nowKst.minusHours(5));
    }

    @Test
    @DisplayName("expireStaleCheckIns: TOGETHER TTL 만료 대상 mealRequestId를 bulk end 전에 수집해 "
            + "각각 conversationService.close로 대화를 닫는다(bulk UPDATE는 엔티티를 안 거치므로 별도 배선 필요)")
    void expire_closesConversationsForExpiredTogether() {
        LocalDateTime togetherThreshold = nowKst.minusHours(3); // props의 togetherTtlHours=3
        when(checkInRepository.findMealRequestIdsOfTogetherMatchedBefore(togetherThreshold))
                .thenReturn(List.of(10L, 20L));
        when(checkInRepository.endTogetherMatchedBefore(togetherThreshold, nowKst)).thenReturn(2);

        int n = service.expireStaleCheckIns();

        assertThat(n).isEqualTo(2); // endActive(0 기본) + endTogether(2) + cancelSeeking(0 기본)
        verify(conversationService).close(10L);
        verify(conversationService).close(20L);

        // 순서 불변식: mealRequestId 수집이 bulk end보다 먼저 일어나야 한다(bulk end 후엔 matchedAt으로 다시 못 찾음).
        InOrder inOrder = Mockito.inOrder(checkInRepository, conversationService);
        inOrder.verify(checkInRepository).findMealRequestIdsOfTogetherMatchedBefore(togetherThreshold);
        inOrder.verify(checkInRepository).endTogetherMatchedBefore(togetherThreshold, nowKst);
        inOrder.verify(conversationService).close(10L);
        inOrder.verify(conversationService).close(20L);
    }

    @Test
    @DisplayName("expireStaleCheckIns: TOGETHER TTL 만료 대상이 없으면 conversationService를 건드리지 않는다")
    void expire_noExpiredTogether_touchesNoConversation() {
        service.expireStaleCheckIns();

        verifyNoInteractions(conversationService);
    }

    @Test
    @DisplayName("expireStaleCheckIns: SEEKING 만료(CANCELLED)도 seekingTtlHours 기준으로 합산한다"
            + "(threshold 스왑 방지 — 세 ttl을 서로 다른 값으로 구성)")
    void expire_includesSeeking() {
        // ttlHours=3, togetherTtlHours=5, seekingTtlHours=7로 서로 다르게 구성해 어느 한 쌍이 뒤바뀌어도 실패하게 한다.
        HonjeongCheckInProperties props3 = new HonjeongCheckInProperties(3, 300_000L, 5, 7);
        CheckInService service3 =
                new CheckInService(checkInRepository, placeService, userRepository, blockRepository,
                        mealRequestRepository, notificationService, clock, props3, badgeService, conversationService);
        when(checkInRepository.endActiveStartedBefore(any(), any())).thenReturn(2);
        when(checkInRepository.endTogetherMatchedBefore(any(), any())).thenReturn(1);
        when(checkInRepository.cancelSeekingStartedBefore(any(), any())).thenReturn(4);

        int n = service3.expireStaleCheckIns();

        assertThat(n).isEqualTo(7); // 2 + 1 + 4

        ArgumentCaptor<LocalDateTime> seekingThreshold = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(checkInRepository).cancelSeekingStartedBefore(seekingThreshold.capture(), eq(nowKst));
        // seekingTtlHours=7 → now-7h(05:00). ttlHours(3)·togetherTtlHours(5)와 모두 달라 스왑 시 실패한다.
        assertThat(seekingThreshold.getValue()).isEqualTo(nowKst.minusHours(7));
        // 만료로 SEEKING을 벗어난 체크인에 걸린 대기 신청까지 catch-all로 정리한다
        verify(mealRequestRepository).expirePendingForEndedTargets(nowKst);
    }
}
