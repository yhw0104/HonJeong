package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.auth.client.AppleTokenClient;
import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.SocialAccount;
import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.auth.repository.RefreshTokenRepository;
import com.honjeong.auth.repository.SocialAccountRepository;
import com.honjeong.badge.repository.UserBadgeRepository;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.file.storage.FileStorage;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.notification.repository.NotificationRepository;
import com.honjeong.notification.repository.NotificationSettingsRepository;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserFoodPreferenceRepository;
import com.honjeong.user.repository.UserRepository;

/**
 * 탈퇴 시 애플 토큰 폐기 동작. 순서와 실패 내성이 핵심이라 그 둘만 본다.
 *
 * <p>셋업은 {@code AccountWithdrawalServiceTest}의 방식을 그대로 따른다 — 협력자를 전부 Mockito
 * mock 필드로 두고 생성자로 직접 조립한다(상속 계층을 새로 만들지 않는다).
 */
class AccountWithdrawalAppleRevokeTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final MealRequestRepository mealRequestRepository = mock(MealRequestRepository.class);
    private final MateRequestRepository mateRequestRepository = mock(MateRequestRepository.class);
    private final MateRepository mateRepository = mock(MateRepository.class);
    private final BlockRepository blockRepository = mock(BlockRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationSettingsRepository notificationSettingsRepository =
            mock(NotificationSettingsRepository.class);
    private final UserBadgeRepository userBadgeRepository = mock(UserBadgeRepository.class);
    private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
    private final FavoriteGroupRepository favoriteGroupRepository = mock(FavoriteGroupRepository.class);
    private final UserFoodPreferenceRepository foodPreferenceRepository = mock(UserFoodPreferenceRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final PhoneVerificationRepository phoneVerificationRepository = mock(PhoneVerificationRepository.class);
    private final ConversationService conversationService = mock(ConversationService.class);
    private final FileStorage fileStorage = mock(FileStorage.class);
    private final AppleTokenClient appleTokenClient = mock(AppleTokenClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-17T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final AccountWithdrawalService withdrawalService = new AccountWithdrawalService(
            userRepository, checkInRepository, mealRequestRepository, mateRequestRepository, mateRepository,
            blockRepository, notificationRepository, notificationSettingsRepository, userBadgeRepository,
            deviceTokenRepository, favoriteGroupRepository, foodPreferenceRepository, refreshTokenRepository,
            socialAccountRepository,
            phoneVerificationRepository, conversationService, fileStorage, appleTokenClient, clock);

    @Test
    @DisplayName("★소셜 연동을 지우기 전에 폐기한다 — 순서가 뒤집히면 폐기할 토큰이 사라진다")
    void 폐기가_삭제보다_먼저다() {
        givenActiveUser(1L);
        when(socialAccountRepository.findAllByUserId(1L)).thenReturn(List.of(appleAccount(1L, "r-token")));

        withdrawalService.withdraw(1L);

        InOrder order = inOrder(appleTokenClient, socialAccountRepository);
        order.verify(appleTokenClient).revoke("r-token");
        order.verify(socialAccountRepository).deleteAllByUserId(1L);
    }

    @Test
    @DisplayName("★폐기가 실패해도 탈퇴는 완료된다 — 탈퇴권이 애플 가용성에 인질로 잡히면 안 된다")
    void 폐기실패해도_탈퇴는_된다() {
        givenActiveUser(1L);
        when(socialAccountRepository.findAllByUserId(1L)).thenReturn(List.of(appleAccount(1L, "r-token")));
        doThrow(new RuntimeException("apple down")).when(appleTokenClient).revoke(any());

        assertThatCode(() -> withdrawalService.withdraw(1L)).doesNotThrowAnyException();

        verify(socialAccountRepository).deleteAllByUserId(1L);
    }

    @Test
    @DisplayName("카카오 전용 회원은 폐기를 부르지 않는다")
    void 카카오전용은_폐기없음() {
        givenActiveUser(1L);
        when(socialAccountRepository.findAllByUserId(1L))
                .thenReturn(List.of(SocialAccount.of(1L, Provider.KAKAO, "kakao-sub", null)));

        withdrawalService.withdraw(1L);

        verify(appleTokenClient, never()).revoke(any());
    }

    @Test
    @DisplayName("보관된 토큰이 없거나 공백뿐인 애플 계정은 폐기를 부르지 않는다 — 가입 때 code 교환에 실패하면 null로 남는다")
    void 토큰없는_애플계정은_폐기없음() {
        givenActiveUser(1L);
        when(socialAccountRepository.findAllByUserId(1L)).thenReturn(List.of(appleAccount(1L, null)));
        withdrawalService.withdraw(1L);

        // 빈 문자열은 현재 어느 경로로도 저장되지 않지만(교환 실패는 null 계약), 필터의 절반이
        // 검증 없이 남지 않도록 함께 고정한다.
        givenActiveUser(2L);
        when(socialAccountRepository.findAllByUserId(2L)).thenReturn(List.of(appleAccount(2L, "   ")));
        withdrawalService.withdraw(2L);

        verify(appleTokenClient, never()).revoke(any());
    }

    @Test
    @DisplayName("★조회가 DB에서 실패하면 애플 실패로 감추지 않고 그대로 터뜨린다 — try는 폐기 호출만 감싼다")
    void 조회_DB실패는_삼키지_않는다() {
        givenActiveUser(1L);
        when(socialAccountRepository.findAllByUserId(1L))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        // 삼키면 트랜잭션이 rollback-only인 채로 흘러가 커밋 때 UnexpectedRollbackException으로 터지고,
        // 로그에는 "애플 폐기 실패"만 남아 원인을 오해하게 된다.
        assertThatThrownBy(() -> withdrawalService.withdraw(1L))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    // --- 픽스처 --------------------------------------------------------------

    /** 애플 연동 1건. token이 null이면 "가입 때 교환에 실패해 토큰이 없는" 계정이 된다. */
    private SocialAccount appleAccount(Long userId, String token) {
        SocialAccount social = SocialAccount.of(userId, Provider.APPLE, "apple-sub-" + userId, null);
        social.attachAppleRefreshToken(token);
        return social;
    }

    /** 탈퇴 가능한 ACTIVE 회원 1명 — 진행 중인 체크인은 없다(이 테스트의 관심사가 아니다). */
    private void givenActiveUser(Long id) {
        User user = User.pending("0101111" + id, null);
        user.completeProfile("닉" + id, null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(user, "id", id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(id), anyCollection())).thenReturn(Optional.empty());
    }
}
