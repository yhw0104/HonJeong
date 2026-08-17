package com.honjeong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.honjeong.auth.client.AppleTokenClient;
import com.honjeong.auth.client.NoopAppleTokenClient;
import com.honjeong.auth.client.RealAppleTokenClient;
import com.honjeong.auth.service.FixedVerificationCodeGenerator;
import com.honjeong.auth.service.MockSmsSender;
import com.honjeong.auth.service.SmsSender;
import com.honjeong.auth.service.VerificationCodeGenerator;
import com.honjeong.file.storage.FileStorage;
import com.honjeong.file.storage.LocalFileStorage;
import com.honjeong.push.service.FcmPushSender;
import com.honjeong.push.service.NoopPushSender;
import com.honjeong.push.service.PushSender;

/**
 * prod 프로파일 설정으로 외부연동(SMS·파일저장·푸시·애플 토큰) 빈이 실제로 조립되는지 확인하는 회귀 테스트.
 *
 * <p><b>배경</b>: mock 구현체들은 {@code @ConditionalOnProperty(havingValue="mock", matchIfMissing=true)}로
 * 등록되는데, application-prod.yml이 이들을 {@code real}로 선언한 시기가 있었다. real 구현체가 존재하지
 * 않으므로 빈이 하나도 등록되지 않아 {@code AuthService}의 생성자 주입이 실패하고 컨텍스트가 죽었다 —
 * {@code docker compose up -d}가 100% 실패했다.
 *
 * <p>이 테스트는 <b>yml 파일 자체</b>를 읽어 검증한다({@link ConfigDataApplicationContextInitializer}가
 * application.yml + application-prod.yml을 Environment에 로드한다). 누군가 prod 기본값을 다시 real로
 * 되돌리면 여기서 잡힌다. 전체 {@code @SpringBootTest}는 DB(Testcontainers)까지 띄워 무겁고 이 결함과
 * 무관하므로 {@link ApplicationContextRunner}로 빈 조립만 가볍게 본다.
 */
class ExternalIntegrationWiringTest {

    // prod 프로파일의 application.yml + application-prod.yml을 실제로 로드하는 러너.
    // @Value 플레이스홀더(${...}) 해석에는 PropertySourcesPlaceholderConfigurer가 필요하다
    // (일반 앱은 자동 등록되지만 ApplicationContextRunner는 직접 추가해야 한다).
    private final ApplicationContextRunner prodRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=prod")
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(
                    MockSmsSender.class,
                    FixedVerificationCodeGenerator.class,
                    LocalFileStorage.class,
                    NoopPushSender.class);

