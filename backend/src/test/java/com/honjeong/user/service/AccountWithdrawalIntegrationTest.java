package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.auth.domain.PhoneVerification;
import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.RefreshToken;
import com.honjeong.auth.domain.SocialAccount;
import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.auth.repository.RefreshTokenRepository;
import com.honjeong.auth.repository.SocialAccountRepository;
import com.honjeong.auth.service.AuthResult;
import com.honjeong.auth.service.AuthService;
import com.honjeong.auth.service.CompleteProfileCommand;
import com.honjeong.badge.domain.UserBadge;
import com.honjeong.badge.repository.UserBadgeRepository;
import com.honjeong.block.domain.Block;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.domain.ConversationStatus;
import com.honjeong.chat.repository.ConversationRepository;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.favorite.domain.Favorite;
import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.meal.service.MealRequestService;
import com.honjeong.notification.domain.Notification;
import com.honjeong.notification.domain.NotificationSettings;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.repository.NotificationRepository;
import com.honjeong.notification.repository.NotificationSettingsRepository;
import com.honjeong.place.domain.Place;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserFoodPreference;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserFoodPreferenceRepository;
import com.honjeong.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * 탈퇴({@link AccountWithdrawalService#withdraw(Long)})의 핵심 계약을 실 Postgres로 검증한다.
 *
 * <p><b>배경.</b> Task 2가 추가한 벌크 JPQL DELETE 11건은 {@code AccountWithdrawalServiceTest}에서 모든
 * 리포지토리를 Mockito로 목 잡고 검증했다 — 즉 부팅 시 쿼리 파싱만 됐을 뿐, FK 순서·{@code favorites} CASCADE·
 * 전 컬럼이 null이 된 {@code users} 행이 실제로 flush되는지는 단 한 번도 실 DB에서 실행된 적이 없다. 이 클래스는
 * 그 공백을 메운다 — {@code verify(repo).deleteAllByUserId(id)}가 아니라 테이블을 다시 읽어 행 수·컬럼값을 직접
 * 확인한다({@code assert on rows, not on interactions}).
 *
 * <p><b>이름이 비슷한 {@link AccountWithdrawalPersistenceRegressionTest}와의 관계.</b> 그 파일은
 * {@code clearAutomatically}가 {@code User}를 detach시켜 탈퇴가 무효화됐던 <b>단 한 건의 회귀</b>만 지키는
 * 전용 가드다. 이 파일은 그것과 별개로, 탈퇴 계약 전반(재가입·11개 벌크 삭제·양방향 만료·TOGETHER 정리)을 더 넓게
 * 검증한다 — 일부 시나리오(SEEKING→CANCELLED)가 겹치는 것은 의도된 중복이다.
 *
 * <p><b>{@code @Transactional} 사용 여부를 테스트군별로 다르게 가져간다(이 파일에서 가장 중요한 설계 결정):</b>
 * <ul>
 *   <li><b>재가입·정지 계정 4건</b>(스펙 스켈레톤)은 클래스에 {@code @Transactional}을 붙이지 않는다. 이 시나리오의
 *       핵심은 {@code findByPhone}·{@code findByProviderAndProviderUserId}가 탈퇴 후 실제로 <b>미스</b>가 나는지인데,
 *       한 트랜잭션 안에서 롤백을 전제하면 UNIQUE 제약·가시성이 실제 커밋 시점과 달라 이 계약을 놓칠 수 있다. 대신
 *       테스트마다 고유한 휴대폰 번호·소셜 sub를 써서 공유 Testcontainers Postgres를 오염 없이 커밋으로 검증한다.</li>
 *   <li><b>SEEKING 정리·11개 벌크 삭제·양방향 만료·TOGETHER 정리</b>는 메서드에 {@code @Transactional}을 붙이고
 *       {@code AccountWithdrawalPersistenceRegressionTest}·{@code ChatLifecycleTest}와 같은 관례로
 *       {@link EntityManager#flush()} + {@link EntityManager#clear()}를 명시적으로 호출한다. FK 제약은 Postgres에서
 *       {@code DEFERRABLE}이 아니므로(마이그레이션에 그런 선언이 없다) 문장 실행 시점에 즉시 검사된다 — 즉
 *       {@code flush()}만으로 벌크 DELETE 11건과 순서 위반 여부가 실제로 실행·검증되고, {@code clear()}로 1차
 *       캐시를 비워야 이후 조회가 반드시 DB 행을 다시 읽는다(안 그러면 메모리 위 엔티티를 돌려받아 버그가 있어도
 *       초록불이 뜬다 — 이 프로젝트가 실제로 겪은 함정). 롤백으로 끝나므로 여러 테이블에 걸친 무거운 픽스처를
 *       만들어도 공유 컨테이너를 오염시키지 않는다.</li>
 * </ul>
 */
@SpringBootTest
class AccountWithdrawalIntegrationTest extends AbstractPostgresTest {

    @Autowired private AccountWithdrawalService withdrawalService;
    @Autowired private AuthService authService;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;

    @Autowired private CheckInService checkInService;
    @Autowired private CheckInRepository checkInRepository;
    @Autowired private MealRequestService mealRequestService;
    @Autowired private MealRequestRepository mealRequestRepository;
    @Autowired private PlaceRepository placeRepository;
    @Autowired private ConversationRepository conversationRepository;

    @Autowired private SocialAccountRepository socialAccountRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserFoodPreferenceRepository userFoodPreferenceRepository;
    @Autowired private FavoriteGroupRepository favoriteGroupRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private MateRepository mateRepository;
    @Autowired private MateRequestRepository mateRequestRepository;
    @Autowired private BlockRepository blockRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationSettingsRepository notificationSettingsRepository;
    @Autowired private UserBadgeRepository userBadgeRepository;
    @Autowired private PhoneVerificationRepository phoneVerificationRepository;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    // ============================================================
    // 재가입·정지 계정 — @Transactional 없음(실 커밋). 브리핑 스펙 스켈레톤 그대로.
    // ============================================================

    @Test
    @DisplayName("탈퇴 후 같은 휴대폰 번호로 다시 가입하면 새 계정이 만들어지고 이전 계정의 기록을 물려받지 않는다")
    void resignUpWithSamePhoneCreatesNewAccount() {
        String phone = freshPhone();
        Long oldId = signUpWithPhone(phone);
        // FIX 5: "새 계정은 이전 기록을 물려받지 않는다"는 주장을 newId != oldId만으로 증명하지 않기 위해,
        // 옛 계정에 뱃지 하나를 심어 두고 탈퇴 후 새 계정에 그 흔적이 없는지까지 직접 확인한다.
        userBadgeRepository.save(UserBadge.of(oldId, "FIRST_CHECKIN", LocalDateTime.now()));

        withdrawalService.withdraw(oldId);

        User old = userRepository.findById(oldId).orElseThrow();
        assertThat(old.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(old.getPhone()).isNull();

        Long newId = signUpWithPhone(phone);
        assertThat(newId).isNotEqualTo(oldId);
        assertThat(userBadgeRepository.findByUserId(newId)).isEmpty();
    }

    @Test
    @DisplayName("탈퇴하면 소셜 연동이 끊겨 같은 카카오 계정도 새 회원으로 가입된다")
    void resignUpWithSameSocialCreatesNewAccount() {
        // MockOAuthVerifier는 "mock-kakao-{idToken}"을 providerUserId로 만든다(결정론적).
        // 같은 idToken을 두 번 넘기면 같은 소셜 신원이 되므로 재가입 경로를 그대로 재현할 수 있다.
        // (honjeong.oauth.mode 기본값 mock — test 프로파일). idToken 자체는 freshKakaoSub()로 실행마다
        // 달라지게 한다(FIX 4) — 같은 값을 쓰면 이 테스트가 커밋한 소셜 계정이 다음 실행과 충돌한다.
        String sub = freshKakaoSub();
        AuthResult first = authService.oauthLogin(Provider.KAKAO, sub);
        Long oldId = completeOnboarding(first);

        withdrawalService.withdraw(oldId);

        AuthResult second = authService.oauthLogin(Provider.KAKAO, sub);
        Long newId = completeOnboarding(second);
        assertThat(newId).isNotEqualTo(oldId);
    }

    @Test
    @DisplayName("탈퇴한 계정은 같은 신원으로 로그인해도 이전 기록을 되찾을 수 없다")
    void withdrawnAccountCannotBeResurrected() {
        String phone = freshPhone();
        Long oldId = signUpWithPhone(phone);
        withdrawalService.withdraw(oldId);

        // 익명화로 phone이 사라졌으니 findByPhone은 미스가 나고 새 회원이 생긴다.
        Long newId = signUpWithPhone(phone);

        assertThat(userRepository.findById(newId).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userRepository.findById(oldId).orElseThrow().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("정지된 계정은 재로그인해도 온보딩 토큰을 받지 못한다")
    void suspendedAccountCannotReenter() {
        String phone = freshPhone();
        Long id = signUpWithPhone(phone);
        User u = userRepository.findById(id).orElseThrow();
        suspend(u);

        assertThatThrownBy(() -> requestAndVerifyCode(phone))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    // ============================================================
    // SEEKING 정리 + 11개 벌크 삭제 + favorites CASCADE + users 전 컬럼 익명화
    // — @Transactional + flush/clear (이유는 클래스 Javadoc 참고)
    // ============================================================

    @Test
    @Transactional
    @DisplayName("탈퇴 시 SEEKING 체크인은 CANCELLED로 정리되고, 개인정보성 11개 테이블이 전부 0행이 되며,"
            + " users 행 전 컬럼이 익명화된 채로 flush된다")
    void withdraw_clearsAllPersonalDataAndAnonymizesUser() {
        LocalDateTime now = LocalDateTime.now();

        User user = createActiveUser("01090000001", "탈퇴풀커버리지");
        Long userId = user.getId();
        // withdraw()가 phone을 null로 바꾸기 전에 반드시 캡처해야 한다 — 이후 phone_verifications 삭제 확인에 쓴다.
        String capturedPhone = user.getPhone();

        User other = createActiveUser("01090000002", "탈퇴상대방");

        Place place = createPlace("WD-FULL-001", "탈퇴테스트식당");
        CheckInResponse seeking = checkInService.createCheckIn(userId, new CheckInRequest(place.getId()));
        assertThat(seeking.status()).isEqualTo("SEEKING");
        Long checkInId = seeking.checkInId();

        // 11개 벌크 삭제 대상 테이블에 각각 최소 1행씩 심는다.
        socialAccountRepository.save(SocialAccount.of(userId, Provider.KAKAO, "kakao-full-cov-" + userId, null));
        refreshTokenRepository.save(RefreshToken.issue(userId, "hash-full-cov-" + userId, now.plusDays(30)));
        userFoodPreferenceRepository.save(UserFoodPreference.of(userId, List.of("한식", "분식")));
        FavoriteGroup group = favoriteGroupRepository.save(
                FavoriteGroup.create(user, "테스트그룹", null, "#FF5A1F", false));
        favoriteRepository.save(Favorite.of(group, place)); // favorite_groups 삭제 시 CASCADE로 함께 지워져야 함
        Long groupId = group.getId();
        mateRepository.save(Mate.create(user, other, now));   // 메이트는 양방향 2행
        mateRepository.save(Mate.create(other, user, now));
        // mate_requests·blocks도 양방향으로 심는다(FIX 2) — 한쪽만 심으면 :271-274의 OR 단언이
        // 한 방향에서는 항상 0행("존재하지 않아서 통과")이 되어, 리포지토리 쿼리가 한쪽 방향만
        // 지워도(관계가 반쯤 살아남는 버그) 초록불이 뜬다.
        mateRequestRepository.save(MateRequest.create(other, user, now));
        mateRequestRepository.save(MateRequest.create(user, other, now));
        blockRepository.save(Block.create(user, other, now));
        blockRepository.save(Block.create(other, user, now));
        notificationRepository.save(Notification.create(user, other, NotificationType.MATE_REQUEST_RECEIVED, now));
        notificationSettingsRepository.save(NotificationSettings.of(userId));
        userBadgeRepository.save(UserBadge.of(userId, "FIRST_CHECKIN", now));
        // phone_verifications는 users FK가 없어(번호 단위 기록) 별도로 심는다.
        phoneVerificationRepository.save(PhoneVerification.issue(capturedPhone, "000000", now.plusMinutes(3)));

        // given: 11개 벌크 삭제 대상과 favorites가 실제로 DB에 들어갔는지 사전에 확인한다.
        // 사후 0건 단언이 무의미해지지 않도록, 픽스처 행이 실제로 들어갔는지 먼저 확인한다.
        assertThat(countRows("SELECT COUNT(*) FROM social_accounts WHERE user_id = ?", userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM user_food_preferences WHERE user_id = ?", userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM favorite_groups WHERE user_id = ?", userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM favorites WHERE group_id = ?", groupId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM mates WHERE user_id = ? OR mate_user_id = ?", userId, userId))
                .isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM mate_requests WHERE from_user_id = ? OR to_user_id = ?",
                userId, userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM blocks WHERE blocker_id = ? OR blocked_id = ?", userId, userId))
                .isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM notifications WHERE user_id = ?", userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM notification_settings WHERE user_id = ?", userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM user_badges WHERE user_id = ?", userId)).isNotZero();
        assertThat(countRows("SELECT COUNT(*) FROM phone_verifications WHERE phone = ?", capturedPhone)).isNotZero();

        // when
        withdrawalService.withdraw(userId);
        // 벌크 DELETE 11건 + user.withdraw()가 실제로 SQL로 나갔는지 강제로 흘려보내고, 1차 캐시를 비워
        // 이후 조회가 반드시 DB 행을 다시 읽게 한다(그렇지 않으면 메모리 위 엔티티로 버그를 가려버린다).
        entityManager.flush();
        entityManager.clear();

        // then: users 행 전 컬럼 익명화(실 DB 재조회 기준).
        User reloaded = userRepository.findById(userId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(reloaded.getPhone()).isNull();
        assertThat(reloaded.getNickname()).isNull();
        assertThat(reloaded.getEmail()).isNull();
        assertThat(reloaded.getProfileImageUrl()).isNull();
        assertThat(reloaded.getGender()).isNull();
        assertThat(reloaded.getBirthDate()).isNull();
        assertThat(reloaded.getIntroduction()).isNull();
        assertThat(reloaded.getRegion()).isNull();
        assertThat(reloaded.getRegionLat()).isNull();
        assertThat(reloaded.getRegionLng()).isNull();
        assertThat(reloaded.getDiningStyle()).isNull();
        assertThat(reloaded.isAllowMealRequest()).isFalse();

        // then: SEEKING이던 체크인이 CANCELLED로 정리됐다(SEEKING으로 남아있으면 안 됨 — end()의 SEEKING no-op 회귀).
        CheckIn reloadedCheckIn = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(reloadedCheckIn.getStatus()).isEqualTo(CheckInStatus.CANCELLED);

        // then: 11개 벌크 삭제 대상 테이블이 실제로 0행이다(verify가 아니라 count로 확인).
        assertThat(countRows("SELECT COUNT(*) FROM social_accounts WHERE user_id = ?", userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM user_food_preferences WHERE user_id = ?", userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM favorite_groups WHERE user_id = ?", userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM mates WHERE user_id = ? OR mate_user_id = ?", userId, userId))
                .isZero();
        assertThat(countRows("SELECT COUNT(*) FROM mate_requests WHERE from_user_id = ? OR to_user_id = ?",
                userId, userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM blocks WHERE blocker_id = ? OR blocked_id = ?", userId, userId))
                .isZero();
        assertThat(countRows("SELECT COUNT(*) FROM notifications WHERE user_id = ?", userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM notification_settings WHERE user_id = ?", userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM user_badges WHERE user_id = ?", userId)).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM phone_verifications WHERE phone = ?", capturedPhone)).isZero();

        // then: favorite_groups 삭제가 favorites를 DB FK ON DELETE CASCADE로 함께 지웠는지(별도 애플리케이션
        // 코드 없이 DB가 지웠어야 한다 — favoriteRepository에 deleteByGroup 호출이 없다).
        assertThat(countRows("SELECT COUNT(*) FROM favorites WHERE group_id = ?", groupId)).isZero();
    }

    // ============================================================
    // expireAllPendingOf 양방향(내가 발신자 / 내 체크인이 대상) — @Transactional + flush/clear
    // ============================================================

    @Test
    @Transactional
    @DisplayName("탈퇴 시 내가 발신자인 PENDING과 내 체크인이 대상(to_check_in)인 PENDING이 모두 EXPIRED로 종결된다(양방향)")
    void withdraw_expiresPendingMealRequests_inBothDirections() {
        User withdrawing = createActiveUser("01091000001", "탈퇴양방향본인");
        User target = createActiveUser("01091000002", "탈퇴양방향상대");     // 탈퇴자가 신청을 보낸 대상
        User requester = createActiveUser("01091000003", "탈퇴양방향신청자"); // 탈퇴자에게 신청을 보낸 사람

        Place place = createPlace("WD-EXP-001", "만료테스트식당");

        // 탈퇴자 본인의 SEEKING 체크인 — requester가 이 체크인을 대상으로 신청을 보낸다(내 체크인이 to_check_in인 PENDING).
        CheckInResponse myCheckIn = checkInService.createCheckIn(withdrawing.getId(), new CheckInRequest(place.getId()));
        // 신청 대상(target)의 SEEKING 체크인 — 탈퇴자가 이 체크인으로 신청을 보낸다(내가 발신자인 PENDING).
        CheckInResponse targetCheckIn = checkInService.createCheckIn(target.getId(), new CheckInRequest(place.getId()));

        MealRequestResponse outgoing = mealRequestService.create(withdrawing.getId(),
                new MealRequestCreateRequest(targetCheckIn.checkInId(), "저요"));
        MealRequestResponse incoming = mealRequestService.create(requester.getId(),
                new MealRequestCreateRequest(myCheckIn.checkInId(), "같이 드실래요"));

        // when
        withdrawalService.withdraw(withdrawing.getId());
        entityManager.flush();
        entityManager.clear();

        // then: 두 방향 모두 EXPIRED(수신자가 직접 거절한 게 아니므로 DECLINED가 아니다).
        MealRequest reloadedOutgoing = mealRequestRepository.findById(outgoing.mealRequestId()).orElseThrow();
        assertThat(reloadedOutgoing.getStatus()).isEqualTo(MealRequestStatus.EXPIRED);

        MealRequest reloadedIncoming = mealRequestRepository.findById(incoming.mealRequestId()).orElseThrow();
        assertThat(reloadedIncoming.getStatus()).isEqualTo(MealRequestStatus.EXPIRED);
    }

    // ============================================================
    // TOGETHER 경로 — 상대가 탈퇴하면 양쪽 체크인 ENDED + 대화 CLOSED. @Transactional + flush/clear
    // ============================================================

    @Test
    @Transactional
    @DisplayName("같이 먹는 중(TOGETHER)에 한쪽이 탈퇴하면 양쪽 체크인이 모두 ENDED되고 대화가 CLOSED된다")
    void withdraw_duringTogetherMatch_endsBothCheckInsAndClosesConversation() {
        User sender = createActiveUser("01092000001", "탈퇴투게더발신");
        User receiver = createActiveUser("01092000002", "탈퇴투게더수신");
        Place place = createPlace("WD-TG-001", "투게더테스트식당");

        CheckInResponse receiverCheckIn = checkInService.createCheckIn(receiver.getId(), new CheckInRequest(place.getId()));
        MealRequestResponse created = mealRequestService.create(sender.getId(),
                new MealRequestCreateRequest(receiverCheckIn.checkInId(), "같이 드실래요?"));
        Long mrId = created.mealRequestId();
        mealRequestService.accept(receiver.getId(), mrId);

        // 발신자의 새 TOGETHER 체크인 id 확보(accept 응답 본문엔 체크인 id가 없다 — ChatLifecycleTest와 같은 패턴).
        Long senderCheckInId = jdbcTemplate.queryForObject(
                "SELECT id FROM check_ins WHERE user_id = ? AND status = 'TOGETHER'", Long.class, sender.getId());

        assertThat(conversationRepository.findByMealRequestId(mrId).orElseThrow().getStatus())
                .isEqualTo(ConversationStatus.ACTIVE);

        // when: 매칭 상대(receiver)가 탈퇴하면
        withdrawalService.withdraw(receiver.getId());
        entityManager.flush();
        entityManager.clear();

        // then: 탈퇴하지 않은 발신자의 체크인도 함께 ENDED여야 한다(한쪽만 끝내면 상대가 "같이 먹는 중"에 갇힌다).
        CheckIn reloadedReceiverCheckIn = checkInRepository.findById(receiverCheckIn.checkInId()).orElseThrow();
        assertThat(reloadedReceiverCheckIn.getStatus()).isEqualTo(CheckInStatus.ENDED);

        CheckIn reloadedSenderCheckIn = checkInRepository.findById(senderCheckInId).orElseThrow();
        assertThat(reloadedSenderCheckIn.getStatus()).isEqualTo(CheckInStatus.ENDED);

        // then: 대화도 CLOSED로 정리된다.
        assertThat(conversationRepository.findByMealRequestId(mrId).orElseThrow().getStatus())
                .isEqualTo(ConversationStatus.CLOSED);
    }

    // --- 헬퍼 ---

    /**
     * 휴대폰 온보딩을 끝까지 진행하고 신규 ACTIVE 회원의 userId를 돌려준다.
     * sendPhoneCode → verifyPhone(코드 000000, 신규라 온보딩 분기) → agreeTerms(필수 4종) → complete(프로필).
     */
    private Long signUpWithPhone(String phone) {
        authService.sendPhoneCode(phone);
        AuthResult result = authService.verifyPhone(phone, "000000");
        assertThat(result.onboarding())
                .withFailMessage("신규 번호는 온보딩 분기를 타야 한다").isTrue();
        return completeOnboarding(result);
    }

    /** 온보딩 결과(온보딩 토큰)에서 userId를 꺼내 약관 동의·프로필 완료까지 마치고 userId를 돌려준다. */
    private Long completeOnboarding(AuthResult result) {
        Long userId = extractUserId(result.onboardingToken());
        authService.agreeTerms(userId, true, true, true, true, false);
        authService.complete(userId, new CompleteProfileCommand(
                "u" + userId, null, null, null, null, null, null, null, null, null));
        return userId;
    }

    /** 온보딩 토큰의 sub(userId) 클레임을 꺼낸다. */
    private Long extractUserId(String onboardingToken) {
        return Long.parseLong(jwtProvider.decode(onboardingToken).getSubject());
    }

    /** 회원을 SUSPENDED로 강제 전이하고 즉시 flush한다(관리자 제재를 흉내). */
    private void suspend(User user) {
        ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
        userRepository.saveAndFlush(user);
    }

    /** 휴대폰 인증번호 발송·검증만 수행한다(온보딩 진행 없이 분기 자체만 확인할 때 사용). */
    private void requestAndVerifyCode(String phone) {
        authService.sendPhoneCode(phone);
        authService.verifyPhone(phone, "000000");
    }

    /**
     * ACTIVE 상태 회원을 바로 저장한다(ChatLifecycleTest와 같은 최소 프로필 패턴 — 온보딩 절차는 생략).
     *
     * <p><b>FIX 1.</b> {@link User#withdraw()}가 비우는 필드(email·gender·birthDate·introduction·
     * region·regionLat·regionLng·diningStyle·profileImageUrl) 전부를 서로 구분되는 값으로 채운다.
     * 예전에는 전부 null로 남겨 뒀는데, 그러면 탈퇴 후 "null인지" 확인하는 단언이 애초에 채워진 적 없는
     * 값이 계속 null인 것만 확인하는 셈이라 {@code withdraw()}에서 해당 필드를 지우는 줄을 통째로 지워도
     * 테스트가 계속 초록불이었다(익명화가 실제로는 안 됐는데 안 걸림).
     */
    private User createActiveUser(String phone, String nickname) {
        User user = User.pending(phone, nickname + "@test.honjeong.com");
        user.completeProfile(nickname, Gender.FEMALE, LocalDate.of(1996, 3, 14), nickname + "입니다",
                "서울 강남구", 37.4979, 127.0276, DiningStyle.QUIET,
                "https://cdn.example.com/profile/" + nickname + ".png");
        return userRepository.save(user);
    }

    // FIX 4: 아래 두 헬퍼는 매 "호출 시점"에 값을 만든다(클래스 로딩 시 한 번 고정되는 static final이 아니다).
    // 이 파일의 재가입 4테스트는 @Transactional이 없어 실제로 커밋되므로, phone은 UNIQUE·(provider,
    // provider_user_id)는 UNIQUE 제약을 진짜로 건드린다 — 같은 JVM에서 클래스가 두 번 실행돼도(IDE
    // 재실행 등) 이전 실행이 남긴 값과 부딪히지 않도록, 실행 시각(밀리초)과 호출 순번을 함께 조합한다.
    private static final AtomicLong FIXTURE_SEQ = new AtomicLong();

    /** 실행마다 값이 달라지는(따라서 재실행에도 안전한) 휴대폰 번호를 만든다.
     *  0102xxxxx 범위(0107·0109 회피)를 사용해 CheckInMealHappyPathE2eTest(0107777x)·
     *  AuthServicePhoneAttemptIntegrationTest(0109999x)와의 JVM 내 충돌을 방지한다. */
    private static String freshPhone() {
        long ms = System.currentTimeMillis() % 10_000L;  // 0-9999
        long seq = FIXTURE_SEQ.incrementAndGet() % 1000L;  // 0-999
        return String.format("0102%04d%03d", ms, seq);
    }

    /** 실행마다 값이 달라지는(따라서 재실행에도 안전한) 카카오 idToken(=재가입 시 사용할 소셜 신원 시드)을 만든다. */
    private static String freshKakaoSub() {
        return "withdraw-test-sub-" + System.currentTimeMillis() + "-" + FIXTURE_SEQ.incrementAndGet();
    }

    /** 공공데이터 마스터 기반 최소 장소를 저장한다. */
    private Place createPlace(String sourceId, String name) {
        return placeRepository.save(Place.ofPublicData(
                sourceId, name, "한식", "서울 어딘가", "서울 도로명",
                37.5665, 126.9780, "02-000-0000", "영업"));
    }

    /** 원시 SQL로 행 수를 센다(리포지토리 verify가 아니라 실제 테이블 상태 확인용). */
    private int countRows(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
