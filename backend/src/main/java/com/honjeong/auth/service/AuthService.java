package com.honjeong.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.auth.client.AppleTokenClient;
import com.honjeong.auth.domain.PhoneVerification;
import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.SocialAccount;
import com.honjeong.auth.domain.TermsAgreement;
import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.auth.repository.SocialAccountRepository;
import com.honjeong.auth.repository.TermsAgreementRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.favorite.service.FavoriteGroupService;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserRepository;
import com.honjeong.user.service.UserFoodPreferenceService;

/**
 * 인증 흐름 전체를 지휘(오케스트레이션)하는 핵심 서비스. 휴대폰 인증·소셜(OAuth) 로그인이라는
 * 두 진입 경로와, 그 뒤에 이어지는 온보딩(약관 동의 → 프로필 완성), 토큰 재발급·로그아웃까지를 담당한다.
 *
 * <p>사용 Controller: AuthController.
 *
 * <p><b>회원 상태와 토큰 종류:</b> 사용자는 가입 도중엔 PENDING(가입 진행 중), 프로필까지 끝내면
 * ACTIVE(정상 회원) 상태가 된다. 진입 시점에 이미 ACTIVE인 회원은 곧바로 <b>정식 토큰</b>(access+refresh)을
 * 받아 로그인되고, 신규이거나 아직 ACTIVE가 아닌 회원은 <b>온보딩 토큰</b>(임시 자격)만 받아 온보딩
 * 단계로 넘어간다. 온보딩 토큰으로 약관 동의·프로필 완성을 마쳐야 비로소 정식 토큰이 나온다.
 *
 * <p><b>온보딩 전체 흐름:</b>
 * (휴대폰) sendPhoneCode → verifyPhone, 또는 (소셜) oauthLogin → [온보딩 토큰] → agreeTerms →
 * complete → [정식 토큰]. 그 후 access 만료 시 refresh로 재발급, 로그아웃 시 logout.
 *
 * <p><b>P1 단순화:</b> 휴대폰 가입 계정과 소셜 가입 계정은 서로 <b>별개의 계정</b>으로 다룬다. 같은
 * 사람이 두 방식으로 가입하면 두 계정이 따로 생긴다(두 방식을 한 계정으로 연동·병합하는 기능은 추후 과제).
 */
