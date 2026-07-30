package com.honjeong.global.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.honjeong.global.security.CurrentUserIdArgumentResolver;

/**
 * 스프링 MVC 커스터마이즈 — {@code @CurrentUserId} 인자 리졸버 등록 + 업로드 파일({@code /files/**})
 * 로컬 정적 서빙 매핑.
 *
 * <p>사용처: 스프링 MVC가 자동 적용한다(직접 참조 없음). 효과는 {@code @CurrentUserId}를 쓰는 모든
 * 컨트롤러와 프로필 사진 등 파일 URL 응답에 미친다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // LocalFileStorage가 저장하는 디렉터리와 동일(같은 기본값). 운영(S3)에선 이 정적 서빙이 불필요하다.
    @Value("${honjeong.files.local-dir:./uploads}")
    private String fileLocalDir;

    /**
     * 커스텀 핸들러 메서드 인자 리졸버를 등록한다.
     *
     * <p>{@link CurrentUserIdArgumentResolver}를 추가해 컨트롤러의 {@code @CurrentUserId Long} 파라미터에
     * JWT의 사용자 id가 주입되도록 한다.
     *
     * @param resolvers 스프링 MVC가 사용하는 인자 리졸버 목록(여기에 우리 리졸버를 더한다)
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserIdArgumentResolver());
    }

    /**
     * 업로드된 파일(프로필 사진 등)을 {@code /files/**} 경로로 정적 서빙한다.
     *
     * <p>개발(mock) 저장소가 돌려준 URL이 실제로 열리도록 로컬 디렉터리를 매핑한다.
     * 운영에선 S3가 직접 서빙하므로 불필요하다.
     *
     * @param registry 리소스 핸들러 레지스트리
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(fileLocalDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }
}
