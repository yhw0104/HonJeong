package com.honjeong.auth.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.honjeong.auth.domain.Provider;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * 1. 기능: 카카오 실검증 OAuth 구현체 — 카카오가 발급한 OIDC ID 토큰의 서명·발급자·대상·만료를 검증해 신원 추출
 *    (honjeong.oauth.mode=real일 때만 등록되어 {@link MockOAuthVerifier}를 대체)
 * 2. 사용처: AuthService(oauthLogin)
 *
 * <p><b>aud 검증이 보안의 핵심이다.</b> 사용자 정보 조회 API만 호출하는 방식은 "다른 앱에서 발급된 카카오 토큰"으로도
 * 우리 서비스에 가입할 수 있다. ID 토큰의 aud(대상 앱)를 우리 앱 키와 대조해 그 경로를 막는다.
 *
 * <p>공개키(JWKS)는 {@link NimbusJwtDecoder}가 캐시하므로 로그인마다 카카오에 조회하지 않는다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.oauth.mode", havingValue = "real")
public class KakaoOAuthVerifier implements OAuthVerifier {

    private final JwtDecoder decoder;

    /**
     * 운영용 — 설정값으로 카카오 JWKS 기반 디코더를 만든다.
     *
     * @param issuer  발급자(카카오 고정값)
     * @param jwksUri 공개키 목록 주소
     * @param appKey  ID 토큰의 aud로 기대하는 우리 앱 키
     */
    public KakaoOAuthVerifier(@Value("${honjeong.oauth.kakao.issuer}") String issuer,
            @Value("${honjeong.oauth.kakao.jwks-uri}") String jwksUri,
            @Value("${honjeong.oauth.kakao.app-key}") String appKey) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        nimbus.setJwtValidator(validators(issuer, appKey));
        this.decoder = nimbus;
    }

    private KakaoOAuthVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    /** 테스트용 — 로컬 키로 만든 디코더를 주입한다(외부 호출 없이 검증 규칙만 확인). */
    static KakaoOAuthVerifier withDecoder(JwtDecoder decoder) {
        return new KakaoOAuthVerifier(decoder);
    }

    /**
     * 검증 규칙: 발급자·만료(스프링 기본) + <b>대상(aud)이 우리 앱 키인지</b>.
     * 운영과 테스트가 같은 규칙을 쓰도록 여기 한 곳에서만 정의한다.
     */
    static OAuth2TokenValidator<Jwt> validators(String issuer, String appKey) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(appKey)));
    }

    /**
     * 기능: 카카오 ID 토큰 검증 → 신원 추출
     * Request: provider — KAKAO만 지원, idToken — 카카오 OIDC ID 토큰
     * Response: OAuthIdentity — providerUserId=sub(카카오 회원번호), email=null(수집 안 함)
     *
     * @throws BusinessException INVALID_INPUT(미지원 공급자) 또는 UNAUTHORIZED(토큰 검증 실패)
     */
    @Override
    public OAuthIdentity verify(Provider provider, String idToken) {
        if (provider != Provider.KAKAO) {
            // 애플 로그인은 미구현 — 성공으로 위장하지 않는다.
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아직 지원하지 않는 소셜 로그인입니다.");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            return new OAuthIdentity(Provider.KAKAO, jwt.getSubject(), null);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 로그인 검증에 실패했어요.");
        }
    }
}
