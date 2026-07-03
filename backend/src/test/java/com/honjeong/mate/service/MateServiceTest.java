package com.honjeong.mate.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.checkin.repository.CheckInRepository.CheckInCountRow;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.dto.MateResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository.MealPairRow;
import java.util.Optional;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

class MateServiceTest {

    private final MateRepository mateRepository = mock(MateRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final MealRequestRepository mealRequestRepository = mock(MealRequestRepository.class);
    private final MateService service = new MateService(mateRepository, checkInRepository, mealRequestRepository);

    @Test
    @DisplayName("deleteMate: 관계 없으면 MATE_NOT_FOUND")
    void deleteMate_notFound() {
        when(mateRepository.findByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteMate(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MATE_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteMate: 양방향 2행 삭제")
    void deleteMate_bidirectional() {
        when(mateRepository.findByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(Optional.of(mock(Mate.class)));
        when(mateRepository.findByUser_IdAndMateUser_Id(2L, 1L)).thenReturn(Optional.of(mock(Mate.class)));
        service.deleteMate(1L, 2L);
        verify(mateRepository, times(2)).delete(any(Mate.class));
    }

    @Test
    @DisplayName("getMyMates: 메이트 없으면 빈 목록(체크인 조회 안 함)")
    void getMyMates_empty() {
        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(List.of());
        assertThat(service.getMyMates(1L)).isEmpty();
        verifyNoInteractions(checkInRepository);
    }

    @Test
    @DisplayName("getMyMates: A=온라인(현재 체크인)·B=오프라인, 필드/카운트 매핑")
    void getMyMates_happyPath() {
        LocalDateTime matesSinceA = LocalDateTime.of(2026, 6, 1, 12, 0);
        LocalDateTime matesSinceB = LocalDateTime.of(2026, 6, 2, 12, 0);
        LocalDateTime startedAtA = LocalDateTime.of(2026, 7, 2, 11, 30);

        User userA = mock(User.class);
        when(userA.getId()).thenReturn(10L);
        when(userA.getNickname()).thenReturn("A닉");
        when(userA.getRegion()).thenReturn("서울 강남구");
        User userB = mock(User.class);
        when(userB.getId()).thenReturn(20L);
        when(userB.getNickname()).thenReturn("B닉");

        Mate mateA = mock(Mate.class);
        when(mateA.getMateUser()).thenReturn(userA);
        when(mateA.getCreatedAt()).thenReturn(matesSinceA);
        Mate mateB = mock(Mate.class);
        when(mateB.getMateUser()).thenReturn(userB);
        when(mateB.getCreatedAt()).thenReturn(matesSinceB);
        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(List.of(mateA, mateB));

        Place placeA = mock(Place.class);
        when(placeA.getId()).thenReturn(100L);
        when(placeA.getName()).thenReturn("김밥천국");
        CheckIn checkInA = mock(CheckIn.class);
        when(checkInA.getUser()).thenReturn(userA);
        when(checkInA.getPlace()).thenReturn(placeA);
        when(checkInA.getStartedAt()).thenReturn(startedAtA);
        when(checkInRepository.findActiveWithPlaceByUserIds(List.of(10L, 20L)))
                .thenReturn(List.of(checkInA));
        List<CheckInCountRow> countRows = List.of(countRow(10L, 5L), countRow(20L, 3L));
        when(checkInRepository.countByUserIds(List.of(10L, 20L))).thenReturn(countRows);

        List<MateResponse> result = service.getMyMates(1L);

        assertThat(result).hasSize(2);
        MateResponse a = result.get(0);
        assertThat(a.mateUserId()).isEqualTo(10L);
        assertThat(a.nickname()).isEqualTo("A닉");
        assertThat(a.region()).isEqualTo("서울 강남구");
        assertThat(a.isOnline()).isTrue();
        assertThat(a.currentPlaceId()).isEqualTo(100L);
        assertThat(a.currentPlaceName()).isEqualTo("김밥천국");
        assertThat(a.checkInStartedAt()).isEqualTo(startedAtA);
        assertThat(a.checkInCount()).isEqualTo(5L);
        assertThat(a.mealsTogether()).isZero();
        assertThat(a.matesSince()).isEqualTo(matesSinceA);

        MateResponse b = result.get(1);
        assertThat(b.mateUserId()).isEqualTo(20L);
        assertThat(b.isOnline()).isFalse();
        assertThat(b.currentPlaceId()).isNull();
        assertThat(b.currentPlaceName()).isNull();
        assertThat(b.checkInStartedAt()).isNull();
        assertThat(b.checkInCount()).isEqualTo(3L);
        assertThat(b.matesSince()).isEqualTo(matesSinceB);
    }

    @Test
    @DisplayName("getMyMates: mealsTogether = 나↔각 메이트 수락 건수(양방향 합산, 비메이트 상대는 무시)")
    void getMyMates_mealsTogether() {
        User userA = mock(User.class);
        when(userA.getId()).thenReturn(10L);
        User userB = mock(User.class);
        when(userB.getId()).thenReturn(20L);
        Mate mateA = mock(Mate.class);
        when(mateA.getMateUser()).thenReturn(userA);
        Mate mateB = mock(Mate.class);
        when(mateB.getMateUser()).thenReturn(userB);
        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(List.of(mateA, mateB));
        when(checkInRepository.findActiveWithPlaceByUserIds(List.of(10L, 20L))).thenReturn(List.of());
        when(checkInRepository.countByUserIds(List.of(10L, 20L))).thenReturn(List.of());
        // 나(1L)↔A(10L): 내가 신청 1 + A가 신청 1 = 2회, 나↔B: 0회, 나↔비메이트(99L): 1회(무시)
        List<MealPairRow> pairs = List.of(pair(1L, 10L), pair(10L, 1L), pair(1L, 99L));
        when(mealRequestRepository.findAcceptedPairsForUser(1L)).thenReturn(pairs);

        List<MateResponse> result = service.getMyMates(1L);

        assertThat(result.get(0).mateUserId()).isEqualTo(10L);
        assertThat(result.get(0).mealsTogether()).isEqualTo(2L);
        assertThat(result.get(1).mateUserId()).isEqualTo(20L);
        assertThat(result.get(1).mealsTogether()).isZero();
    }

    private CheckInCountRow countRow(long userId, long cnt) {
        CheckInCountRow row = mock(CheckInCountRow.class);
        when(row.getUserId()).thenReturn(userId);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }

    private MealPairRow pair(long fromId, long toId) {
        MealPairRow row = mock(MealPairRow.class);
        when(row.getFromId()).thenReturn(fromId);
        when(row.getToId()).thenReturn(toId);
        return row;
    }
}
