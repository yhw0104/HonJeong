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

    // @Value 플레이스홀더(${...}) 해석에는 PropertySourcesPlaceholderConfigurer가 필요하다
    // (일반 스프링 부트 앱은 자동 등록되지만, ApplicationContextRunner는 직접 추가해야 한다).
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations
                    .of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(MockOAuthVerifier.class, KakaoOAuthVerifier.class);

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
    @DisplayName("mode=real + 필수값을 채우면 KakaoOAuthVerifier만 조립된다(부팅 실패 회귀 방지)")
    void mode_real시_Kakao만_조립된다() {
        contextRunner
                .withPropertyValues(
                        "honjeong.oauth.mode=real",
                        "honjeong.oauth.kakao.issuer=" + ISSUER,
                        "honjeong.oauth.kakao.jwks-uri=" + JWKS_URI,
                        "honjeong.oauth.kakao.app-key=" + APP_KEY)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OAuthVerifier.class);
                    assertThat(context.getBean(OAuthVerifier.class)).isInstanceOf(KakaoOAuthVerifier.class);
                });
    }

    @Test
    @DisplayName("mode=real인데 app-key가 비어 있으면 부팅이 실패한다(Assert.hasText 검사 회귀 방지)")
    void mode_real_appKey가_비어있으면_부팅실패한다() {
        contextRunner
                .withPropertyValues(
                        "honjeong.oauth.mode=real",
                        "honjeong.oauth.kakao.issuer=" + ISSUER,
                        "honjeong.oauth.kakao.jwks-uri=" + JWKS_URI,
                        "honjeong.oauth.kakao.app-key=")
                .run(context -> assertThat(context).hasFailed());
    }

    private void assertMockAssembled(AssertableApplicationContext context) {
        assertThat(context).hasNotFailed();
        assertThat(context).hasSingleBean(OAuthVerifier.class);
        assertThat(context.getBean(OAuthVerifier.class)).isInstanceOf(MockOAuthVerifier.class);
    }
}
