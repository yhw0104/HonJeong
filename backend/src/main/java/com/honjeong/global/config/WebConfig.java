package com.honjeong.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.honjeong.global.security.CurrentUserIdArgumentResolver;

/** MVC 커스터마이즈 — {@code @CurrentUserId} 인자 리졸버 등록. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 커스텀 핸들러 메서드 인자 리졸버를 등록한다.
     * {@link CurrentUserIdArgumentResolver}를 추가해 컨트롤러의 {@code @CurrentUserId Long} 파라미터에
     * JWT의 사용자 id가 주입되도록 한다.
     *
     * @param resolvers 스프링 MVC가 사용하는 인자 리졸버 목록(여기에 우리 리졸버를 더한다)
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserIdArgumentResolver());
    }
}
