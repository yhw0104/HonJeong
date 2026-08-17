package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.honjeong.auth.domain.Provider;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * AppleOAuthVerifier 단위 테스트. 외부 호출 없이 테스트용 RSA 키쌍으로 직접 서명한 ID 토큰으로
 * 검증 규칙(서명·발급자·대상·만료)만 확인한다. KakaoOAuthVerifierTest와 같은 구조다.
 */
class AppleOAuthVerifierTest {

    private static final String ISSUER = "https://appleid.apple.com";
    private static final String CLIENT_ID = "com.honjeong.app";

    private static RSAPublicKey publicKey;
    private static RSAPrivateKey privateKey;
    private static RSAPrivateKey otherPrivateKey;

    @BeforeAll
    static void generateKeys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) pair.getPublic();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        otherPrivateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
    }

    private AppleOAuthVerifier verifier() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(AppleOAuthVerifier.validators(ISSUER, CLIENT_ID));
        return AppleOAuthVerifier.withDecoder(decoder);
    }

    private String signedToken(String issuer, String audience, Instant expiresAt, RSAPrivateKey key)
            throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("001234.abcdef0123456789.0000")
                .issueTime(new Date())
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    @Test
    @DisplayName("정상 토큰: sub(애플 사용자 식별자)를 소셜 식별자로 돌려준다")
    void 정상토큰은_sub를_식별자로_준다() throws Exception {
        String token = signedToken(ISSUER, CLIENT_ID, Instant.now().plusSeconds(600), privateKey);

        OAuthIdentity identity = verifier().verify(Provider.APPLE, token);

        assertThat(identity.provider()).isEqualTo(Provider.APPLE);
        assertThat(identity.providerUserId()).isEqualTo("001234.abcdef0123456789.0000");
    }

    @Test
    @DisplayName("★이메일은 토큰에 있어도 버린다 — 처리방침이 '수집하지 않는다'고 적혀 있다")
    void 이메일은_버린다() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER).audience(CLIENT_ID).subject("001234.abc.0000")
                .claim("email", "someone@privaterelay.appleid.com")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(600)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims);
        jwt.sign(new RSASSASigner(privateKey));

        OAuthIdentity identity = verifier().verify(Provider.APPLE, jwt.serialize());

        assertThat(identity.email()).isNull();
    }

    @Test
    @DisplayName("★보안 핵심: 다른 앱(aud 불일치) 토큰은 거부한다")
    void 다른앱_토큰은_거부한다() throws Exception {
        String token = signedToken(ISSUER, "com.someone.else", Instant.now().plusSeconds(600), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.APPLE, token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("발급자(iss)가 애플이 아니면 거부한다")
    void 발급자가_다르면_거부한다() throws Exception {
        String token = signedToken("https://evil.example.com", CLIENT_ID, Instant.now().plusSeconds(600), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.APPLE, token))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void 만료토큰은_거부한다() throws Exception {
        String token = signedToken(ISSUER, CLIENT_ID, Instant.now().minusSeconds(60), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.APPLE, token))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("다른 키로 서명한(위조) 토큰은 거부한다")
    void 위조서명은_거부한다() throws Exception {
        String token = signedToken(ISSUER, CLIENT_ID, Instant.now().plusSeconds(600), otherPrivateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.APPLE, token))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("카카오는 이 검증기가 처리하지 않는다 — 조용히 통과시키지 않고 거부한다")
    void 카카오는_거부한다() throws Exception {
        String token = signedToken(ISSUER, CLIENT_ID, Instant.now().plusSeconds(600), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.KAKAO, token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
