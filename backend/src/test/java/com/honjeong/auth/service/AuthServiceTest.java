package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.auth.domain.PhoneVerification;
import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.SocialAccount;
import com.honjeong.auth.domain.TermsAgreement;
import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.auth.repository.SocialAccountRepository;
import com.honjeong.auth.repository.TermsAgreementRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * {@link AuthService}의 단위 테스트. 인증 진입(휴대폰·소셜)과 온보딩(약관·프로필 완료)의 핵심 분기·검증을
 * 확인한다. 저장소·외부 연동·토큰 발급기는 모두 Mockito로 모킹해 DB·네트워크 없이 순수 비즈니스 로직만
 * 검증한다.
 *
 * <p>주의: {@code clock}은 인증번호 만료 비교를 결정론적으로 만들기 위한 고정 시계다. 검증용 인증번호의
 * 만료 시각도 이 시계 기준으로 만들어, "유효/만료"를 테스트가 정확히 통제한다.
 */
class AuthServiceTest {

    private static final String PHONE = "01012345678";
    private static final String CODE = "000000"; // mock 환경 고정 인증번호와 동일

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final PhoneVerificationRepository phoneVerificationRepository = mock(PhoneVerificationRepository.class);
    private final PhoneAttemptRecorder phoneAttemptRecorder = mock(PhoneAttemptRecorder.class);
    private final TermsAgreementRepository termsAgreementRepository = mock(TermsAgreementRepository.class);
    private final OAuthVerifier oAuthVerifier = mock(OAuthVerifier.class);
    private final SmsSender smsSender = mock(SmsSender.class);
    private final VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T00:00:00Z"), ZoneOffset.UTC);

    private final AuthService authService = new AuthService(userRepository, socialAccountRepository,
            phoneVerificationRepository, phoneAttemptRecorder, termsAgreementRepository, oAuthVerifier, smsSender,
            codeGenerator, tokenService, jwtProvider, clock);

    /**
     * 테스트용 User를 만든다. active=true면 프로필을 채워 ACTIVE 상태로 만들고, 엔티티에는 보통 자동
     * 생성되는 id를 리플렉션으로 강제 주입해 모킹 반환값으로 쓸 수 있게 한다.
     */
    private User userWithId(long id, String phone, boolean active) {
        User user = User.pending(phone, null);
        if (active) {
            user.completeProfile("닉", null, null, null, null, null, null, null, null); // ACTIVE 전환
        }
        ReflectionTestUtils.setField(user, "id", id); // 자동 생성 id를 테스트에서 강제 지정
        return user;
    }

    /** 고정 시계 기준 3분 뒤 만료되는 "유효한" 인증번호 발송 이력을 만든다. */
    private PhoneVerification validVerification() {
        return PhoneVerification.issue(PHONE, CODE, LocalDateTime.now(clock).plusMinutes(3));
    }

    /** 닉네임만 지정하고 나머지 프로필 항목은 null인 프로필 완료 명령을 만든다. */
    private CompleteProfileCommand command(String nickname) {
        return new CompleteProfileCommand(nickname, null, null, null, null, null, null, null, null);
    }

    /**
     * sendPhoneCode 검증.
     * given: 코드 생성기가 "000000"을 반환하도록 모킹.
     * when: 번호로 인증번호 발송 요청.
     * then: 발송 이력이 저장되고, SMS 발송기에 (번호, 코드)로 발송이 위임된다.
     */
    @Test
    @DisplayName("sendPhoneCode: 코드를 생성·저장하고 SMS로 발송한다")
    void sendPhoneCode_savesAndSends() {
        when(codeGenerator.generate()).thenReturn(CODE);

        authService.sendPhoneCode(PHONE);

        verify(phoneVerificationRepository).save(any(PhoneVerification.class));
        verify(smsSender).send(PHONE, CODE);
    }

    /**
     * verifyPhone 신규 분기 검증.
     * given: 유효한 인증번호 이력이 있고, 해당 번호로 가입된 회원은 없으며, 새 PENDING 회원(id 1)이
     *        저장되고 온보딩 토큰이 발급되도록 모킹.
     * when: 올바른 코드로 검증.
     * then: 결과가 온보딩(onboarding=true)이고 온보딩 토큰이 담긴다.
     */
    @Test
    @DisplayName("verifyPhone: 신규 번호면 PENDING 회원 생성 + 온보딩 토큰")
    void verifyPhone_newUser_returnsOnboarding() {
        when(phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(validVerification()));
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(userWithId(1L, PHONE, false));
        when(jwtProvider.createOnboardingToken(1L)).thenReturn("onboarding-token");

        AuthResult result = authService.verifyPhone(PHONE, CODE);

        assertThat(result.onboarding()).isTrue();
        assertThat(result.onboardingToken()).isEqualTo("onboarding-token");
    }

    /**
     * verifyPhone 기존 ACTIVE 분기 검증.
     * given: 유효한 인증번호 이력이 있고, 해당 번호로 ACTIVE 회원(id 5)이 조회되며, 그 회원에게
     *        정식 토큰이 발급되도록 모킹.
     * when: 올바른 코드로 검증.
     * then: 온보딩이 아니라(onboarding=false) 정식 토큰(tokens)이 그대로 반환된다.
     */
    @Test
    @DisplayName("verifyPhone: 기존 ACTIVE 회원이면 바로 로그인 토큰")
    void verifyPhone_existingActive_returnsLogin() {
        when(phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(validVerification()));
        when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(userWithId(5L, PHONE, true)));
        TokenPair pair = new TokenPair("acc", "ref", 3600);
        when(tokenService.issue(5L)).thenReturn(pair);