@Service
public class AuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(3); // 휴대폰 인증번호 유효기간: 3분
    private static final int MAX_ATTEMPTS = 5;                      // 인증번호 검증 최대 시도 횟수(무차별 대입 방지)
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;                             // 회원 조회·저장
    private final SocialAccountRepository socialAccountRepository;           // 소셜 계정(공급자별 식별자) 매핑
    private final PhoneVerificationRepository phoneVerificationRepository;   // 휴대폰 인증번호 발송 이력
    private final PhoneAttemptRecorder phoneAttemptRecorder;                 // 시도 카운트를 독립 트랜잭션으로 누적
    private final TermsAgreementRepository termsAgreementRepository;         // 약관 동의 기록
    private final OAuthVerifier oAuthVerifier;                               // 소셜 토큰 검증(공급자 신원 확인)
    private final SmsSender smsSender;                                       // 인증번호 SMS 발송
    private final VerificationCodeGenerator codeGenerator;                   // 인증번호 생성기
    private final TokenService tokenService;                                 // 정식 토큰(access+refresh) 발급
    private final JwtProvider jwtProvider;                                   // 온보딩 토큰 발급
    private final Clock clock;                                               // 현재 시각 공급자(테스트 시 고정 주입)
    private final UserFoodPreferenceService foodPreferenceService;           // 선호 음식 upsert(가입 시 저장)
    private final FavoriteGroupService favoriteGroupService;                 // 기본 즐겨찾기 그룹 생성
    private final AppleTokenClient appleTokenClient;                         // 애플 refresh token 교환(탈퇴 시 폐기용)

    /**
     * 인증에 필요한 저장소·외부 연동·토큰 발급기를 모두 주입받아 서비스를 구성한다.
     * (생성자 주입 + final로 불변 의존성을 보장한다.)
     */
    public AuthService(UserRepository userRepository, SocialAccountRepository socialAccountRepository,
            PhoneVerificationRepository phoneVerificationRepository, PhoneAttemptRecorder phoneAttemptRecorder,
            TermsAgreementRepository termsAgreementRepository,
            OAuthVerifier oAuthVerifier, SmsSender smsSender, VerificationCodeGenerator codeGenerator,
            TokenService tokenService, JwtProvider jwtProvider, Clock clock,
            UserFoodPreferenceService foodPreferenceService, FavoriteGroupService favoriteGroupService,
            AppleTokenClient appleTokenClient) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.phoneAttemptRecorder = phoneAttemptRecorder;
        this.termsAgreementRepository = termsAgreementRepository;
        this.oAuthVerifier = oAuthVerifier;
        this.smsSender = smsSender;
        this.codeGenerator = codeGenerator;
        this.tokenService = tokenService;
        this.jwtProvider = jwtProvider;
        this.clock = clock;
        this.foodPreferenceService = foodPreferenceService;
        this.favoriteGroupService = favoriteGroupService;
        this.appleTokenClient = appleTokenClient;
    }

    /**
     * 휴대폰 인증번호를 발급해 SMS로 보낸다. 휴대폰 가입·로그인의 첫 단계다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>인증번호를 생성한다(mock 환경은 항상 "000000", real은 랜덤 6자리).</li>
     *   <li>만료 시각을 (현재 + 3분)으로 정한다.</li>
     *   <li>해당 번호·코드·만료를 한 건의 발송 이력으로 DB에 저장한다(검증 때 최신 1건을 조회).</li>
     *   <li>SMS 발송기로 번호와 코드를 보낸다(mock 환경은 실제 발송 대신 로그만 남김).</li>
     * </ol>
     *
     * @param phone 인증번호를 받을 휴대폰 번호
     */
    @Transactional // 인증 이력 저장(쓰기)을 트랜잭션으로 묶는다
    public void sendPhoneCode(String phone) {
        String code = codeGenerator.generate();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plus(CODE_TTL); // 현재 + 3분
        phoneVerificationRepository.save(PhoneVerification.issue(phone, code, expiresAt));
        smsSender.send(phone, code);
    }

    /**
     * 휴대폰 인증번호를 검증하고, 그 결과로 로그인 또는 온보딩으로 분기한다. 휴대폰 가입·로그인의
     * 두 번째 단계로, 이 서비스의 핵심 분기 로직이다.
     *
     * <p>검증 단계(하나라도 실패하면 즉시 예외):
     * <ol>
     *   <li>해당 번호의 <b>가장 최근</b> 발송 이력을 조회한다. 없으면 코드 불일치로 본다({@code PHONE_CODE_MISMATCH}).</li>
     *   <li>만료(현재 시각 기준)됐으면 {@code PHONE_CODE_EXPIRED}.</li>
     *   <li>시도 횟수가 한도(5회) 이상이면 {@code PHONE_ATTEMPTS_EXCEEDED}(무차별 대입 차단).</li>
     *   <li>시도 횟수를 1 증가시킨 뒤, 입력 코드가 저장된 코드와 다르면 {@code PHONE_CODE_MISMATCH}.</li>
     *   <li>모두 통과하면 인증 완료로 표시(markVerified)한다.</li>
     * </ol>
     *
     * <p>분기(검증 통과 후):
     * <ul>
     *   <li>이 번호로 가입된 회원이 이미 있고 그 회원이 ACTIVE면 → 정식 토큰을 발급해 <b>바로 로그인</b>.</li>
     *   <li>그렇지 않으면(회원이 없거나, 있지만 아직 ACTIVE가 아니면) → 신규일 땐 PENDING 회원을 새로
     *       만들고, 그 회원의 <b>온보딩 토큰</b>을 발급해 온보딩 단계로 보낸다.</li>
     * </ul>
     *
     * @param phone 검증할 휴대폰 번호
     * @param code  사용자가 입력한 인증번호
     * @return 로그인 결과(tokens) 또는 온보딩 결과(onboardingToken)를 담은 {@link AuthResult}
     * @throws BusinessException 인증번호가 없음/만료/시도초과/불일치일 때
     */
    @Transactional // 시도횟수 증가·인증완료 표시·회원 생성 등 여러 쓰기를 한 트랜잭션으로 묶는다
    public AuthResult verifyPhone(String phone, String code) {
        // 같은 번호로 여러 번 받았을 수 있으니 가장 최근 발송분을 기준으로 검증한다.
        PhoneVerification verification = phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHONE_CODE_MISMATCH));

        // 3분 지났으면 만료
        if (verification.isExpired(LocalDateTime.now(clock))) {
            throw new BusinessException(ErrorCode.PHONE_CODE_EXPIRED);
        }
        
        // 5회 이상 시도했으면 차단
        if (verification.getAttempts() >= MAX_ATTEMPTS) {            
            throw new BusinessException(ErrorCode.PHONE_ATTEMPTS_EXCEEDED);
        }

        // 이번 시도를 카운트한다(불일치여도 누적). 아래 불일치 throw로 이 트랜잭션이 롤백돼도 카운트는 남아야
        // rate-limit이 동작하므로, 같은 트랜잭션의 incrementAttempts()가 아니라 REQUIRES_NEW로 독립 커밋한다.
        phoneAttemptRecorder.record(verification.getId());

        // 코드 불일치
        if (!verification.matches(code)) {                          
            throw new BusinessException(ErrorCode.PHONE_CODE_MISMATCH);
        }

        // 검증 성공 표시
        verification.markVerified();                                

        // users 테이블에 phone으로 해당 유저 있는지 판단.
        Optional<User> existing = userRepository.findByPhone(phone);

        // 정지·탈퇴 계정은 온보딩으로 되돌려 보내지 않는다. 되돌려 보내면 complete()가 status를 ACTIVE로
        // 되돌려버려 제재가 무력화된다(부활 경로). "SUSPENDED/WITHDRAWN이면"이 아니라 "PENDING도 ACTIVE도
        // 아니면"으로 표현해, 나중에 상태가 늘어나도(DORMANT 등) 새 상태가 fail-closed로 걸러지게 한다.
        existing.filter(u -> u.getStatus() != UserStatus.PENDING && u.getStatus() != UserStatus.ACTIVE)
                .ifPresent(u -> { throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE); });

        // 이미 정상 회원 → 즉시 로그인
        if (existing.isPresent() && existing.get().isActive()) {
            return AuthResult.login(tokenService.issue(existing.get().getId()));
        }

        // 신규면 PENDING 회원을 새로 생성, 미완(PENDING) 회원이면 그대로 재사용 → 온보딩 토큰 발급
        User user = existing.orElseGet(() -> userRepository.save(User.pending(phone, null)));
        return AuthResult.onboarding(jwtProvider.createOnboardingToken(user.getId()));
    }

    /**
     * 소셜(OAuth) 로그인을 처리한다. 카카오·애플 등 공급자가 발급한 idToken을 검증해 신원을 확인하고,
     * 그 신원에 연결된 회원을 찾아 로그인 또는 온보딩으로 분기한다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>공급자와 idToken으로 토큰을 검증해 공급자 측 신원({@link OAuthIdentity})을 얻는다. 여기서
     *       공급자 고유 식별자(providerUserId)와 이메일(선택)이 나온다.</li>
     *   <li>(공급자, 공급자식별자) 조합으로 이미 연결된 소셜 계정이 있는지 조회한다.</li>
     *   <li><b>있으면(재방문):</b> 연결된 회원을 찾아(없으면 데이터 불일치로 {@code USER_NOT_FOUND}),
     *       ACTIVE면 정식 토큰으로 로그인, 아직 아니면 온보딩 토큰을 준다.</li>
     *   <li><b>없으면(신규):</b> PENDING 회원을 새로 만들고, 그 회원에 소셜 계정을 연결 저장한 뒤
     *       온보딩 토큰을 발급한다.</li>
     * </ol>
     *
     * <p><b>애플 authorizationCode:</b> 애플만 보내는 1회용 코드로, 신규 가입 시 refresh token으로 교환해
     * 소셜 계정에 보관한다(탈퇴 때 애플에 폐기를 요청하려면 이 토큰이 필요하다 — 심사 지침 5.1.1(v)).
     * 교환에 실패해도 가입은 그대로 진행한다.
     *
     * @param provider 소셜 공급자(KAKAO·APPLE 등)
     * @param idToken  공급자가 발급한 ID 토큰(검증 대상)
     * @param authorizationCode 애플이 준 1회용 인가 코드(nullable — 카카오는 보내지 않는다)
     * @return 로그인 결과 또는 온보딩 결과를 담은 {@link AuthResult}
     * @throws BusinessException 소셜 계정은 있는데 연결된 회원을 못 찾을 때({@code USER_NOT_FOUND})
     */
    @Transactional // 회원·소셜계정 생성 등 쓰기를 한 트랜잭션으로 묶는다
    public AuthResult oauthLogin(Provider provider, String idToken, String authorizationCode) {
        OAuthIdentity identity = oAuthVerifier.verify(provider, idToken); // 공급자 토큰 검증 → 신원 추출
        Optional<SocialAccount> account =
                socialAccountRepository.findByProviderAndProviderUserId(provider, identity.providerUserId());
        if (account.isPresent()) { // 이미 가입한 소셜 계정(재방문)
            User user = userRepository.findById(account.get().getUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            // verifyPhone과 같은 이유로 같은 모양("PENDING도 ACTIVE도 아니면")으로 fail-closed 판정한다.
            if (user.getStatus() != UserStatus.PENDING && user.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            // 재방문 계정에는 애플 코드를 교환하지 않는다. 가입 때 교환에 실패해 apple_refresh_token이 비어
            // 있으면 그 회원은 영영 비어 있게 되지만(탈퇴 시 애플 폐기를 건너뛴다), 여기서 뒤늦게 채우려면
            // 지금은 읽기만 하는 로그인 경로에 외부 HTTP 호출과 쓰기가 매번 끼어든다. 감수하는 한계다.
            return user.isActive()
                    ? AuthResult.login(tokenService.issue(user.getId()))                     // 정상 회원 → 로그인
                    : AuthResult.onboarding(jwtProvider.createOnboardingToken(user.getId())); // 미완 → 온보딩 계속
        }
        // 신규 가입 경로. 애플이면 여기서 코드를 교환한다.
        //
        // ★실패하면 null이 돌아온다(AppleTokenClient의 계약 — 어떤 실패든 예외 대신 null). 로그인을
        // 막지 않는다: 애플 장애로 가입이 막히면 안 된다. 이 경우 refresh token 없이 저장되고, 탈퇴 시
        // revoke를 건너뛰게 된다. 예외가 나오지 않으므로 트랜잭션을 롤백시키지도 않는다.
        //
        // ★트랜잭션 안에서 외부 호출을 한다(CLAUDE.md의 "트랜잭션 내 외부 호출 지양"에 대한 의도적 예외).
        // 밖으로 빼려면 자기호출(같은 빈의 메서드 호출은 @Transactional이 조용히 무효가 된다 — 더 나쁜
        // 함정)이나 새 빈이 필요한데, 이 호출은 매 로그인이 아니라 "생애 최초 가입" 한 번뿐이라 그만한
        // 값이 없다고 봤다.
        //
        // ★대신 "모든 쓰기보다 앞"에 둔다. 아래 userRepository.save() 뒤에 두면 커밋되지 않은 users
        // insert를 붙든 채로 최대 15초(연결 5초 + 응답 10초)를 애플 응답에 매달리게 된다. 이 메서드의
        // 다른 외부 호출(oAuthVerifier.verify)도 쓰기보다 앞이다 — 같은 자리로 맞춘 것이다.
        String appleRefreshToken = provider == Provider.APPLE
                ? appleTokenClient.exchangeRefreshToken(authorizationCode)
                : null;
        // PENDING 회원 생성 + 소셜 계정 연결 저장 → 온보딩 토큰
        User user = userRepository.save(User.pending(null, identity.email()));
        // 위 재방문 분기의 조회 결과(account)와 이름이 겹치지 않게 newAccount로 둔다.
        SocialAccount newAccount =
                SocialAccount.of(user.getId(), provider, identity.providerUserId(), identity.email());
        newAccount.attachAppleRefreshToken(appleRefreshToken); // 애플이 아니거나 교환에 실패했으면 null
        socialAccountRepository.save(newAccount);
        return AuthResult.onboarding(jwtProvider.createOnboardingToken(user.getId()));
    }

    /**
     * authorizationCode 없이 호출하는 경로를 위한 오버로드 — 현재 호출자는 카카오 경로의 기존 테스트뿐이다
     * (운영 코드는 컨트롤러가 3인자 쪽을 부른다).
     *
     * <p>★{@code @Transactional}을 지우면 안 된다. "3인자 쪽에 이미 붙어 있으니 중복"처럼 보이지만,
     * 아래 위임은 <b>자기호출</b>이라 프록시를 거치지 않아 3인자의 {@code @Transactional}이 다시 적용되지
     * 않는다. 지우는 순간 이 경로의 회원 저장과 소셜 계정 저장이 서로 다른 트랜잭션으로 쪼개진다
     * (= 3인자 본문이 경고하는 바로 그 함정).
     *
     * @param provider 소셜 공급자
     * @param idToken  공급자가 발급한 ID 토큰
     * @return {@link #oauthLogin(Provider, String, String)}의 결과
     */
    @Transactional
    public AuthResult oauthLogin(Provider provider, String idToken) {
        return oauthLogin(provider, idToken, null);
    }

    /**
     * 약관 동의를 기록한다(온보딩 단계). 필수 4종(만14세·서비스 이용·개인정보·위치)에 모두 동의해야
     * 통과하며, 마케팅은 선택이다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>필수 4종이 모두 true가 아니면 {@code TERMS_REQUIRED} 예외로 거부한다.</li>
     *   <li>이미 동의 기록이 있으면 아무것도 하지 않고 반환한다(<b>멱등</b> — 두 번 호출해도 안전).</li>
     *   <li>그렇지 않으면 동의 항목들과 현재 시각을 한 건으로 저장한다.</li>
     * </ol>
     *
     * @param userId    동의하는 사용자(온보딩 토큰의 주체)
     * @param age       만 14세 이상 확인(필수)
     * @param service   서비스 이용약관 동의(필수)
     * @param privacy   개인정보 처리방침 동의(필수)
     * @param location  위치정보 이용약관 동의(필수)
     * @param marketing 마케팅 수신 동의(선택)
     * @throws BusinessException 필수 약관을 하나라도 동의하지 않았을 때({@code TERMS_REQUIRED})
     */
    @Transactional // 동의 기록 저장(쓰기)을 트랜잭션으로 묶는다
    public void agreeTerms(long userId, boolean age, boolean service, boolean privacy, boolean location, boolean marketing) {
        if (!(age && service && privacy && location)) { // 필수 4종 중 하나라도 미동의면 거부
            throw new BusinessException(ErrorCode.TERMS_REQUIRED);
        }
        if (termsAgreementRepository.existsByUserId(userId)) {
            return; // 이미 동의 — 멱등(재호출해도 중복 저장하지 않음)
        }
        termsAgreementRepository.save(
                TermsAgreement.of(userId, age, service, privacy, location, marketing, LocalDateTime.now(clock)));
    }

    /**
     * 프로필을 확정해 온보딩을 끝내고 정식 토큰을 발급한다. 온보딩의 마지막 단계로, 이 호출이 성공하면
     * 회원은 PENDING에서 ACTIVE로 전환되어 정상 회원이 된다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>userId로 회원을 조회한다(없으면 {@code USER_NOT_FOUND}).</li>
     *   <li>PENDING도 ACTIVE도 아니면(정지·탈퇴 등) {@code ACCOUNT_INACTIVE}로 거부한다.</li>
     *   <li>이미 ACTIVE면(더블탭·네트워크 재시도로 같은 온보딩 토큰이 두 번 들어온 경우) 부활 경로는 아니므로
     *       {@code ONBOARDING_ALREADY_COMPLETED}(409)로 거부한다 — 401이 아니어야 클라의 401 인터셉터가
     *       "재로그인 필요"로 오인해 방금 만든 세션을 파괴하지 않는다.</li>
     *   <li>입력한 닉네임이 이미 사용 중이면 {@code NICKNAME_DUPLICATE}로 거부한다.</li>
     *   <li>프로필 정보(닉네임·성별·연령대·소개·지역·좌표·식사스타일·프로필이미지)를 채워
     *       프로필을 완성한다 — 이 시점에 회원 상태가 ACTIVE로 바뀐다.</li>
     *   <li>정식 토큰(access+refresh)을 발급해 반환한다.</li>
     * </ol>
     *
     * @param userId  온보딩을 끝낼 사용자(온보딩 토큰의 주체)
     * @param command 프로필 입력값 묶음
     * @return 새로 발급된 정식 토큰 쌍
     * @throws BusinessException 회원이 없거나({@code USER_NOT_FOUND}), 정지·탈퇴 등 사용 불가 상태이거나
     *         ({@code ACCOUNT_INACTIVE}), 이미 온보딩을 마쳤거나({@code ONBOARDING_ALREADY_COMPLETED})
     *         닉네임이 중복일 때({@code NICKNAME_DUPLICATE})
     */
    @Transactional // 닉네임 검증·프로필 완료·토큰 발급을 한 트랜잭션으로 묶는다
    public TokenPair complete(long userId, CompleteProfileCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserStatus status = user.getStatus();
        // "PENDING이 아니면 거부"가 아니라 "PENDING도 ACTIVE도 아니면 거부"로 표현해, 새 상태(DORMANT 등)가
        // 추가돼도 fail-closed로 걸러지게 한다. 정지·탈퇴 등은 온보딩 토큰으로 부활을 시도하는 것이므로 401.
        if (status != UserStatus.PENDING && status != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        // 이미 ACTIVE면 부활이 아니라 "이미 끝난 온보딩을 다시 호출"한 것뿐이다(더블탭·앱의 401 재시도 등).
        // 이 경우까지 401을 주면 클라의 401 인터셉터가 방금 만든 세션을 로그아웃시켜버리므로 409로 구분한다.
        if (status == UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }
        if (userRepository.existsByNickname(command.nickname())) { // 닉네임 중복 검사
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
        LocalDate today = LocalDate.now(clock.withZone(KST));
        if (command.birthDate() != null && command.birthDate().isAfter(today.minusYears(14))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "만 14세 이상만 가입할 수 있어요.");
        }
        // 프로필을 채워 완성 → 내부에서 회원 상태가 ACTIVE로 전환된다.
        user.completeProfile(command.nickname(), command.gender(), command.birthDate(), command.introduction(),
                command.region(), command.regionLat(), command.regionLng(), command.diningStyle(),
                command.profileImageUrl());
        foodPreferenceService.replaceFoods(userId, command.favoriteFoods()); // 선호 음식 저장(null이면 미변경)
        favoriteGroupService.createDefaultGroup(userId); // 기본 즐겨찾기 그룹 생성(멱등)
        return tokenService.issue(userId); // 정식 토큰 발급
    }

    /**
     * refresh 토큰을 회전해 access 토큰을 재발급하고, 회전 직후 회원 상태를 확인해 비ACTIVE면 새 토큰도
     * 즉시 회수한다.
     *
     * <p>{@link TokenService}는 토큰 메커니즘(발급·해시·만료)만 아는 계층이라 {@code users.status}를 모른다
     * ({@code UserRepository}·{@code UserStatus}를 의도적으로 참조하지 않는다). 그래서 회원 상태 확인은 그
     * 도메인 지식을 이미 가진 이 서비스가 맡는다 — {@link TokenService#rotate}로 새 토큰 쌍을 받은 뒤, 그
     * access 토큰의 sub(userId)로 상태를 조회해 ACTIVE가 아니면 방금 발급된 refresh까지 즉시 회수하고
     * {@code ACCOUNT_INACTIVE}로 거부한다. {@code ActiveUserFilter}가 이미 로그인된 세션의 API 호출은
     * 막아 주지만, {@code /api/auth/refresh}는 {@code permitAll}이라 그 필터를 거치지 않으므로 여기서 막지
     * 않으면 정지·탈퇴 계정이 회전할 때마다 refresh TTL을 계속 연장해 계정이 죽지 않는다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>{@link TokenService#rotate}로 기존 refresh를 회수하고 새 토큰 쌍을 발급한다.</li>
     *   <li>새 access 토큰을 디코드해 sub(userId)를 얻는다.</li>
     *   <li>그 userId의 현재 상태를 조회한다(없으면 null 취급).</li>
     *   <li>ACTIVE가 아니면, 방금 발급된 refresh를 즉시 회수해(살아있는 토큰을 남기지 않고)
     *       {@code ACCOUNT_INACTIVE}로 거부한다.</li>
     *   <li>ACTIVE면 새 토큰 쌍을 그대로 반환한다.</li>
     * </ol>
     *
     * @param rawRefreshToken 클라가 보관 중인 refresh 원문
     * @return 새로 발급된 토큰 쌍
     * @throws BusinessException refresh가 무효하거나({@code INVALID_REFRESH_TOKEN}) 회원이 ACTIVE가
     *         아닐 때({@code ACCOUNT_INACTIVE})
     */
    public TokenPair refresh(String rawRefreshToken) {
        TokenPair pair = tokenService.rotate(rawRefreshToken);
        long userId = Long.parseLong(jwtProvider.decode(pair.accessToken()).getSubject());
        UserStatus status = userRepository.findStatusById(userId).orElse(null);
        if (status != UserStatus.ACTIVE) {
            tokenService.revoke(pair.refreshToken()); // 비ACTIVE 계정에 살아있는 refresh를 남기지 않는다
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        return pair;
    }

    /**
     * 로그아웃. 제시된 refresh를 무효화하도록 {@link TokenService#revoke}에 위임한다(없으면 조용히 무시).
     *
     * @param rawRefreshToken 무효화할 refresh 원문
     */
    public void logout(String rawRefreshToken) {
        tokenService.revoke(rawRefreshToken);
    }
}
