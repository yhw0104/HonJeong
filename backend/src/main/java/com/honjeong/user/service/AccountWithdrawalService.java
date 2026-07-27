package com.honjeong.user.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.auth.repository.RefreshTokenRepository;
import com.honjeong.auth.repository.SocialAccountRepository;
import com.honjeong.badge.repository.UserBadgeRepository;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.file.storage.FileStorage;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.notification.repository.NotificationRepository;
import com.honjeong.notification.repository.NotificationSettingsRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserFoodPreferenceRepository;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: 회원 탈퇴 — 진행 중인 관계를 정리하고 개인정보성 데이터를 지운 뒤 users 행을 익명화한다
 * 2. 사용 Controller: UserController(DELETE /api/users/me)
 *
 * <p><b>왜 소프트 탈퇴인가.</b> {@code check_ins.user_id}가 NOT NULL이라 행을 지우면 혼밥 통계가 함께
 * 사라지고, 리뷰·대화도 FK로 묶여 있어 하드 삭제는 스키마를 9곳 고쳐야 한다. 대신 개인정보 컬럼만 비워
 * (익명화) 콘텐츠는 '알 수 없음' 작성자로 남긴다.
 *
 * <p><b>재가입.</b> phone과 social_accounts가 사라지므로 {@code findByPhone}·
 * {@code findByProviderAndProviderUserId}가 모두 미스가 나서, 같은 번호·같은 카카오 계정으로 다시 가입하면
 * 완전히 새 회원이 만들어진다(이전 기록은 딸려오지 않는다).
 *
 * <p>정리 과정에서 <b>알림은 발행하지 않는다</b>(BlockService의 기존 관례와 동일).
 */
