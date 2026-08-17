package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
 * KakaoOAuthVerifier 단위 테스트. 외부 호출 없이, 테스트용 RSA 키쌍으로 직접 서명한 ID 토큰을 넣어
 * 검증 규칙(서명·발급자·대상·만료)만 확인한다. aud 검증은 "다른 앱 토큰으로 가입" 취약점을 막는 핵심이라
 * 반드시 포함한다.
 */
class KakaoOAuthVerifierTest {

    private static final String ISSUER = "https://kauth.kakao.com";
    private static final String APP_KEY = "our-app-key";

    private static RSAPublicKey publicKey;
    private static RSAPrivateKey privateKey;
    private static RSAPublicKey otherPublicKey;
    private static RSAPrivateKey otherPrivateKey;

    @BeforeAll
    static void generateKeys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) pair.getPublic();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        var otherPair = generator.generateKeyPair();
        otherPublicKey = (RSAPublicKey) otherPair.getPublic();
        otherPrivateKey = (RSAPrivateKey) otherPair.getPrivate();
    }

    /** 우리 공개키 + 실제 검증 규칙(iss/aud/exp)을 그대로 쓰는 디코더 — 운영과 같은 규칙을 테스트한다. */
    private KakaoOAuthVerifier verifier() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        decoder.setJwtValidator(KakaoOAuthVerifier.validators(ISSUER, APP_KEY));
        return KakaoOAuthVerifier.withDecoder(decoder);
    }

    private String signedToken(String issuer, String audience, Instant expiresAt, RSAPrivateKey key)
            throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("1234567890")
                .issueTime(new Date())
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    @Test
    @DisplayName("정상 토큰: sub(카카오 회원번호)를 소셜 식별자로 돌려준다")
    void 정상토큰은_sub를_식별자로_준다() throws Exception {
        String token = signedToken(ISSUER, APP_KEY, Instant.now().plusSeconds(600), privateKey);

        OAuthIdentity identity = verifier().verify(Provider.KAKAO, token);

        assertThat(identity.provider()).isEqualTo(Provider.KAKAO);
        assertThat(identity.providerUserId()).isEqualTo("1234567890");
        assertThat(identity.email()).isNull(); // 이메일은 수집하지 않는다(비즈앱 필요)
    }

    @Test
    @DisplayName("★보안 핵심: 다른 앱(aud 불일치) 토큰은 거부한다")
    void 다른앱_토큰은_거부한다() throws Exception {
        String token = signedToken(ISSUER, "someone-elses-app-key", Instant.now().plusSeconds(600), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.KAKAO, token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("발급자(iss)가 카카오가 아니면 거부한다")
    void 발급자가_다르면_거부한다() throws Exception {
        String token = signedToken("https://evil.example.com", APP_KEY, Instant.now().plusSeconds(600), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.KAKAO, token))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void 만료토큰은_거부한다() throws Exception {
        String token = signedToken(ISSUER, APP_KEY, Instant.now().minusSeconds(60), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.KAKAO, token))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("다른 키로 서명한(위조) 토큰은 거부한다")
    void 위조서명은_거부한다() throws Exception {
        String token = signedToken(ISSUER, APP_KEY, Instant.now().plusSeconds(600), otherPrivateKey);
        assertThat(otherPublicKey).isNotEqualTo(publicKey); // 서로 다른 키쌍임을 확인

        assertThatThrownBy(() -> verifier().verify(Provider.KAKAO, token))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("애플은 이 검증기가 처리하지 않는다 — 조용히 통과시키지 않고 거부한다(라우팅은 DelegatingOAuthVerifier가 한다)")
    void 애플은_거부한다() throws Exception {
        String token = signedToken(ISSUER, APP_KEY, Instant.now().plusSeconds(600), privateKey);

        assertThatThrownBy(() -> verifier().verify(Provider.APPLE, token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
