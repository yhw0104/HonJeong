package com.honjeong.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 인증 흐름 전체를 지휘(오케스트레이션)하는 핵심 서비스. 휴대폰 인증·소셜(OAuth) 로그인이라는
 * 두 진입 경로와, 그 뒤에 이어지는 온보딩(약관 동의 → 프로필 완성)까지를 담당한다.
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

    /**
     * 인증에 필요한 저장소·외부 연동·토큰 발급기를 모두 주입받아 서비스를 구성한다.
     * (생성자 주입 + final로 불변 의존성을 보장한다.)
     */
    public AuthService(UserRepository userRepository, SocialAccountRepository socialAccountRepository,
            PhoneVerificationRepository phoneVerificationRepository, PhoneAttemptRecorder phoneAttemptRecorder,
            TermsAgreementRepository termsAgreementRepository,
            OAuthVerifier oAuthVerifier, SmsSender smsSender, VerificationCodeGenerator codeGenerator,
            TokenService tokenService, JwtProvider jwtProvider, Clock clock) {
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
        // 이미 정상 회원 → 즉시 로그인
        Optional<User> existing = userRepository.findByPhone(phone);
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
     * @param provider 소셜 공급자(KAKAO·APPLE 등)
     * @param idToken  공급자가 발급한 ID 토큰(검증 대상)
     * @return 로그인 결과 또는 온보딩 결과를 담은 {@link AuthResult}
     * @throws BusinessException 소셜 계정은 있는데 연결된 회원을 못 찾을 때({@code USER_NOT_FOUND})
     */
    @Transactional // 회원·소셜계정 생성 등 쓰기를 한 트랜잭션으로 묶는다
    public AuthResult oauthLogin(Provider provider, String idToken) {
        OAuthIdentity identity = oAuthVerifier.verify(provider, idToken); // 공급자 토큰 검증 → 신원 추출
        Optional<SocialAccount> account =
                socialAccountRepository.findByProviderAndProviderUserId(provider, identity.providerUserId());
        if (account.isPresent()) { // 이미 가입한 소셜 계정(재방문)
            User user = userRepository.findById(account.get().getUserId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            return user.isActive()
                    ? AuthResult.login(tokenService.issue(user.getId()))                     // 정상 회원 → 로그인
                    : AuthResult.onboarding(jwtProvider.createOnboardingToken(user.getId())); // 미완 → 온보딩 계속
        }
        // 신규: PENDING 회원 생성 + 소셜 계정 연결 저장 → 온보딩 토큰
        User user = userRepository.save(User.pending(null, identity.email()));
        socialAccountRepository.save(SocialAccount.of(user.getId(), provider, identity.providerUserId(), identity.email()));
        return AuthResult.onboarding(jwtProvider.createOnboardingToken(user.getId()));
    }

    /**
     * 약관 동의를 기록한다(온보딩 단계). 필수 약관 3종(서비스 이용·개인정보·위치)에 모두 동의해야
     * 통과하며, 마케팅은 선택이다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>필수 3종이 모두 true가 아니면 {@code TERMS_REQUIRED} 예외로 거부한다.</li>
     *   <li>이미 동의 기록이 있으면 아무것도 하지 않고 반환한다(<b>멱등</b> — 두 번 호출해도 안전).</li>
     *   <li>그렇지 않으면 동의 항목들과 현재 시각을 한 건으로 저장한다.</li>
     * </ol>
     *
     * @param userId    동의하는 사용자(온보딩 토큰의 주체)
     * @param service   서비스 이용약관 동의(필수)
     * @param privacy   개인정보 처리방침 동의(필수)
     * @param location  위치정보 이용약관 동의(필수)
     * @param marketing 마케팅 수신 동의(선택)
     * @throws BusinessException 필수 약관을 하나라도 동의하지 않았을 때({@code TERMS_REQUIRED})
     */
    @Transactional // 동의 기록 저장(쓰기)을 트랜잭션으로 묶는다
    public void agreeTerms(long userId, boolean service, boolean privacy, boolean location, boolean marketing) {
        if (!(service && privacy && location)) { // 필수 3종 중 하나라도 미동의면 거부
            throw new BusinessException(ErrorCode.TERMS_REQUIRED);
        }
        if (termsAgreementRepository.existsByUserId(userId)) {
            return; // 이미 동의 — 멱등(재호출해도 중복 저장하지 않음)
        }
        termsAgreementRepository.save(
                TermsAgreement.of(userId, service, privacy, location, marketing, LocalDateTime.now(clock)));
    }

    /**
     * 프로필을 확정해 온보딩을 끝내고 정식 토큰을 발급한다. 온보딩의 마지막 단계로, 이 호출이 성공하면
     * 회원은 PENDING에서 ACTIVE로 전환되어 정상 회원이 된다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>userId로 회원을 조회한다(없으면 {@code USER_NOT_FOUND}).</li>
     *   <li>입력한 닉네임이 이미 사용 중이면 {@code NICKNAME_DUPLICATE}로 거부한다.</li>
     *   <li>프로필 정보(닉네임·성별·연령대·소개·지역·좌표·식사스타일·프로필이미지)를 채워
     *       프로필을 완성한다 — 이 시점에 회원 상태가 ACTIVE로 바뀐다.</li>
     *   <li>정식 토큰(access+refresh)을 발급해 반환한다.</li>
     * </ol>
     *
     * @param userId  온보딩을 끝낼 사용자(온보딩 토큰의 주체)
     * @param command 프로필 입력값 묶음
     * @return 새로 발급된 정식 토큰 쌍
     * @throws BusinessException 회원이 없거나({@code USER_NOT_FOUND}) 닉네임이 중복일 때({@code NICKNAME_DUPLICATE})
     */
    @Transactional // 닉네임 검증·프로필 완료·토큰 발급을 한 트랜잭션으로 묶는다
    public TokenPair complete(long userId, CompleteProfileCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (userRepository.existsByNickname(command.nickname())) { // 닉네임 중복 검사
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
        // 프로필을 채워 완성 → 내부에서 회원 상태가 ACTIVE로 전환된다.
        user.completeProfile(command.nickname(), command.gender(), command.ageGroup(), command.introduction(),
                command.region(), command.regionLat(), command.regionLng(), command.diningStyle(),
                command.profileImageUrl());
        return tokenService.issue(userId); // 정식 토큰 발급
    }

    /**
     * access 토큰 재발급. 클라가 보관한 refresh 원문으로 {@link TokenService#rotate}를 호출하는 얇은
     * 위임 메서드다(기존 refresh 회수 + 새 토큰 쌍 발급). 토큰 회전 자체는 트랜잭션이 필요하므로
     * 여기서는 별도 {@code @Transactional}을 두지 않고 TokenService에 위임한다.
     *
     * @param rawRefreshToken 클라가 보관 중인 refresh 원문
     * @return 새로 발급된 토큰 쌍
     */
    public TokenPair refresh(String rawRefreshToken) {
        return tokenService.rotate(rawRefreshToken);
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
