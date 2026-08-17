package com.honjeong.auth.client;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.util.Assert;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * 애플 토큰 엔드포인트에 보낼 client secret(JWT)을 만든다.
 *
 * <p>애플은 다른 OAuth 공급자와 달리 <b>고정 문자열 시크릿을 쓰지 않는다.</b> 개발자 포털에서 받은
 * Sign in with Apple 키(.p8)로 ES256 서명한 짧은 수명의 JWT를 매 호출마다 만들어 보내야 한다.
 *
 * <p>키는 {@code .p8} 파일 전체를 base64로 인코딩한 값으로 주입받는다
 * ({@code FIREBASE_CREDENTIALS_BASE64}와 같은 방식 — 줄바꿈이 든 파일을 환경변수로 안전하게 나르기 위함).
 *
 * <p>HTTP를 모르는 순수 로직이라 {@link RealAppleTokenClient}와 분리돼 있다 — 서명 규격은 네트워크
 * 없이 단위 테스트로 못 박고, 이 클래스는 로그를 남기지 않는다(개인키가 흘러나갈 자리를 만들지 않는다).
 */
public class AppleClientSecretFactory {

    /** 애플이 요구하는 client secret 수명 상한은 6개월이지만, 매 호출 생성이라 짧게 잡는다. */
    private static final long TTL_SECONDS = 300;
    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    private final String teamId;
    private final String clientId;
    private final String keyId;
    private final PrivateKey privateKey;

    /**
     * @param teamId           Apple Developer 팀 ID
     * @param clientId         번들 ID
     * @param keyId            Sign in with Apple 키 ID
     * @param privateKeyBase64 .p8 파일 전체를 base64로 인코딩한 값
     * @throws IllegalArgumentException 값이 비었거나 키를 읽을 수 없을 때 — 애플에 호출을 보내 보고
     *         알게 되는 게 아니라 이 자리에서 즉시 드러낸다
     */
    public AppleClientSecretFactory(String teamId, String clientId, String keyId, String privateKeyBase64) {
        Assert.hasText(teamId, "honjeong.apple.team-id가 비어 있습니다. 환경변수 APPLE_TEAM_ID를 채워주세요.");
        Assert.hasText(clientId, "honjeong.apple.client-id가 비어 있습니다.");
        Assert.hasText(keyId, "honjeong.apple.key-id가 비어 있습니다. 환경변수 APPLE_KEY_ID를 채워주세요.");
        Assert.hasText(privateKeyBase64,
                "honjeong.apple.private-key-base64가 비어 있습니다. .p8 파일을 base64로 인코딩해 "
                        + "APPLE_PRIVATE_KEY_BASE64에 넣어주세요.");
        this.teamId = teamId;
        this.clientId = clientId;
        this.keyId = keyId;
        this.privateKey = readPrivateKey(privateKeyBase64);
    }

    /**
     * base64(.p8 PEM) → PEM 텍스트 → 헤더/개행 제거 → DER → EC 개인키.
     *
     * <p>실패 메시지에 입력값을 절대 싣지 않는다 — 그 입력이 곧 개인키다.
     */
    private static PrivateKey readPrivateKey(String privateKeyBase64) {
        try {
            String pem = new String(Base64.getDecoder().decode(privateKeyBase64.trim()));
            String der = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] bytes = Base64.getDecoder().decode(der);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "애플 개인키(.p8)를 읽지 못했습니다. APPLE_PRIVATE_KEY_BASE64가 .p8 파일 전체를 "
                            + "base64로 인코딩한 값인지 확인해주세요.", e);
        }
    }

    /**
     * 이번 호출에 쓸 client secret JWT를 만든다.
     *
     * @return 직렬화된 JWT
     * @throws IllegalStateException 서명에 실패했을 때 — 호출자({@link RealAppleTokenClient})가
     *         다른 실패와 똑같이 삼킨다
     */
    public String create() {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(teamId)
                .subject(clientId)
                .audience(List.of(APPLE_AUDIENCE))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(TTL_SECONDS)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(), claims);
            jwt.sign(new ECDSASigner(privateKey, Curve.P_256));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("애플 client secret 생성에 실패했습니다.", e);
        }
    }
}
