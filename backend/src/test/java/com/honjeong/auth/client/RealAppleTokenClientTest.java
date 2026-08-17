package com.honjeong.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.nimbusds.jwt.SignedJWT;

/**
 * 애플 토큰 클라이언트의 <b>계약</b> 테스트 — 이 브랜치의 핵심 약속 둘이 여기 걸려 있다.
 *
 * <ul>
 *   <li>{@code exchangeRefreshToken}은 어떤 실패에도 null만 돌려준다 → <b>가입이 계속 성공한다</b>
 *   <li>{@code revoke}는 어떤 실패도 삼킨다 → <b>탈퇴가 끝까지 완료된다</b>
 * </ul>
 *
 * <p>애플로 나가는 요청 본문(form 필드)도 함께 고정한다. 필드 하나가 빠지거나 이름이 틀리면 운영에서만
 * 조용히 실패하는데, 그 실패는 위 두 약속 때문에 로그 한 줄로만 남아 아무도 모르게 지나간다.
 *
 * <p>운영 생성자는 애플 주소로 곧장 나가므로, {@code RealAppleTokenClient.withRestClient}로
 * 목 서버에 물린 RestClient를 넣어 네트워크 없이 확인한다.
 */
class RealAppleTokenClientTest {

    private static final String CLIENT_ID = "com.honjeong.app";
    private static final String TOKEN_URI = "https://appleid.apple.com/auth/token";
    private static final String REVOKE_URI = "https://appleid.apple.com/auth/revoke";

    private static AppleClientSecretFactory secretFactory;

    private MockRestServiceServer server;
    private RealAppleTokenClient client;

    @BeforeAll
    static void createSecretFactory() throws Exception {
        // 실제 서명 경로를 그대로 태운다 — client_secret이 "우리가 서명한 JWT"임을 아래에서 확인하기 위함.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        secretFactory = new AppleClientSecretFactory("TEAM123456", CLIENT_ID, "KEY1234567",
                Base64.getEncoder().encodeToString(pem.getBytes()));
    }

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = RealAppleTokenClient.withRestClient(
                builder.build(), secretFactory, CLIENT_ID, TOKEN_URI, REVOKE_URI);
    }

    @Test
    @DisplayName("교환 성공 시 refresh_token을 꺼내 돌려주고, 애플 규격대로 네 필드를 보낸다")
    void 교환에_성공하면_토큰을_돌려준다() {
        server.expect(requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(formIs(Map.of(
                        "client_id", CLIENT_ID,
                        "code", "APPLE_CODE",
                        "grant_type", "authorization_code")))
                .andRespond(withSuccess("""
                        {"access_token":"a","token_type":"Bearer","expires_in":3600,
                         "refresh_token":"r-1","id_token":"i"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.exchangeRefreshToken("APPLE_CODE")).isEqualTo("r-1");
        server.verify();
    }

    @Test
    @DisplayName("★교환이 4xx로 실패해도 예외를 던지지 않고 null을 준다 — 가입은 그대로 성공해야 한다")
    void 교환이_4xx여도_가입을_막지_않는다() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));

        assertThatCode(() -> assertThat(client.exchangeRefreshToken("APPLE_CODE")).isNull())
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("★교환이 5xx로 실패해도 예외를 던지지 않고 null을 준다 — 애플 장애가 가입을 막으면 안 된다")
    void 교환이_5xx여도_가입을_막지_않는다() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

        assertThatCode(() -> assertThat(client.exchangeRefreshToken("APPLE_CODE")).isNull())
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("200인데 응답에 refresh_token이 없으면 null을 준다 — 없는 토큰을 지어내지 않는다")
    void 응답에_토큰이_없으면_null이다() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"a\",\"token_type\":\"Bearer\"}",
                        MediaType.APPLICATION_JSON));

        assertThatCode(() -> assertThat(client.exchangeRefreshToken("APPLE_CODE")).isNull())
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("폐기 시 애플 규격대로 네 필드를 보낸다")
    void 폐기는_규격대로_보낸다() {
        server.expect(requestTo(REVOKE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(formIs(Map.of(
                        "client_id", CLIENT_ID,
                        "token", "r-1",
                        "token_type_hint", "refresh_token")))
                .andRespond(withSuccess());

        assertThatCode(() -> client.revoke("r-1")).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("★폐기가 5xx로 실패해도 예외를 던지지 않는다 — 탈퇴가 애플 가용성에 인질로 잡히면 안 된다")
    void 폐기가_5xx여도_탈퇴를_막지_않는다() {
        server.expect(requestTo(REVOKE_URI)).andRespond(withServerError());

        assertThatCode(() -> client.revoke("r-1")).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("★폐기가 4xx로 실패해도 예외를 던지지 않는다 — 이미 폐기된 토큰도 탈퇴를 막지 못한다")
    void 폐기가_4xx여도_탈퇴를_막지_않는다() {
        server.expect(requestTo(REVOKE_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_token\"}"));

        assertThatCode(() -> client.revoke("r-1")).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("보관된 토큰이 없으면(null·공백) 애플을 아예 부르지 않는다")
    void 토큰이_없으면_호출하지_않는다() {
        // 기대 요청을 하나도 등록하지 않았다 — 호출이 나가면 목 서버가 AssertionError로 이 테스트를 깬다.
        assertThatCode(() -> {
            client.revoke(null);
            client.revoke("   ");
        }).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("코드가 없으면(null·공백) 애플을 아예 부르지 않고 null을 준다")
    void 코드가_없으면_호출하지_않는다() {
        assertThatCode(() -> {
            assertThat(client.exchangeRefreshToken(null)).isNull();
            assertThat(client.exchangeRefreshToken("   ")).isNull();
        }).doesNotThrowAnyException();
        server.verify();
    }

    /**
     * 애플로 나가는 form 본문을 뜯어본다: 필드 집합이 정확히 일치하는지(빠짐·군더더기 모두 잡는다),
     * 고정값 필드가 기대값인지, 그리고 {@code client_secret}이 우리 키로 서명된 JWT인지.
     *
     * <p>{@code client_secret}만 값 비교를 못 한다 — 매 호출 새로 서명해 값이 매번 다르기 때문이다.
     */
    private static RequestMatcher formIs(Map<String, String> expectedFixedFields) {
        return request -> {
            MultiValueMap<String, String> form = parseForm((MockClientHttpRequest) request);

            Set<String> expectedKeys = new LinkedHashSet<>(expectedFixedFields.keySet());
            expectedKeys.add("client_secret");
            assertThat(form.keySet()).containsExactlyInAnyOrderElementsOf(expectedKeys);

            expectedFixedFields.forEach((key, value) -> assertThat(form.getFirst(key)).as(key).isEqualTo(value));
            assertThat(subjectOf(form.getFirst("client_secret")))
                    .as("client_secret의 sub")
                    .isEqualTo(CLIENT_ID);
        };
    }

    private static MultiValueMap<String, String> parseForm(MockClientHttpRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (String pair : request.getBodyAsString().split("&")) {
            int separator = pair.indexOf('=');
            form.add(URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8));
        }
        return form;
    }

    private static String subjectOf(String clientSecret) {
        try {
            return SignedJWT.parse(clientSecret).getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new AssertionError("client_secret이 파싱 가능한 JWT가 아닙니다.", e);
        }
    }
}
