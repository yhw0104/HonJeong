package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.auth.client.AppleTokenClient;
import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.SocialAccount;
import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.auth.repository.SocialAccountRepository;
import com.honjeong.auth.repository.TermsAgreementRepository;
import com.honjeong.favorite.service.FavoriteGroupService;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;
import com.honjeong.user.service.UserFoodPreferenceService;

/**
 * 애플 로그인에서 refresh token을 받아 두는 동작만 본다({@link AuthServiceTest}가 이미 덮는 분기는
 * 다시 보지 않는다).
 *
 * <p>이 클래스는 AuthService의 기존 협력자 전부를 모킹해야 하므로, 프로젝트의 기존
 * {@link AuthServiceTest}가 쓰는 셋업 방식(모든 저장소·서비스 Mockito mock 필드 +
 * {@code new AuthService(...)} 조립)을 그대로 따른다. 공통 셋업을 부모 클래스로 뽑지 않은 이유는
 * 이 저장소가 그런 구조를 쓰지 않기 때문이다 — 이 한 파일을 위해 상속 계층을 새로 만들지 않는다.
 *
 * <p><b>여기서 지키는 계약</b>: 애플 토큰 교환은 가입에 <b>딸린</b> 작업이다. 실패하면 refresh token만
 * 없는 채로 가입이 그대로 끝나야 한다 — 애플 서버 장애가 회원가입을 막으면 안 된다.
 */
class AuthServiceAppleTest {

    private static final String ID_TOKEN = "id-token";

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
    private final UserFoodPreferenceService userFoodPreferenceService = mock(UserFoodPreferenceService.class);
    private final FavoriteGroupService favoriteGroupService = mock(FavoriteGroupService.class);
    private final AppleTokenClient appleTokenClient = mock(AppleTokenClient.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC);

    private final AuthService authService = new AuthService(userRepository, socialAccountRepository,
            phoneVerificationRepository, phoneAttemptRecorder, termsAgreementRepository, oAuthVerifier, smsSender,
            codeGenerator, tokenService, jwtProvider, clock, userFoodPreferenceService, favoriteGroupService,
            appleTokenClient);

    /**
     * "이 공급자의 이 sub로 처음 들어온 사람"을 재현한다 — 신규 가입 분기(회원 생성 + 소셜 계정 저장)로
     * 흐르게 하는 최소 스텁 셋이다.
     *
     * <ol>
     *   <li>토큰 검증이 해당 신원을 돌려준다(이메일은 애플이 늘 null이라 여기서도 null이다).</li>
     *   <li>그 (공급자, sub)로 연결된 소셜 계정이 아직 없다 → 신규 분기.</li>
     *   <li>회원 저장이 id가 박힌 User를 돌려준다(온보딩 토큰 발급에 id가 필요하다).</li>
     *   <li>그 id로 온보딩 토큰이 발급된다 → 반환값으로 "가입이 끝났음"을 확인할 수 있다.</li>
     * </ol>
     */
    private void givenNewSocialUser(Provider provider, String sub) {
        when(oAuthVerifier.verify(provider, ID_TOKEN)).thenReturn(new OAuthIdentity(provider, sub, null));
        when(socialAccountRepository.findByProviderAndProviderUserId(provider, sub)).thenReturn(Optional.empty());
        User user = User.pending(null, null);
        ReflectionTestUtils.setField(user, "id", 1L); // 자동 생성 id를 테스트에서 강제 지정
        when(userRepository.save(any())).thenReturn(user);
        when(jwtProvider.createOnboardingToken(1L)).thenReturn("onb");
    }

    /**
     * "이 애플 계정으로 예전에 이미 가입한 사람"을 재현한다 — 재방문(로그인) 분기로 흐르게 한다.
     * 가입 때 교환에 실패해 apple_refresh_token이 비어 있는 계정을 일부러 쓴다(backfill 유혹이 가장
     * 큰 상황이 정확히 이 모양이다).
     */
    private void givenReturningAppleUser(String sub) {
        when(oAuthVerifier.verify(Provider.APPLE, ID_TOKEN))
                .thenReturn(new OAuthIdentity(Provider.APPLE, sub, null));
        when(socialAccountRepository.findByProviderAndProviderUserId(Provider.APPLE, sub))
                .thenReturn(Optional.of(SocialAccount.of(5L, Provider.APPLE, sub, null)));
        User user = User.pending(null, null);
        user.completeProfile("닉", null, null, null, null, null, null, null, null); // ACTIVE 전환
        ReflectionTestUtils.setField(user, "id", 5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tokenService.issue(5L)).thenReturn(new TokenPair("acc", "ref", 3600));
    }

    /** 저장된 소셜 계정을 붙잡아 돌려준다(저장 자체가 일어났는지도 함께 검증된다). */
    private SocialAccount savedSocialAccount() {
        ArgumentCaptor<SocialAccount> saved = ArgumentCaptor.forClass(SocialAccount.class);
        verify(socialAccountRepository).save(saved.capture());
        return saved.getValue();
    }