        AuthResult result = authService.verifyPhone(PHONE, CODE);

        assertThat(result.onboarding()).isFalse();
        assertThat(result.tokens()).isEqualTo(pair);
    }

    /**
     * verifyPhone 만료 검증.
     * given: 고정 시계 기준 1분 전에 이미 만료된 인증번호 이력이 조회되도록 모킹.
     * when/then: 검증하면 PHONE_CODE_EXPIRED 계열의 BusinessException이 발생한다.
     */
    @Test
    @DisplayName("verifyPhone: 만료된 코드는 거부")
    void verifyPhone_expired_throws() {
        PhoneVerification expired = PhoneVerification.issue(PHONE, CODE, LocalDateTime.now(clock).minusMinutes(1));
        when(phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.verifyPhone(PHONE, CODE)).isInstanceOf(BusinessException.class);
    }

    /**
     * verifyPhone 불일치 검증.
     * given: 저장된 코드가 "000000"인 유효한 이력이 조회되도록 모킹.
     * when/then: 다른 코드("999999")로 검증하면 PHONE_CODE_MISMATCH 계열의 BusinessException이 발생한다.
     */
    @Test
    @DisplayName("verifyPhone: 코드 불일치는 거부")
    void verifyPhone_mismatch_throws() {
        when(phoneVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(validVerification()));

        assertThatThrownBy(() -> authService.verifyPhone(PHONE, "999999")).isInstanceOf(BusinessException.class);
    }

    /**
     * oauthLogin 신규 분기 검증.
     * given: 검증기가 카카오 신원을 돌려주고, 그 (공급자, 식별자)로 연결된 소셜 계정이 없으며, 새 회원
     *        (id 2)이 저장되고 온보딩 토큰이 발급되도록 모킹.
     * when: 소셜 로그인.
     * then: 결과가 온보딩이고, 소셜 계정 저장이 호출된다(회원-소셜 연결 생성 확인).
     */
    @Test
    @DisplayName("oauthLogin: 신규면 회원+소셜 생성 후 온보딩 토큰")
    void oauthLogin_newUser_createsUserAndSocial() {
        when(oAuthVerifier.verify(Provider.KAKAO, "idtok")).thenReturn(new OAuthIdentity(Provider.KAKAO, "kakao-123", null));
        when(socialAccountRepository.findByProviderAndProviderUserId(Provider.KAKAO, "kakao-123")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(userWithId(2L, null, false));
        when(jwtProvider.createOnboardingToken(2L)).thenReturn("onb");

        AuthResult result = authService.oauthLogin(Provider.KAKAO, "idtok");

        assertThat(result.onboarding()).isTrue();
        verify(socialAccountRepository).save(any(SocialAccount.class));
    }

    /**
     * agreeTerms 필수 누락 검증.
     * given: (모킹 불필요) 필수 약관 중 privacy=false로 호출.
     * when/then: 필수 3종을 다 채우지 못했으므로 TERMS_REQUIRED 계열의 BusinessException이 발생한다.
     */
    @Test
    @DisplayName("agreeTerms: 필수 약관 누락 시 거부")
    void agreeTerms_missingRequired_throws() {
        assertThatThrownBy(() -> authService.agreeTerms(1L, true, false, true, false))
                .isInstanceOf(BusinessException.class);
    }

    /**
     * agreeTerms 정상 저장 검증.
     * given: 해당 사용자의 기존 동의 기록이 없도록(existsByUserId=false) 모킹.
     * when: 필수 3종 동의(마케팅은 false)로 호출.
     * then: 동의 기록이 한 건 저장된다.
     */
    @Test
    @DisplayName("agreeTerms: 필수 동의 시 저장")
    void agreeTerms_valid_saves() {
        when(termsAgreementRepository.existsByUserId(1L)).thenReturn(false);

        authService.agreeTerms(1L, true, true, true, false);

        verify(termsAgreementRepository).save(any(TermsAgreement.class));
    }

    /**
     * complete 닉네임 중복 검증.
     * given: 대상 회원(id 1)은 조회되지만, 닉네임 "dup"이 이미 사용 중(existsByNickname=true)이도록 모킹.
     * when/then: 프로필 완료를 시도하면 NICKNAME_DUPLICATE 계열의 BusinessException이 발생한다.
     */
    @Test
    @DisplayName("complete: 닉네임 중복이면 거부")
    void complete_duplicateNickname_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId(1L, PHONE, false)));
        when(userRepository.existsByNickname("dup")).thenReturn(true);

        assertThatThrownBy(() -> authService.complete(1L, command("dup"))).isInstanceOf(BusinessException.class);
    }

    /**
     * complete 정상 경로 검증 — 온보딩 마지막 단계.
     * given: PENDING 회원(id 1)이 조회되고, 닉네임은 미사용(existsByNickname=false)이며, 정식 토큰이
     *        발급되도록 모킹.
     * when: 닉네임으로 프로필 완료.
     * then: 회원이 ACTIVE로 전환되고, 발급된 토큰 쌍이 그대로 반환된다.
     */
    @Test
    @DisplayName("complete: 프로필 확정 시 ACTIVE 전환 + 토큰 발급")
    void complete_valid_activatesAndIssues() {
        User user = userWithId(1L, PHONE, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("닉네임")).thenReturn(false);
        TokenPair pair = new TokenPair("a", "r", 3600);
        when(tokenService.issue(1L)).thenReturn(pair);

        TokenPair result = authService.complete(1L, command("닉네임"));

        assertThat(user.isActive()).isTrue();
        assertThat(result).isEqualTo(pair);
    }
}
