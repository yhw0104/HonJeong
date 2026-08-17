package com.honjeong.auth.service;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.honjeong.auth.domain.Provider;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * 공급자별 검증기로 넘기는 위임 구현체. {@code honjeong.oauth.mode=real}일 때 카카오·애플 검증기가
 * <b>둘 다</b> 등록되므로, {@code AuthService}가 주입받을 단일 빈이 필요하다.
 *
 * <p><b>왜 이 방식인가</b>: {@code AuthService}는 {@code OAuthVerifier} 하나를 주입받도록 이미
 * 작성돼 있다. 여기에 {@code @Primary}를 붙이면 <b>AuthService를 한 줄도 고치지 않고</b> 공급자를
 * 늘릴 수 있다. mock 모드에서는 이 빈이 등록되지 않고 {@code MockOAuthVerifier} 하나만 남으므로
 * 기존 동작이 그대로 유지된다.
 */
@Component
@Primary
@ConditionalOnProperty(name = "honjeong.oauth.mode", havingValue = "real")
public class DelegatingOAuthVerifier implements OAuthVerifier {

    private final Map<Provider, OAuthVerifier> byProvider = new EnumMap<>(Provider.class);

    public DelegatingOAuthVerifier(KakaoOAuthVerifier kakao, AppleOAuthVerifier apple) {
        byProvider.put(Provider.KAKAO, kakao);
        byProvider.put(Provider.APPLE, apple);
    }

    /**
     * 공급자에 맞는 검증기로 넘긴다.
     *
     * @throws BusinessException INVALID_INPUT — provider가 null이거나 등록된 검증기가 없을 때.
     *         조용히 아무 검증기로 보내면 "다른 공급자 토큰으로 가입"이 열리므로 명시적으로 거부한다.
     */
    @Override
    public OAuthIdentity verify(Provider provider, String idToken) {
        OAuthVerifier verifier = provider == null ? null : byProvider.get(provider);
        if (verifier == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "아직 지원하지 않는 소셜 로그인입니다.");
        }
        return verifier.verify(provider, idToken);
    }
}
