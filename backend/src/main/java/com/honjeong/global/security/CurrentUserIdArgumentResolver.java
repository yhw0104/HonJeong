package com.honjeong.global.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * 1. 기능: @CurrentUserId가 붙은 Long 파라미터에 SecurityContext의 JWT sub(userId)를 꺼내 주입하는 MVC 인자 리졸버
 * 2. 사용처: WebConfig(addArgumentResolvers)가 등록 — @CurrentUserId를 쓰는 모든 컨트롤러 핸들러에 적용
 *
 * <p>[기존 주석] {@link CurrentUserId}가 붙은 Long 파라미터에 JWT sub(userId)를 주입한다.
 * 인증 컨텍스트가 없거나 JWT principal이 아니면 401.
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 기능: 이 리졸버가 해당 파라미터를 처리할지 판단한다 — @CurrentUserId + Long/long 타입일 때만 대상
     * Request: parameter — 검사할 컨트롤러 메서드 파라미터
     * Response: boolean — 처리 대상 여부
     *
     * <p>[기존 주석] 이 리졸버가 해당 파라미터를 처리할지 판단한다.
     * {@code @CurrentUserId}가 붙어 있고 타입이 {@code Long} 또는 {@code long}일 때만 true.
     *
     * @param parameter 검사할 컨트롤러 메서드 파라미터
     * @return 처리 대상이면 true
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && (parameter.getParameterType().equals(Long.class)
                        || parameter.getParameterType().equals(long.class));
    }

    /**
     * 기능: SecurityContext의 JWT principal에서 sub 클레임을 사용자 id(Long)로 변환해 파라미터 값으로 반환한다
     * Request: parameter·mavContainer·webRequest·binderFactory — MVC 리졸버 규약 인자(실제로는 SecurityContext만 사용)
     * Response: Object(Long) — JWT sub를 파싱한 사용자 id / 인증 정보가 없거나 JWT가 아니면 BusinessException(UNAUTHORIZED, 401)
     *
     * <p>[기존 주석] SecurityContext에 있는 JWT principal의 sub 클레임을 사용자 id(Long)로 변환해 주입한다.
     * 인증 컨텍스트가 없거나 principal이 JWT가 아니면 {@link ErrorCode#UNAUTHORIZED}(401)를 던진다.
     *
     * @return JWT sub를 파싱한 사용자 id(Long)
     * @throws BusinessException 인증 정보가 없거나 JWT principal이 아닐 때(UNAUTHORIZED)
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 인증이 없거나(principal 미존재) JWT 토큰 인증이 아니면 사용자 id를 알 수 없으므로 401 처리.
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // sub 클레임에는 발급 시 넣은 userId 문자열이 들어 있으므로 Long으로 되돌린다.
        return Long.valueOf(jwt.getSubject());
    }
}
