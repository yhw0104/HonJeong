package com.honjeong.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
     * 계정 생성 가능). 이 설정값은 이 프로젝트에서 가장 결과가 큰 설정이다.
     *
     * <p>이 테스트가 지키는 것은 <b>yml의 기본값</b> 하나뿐이다 — 환경변수가 그 위에 얹히는 경로는
     * 범위 밖이고, 실제 사고는 정확히 그 경로로 났다. 그쪽은 {@link OAuthRealModeCheck}가 값 자체를
     * 부팅에서 확인한다(아래 {@code prod에서_oauth모드가_mock이면_기동실패}). 두 테스트는 서로를
     * 대체하지 않는다: 여기는 "안전한 기본값", 저기는 "덮어써도 안 뜬다"를 맡는다.
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

    // --- 애플 토큰 클라이언트가 "정확히 하나" 뜨는지 ---
    //
    // 위 테스트들은 후보를 한쪽만 등록해 돌리므로, 두 @ConditionalOnProperty가 서로 배타적인지를
    // 증명하지 못한다. 아래 두 테스트는 mock·real 후보를 <b>동시에</b> 올리고 mode 값 하나만 다르게 둔다.

    /**
     * {@link AppleTokenClient}를 생성자로 요구하는 소비자. 실제로는 {@code AuthService}가 이 역할이다
     * (가입 시 애플 코드를 교환하려고 주입받는다). AuthService를 그대로 올리려면 저장소·서비스 열넷을
     * 함께 세워야 해 이 파일의 목적(배선만 가볍게 보기)에서 벗어나므로, 같은 모양의 최소 빈으로 대신한다.
     */
    @Configuration(proxyBeanMethods = false)
    static class AppleTokenClientConsumer {
        private final AppleTokenClient appleTokenClient;

        AppleTokenClientConsumer(AppleTokenClient appleTokenClient) {
            this.appleTokenClient = appleTokenClient;
        }

        AppleTokenClient client() {
            return appleTokenClient;
        }
    }

    /**
     * mock·real 후보와 소비자를 함께 올리는 러너. 자격증명은 real이 실제로 조립될 수 있게 채워 둔다 —
     * 이 러너를 쓰는 두 테스트는 오직 {@code honjeong.apple.mode} 값 하나만 다르다(대조군/실험군).
     */
    private ApplicationContextRunner appleCandidatesRunner() {
        return prodRunner
                .withUserConfiguration(NoopAppleTokenClient.class, RealAppleTokenClient.class,
                        AppleTokenClientConsumer.class)
                .withPropertyValues("honjeong.apple.team-id=TEAM123456",
                        "honjeong.apple.key-id=KEY1234567",
                        "honjeong.apple.private-key-base64=" + applePrivateKeyBase64);
    }

    /**
     * mode를 지정하지 않은 <b>prod 기본값</b> 그대로 돌린다 — 운영 서버가 실제로 뜨는 조건이다.
     * 후보 둘이 동시에 뜨지도(주입 모호), 둘 다 빠지지도(주입 불가) 않고 정확히 하나여야 한다.
     */
    @Test
    @DisplayName("prod 기본값에서 애플 토큰 클라이언트가 정확히 하나 조립된다 — 후보 둘이 겹치거나 둘 다 빠지지 않는다")
    void prod기본값에서_애플토큰클라이언트가_정확히_하나다() {
        appleCandidatesRunner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AppleTokenClient.class);
            // prod 기본값은 real이다(application-prod.yml) — mock으로 조용히 떨어지지 않았음도 함께 고정한다.
            assertThat(context).hasSingleBean(RealAppleTokenClient.class);
            assertThat(context).doesNotHaveBean(NoopAppleTokenClient.class);
            assertThat(context.getBean(AppleTokenClientConsumer.class).client()).isNotNull();
        });
    }

    /**
     * ★위 대조군과 {@code honjeong.apple.mode} 값 하나만 다르다. "rea1"은 mock에도 real에도 걸리지 않아
     * 빈이 <b>0개</b>가 된다.
     *
     * <p>여기서 고정하는 명제는 정확히 이것이다 — <b>빈이 0개면 이 빈을 생성자로 받는 어떤 소비자든
     * 기동에 실패한다</b>. 즉 오타는 "애플 연동만 조용히 죽은 서버"가 아니라 즉시 드러나는 부팅 실패로
     * 나타난다(빈값 자격증명 가드와 같은 fail-closed 방향).
     *
     * <p>실패를 내는 소비자는 이 파일의 {@link AppleTokenClientConsumer}지 {@code AuthService}가 아니다.
     * 그러니 이 테스트는 "운영이 반드시 죽는다"까지는 보장하지 못한다 — {@code AuthService}의 의존이
     * 나중에 {@code ObjectProvider}나 {@code @Nullable}로 느슨해지면 이 테스트는 초록인 채로 운영만
     * 애플 없이 조용히 뜰 수 있다. 그때는 이 테스트가 아니라 그 변경을 의심할 것.
     */
    @Test
    @DisplayName("honjeong.apple.mode에 오타가 있으면 기동에 실패한다 — 빈 0개인 채로 조용히 뜨지 않는다")
    void 애플mode에_오타가_있으면_기동실패() {
        appleCandidatesRunner().withPropertyValues("honjeong.apple.mode=rea1").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context).getFailure().rootCause()
                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                    .hasMessageContaining("AppleTokenClient");
        });
    }

    // --- 두 모드 스위치의 정합성 ---
    //
    // 위 테스트들은 honjeong.apple.mode 하나만 본다. 그런데 애플 기능의 나머지 반쪽(로그인 검증기)은
    // honjeong.oauth.mode로 켜진다. 두 설정이 어긋난 조합을 부팅에서 거부하는지는 별도 문제다.

    /**
     * {@link AppleModeConsistencyCheck}만 올리는 러너. <b>prod 프로파일</b>이고(prodRunner) oauth.mode는
     * prod 기본값(real)을 그대로 쓴다 — 아래 두 테스트는 오직 {@code honjeong.apple.mode} 값 하나만
     * 다르다(대조군/실험군).
     */
    private ApplicationContextRunner modeConsistencyRunner() {
        return prodRunner.withUserConfiguration(AppleModeConsistencyCheck.class);
    }

    @Test
    @DisplayName("oauth.mode=real + apple.mode=real이면 정상 기동한다(아래 기동실패 테스트의 대조군)")
    void 두_모드가_모두real이면_기동한다() {
        modeConsistencyRunner().withPropertyValues("honjeong.apple.mode=real")
                .run(context -> assertThat(context).hasNotFailed());
    }

    /**
     * ★위 대조군과 {@code honjeong.apple.mode} 값 하나만 다르다.
     *
     * <p>이 조합({@code oauth.mode=real} + {@code apple.mode=mock})은 <b>고장이 아니라 설정값 하나로</b>
     * 만들어지고, 만들어지고 나면 아무 신호도 남기지 않는다 — 애플 로그인은 성공하고, 가입은 매번
     * refresh token 없이 저장되고, 탈퇴는 폐기를 건너뛴다. 심사 지침 5.1.1(v) 위반 상태 그대로 배포된다.
     * 그래서 "부팅이 실패한다"만이 아니라 <b>실패 이유</b>까지 고정한다(다른 이유로 죽은 컨텍스트가
     * {@code hasFailed()}만으로 통과해 이 가드가 사라진 걸 놓치지 않도록).
     */
    @Test
    @DisplayName("prod에서 oauth.mode=real인데 apple.mode가 real이 아니면 기동에 실패한다 — 두 스위치가 어긋난 채로 뜨지 않는다")
    void prod에서_두_모드가_어긋나면_기동실패() {
        modeConsistencyRunner().withPropertyValues("honjeong.apple.mode=mock").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context).getFailure().rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("honjeong.oauth.mode=real")
                    .hasMessageContaining("honjeong.apple.mode=mock");
        });
    }

    /**
     * ★위 실험군과 <b>프로파일 하나만</b> 다르다(local). 같은 어긋난 조합인데 여기서는 떠야 한다.
     *
     * <p>가드에서 local을 뺀 이유를 고정하는 테스트다: 로컬에는 카카오 로그인만 실서버로 검증하려고
     * {@code OAUTH_MODE=real}을 켜는 워크플로가 있고, 전 프로파일에서 검사하면 카카오만 보려는
     * 개발자에게 운영 애플 서명키(.p8)를 노트북에 두라고 요구하게 된다. 막으려는 규정 위반은 배포에서만
     * 생긴다(로컬 DB의 계정은 심사 대상이 아니다). 이 단언이 없으면 {@link DeployedProfiles}의 제외
     * 목록이 조용히 사라져도 아무도 모르고, 그 회귀는 테스트가 아니라 개발자의 로컬 기동 실패로 드러난다.
     *
     * <p>같은 이유로 {@link OAuthRealModeCheck}도 로컬에서는 돌지 않는다 — 카카오 키 없이 서버만
     * 띄워 화면을 확인하는 것이 로컬의 정상 워크플로다.
     *
     * <p>로컬 풀스택 경로({@code docker compose up -d})는 여전히 검사 대상이다 — compose가
     * {@code SPRING_PROFILES_ACTIVE=prod}로 띄우기 때문이다(위 실험군이 그 경로를 덮는다).
     */
    @Test
    @DisplayName("local 프로파일에서는 배포 가드가 등록되지 않는다 — 로컬 카카오 real 검증에 애플 .p8을 요구하지 않는다")
    void local에서는_배포가드가_등록되지_않는다() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withUserConfiguration(AppleModeConsistencyCheck.class, OAuthRealModeCheck.class)
                .withPropertyValues("spring.profiles.active=local",
                        "honjeong.oauth.mode=real", "honjeong.apple.mode=mock")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AppleModeConsistencyCheck.class);
                    assertThat(context).doesNotHaveBean(OAuthRealModeCheck.class);
                });
    }

    // --- 소셜 토큰 검증 스위치 자체(honjeong.oauth.mode) ---
    //
    // 위 정합성 가드는 "oauth.mode=real일 때 apple.mode도 real인가"만 본다. 그런데 이 프로젝트에서
    // 결과가 가장 큰 설정은 oauth.mode 그 자체다 — 배포에서 이 값이 mock이면 서버는 소셜 ID 토큰을
    // 검증하지 않고 문자열을 그대로 신뢰한다(임의 문자열로 남의 계정 생성·탈취 가능).

    /** {@link OAuthRealModeCheck}만 올리는 러너. 아래 두 테스트는 oauth.mode 값 하나만 다르다. */
    private ApplicationContextRunner oauthRealModeRunner() {
        return prodRunner.withUserConfiguration(OAuthRealModeCheck.class);
    }

    @Test
    @DisplayName("prod 기본값(oauth.mode=real)에서는 정상 기동한다(아래 기동실패 테스트의 대조군)")
    void prod기본값에서_oauth가드를_통과한다() {
        oauthRealModeRunner().run(context -> assertThat(context).hasNotFailed());
    }

    /**
     * ★위 대조군과 {@code honjeong.oauth.mode} 값 하나만 다르다.
     *
     * <p>2026-07-27에 실제로 열려 있던 상태를 부팅에서 거부한다. 그때는 compose의
     * {@code OAUTH_MODE:-mock} 기본값이 prod yml의 real 선언을 조용히 덮어써, 배포 서버가 소셜 토큰을
     * 검증하지 않는 채로 떠 있었다. 이 파일의 {@code prod에서_oauth모드_기본값은_real이다}는 <b>yml의
     * 기본값</b>만 지킨다 — 환경변수 한 줄로 그 기본값을 덮는 경로는 그 테스트의 범위 밖이고, 실제
     * 사고도 정확히 그 경로로 났다. 여기서 그 경로를 닫는다.
     */
    @Test
    @DisplayName("prod에서 OAUTH_MODE=mock으로 내리면 기동에 실패한다 — 2026-07-27 인증 우회를 환경변수 한 줄로 되살릴 수 없다")
    void prod에서_oauth모드가_mock이면_기동실패() {
        oauthRealModeRunner().withPropertyValues("honjeong.oauth.mode=mock").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context).getFailure().rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("honjeong.oauth.mode=mock");
        });
    }

    // --- 가드가 도는 프로파일 집합(DeployedProfiles) ---
    //
    // 위 테스트들은 전부 prod에서 돈다. 그래서 가드가 @Profile("prod")로 좁혀져 있어도 모두 통과한다 —
    // 즉 "prod에서 돈다"는 고정하지만 "prod가 아닌 배포 환경에서도 도는가"는 아무도 지키지 않는다.
    // 아래 두 테스트가 그 명제를 맡는다.

    /**
     * 이 저장소에 <b>아직 존재하지 않는</b> 프로파일 이름으로 도는 러너. application.yml을 읽지 않으므로
     * (ConfigData 초기화 없음) honjeong.* 값은 각 테스트가 직접 준다.
     */
    private ApplicationContextRunner unknownProfileRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withPropertyValues("spring.profiles.active=staging");
    }

    /**
     * ★가드를 {@code @Profile("prod")}로 두면 이 테스트만 빨개진다. 그 형태는 <b>허용이 기본</b>이라,
     * 나중에 staging·canary 같은 프로파일이 생기면 검사가 조용히 빠진 채로 배포된다 — 그리고 그 사실은
     * 심사나 사고로만 드러난다. {@link DeployedProfiles}는 반대로 <b>거부가 기본</b>이라, 새 프로파일은
     * 자동으로 검사 대상이 된다(제외하려면 명시적으로 이름을 적어야 한다).
     */
    @Test
    @DisplayName("prod가 아닌 새 프로파일에서도 oauth 가드가 돈다 — 프로파일을 새로 만들어 검사를 빠져나갈 수 없다")
    void 새_프로파일에서도_oauth가드가_돈다() {
        unknownProfileRunner().withUserConfiguration(OAuthRealModeCheck.class).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context).getFailure().rootCause()
                    .isInstanceOf(IllegalArgumentException.class)
                    // 값을 주지 않았으니 @Value의 기본값(mock)이 걸린 것이다 —
                    // 설정이 통째로 빠진 새 환경도 "검증 없음"으로 뜨지 못한다.
                    .hasMessageContaining("honjeong.oauth.mode=mock");
        });
    }

    /** 위와 같은 명제를 정합성 가드 쪽에서 고정한다. */
    @Test
    @DisplayName("prod가 아닌 새 프로파일에서도 두 모드 정합성 가드가 돈다")
    void 새_프로파일에서도_정합성가드가_돈다() {
        unknownProfileRunner()
                .withUserConfiguration(AppleModeConsistencyCheck.class)
                .withPropertyValues("honjeong.oauth.mode=real", "honjeong.apple.mode=mock")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure().rootCause()
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("honjeong.apple.mode=mock");
                });
    }
}
