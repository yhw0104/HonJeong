package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * honjeong.oauth.mode 값에 따라 {@link OAuthVerifier} 빈이 올바르게 하나만 조립되는지 확인하는 회귀 테스트.
 *
 * <p><b>배경</b>: {@link KakaoOAuthVerifier}에 생성자가 둘(운영용 3-인자 / 테스트용 private) 있는데
 * 어느 쪽에도 {@code @Autowired}가 없으면, 스프링은 둘 중 무엇을 쓸지 정하지 못하고 결국 기본(무인자)
 * 생성자를 찾다가 부팅에 실패한다({@code No default constructor found}). 단위 테스트
 * ({@code KakaoOAuthVerifierTest})는 {@code withDecoder}(private 생성자) 경로만 타서 이 결함을 잡지 못했고,
 * {@code mode=real}로 스프링 컨텍스트를 올리는 테스트가 그동안 하나도 없었다.
 *
 * <p>전체 {@code @SpringBootTest}는 DB(Testcontainers)까지 띄워 무겁고 이 결함과 무관하므로,
 * {@link ApplicationContextRunner}로 {@link OAuthVerifier} 빈 조립만 가볍게 검증한다.
 */
class OAuthVerifierWiringTest {

    private static final String ISSUER = "https://dummy-issuer.example";
    private static final String JWKS_URI = "https://dummy-issuer.example/.well-known/jwks.json";
    private static final String APP_KEY = "dummy-app-key"; // 실제 카카오 키 아님

    private static final String APPLE_ISSUER = "https://dummy-apple.example";
    private static final String APPLE_JWKS_URI = "https://dummy-apple.example/keys";
    private static final String APPLE_CLIENT_ID = "com.honjeong.app";

    // @Value 플레이스홀더(${...}) 해석에는 PropertySourcesPlaceholderConfigurer가 필요하다
    // (일반 스프링 부트 앱은 자동 등록되지만, ApplicationContextRunner는 직접 추가해야 한다).
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
                    .of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(MockOAuthVerifier.class, KakaoOAuthVerifier.class,
                    AppleOAuthVerifier.class, DelegatingOAuthVerifier.class);

    @Test
    @DisplayName("mode 미지정 시 MockOAuthVerifier만 조립된다")
    void mode_미지정시_Mock만_조립된다() {
        contextRunner.run(this::assertMockAssembled);
    }

    @Test
    @DisplayName("mode=mock 시 MockOAuthVerifier만 조립된다")
    void mode_mock시_Mock만_조립된다() {
        contextRunner.withPropertyValues("honjeong.oauth.mode=mock").run(this::assertMockAssembled);
    }

    @Test
    @DisplayName("mode=real + 필수값을 채우면 카카오·애플 검증기가 조립되고 위임 빈이 대표가 된다")
    void mode_real시_위임빈이_대표가_된다() {
        contextRunner
                .withPropertyValues(
                        "honjeong.oauth.mode=real",
                        "honjeong.oauth.kakao.issuer=" + ISSUER,
                        "honjeong.oauth.kakao.jwks-uri=" + JWKS_URI,
                        "honjeong.oauth.kakao.app-key=" + APP_KEY,
                        "honjeong.apple.issuer=" + APPLE_ISSUER,
                        "honjeong.apple.jwks-uri=" + APPLE_JWKS_URI,
                        "honjeong.apple.client-id=" + APPLE_CLIENT_ID)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KakaoOAuthVerifier.class);
                    assertThat(context).hasSingleBean(AppleOAuthVerifier.class);
                    // @Primary라 주입 대상은 위임 빈이다 — AuthService가 받는 것이 이것이다.
                    assertThat(context.getBean(OAuthVerifier.class))
                            .isInstanceOf(DelegatingOAuthVerifier.class);
                });
    }

    /**
     * ★{@code hasFailed()}만으로는 부족하다. 이 테스트가 지금 "카카오 app-key 가드"에 귀속되는 근거는
     * 오직 <b>나머지 필수값을 전부 채워 둬서 다른 실패 원인이 없다</b>는 것뿐이다. 카카오·애플 검증기에
     * 필수값이 하나라도 추가되면(그 값의 기본이 비어 있으면) 이 테스트는 그 새 가드가 낸 실패로 계속
     * 초록이고, 정작 app-key 가드가 사라진 건 아무도 모른다. 그래서 실패 <b>원인</b>까지 못 박는다
     * ({@code ExternalIntegrationWiringTest}가 세운 방식).
     */
    @Test
    @DisplayName("mode=real인데 카카오 app-key가 비어 있으면 부팅이 실패한다(Assert.hasText 검사 회귀 방지)")
    void mode_real_appKey가_비어있으면_부팅실패한다() {
        // 애플 값도 일부러 채운다 — 비워 두면 애플 쪽 검증기가 먼저 실패해 이 테스트가 지키려는
        // "카카오 app-key 비었을 때" 가드를 더 이상 보장하지 못하게 된다.
        contextRunner
                .withPropertyValues(
                        "honjeong.oauth.mode=real",
                        "honjeong.oauth.kakao.issuer=" + ISSUER,
                        "honjeong.oauth.kakao.jwks-uri=" + JWKS_URI,
                        "honjeong.oauth.kakao.app-key=",
                        "honjeong.apple.issuer=" + APPLE_ISSUER,
                        "honjeong.apple.jwks-uri=" + APPLE_JWKS_URI,
                        "honjeong.apple.client-id=" + APPLE_CLIENT_ID)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure().rootCause()
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("honjeong.oauth.kakao.app-key");
                });
    }

    /** 위 테스트와 같은 이유로 실패 원인까지 단언한다(빈 값 하나만 다른 대조 구조). */
    @Test
    @DisplayName("mode=real인데 애플 client-id가 비어 있으면 부팅이 실패한다")
    void mode_real_애플clientId가_비어있으면_부팅실패한다() {
        contextRunner
                .withPropertyValues(
                        "honjeong.oauth.mode=real",
                        "honjeong.oauth.kakao.issuer=" + ISSUER,
                        "honjeong.oauth.kakao.jwks-uri=" + JWKS_URI,
                        "honjeong.oauth.kakao.app-key=" + APP_KEY,
                        "honjeong.apple.issuer=" + APPLE_ISSUER,
                        "honjeong.apple.jwks-uri=" + APPLE_JWKS_URI,
                        "honjeong.apple.client-id=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure().rootCause()
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("honjeong.apple.client-id");
                });
    }

    private void assertMockAssembled(AssertableApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(OAuthVerifier.class);
        assertThat(context.getBean(OAuthVerifier.class)).isInstanceOf(MockOAuthVerifier.class);
    }
}