    @Test
    @DisplayName("prod 프로파일에서 SMS 빈이 조립된다(부팅 불가 회귀 방지)")
    void prod에서_SMS빈이_조립된다() {
        prodRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SmsSender.class);
            assertThat(context).hasSingleBean(VerificationCodeGenerator.class);
        });
    }

    @Test
    @DisplayName("prod 프로파일에서 파일저장 빈이 조립된다")
    void prod에서_파일저장빈이_조립된다() {
        prodRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(FileStorage.class);
        });
    }

    @Test
    @DisplayName("SMS_MODE=real로 덮어쓰면 SMS 빈이 사라진다(real 미구현 사실을 고정)")
    void SMS_MODE가_real이면_빈이_없다() {
        prodRunner.withPropertyValues("honjeong.sms.mode=real").run(context -> {
            assertThat(context).doesNotHaveBean(SmsSender.class);
            assertThat(context).doesNotHaveBean(VerificationCodeGenerator.class);
        });
    }

    @Test
    @DisplayName("FILES_BASE_URL 환경변수가 파일 공개 URL을 덮어쓴다(배포 시 사진이 열리는 근거)")
    void FILES_BASE_URL이_적용된다() {
        prodRunner
                .withPropertyValues("FILES_BASE_URL=https://example.test/files")
                .withUserConfiguration(BaseUrlProbe.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(BaseUrlProbe.class).baseUrl)
                            .isEqualTo("https://example.test/files");
                });
    }

    /** honjeong.files.base-url이 최종적으로 어떤 값으로 해석되는지만 들여다보는 테스트용 빈. */
    @Configuration(proxyBeanMethods = false)
    static class BaseUrlProbe {
        @Value("${honjeong.files.base-url}")
        String baseUrl;
    }

    /**
     * prod 프로파일의 honjeong.oauth.mode 기본값이 real로 고정돼 있는지 확인한다.
     *
     * <p><b>배경</b>: 2026-07-27, compose의 {@code OAUTH_MODE:-mock} 기본값이 이 yml의 real 선언을
     * 조용히 덮어써 카카오 로그인이 검증 없이(mock) 통과하는 열린 인증 우회가 있었다(임의 문자열로
     * 계정 생성 가능). 이 설정값은 이 프로젝트에서 가장 결과가 큰 설정이라, 지금은 소스 코드 주석
     * ({@code docker-compose.yml}의 {@code OAUTH_MODE} 블록)으로만 지켜지고 있다. 이 yml 자체가
     * real이 아닌 다른 값으로 되돌아가면 여기서 잡는다(compose 쪽 우회는 이 테스트의 범위 밖).
     */
    @Test
    @DisplayName("prod 프로파일에서 honjeong.oauth.mode 기본값이 real로 고정된다(2026-07-27 조용한 mock 우회 회귀 방지)")
    void prod에서_oauth모드_기본값은_real이다() {
        prodRunner
                .withUserConfiguration(OAuthModeProbe.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(OAuthModeProbe.class).mode).isEqualTo("real");
                });
    }

    /** honjeong.oauth.mode가 prod 프로파일에서 최종적으로 어떤 값으로 해석되는지만 들여다보는 테스트용 빈. */
    @Configuration(proxyBeanMethods = false)
    static class OAuthModeProbe {
        @Value("${honjeong.oauth.mode}")
        String mode;
    }

    @Test
    @DisplayName("prod에서 푸시 발송기가 조립된다")
    void prod에서_푸시빈이_조립된다() {
        prodRunner.withPropertyValues("honjeong.push.mode=mock").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PushSender.class);
        });
    }

    @Test
    @DisplayName("push.mode=real인데 자격증명이 없으면 기동에 실패한다 — 조용히 mock으로 떨어지지 않는다")
    void real인데_자격증명이_없으면_기동실패() {
        prodRunner
                .withUserConfiguration(FcmPushSender.class)
                .withPropertyValues("honjeong.push.mode=real", "honjeong.push.credentials-base64=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("push.mode=real이면 mock 발송기가 등록되지 않는다")
    void real이면_mock빈이_없다() {
        prodRunner.withPropertyValues("honjeong.push.mode=real")
                .run(context -> assertThat(context).doesNotHaveBean(NoopPushSender.class));
    }

    // --- 애플 토큰 클라이언트 배선 ---

    /**
     * real 대조군에 넣을 자격증명. .p8 개인키를 저장소에 커밋하지 않으려고 실행할 때마다 새로 만든다
     * (운영에 넣는 값과 같은 모양: PEM 파일 전체를 base64로 인코딩한 값).
     */
    private static String applePrivateKeyBase64;

    @BeforeAll
    static void generateApplePrivateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        applePrivateKeyBase64 = Base64.getEncoder().encodeToString(pem.getBytes());
    }

    /**
     * apple.mode=real로 {@link RealAppleTokenClient}를 올리는 러너. <b>자격증명 셋만</b> 인자로 받는다 —
     * 아래 두 테스트가 정확히 그 값들만 다르게 두고 돌기 위한 장치다(대조군/실험군).
     */
    private ApplicationContextRunner appleRealRunner(String teamId, String keyId, String privateKeyBase64) {
        return prodRunner
                .withUserConfiguration(RealAppleTokenClient.class)
                .withPropertyValues("honjeong.apple.mode=real",
                        "honjeong.apple.team-id=" + teamId,
                        "honjeong.apple.key-id=" + keyId,
                        "honjeong.apple.private-key-base64=" + privateKeyBase64);
    }

    @Test
    @DisplayName("apple.mode 기본(mock)에서 애플 토큰 클라이언트가 조립된다")
    void 애플_토큰클라이언트가_조립된다() {
        prodRunner
                .withPropertyValues("honjeong.apple.mode=mock")
                .withUserConfiguration(NoopAppleTokenClient.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AppleTokenClient.class);
                });
    }

    @Test
    @DisplayName("apple.mode=real이고 자격증명이 채워져 있으면 정상 기동한다(아래 기동실패 테스트의 대조군)")
    void 애플real이고_자격증명이_있으면_기동한다() {
        appleRealRunner("TEAM123456", "KEY1234567", applePrivateKeyBase64).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AppleTokenClient.class);
        });
    }

    /**
     * ★이 테스트는 위 대조군과 <b>자격증명 값 셋만</b> 다르다. 그래서 실패는 다른 배선 문제가 아니라
     * {@code AppleClientSecretFactory}의 빈값 가드가 낸 것이다 — 근거를 예외 타입·메시지로 한 번 더 고정한다.
     * ({@code hasFailed()}만 단언하면 엉뚱한 이유로 죽은 컨텍스트도 통과한다 — 이 브랜치의 카카오
     * 회귀 테스트가 실제로 그렇게 통과하고 있었다.)
     */
    @Test
    @DisplayName("apple.mode=real인데 자격증명이 없으면 기동에 실패한다 — 조용히 mock으로 떨어지지 않는다")
    void 애플real인데_자격증명이_없으면_기동실패() {
        appleRealRunner("", "", "").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context).getFailure().rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("honjeong.apple.team-id");
        });
    }
}
