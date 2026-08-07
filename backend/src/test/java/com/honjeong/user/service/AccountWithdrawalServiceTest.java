package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.file.storage.FileStorage;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.notification.repository.NotificationRepository;
import com.honjeong.notification.repository.NotificationSettingsRepository;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.auth.repository.RefreshTokenRepository;
import com.honjeong.auth.repository.SocialAccountRepository;
import com.honjeong.badge.repository.UserBadgeRepository;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserFoodPreferenceRepository;
import com.honjeong.user.repository.UserRepository;

class AccountWithdrawalServiceTest {

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
    // 목이라 "실제로 지워졌는지"는 여기서 검증할 수 없다 — device_tokens 삭제 계약은
    // AccountWithdrawalIntegrationTest(실 Postgres)가 테이블 행 수로 지킨다.
    private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
    private final FavoriteGroupRepository favoriteGroupRepository = mock(FavoriteGroupRepository.class);
    private final UserFoodPreferenceRepository foodPreferenceRepository = mock(UserFoodPreferenceRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final PhoneVerificationRepository phoneVerificationRepository = mock(PhoneVerificationRepository.class);
    private final ConversationService conversationService = mock(ConversationService.class);
    private final FileStorage fileStorage = mock(FileStorage.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-28T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final AccountWithdrawalService service = new AccountWithdrawalService(
            userRepository, checkInRepository, mealRequestRepository, mateRequestRepository, mateRepository,
            blockRepository, notificationRepository, notificationSettingsRepository, userBadgeRepository,
            deviceTokenRepository, favoriteGroupRepository, foodPreferenceRepository, refreshTokenRepository,
            socialAccountRepository,
            phoneVerificationRepository, conversationService, fileStorage, clock);

    @Test
    @DisplayName("탈퇴하면 개인정보가 익명화되고 상태가 WITHDRAWN이 된다")
    void anonymizesUser() {
        User user = activeUser(1L, "http://localhost:8080/files/me.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());

        service.withdraw(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getNickname()).isNull();
        assertThat(user.getPhone()).isNull();
    }

    @Test
    @DisplayName("탈퇴하면 개인정보성 테이블을 전부 지운다")
    void deletesPersonalData() {
        User user = activeUser(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());

        service.withdraw(1L);

        verify(socialAccountRepository).deleteAllByUserId(1L);
        verify(refreshTokenRepository).deleteAllByUserId(1L);
        verify(foodPreferenceRepository).deleteAllByUserId(1L);
        verify(favoriteGroupRepository).deleteAllByUserId(1L);
        verify(mateRepository).deleteAllInvolvingUser(1L);
        verify(mateRequestRepository).deleteAllInvolvingUser(1L);
        verify(blockRepository).deleteAllInvolvingUser(1L);
        verify(notificationRepository).deleteAllByUserId(1L);
        verify(notificationSettingsRepository).deleteAllByUserId(1L);
        verify(userBadgeRepository).deleteAllByUserId(1L);
    }

    @Test
    @DisplayName("프로필 사진이 있으면 파일까지 지우고, 없으면 저장소를 건드리지 않는다")
    void deletesProfilePhotoOnlyWhenPresent() {
        User withPhoto = activeUser(1L, "http://localhost:8080/files/me.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(withPhoto));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());
        service.withdraw(1L);
        verify(fileStorage).delete("http://localhost:8080/files/me.jpg");

        User noPhoto = activeUser(2L, null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(noPhoto));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(2L), anyCollection())).thenReturn(Optional.empty());
        service.withdraw(2L);
        verify(fileStorage, never()).delete(null);
    }

    @Test
    @DisplayName("진행 중인 PENDING 같이먹기 신청을 EXPIRED로 종결한다 — 상대가 거절한 게 아니므로 DECLINED가 아니다")
    void expiresPendingMealRequests() {
        User user = activeUser(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());

        service.withdraw(1L);

        verify(mealRequestRepository).expireAllPendingOf(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("같이 먹는 중(TOGETHER)이면 파트너 체크인도 함께 종료하고 대화를 닫는다")
    void endsTogetherPairAndClosesConversation() {
        User user = activeUser(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CheckIn mine = mock(CheckIn.class);
        when(mine.getStatus()).thenReturn(CheckInStatus.TOGETHER);
        when(mine.getMealRequestId()).thenReturn(77L);
        CheckIn partner = mock(CheckIn.class);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(77L)).thenReturn(List.of(mine, partner));

        service.withdraw(1L);

        verify(mine).end(any(LocalDateTime.class));
        verify(partner).end(any(LocalDateTime.class));   // 한쪽만 끝내면 상대가 "같이 먹는 중"에 갇힌다
        verify(conversationService).close(77L);
    }

    @Test
    @DisplayName("혼자 혼밥 중(ACTIVE)이면 내 체크인만 종료하고 대화는 건드리지 않는다")
    void endsSoloCheckInOnly() {
        User user = activeUser(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CheckIn solo = mock(CheckIn.class);
        when(solo.getStatus()).thenReturn(CheckInStatus.ACTIVE);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.of(solo));

        service.withdraw(1L);

        verify(solo).end(any(LocalDateTime.class));
        verify(conversationService, never()).close(anyLong());
    }

    @Test
    @DisplayName("모집중(SEEKING)이면 end가 아니라 cancel로 종료한다 — end()는 SEEKING을 무시하는 가드가 있다")
    void cancelsSeekingCheckInInsteadOfEnding() {
        User user = activeUser(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        CheckIn seeking = mock(CheckIn.class);
        when(seeking.getStatus()).thenReturn(CheckInStatus.SEEKING);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.of(seeking));

        service.withdraw(1L);

        verify(seeking).cancel(any(LocalDateTime.class));
        verify(seeking, never()).end(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("휴대폰 번호가 있으면 인증 기록도 지우고, 소셜 온리(휴대폰 없음) 계정은 건드리지 않는다")
    void deletesPhoneVerificationsOnlyWhenPhonePresent() {
        User withPhone = activeUser(1L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(withPhone));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());
        service.withdraw(1L);
        verify(phoneVerificationRepository).deleteAllByPhone("01011111");

        User socialOnly = User.pending(null, "a@b.com");
        ReflectionTestUtils.setField(socialOnly, "id", 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(socialOnly));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(2L), anyCollection())).thenReturn(Optional.empty());
        service.withdraw(2L);
        verify(phoneVerificationRepository, never()).deleteAllByPhone(null);
    }

    @Test
    @DisplayName("없는 회원이면 USER_NOT_FOUND")
    void unknownUser() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(9L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // --- 픽스처 --------------------------------------------------------------

    private User activeUser(Long id, String photoUrl) {
        User u = User.pending("0101111" + id, null);
        u.completeProfile("닉" + id, null, null, null, null, null, null, null, photoUrl);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

}
