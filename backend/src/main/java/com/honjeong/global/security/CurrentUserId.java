package com.honjeong.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 현재 인증된 사용자 id(JWT의 sub 클레임)를 DB 조회 없이 주입하는 마커
 * 애너테이션.
 *
 * <p>사용처: 인증이 필요한 전 도메인 컨트롤러의 핸들러 파라미터. 실제 주입은
 * {@link CurrentUserIdArgumentResolver}(WebConfig가 등록)가 수행하며, Long 타입 파라미터에만 붙인다.
 *
 * <p>런타임에 리졸버가 읽어야 하므로 {@code RUNTIME} 보존, 파라미터에만 적용되도록 {@code PARAMETER}
 * 타깃을 둔다.
 *
 * <p>사용 예: {@code public XxxResponse me(@CurrentUserId Long userId) { ... }}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
