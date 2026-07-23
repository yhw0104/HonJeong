package com.honjeong.badge.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import com.honjeong.badge.domain.UserBadge;
import com.honjeong.badge.dto.BadgeStatusResponse;
import com.honjeong.badge.repository.UserBadgeRepository;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.review.repository.ReviewRepository;

class BadgeServiceTest {

    private final UserBadgeRepository badgeRepository = mock(UserBadgeRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final MateRepository mateRepository = mock(MateRepository.class);
    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-23T03:00:00Z"), ZoneOffset.UTC);
    private final BadgeService service = new BadgeService(badgeRepository, checkInRepository,
            reviewRepository, mateRepository, favoriteRepository, notificationService, clock);

    private void counts(long solo, long review, long together, long mate, long fav) {
        when(checkInRepository.countSoloCompletedByUser(1L)).thenReturn(solo);
        when(reviewRepository.countSoloAuthenticatedByUser(1L)).thenReturn(review);
        when(checkInRepository.countTogetherByUser(1L)).thenReturn(together);
        when(mateRepository.countByUser_Id(1L)).thenReturn(mate);
        when(favoriteRepository.countDistinctPlaceByUserId(1L)).thenReturn(fav);
    }

    @Test
    @DisplayName("임계 경계: 혼밥 9면 SOLO_10 미지급, 10이면 지급")
    void thresholdBoundary() {
        when(badgeRepository.findKeysByUserId(1L)).thenReturn(List.of());
        counts(9, 0, 0, 0, 0);
        service.checkAndAward(1L, false);
        verify(badgeRepository, never()).save(argThat(b -> b.getBadgeKey().equals("SOLO_10")));
        verify(badgeRepository).save(argThat(b -> b.getBadgeKey().equals("SOLO_1"))); // 9>=1

        clearInvocations(badgeRepository);
        when(badgeRepository.findKeysByUserId(1L)).thenReturn(List.of());
        counts(10, 0, 0, 0, 0);
        service.checkAndAward(1L, false);
        verify(badgeRepository).save(argThat(b -> b.getBadgeKey().equals("SOLO_10")));
    }

    @Test
    @DisplayName("이미 보유한 뱃지는 다시 저장하지 않음(멱등)")
    void skipsOwned() {
        when(badgeRepository.findKeysByUserId(1L)).thenReturn(List.of("SOLO_1"));
        counts(5, 0, 0, 0, 0);
        service.checkAndAward(1L, false);
        verify(badgeRepository, never()).save(argThat(b -> b.getBadgeKey().equals("SOLO_1")));
    }

    @Test
    @DisplayName("notify=true면 새 뱃지마다 BADGE_EARNED 발행, false면 미발행")
    void notifyFlag() {
        when(badgeRepository.findKeysByUserId(1L)).thenReturn(List.of());
        counts(1, 0, 0, 0, 0); // SOLO_1 하나만
        service.checkAndAward(1L, true);
        verify(notificationService).publish(1L, NotificationType.BADGE_EARNED, null);

        clearInvocations(notificationService, badgeRepository);
        when(badgeRepository.findKeysByUserId(1L)).thenReturn(List.of());
        counts(1, 0, 0, 0, 0);
        service.checkAndAward(1L, false);
        verify(notificationService, never()).publish(anyLong(), any(), any());
    }

    @Test
    @DisplayName("동시 지급 경합: save가 유니크 위반 던져도 삼키고 계속")
    void swallowsUniqueViolation() {
        when(badgeRepository.findKeysByUserId(1L)).thenReturn(List.of());
        counts(1, 0, 0, 0, 0);
        when(badgeRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertThatCode(() -> service.checkAndAward(1L, true)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getMyBadges: 10종 전부, 획득 플래그·시각 결합")
    void getMyBadges() {
        when(badgeRepository.findKeysByUserId(1L)).thenReturn(List.of("SOLO_1"));
        counts(1, 0, 0, 0, 0);
        when(badgeRepository.findByUserId(1L))
                .thenReturn(List.of(UserBadge.of(1L, "SOLO_1", LocalDateTime.of(2026, 7, 20, 12, 0))));

        List<BadgeStatusResponse> out = service.getMyBadges(1L);
        assertThat(out).hasSize(10);
        assertThat(out.get(0).key()).isEqualTo("SOLO_1");
        assertThat(out.get(0).earned()).isTrue();
        assertThat(out.get(0).earnedAt()).isEqualTo(LocalDateTime.of(2026, 7, 20, 12, 0));
        assertThat(out).anySatisfy(b -> {
            assertThat(b.key()).isEqualTo("SOLO_50");
            assertThat(b.earned()).isFalse();
            assertThat(b.earnedAt()).isNull();
        });
    }
}
