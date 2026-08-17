package com.honjeong.auth.client;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 애플 토큰 엔드포인트 실연동. {@code honjeong.apple.mode=real}일 때 등록된다.
 *
 * <p>★<b>실패를 예외로 퍼뜨리지 않는다.</b> 이 클래스의 두 호출은 모두 사용자의 핵심 동작(가입·탈퇴)에
 * 딸린 부수 작업이다. 애플 서버 장애 때문에 가입이 막히거나 탈퇴가 막히면 안 된다 — 실패는 로그로만 남긴다.
 * client secret 생성(서명)까지 try 안에 두는 이유도 같다: 키 문제로 던져진 예외가 새어 나가면
 * 그 순간 가입·탈퇴가 통째로 막힌다.
 *
 * <p>★<b>로그에 자격증명을 싣지 않는다.</b> client secret·refresh token·개인키는 어느 로그 문장에도
 * 넣지 않고, 실패 시에도 예외 메시지(응답 상태·본문)만 남긴다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.apple.mode", havingValue = "real")
public class RealAppleTokenClient implements AppleTokenClient {

    private static final Logger log = LoggerFactory.getLogger(RealAppleTokenClient.class);

    /** 응답이 안 오는 것도 "실패"다 — 타임아웃이 없으면 애플이 느릴 때 가입·탈퇴 요청이 그대로 매달린다. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final AppleClientSecretFactory secretFactory;
    private final String clientId;
    private final String tokenUri;
    private final String revokeUri;

    /**
     * @param clientId         번들 ID(애플에 보내는 client_id)
     * @param teamId           Apple Developer 팀 ID
     * @param keyId            Sign in with Apple 키 ID
     * @param privateKeyBase64 .p8 파일 전체를 base64로 인코딩한 값
     * @param tokenUri         토큰 교환 주소
     * @param revokeUri        토큰 폐기 주소
     */
    // 생성자가 (아래 테스트용 private 생성자와) 둘이라 스프링이 어느 쪽을 쓸지 스스로 정하지 못하고
    // 기본(무인자) 생성자를 찾다가 부팅에 실패한다("No default constructor found").
    // @Autowired로 이 생성자를 쓰도록 명시한다(KakaoOAuthVerifier·FcmPushSender와 같은 이유).
    @Autowired
    public RealAppleTokenClient(@Value("${honjeong.apple.client-id}") String clientId,
            @Value("${honjeong.apple.team-id}") String teamId,
            @Value("${honjeong.apple.key-id}") String keyId,
            @Value("${honjeong.apple.private-key-base64}") String privateKeyBase64,
            @Value("${honjeong.apple.token-uri}") String tokenUri,
            @Value("${honjeong.apple.revoke-uri}") String revokeUri) {
        // 자격증명이 비어 있으면 여기서 부팅이 실패한다(fail-closed) — 조용히 mock으로 떨어지면
        // "배포는 됐는데 탈퇴 시 애플 토큰만 안 지워지는" 상태가 되고 그건 심사에서야 드러난다.
        this.secretFactory = new AppleClientSecretFactory(teamId, clientId, keyId, privateKeyBase64);
        this.restClient = RestClient.builder().requestFactory(requestFactory()).build();
        this.clientId = clientId;
        this.tokenUri = tokenUri;
        this.revokeUri = revokeUri;
        log.info("[APPLE] real 모드 — 애플 토큰을 교환·폐기합니다.");
    }

    private RealAppleTokenClient(RestClient restClient, AppleClientSecretFactory secretFactory,
            String clientId, String tokenUri, String revokeUri) {
        this.restClient = restClient;
        this.secretFactory = secretFactory;
        this.clientId = clientId;
        this.tokenUri = tokenUri;
        this.revokeUri = revokeUri;
    }

    /**
     * 테스트용 — 목 서버에 물린 {@link RestClient}를 주입한다(네트워크 없이 요청 형식과 실패 처리
     * 규칙만 확인). 운영 생성자는 애플 주소로 직접 나가므로 테스트가 그 경로를 가로챌 수 없고,
     * {@code RestClient.Builder} 빈 주입도 이 프로젝트에서는 불가능하다(위 {@link #requestFactory()}
     * 주석 참고) — {@code KakaoOAuthVerifier.withDecoder}와 같은 방식으로 협력자를 열어 둔다.
     */
    static RealAppleTokenClient withRestClient(RestClient restClient, AppleClientSecretFactory secretFactory,
            String clientId, String tokenUri, String revokeUri) {
        return new RealAppleTokenClient(restClient, secretFactory, clientId, tokenUri, revokeUri);
    }

    /**
     * RestClient를 직접 만든다.
     *
     * <p>{@code RestClient.Builder} 빈을 주입받지 않는 이유: 그 빈을 등록하는
     * {@code RestClientAutoConfiguration}이 이 프로젝트 클래스패스에 없다(스프링 부트 4가 별도
     * 모듈로 분리했고 {@code spring-boot-starter-webmvc}는 끌어오지 않는다). 주입을 시도하면
     * 기동에 실패한다. 의존성을 늘리는 대신 정적 팩토리로 만든다.
     */
    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /** 애플 응답에서 refresh_token만 꺼내 쓴다. 나머지 필드(access_token·id_token 등)는 무시한다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@JsonProperty("refresh_token") String refreshToken) {
    }

    /**
     * authorizationCode를 refresh token으로 교환한다.
     *
     * @param authorizationCode 앱이 넘긴 1회용 코드
     * @return refresh token, 실패하면 null(가입은 그대로 진행된다)
     */
    @Override
    public String exchangeRefreshToken(String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            return null;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", secretFactory.create());
            form.add("code", authorizationCode);
            form.add("grant_type", "authorization_code");
            TokenResponse response = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            return response == null ? null : response.refreshToken();
        } catch (Exception e) {
            // 가입을 막지 않는다 — 토큰만 못 받고 넘어간다(탈퇴 시 revoke를 건너뛰게 된다).
            log.warn("애플 authorizationCode 교환 실패 — refresh token 없이 진행합니다: {}", e.getMessage());
            return null;
        }
    }

    /**
     * refresh token을 폐기한다. 실패해도 조용히 넘어간다.
     *
     * @param refreshToken 폐기할 토큰(없으면 아무 것도 하지 않는다)
     */
    @Override
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", secretFactory.create());
            form.add("token", refreshToken);
            form.add("token_type_hint", "refresh_token");
            restClient.post()
                    .uri(revokeUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // ★탈퇴를 막지 않는다. 사용자의 탈퇴권이 애플 서버 가용성에 인질로 잡히면 안 된다.
            log.warn("애플 토큰 폐기 실패 — 탈퇴는 그대로 진행합니다: {}", e.getMessage());
        }
    }
}
