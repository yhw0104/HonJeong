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
 * 애플 실검증 OAuth 구현체 — Sign in with Apple이 발급한 ID 토큰의 서명·발급자·대상·만료를 검증해
 * 신원을 추출한다. {@code honjeong.oauth.mode=real}일 때만 등록되며,
 * {@link DelegatingOAuthVerifier}가 APPLE 요청을 여기로 보낸다.
 *
 * <p><b>aud 검증이 보안의 핵심이다.</b> 다른 앱에서 발급된 애플 토큰으로 우리 서비스에 가입하는 경로를
 * aud(번들 ID) 대조로 막는다. {@link KakaoOAuthVerifier}와 같은 이유·같은 구조다.
 *
 * <p>★<b>이메일을 읽지 않는다.</b> 애플은 사용자가 허용하면 토큰에 email 클레임을 실어 주지만, 우리는
 * 수집하지 않기로 했고 그 사실을 2026-08-14 게시한 개인정보 처리방침에 적었다. 앱이 스코프를 아예
 * 요청하지 않으므로 보통 클레임 자체가 없지만, 있더라도 여기서 버린다.
 *
 * <p>공개키(JWKS)는 {@link NimbusJwtDecoder}가 캐시하므로 로그인마다 애플에 조회하지 않는다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.oauth.mode", havingValue = "real")
public class AppleOAuthVerifier implements OAuthVerifier {

    private static final Logger log = LoggerFactory.getLogger(AppleOAuthVerifier.class);

    private final JwtDecoder decoder;

    /**
     * 운영용 — 설정값으로 애플 JWKS 기반 디코더를 만든다.
     *
     * @param issuer   발급자(애플 고정값)
     * @param jwksUri  공개키 목록 주소
     * @param clientId ID 토큰의 aud로 기대하는 번들 ID
     */
    // KakaoOAuthVerifier와 같은 이유로 @Autowired를 명시한다 — 생성자가 둘이면 스프링이
    // 기본 생성자를 찾다가 부팅에 실패한다.
    @Autowired
    public AppleOAuthVerifier(@Value("${honjeong.apple.issuer}") String issuer,
            @Value("${honjeong.apple.jwks-uri}") String jwksUri,
            @Value("${honjeong.apple.client-id}") String clientId) {
        Assert.hasText(issuer, "honjeong.apple.issuer 설정이 비어 있습니다.");
        Assert.hasText(jwksUri, "honjeong.apple.jwks-uri 설정이 비어 있습니다.");
        Assert.hasText(clientId,
                "honjeong.apple.client-id 설정이 비어 있습니다. 환경변수 APPLE_CLIENT_ID(번들 ID)를 채워주세요.");
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        nimbus.setJwtValidator(validators(issuer, clientId));
        this.decoder = nimbus;
    }

    private AppleOAuthVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    /** 테스트용 — 로컬 키로 만든 디코더를 주입한다(외부 호출 없이 검증 규칙만 확인). */
    static AppleOAuthVerifier withDecoder(JwtDecoder decoder) {
        return new AppleOAuthVerifier(decoder);
    }

    /**
     * 검증 규칙: 발급자·만료(스프링 기본) + <b>대상(aud)이 우리 번들 ID인지</b>.
     * 운영과 테스트가 같은 규칙을 쓰도록 여기 한 곳에서만 정의한다.
     */
    static OAuth2TokenValidator<Jwt> validators(String issuer, String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(clientId)));
    }

    /**
     * 애플 ID 토큰을 검증해 신원을 추출한다.
     *
     * @param provider 소셜 공급자 — APPLE만 지원한다
     * @param idToken  애플 ID 토큰
     * @return providerUserId=sub(애플 사용자 식별자), email=null(수집하지 않음)
     * @throws BusinessException INVALID_INPUT(미지원 공급자) 또는 UNAUTHORIZED(토큰 검증 실패)
     */
    @Override
    public OAuthIdentity verify(Provider provider, String idToken) {
        if (provider != Provider.APPLE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아직 지원하지 않는 소셜 로그인입니다.");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            // email은 의도적으로 읽지 않는다(위 클래스 주석 참고).
            return new OAuthIdentity(Provider.APPLE, jwt.getSubject(), null);
        } catch (JwtException e) {
            // ID 토큰 원문은 자격증명이라 절대 로깅하지 않고, 예외 메시지만 남긴다.
            log.warn("애플 ID 토큰 검증 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "애플 로그인 검증에 실패했어요.");
        }
    }
}
