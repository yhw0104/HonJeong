package com.honjeong.auth.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.util.Assert;

import com.honjeong.auth.domain.Provider;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * 카카오 실검증 OAuth 구현체 — 카카오가 발급한 OIDC ID 토큰의 서명·발급자·대상·만료를 검증해 신원을
 * 추출한다. {@code honjeong.oauth.mode=real}일 때만 등록되어 {@link MockOAuthVerifier}를 대체한다.
 *
 * <p>사용처: AuthService(oauthLogin).
 *
 * <p><b>aud 검증이 보안의 핵심이다.</b> 사용자 정보 조회 API만 호출하는 방식은 "다른 앱에서 발급된 카카오 토큰"으로도
 * 우리 서비스에 가입할 수 있다. ID 토큰의 aud(대상 앱)를 우리 앱 키와 대조해 그 경로를 막는다.
 *
 * <p>공개키(JWKS)는 {@link NimbusJwtDecoder}가 캐시하므로 로그인마다 카카오에 조회하지 않는다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.oauth.mode", havingValue = "real")
public class KakaoOAuthVerifier implements OAuthVerifier {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthVerifier.class);

    private final JwtDecoder decoder;

    /**
     * 운영용 — 설정값으로 카카오 JWKS 기반 디코더를 만든다.
     *
     * <p>{@code app-key}는 기본값이 빈 문자열(`${KAKAO_APP_KEY:}`)이라, 환경변수를 채우지 않고
     * mode=real로 띄우면 aud 검증({@code aud.contains("")})이 항상 false가 되어 <b>모든 로그인이
     * 조용히 401</b>로 실패한다. 그 고장을 런타임이 아니라 부팅 시점에 드러내기 위해 필수값을 검사한다.
     *
     * @param issuer  발급자(카카오 고정값)
     * @param jwksUri 공개키 목록 주소
     * @param appKey  ID 토큰의 aud로 기대하는 우리 앱 키
     */
    // 생성자가 (아래 테스트용 private 생성자와) 둘이라 스프링이 어느 쪽을 쓸지 스스로 정하지 못하고
    // 기본(무인자) 생성자를 찾다가 부팅에 실패한다("No default constructor found"). @Autowired로
    // 이 생성자를 쓰도록 명시한다.
    @Autowired
    public KakaoOAuthVerifier(@Value("${honjeong.oauth.kakao.issuer}") String issuer,
            @Value("${honjeong.oauth.kakao.jwks-uri}") String jwksUri,
            @Value("${honjeong.oauth.kakao.app-key}") String appKey) {
        Assert.hasText(issuer, "honjeong.oauth.kakao.issuer 설정이 비어 있습니다.");
        Assert.hasText(jwksUri, "honjeong.oauth.kakao.jwks-uri 설정이 비어 있습니다.");
        Assert.hasText(appKey,
                "honjeong.oauth.kakao.app-key 설정이 비어 있습니다. 환경변수 KAKAO_APP_KEY를 채워주세요"
                        + "(카카오 개발자 콘솔 > 앱 설정 > 앱 키 > REST API 키).");
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
     * 카카오 ID 토큰을 검증해 신원을 추출한다.
     *
     * @param provider 소셜 공급자 — KAKAO만 지원한다
     * @param idToken 카카오 OIDC ID 토큰
     * @return providerUserId=sub(카카오 회원번호), email=null(수집하지 않음)
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
            // aud 불일치/만료/위조 서명/JWKS 조회 실패/앱 키 오설정이 모두 여기로 모이므로 원인을 남긴다.
            // ID 토큰 원문은 자격증명이라 절대 로깅하지 않고, 예외 메시지만 남긴다.
            log.warn("카카오 ID 토큰 검증 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 로그인 검증에 실패했어요.");
        }
    }
}
