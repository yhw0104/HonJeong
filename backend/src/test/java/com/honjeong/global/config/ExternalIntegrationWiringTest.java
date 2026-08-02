package com.honjeong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.honjeong.auth.service.FixedVerificationCodeGenerator;
import com.honjeong.auth.service.MockSmsSender;
import com.honjeong.auth.service.SmsSender;
import com.honjeong.auth.service.VerificationCodeGenerator;
import com.honjeong.file.storage.FileStorage;
import com.honjeong.file.storage.LocalFileStorage;
import com.honjeong.geo.service.MockReverseGeocoder;
import com.honjeong.geo.service.ReverseGeocoder;

/**
 * prod 프로파일 설정으로 외부연동(SMS·역지오코딩·파일저장) 빈이 실제로 조립되는지 확인하는 회귀 테스트.
 *
 * <p><b>배경</b>: mock 구현체들은 {@code @ConditionalOnProperty(havingValue="mock", matchIfMissing=true)}로
 * 등록되는데, application-prod.yml이 이들을 {@code real}로 선언한 시기가 있었다. real 구현체가 존재하지
 * 않으므로 빈이 하나도 등록되지 않아 {@code AuthService}·{@code GeoService}의 생성자 주입이 실패하고
 * 컨텍스트가 죽었다 — {@code docker compose up -d}가 100% 실패했다.
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
                    MockReverseGeocoder.class,
                    LocalFileStorage.class);

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
    @DisplayName("prod 프로파일에서 역지오코딩 빈이 조립된다(부팅 불가 회귀 방지)")
    void prod에서_역지오코딩빈이_조립된다() {
        prodRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ReverseGeocoder.class);
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
}