    /**
     * given: 애플로 처음 들어온 사용자 + 코드 교환이 성공하도록 모킹.
     * when: authorizationCode를 함께 넘겨 소셜 로그인.
     * then: 교환해 받은 refresh token이 저장되는 소셜 계정에 실려 있다(탈퇴 시 폐기에 쓸 값).
     */
    @Test
    @DisplayName("애플 신규 가입: authorizationCode를 교환해 refresh token을 소셜 계정에 저장한다")
    void 애플가입시_refreshToken을_저장한다() {
        givenNewSocialUser(Provider.APPLE, "apple-sub");
        when(appleTokenClient.exchangeRefreshToken("code-123")).thenReturn("r-token");

        authService.oauthLogin(Provider.APPLE, ID_TOKEN, "code-123");

        assertThat(savedSocialAccount().getAppleRefreshToken()).isEqualTo("r-token");
    }

    /**
     * ★이 프로젝트에서 가장 중요한 애플 관련 계약. given: 코드 교환이 실패(null)하도록 모킹.
     * when: 소셜 로그인.
     * then: 예외 없이 온보딩 토큰이 나오고, 소셜 계정은 refresh token만 null인 채로 저장된다.
     *
     * <p>교환 실패가 예외로 번지거나 저장을 건너뛰도록 바뀌면 여기서 잡힌다 — 온보딩 토큰 단언이
     * "가입이 끝까지 갔다"를, 저장 단언이 "소셜 계정 연결은 그대로 만들어졌다"를 각각 고정한다.
     */
    @Test
    @DisplayName("★교환에 실패해도 로그인은 성공한다 — 애플 장애가 가입을 막으면 안 된다")
    void 교환실패해도_가입은_된다() {
        givenNewSocialUser(Provider.APPLE, "apple-sub");
        when(appleTokenClient.exchangeRefreshToken(any())).thenReturn(null);

        AuthResult result = authService.oauthLogin(Provider.APPLE, ID_TOKEN, "code-123");

        assertThat(result.onboarding()).isTrue();
        assertThat(result.onboardingToken()).isEqualTo("onb"); // 온보딩 토큰이 정상 발급된다
        assertThat(savedSocialAccount().getAppleRefreshToken()).isNull();
    }

    /**
     * given: 카카오로 처음 들어온 사용자.
     * when: 소셜 로그인(카카오는 authorizationCode를 보내지 않으므로 null).
     * then: 애플 토큰 클라이언트를 아예 부르지 않는다 — 카카오 가입 경로에 애플 HTTP 호출이 끼어들면
     * 안 된다(공급자 분기가 사라지면 여기서 잡힌다).
     */
    @Test
    @DisplayName("카카오 로그인은 애플 토큰 클라이언트를 부르지 않는다")
    void 카카오는_애플을_안부른다() {
        givenNewSocialUser(Provider.KAKAO, "kakao-sub");

        authService.oauthLogin(Provider.KAKAO, ID_TOKEN, null);

        verify(appleTokenClient, never()).exchangeRefreshToken(any());
        assertThat(savedSocialAccount().getAppleRefreshToken()).isNull();
    }

    /**
     * given: 이미 가입한 애플 계정(가입 때 교환에 실패해 refresh token이 비어 있다) + 앱이 이번에도
     * authorizationCode를 보냄.
     * when: 재방문 로그인.
     * then: 코드를 교환하지도, 소셜 계정을 다시 저장하지도 않고 그대로 로그인만 된다.
     *
     * <p>"재방문에는 backfill하지 않는다"는 결정을 코드가 아니라 <b>테스트</b>로 고정한다. 이 단언이
     * 없으면 ⓐ 친절하게 뒤늦게 채우는 코드가 들어와도, ⓑ 교환을 {@code account.isPresent()} 검사보다
     * 위로 옮겨 매 로그인마다 애플로 나가게 돼도 전 스위트가 초록으로 통과한다 — 둘 다 지금은 읽기만
     * 하는 로그인 경로에 외부 HTTP 호출(과 쓰기)을 얹는 변경이다.
     *
     * <p>★<b>이건 "영원히 지키고 싶은 성질"이 아니라 의도적으로 미뤄 둔 결정이다.</b> 교환에 실패한
     * 계정은 지금 설계상 영영 refresh token 없이 남고, 그만큼 심사 지침 5.1.1(v) 폐기가 조용히
     * 빠진다(자격증명이 잘못 설정돼 있으면 <b>모든</b> 애플 계정이 그렇다). 나중에 이 구멍을 메우기로
     * 하면 <b>이 테스트를 먼저 고쳐야 한다</b> — 이 단언은 그때 지워도 되는 것이고, 지운다고 계약을
     * 깨는 게 아니다. 다만 되돌릴 때 치를 값은 알고 있을 것: 자격증명이 계속 틀린 배포에서는 해당
     * 사용자의 <b>매 로그인</b>이 애플 응답을 최대 15초(연결 5초 + 응답 10초) 기다리게 된다.
     */
    @Test
    @DisplayName("재방문 로그인은 코드를 교환하지 않는다 — backfill하지 않기로 한 결정을 고정한다")
    void 재방문에는_교환하지_않는다() {
        givenReturningAppleUser("apple-sub");

        AuthResult result = authService.oauthLogin(Provider.APPLE, ID_TOKEN, "code-123");

        assertThat(result.onboarding()).isFalse(); // 기존 ACTIVE 회원 → 곧바로 로그인
        verify(appleTokenClient, never()).exchangeRefreshToken(any());
        verify(socialAccountRepository, never()).save(any());
    }
}
