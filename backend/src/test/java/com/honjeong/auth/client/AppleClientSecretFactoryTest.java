package com.honjeong.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;

/**
 * client secret JWT 생성 로직 단위 테스트. 애플에 실제로 보내는 값이므로 클레임과 서명이
 * 애플 규격에 맞는지 확인한다(iss=팀 ID, sub=클라이언트 ID, aud=애플, kid 헤더, ES256).
 */
class AppleClientSecretFactoryTest {

    private static final String TEAM_ID = "TEAM123456";
    private static final String CLIENT_ID = "com.honjeong.app";
    private static final String KEY_ID = "KEY1234567";

    private static String privateKeyBase64;
    private static ECPublicKey publicKey;

    @BeforeAll
    static void generateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair pair = generator.generateKeyPair();
        publicKey = (ECPublicKey) pair.getPublic();
        // 운영에서 넣는 값과 같은 모양을 만든다: .p8(PEM) 파일 전체를 base64로 인코딩한 값
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        privateKeyBase64 = Base64.getEncoder().encodeToString(pem.getBytes());
    }

    private AppleClientSecretFactory factory() {
        return new AppleClientSecretFactory(TEAM_ID, CLIENT_ID, KEY_ID, privateKeyBase64);
    }

    @Test
    @DisplayName("애플 규격대로 클레임을 담는다: iss=팀 ID, sub=클라이언트 ID, aud=애플")
    void 클레임이_애플규격이다() throws Exception {
        SignedJWT jwt = SignedJWT.parse(factory().create());

        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo(TEAM_ID);
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(CLIENT_ID);
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("https://appleid.apple.com");
    }

    @Test
    @DisplayName("헤더에 kid를 싣고 ES256으로 서명한다 — 애플이 어느 키인지 못 찾으면 거부한다")
    void 헤더가_ES256과_kid다() throws Exception {
        SignedJWT jwt = SignedJWT.parse(factory().create());

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo(KEY_ID);
    }

    @Test
    @DisplayName("개인키에 대응하는 공개키로 서명이 검증된다")
    void 서명이_유효하다() throws Exception {
        SignedJWT jwt = SignedJWT.parse(factory().create());

        assertThat(jwt.verify(new ECDSAVerifier(publicKey))).isTrue();
    }

    @Test
    @DisplayName("만료가 미래로 설정된다")
    void 만료가_미래다() throws Exception {
        SignedJWT jwt = SignedJWT.parse(factory().create());

        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("키가 비어 있으면 생성 시점에 실패한다 — 애플 호출 후에 알게 되지 않는다")
    void 키가_비면_실패한다() {
        assertThatThrownBy(() -> new AppleClientSecretFactory(TEAM_ID, CLIENT_ID, KEY_ID, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
