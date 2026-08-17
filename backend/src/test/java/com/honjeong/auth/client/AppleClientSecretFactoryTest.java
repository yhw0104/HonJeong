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
        KeyPair pair = generateKeyPair("secp256r1");
        publicKey = (ECPublicKey) pair.getPublic();
        privateKeyBase64 = encodeAsP8(pair.getPrivate().getEncoded());
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

    /**
     * ★예외 타입만 단언하면 이 테스트는 <b>아무 것도 지키지 못한다</b>. 생성자의
     * {@code Assert.hasText(privateKeyBase64, ...)}를 지워도 빈 문자열은 그대로
     * {@code readPrivateKey}로 흘러들어가 {@code KeyFactory.generatePrivate}에서 터지고, 그것도
     * {@code IllegalArgumentException}으로 다시 감싸져 나온다 — 타입이 같아 테스트는 초록이다
     * (실제로 확인했다). 그래서 <b>어느 가드가 잡았는지</b>를 메시지로 못 박는다.
     */
    @Test
    @DisplayName("키가 비어 있으면 생성 시점에 실패한다 — 애플 호출 후에 알게 되지 않는다")
    void 키가_비면_실패한다() {
        assertThatThrownBy(() -> new AppleClientSecretFactory(TEAM_ID, CLIENT_ID, KEY_ID, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("honjeong.apple.private-key-base64가 비어 있습니다");
    }

    /**
     * ★잘못된 곡선의 키는 <b>예외를 던져 주지 않는다</b> — 그래서 이 테스트가 필요하다.
     *
     * <p>P-384 키를 줘도 파싱은 물론 <b>서명까지 예외 없이 성공한다</b>(JCA가 SHA256withECDSA로
     * 서명해 준다). 다만 그 서명은 ES256 규격이 아니라서 애플이 거부한다. 즉 "부팅 성공 → 매 호출
     * 조용히 거부"가 되고, 그 거부는 {@link RealAppleTokenClient}가 설계상 삼키므로(가입·탈퇴를
     * 막으면 안 되니까) 로그 한 줄로만 남는다. 배포는 멀쩡해 보이는데 애플 토큰만 영영 안 지워지고,
     * 그건 심사에서야 드러난다.
     *
     * <p>그래서 생성자는 "서명이 되는지"가 아니라 <b>곡선이 P-256인지</b>를 본다.
     */
    @Test
    @DisplayName("P-256이 아닌 키는 파싱도 서명도 되지만 애플이 거부한다 — 생성 시점에 세운다")
    void 곡선이_다른키는_생성시점에_실패한다() throws Exception {
        String wrongCurveKey = encodeAsP8(generateKeyPair("secp384r1").getPrivate().getEncoded());

        assertThatThrownBy(() -> new AppleClientSecretFactory(TEAM_ID, CLIENT_ID, KEY_ID, wrongCurveKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("P-256");
    }

    private static KeyPair generateKeyPair(String curve) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    /** 운영에서 넣는 값과 같은 모양으로 감싼다: .p8(PEM) 파일 전체를 base64로 인코딩한 값. */
    private static String encodeAsP8(byte[] pkcs8) {
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pkcs8)
                + "\n-----END PRIVATE KEY-----\n";
        return Base64.getEncoder().encodeToString(pem.getBytes());
    }
}