@Service
public class AccountWithdrawalService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final CheckInRepository checkInRepository;
    private final MealRequestRepository mealRequestRepository;
    private final MateRequestRepository mateRequestRepository;
    private final MateRepository mateRepository;
    private final BlockRepository blockRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final FavoriteGroupRepository favoriteGroupRepository;
    private final UserFoodPreferenceRepository foodPreferenceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final ConversationService conversationService;
    private final FileStorage fileStorage;
    private final Clock clock;

    public AccountWithdrawalService(UserRepository userRepository, CheckInRepository checkInRepository,
            MealRequestRepository mealRequestRepository, MateRequestRepository mateRequestRepository,
            MateRepository mateRepository, BlockRepository blockRepository,
            NotificationRepository notificationRepository,
            NotificationSettingsRepository notificationSettingsRepository,
            UserBadgeRepository userBadgeRepository, FavoriteGroupRepository favoriteGroupRepository,
            UserFoodPreferenceRepository foodPreferenceRepository,
            RefreshTokenRepository refreshTokenRepository, SocialAccountRepository socialAccountRepository,
            PhoneVerificationRepository phoneVerificationRepository,
            ConversationService conversationService, FileStorage fileStorage, Clock clock) {
        this.userRepository = userRepository;
        this.checkInRepository = checkInRepository;
        this.mealRequestRepository = mealRequestRepository;
        this.mateRequestRepository = mateRequestRepository;
        this.mateRepository = mateRepository;
        this.blockRepository = blockRepository;
        this.notificationRepository = notificationRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.favoriteGroupRepository = favoriteGroupRepository;
        this.foodPreferenceRepository = foodPreferenceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.conversationService = conversationService;
        this.fileStorage = fileStorage;
        this.clock = clock;
    }

    /**
     * 기능: 회원 탈퇴 — 진행중 종료 → 개인정보 삭제 → 익명화를 한 트랜잭션에서 수행
     * Request: userId — 탈퇴하는 사용자 ID
     * Response: 없음(void)
     *
     * @throws BusinessException 회원이 없을 때({@code USER_NOT_FOUND})
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), KST);

        endOngoing(userId, now);
        deletePersonalData(userId, user.getProfileImageUrl(), user.getPhone());
        user.withdraw();
    }

    /** 진행 중인 체크인·신청·대화를 정리한다. 대상이 없으면 아무 일도 하지 않는다(멱등). */
    private void endOngoing(Long userId, LocalDateTime now) {
        // ① 진행 중인 체크인 종료. TOGETHER면 파트너도 함께 끝내고 대화를 닫는다
        //    — 한쪽만 끝내면 상대가 "같이 먹는 중"에 갇힌다(CheckInService.endCheckIn과 같은 규칙).
        //    SEEKING은 end()가 (ACTIVE/TOGETHER 전용 가드 때문에) 조용히 무시하므로 cancel()로 보낸다 —
        //    end()로 두면 SEEKING인 채로 남아 익명화된 계정이 모집중 목록·지도 집계에 계속 잡힌다.
        checkInRepository.findByUser_IdAndStatusIn(userId,
                        List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE, CheckInStatus.TOGETHER))
                .ifPresent(mine -> {
                    if (mine.getStatus() == CheckInStatus.TOGETHER && mine.getMealRequestId() != null) {
                        checkInRepository.findTogetherByMealRequestId(mine.getMealRequestId())
                                .forEach(c -> c.end(now));
                        conversationService.close(mine.getMealRequestId());
                    } else if (mine.getStatus() == CheckInStatus.SEEKING) {
                        mine.cancel(now);
                    } else {
                        mine.end(now);
                    }
                });
        // ② PENDING 같이먹기 신청 종결 — meal_requests는 이력으로 보존하므로 상태를 남긴다.
        //    상대가 직접 거절한 게 아니라 요청자가 사라진 것이므로 DECLINED가 아니라 EXPIRED다.
        //    메이트 신청은 여기서 손대지 않는다 — deletePersonalData가 행 자체를 지운다.
        mealRequestRepository.expireAllPendingOf(userId, now);
    }

    /**
     * 개인정보성 데이터를 지운다.
     *
     * <p>소프트 탈퇴라 {@code ON DELETE CASCADE}가 동작하지 않으므로(users 행을 지우지 않는다)
     * 전부 명시적으로 삭제한다. mates·mate_requests·blocks는 <b>양방향</b>이다 — 내 쪽만 지우면
     * 상대가 나를 향해 만든 행이 남아 관계가 반쯤 살아 있게 된다.
     *
     * <p>{@code phone_verifications}는 {@code users} FK가 없어(번호만으로 기록) 이 정리 대상에서
     * 빠지기 쉽다 — 원문 휴대폰 번호가 남는 테이블이라 phone 값 기준으로 별도 삭제한다.
     */
    private void deletePersonalData(Long userId, String profileImageUrl, String phone) {
        socialAccountRepository.deleteAllByUserId(userId);   // 재가입에 필수
        refreshTokenRepository.deleteAllByUserId(userId);    // 세션 즉시 무효화
        foodPreferenceRepository.deleteAllByUserId(userId);
        favoriteGroupRepository.deleteAllByUserId(userId);   // favorites는 DB FK CASCADE로 함께 삭제
        mateRepository.deleteAllInvolvingUser(userId);
        mateRequestRepository.deleteAllInvolvingUser(userId);
        blockRepository.deleteAllInvolvingUser(userId);
        notificationRepository.deleteAllByUserId(userId);    // 내가 받은 것만. 내가 일으킨 알림(actor)은 상대 알림함에 남는다
        notificationSettingsRepository.deleteAllByUserId(userId);
        userBadgeRepository.deleteAllByUserId(userId);
        if (phone != null) {
            // users FK가 없어 하드 삭제 스윕에 걸리지 않던 테이블 — 소셜 온리 계정은 phone이 애초에 null.
            phoneVerificationRepository.deleteAllByPhone(phone);
        }
        if (profileImageUrl != null) {
            // 커밋 전에 파일부터 지운다 — 이후 flush 실패로 트랜잭션이 롤백되면 DB는 되돌아가도 파일은
            // 이미 사라진 상태가 된다. after-commit 훅이 더 정확하지만, 이 한 흐름을 위해 훅 인프라를
            // 새로 두는 비용은 아니라고 보고 의도적으로 감수한다(오밤중 실수 아님).
            fileStorage.delete(profileImageUrl);
        }
    }
}
