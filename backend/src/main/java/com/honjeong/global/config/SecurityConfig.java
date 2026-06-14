package com.honjeong.global.config;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 무상태 JWT 보안(Spring Security 7). 토큰의 {@code typ} 클레임을 권한으로 매핑한다:
 * typ=access → ROLE_USER, typ=onboarding → ROLE_ONBOARDING.
 *
 * <p>인가: 인증/헬스는 공개, 온보딩 엔드포인트는 ONBOARDING|USER, 그 외 전부 USER.
 * (⚠️ {@code anyRequest().authenticated()}를 쓰면 온보딩 토큰이 일반 API를 통과하므로 USER로 게이팅.)
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(HonjeongJwtProperties.class)
public class SecurityConfig {

    /**
     * 자체 발급 JWT(access·onboarding)의 생성·검증을 담당하는 {@link JwtProvider}를 빈으로 등록한다.
     * 시크릿·TTL은 {@link HonjeongJwtProperties}에서 받고, 시각은 UTC Clock으로 고정한다.
     *
     * @param props honjeong.jwt.* 설정값(시크릿·access/onboarding TTL)
     * @return 구성된 JwtProvider
     */
    @Bean
    JwtProvider jwtProvider(HonjeongJwtProperties props) {
        return new JwtProvider(props.secret(), props.accessTokenTtlSeconds(),
                props.onboardingTokenTtlSeconds(), Clock.systemUTC());
    }

    /**
     * 리소스 서버(oauth2ResourceServer().jwt())가 들어온 토큰을 검증할 때 쓸 {@link JwtDecoder} 빈.
     * JwtProvider가 가진 디코더(같은 대칭키·HS256)를 그대로 노출해 발급과 검증의 키를 일치시킨다.
     *
     * @param jwtProvider 토큰 발급/검증 컴포넌트
     * @return JwtProvider 내부 디코더
     */
    @Bean
    JwtDecoder jwtDecoder(JwtProvider jwtProvider) {
        return jwtProvider.getDecoder();
    }

    /**
     * 무상태(STATELESS) JWT 기반 보안 필터 체인을 구성한다.
     * <ul>
     *   <li>CSRF 비활성화(쿠키·세션 미사용, Bearer 토큰만 받는 API라 불필요).</li>
     *   <li>세션을 만들지 않음(STATELESS) — 매 요청을 토큰으로만 인증한다.</li>
     *   <li>경로별 인가 규칙 적용 후, 토큰 검증과 401/403 처리기를 연결한다.</li>
     * </ul>
     *
     * @param http Security DSL 빌더
     * @param jwtDecoder 들어온 JWT를 검증할 디코더
     * @return 빌드된 SecurityFilterChain
     * @throws Exception DSL 구성 중 발생할 수 있는 예외
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                // 토큰 기반 무상태 API라 CSRF 보호가 불필요하므로 끈다.
                .csrf(AbstractHttpConfigurer::disable)
                // 서버 세션을 생성/사용하지 않고 매 요청을 JWT로만 인증한다.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 헬스 체크와 로그인 전(前) 인증 흐름(소셜/휴대폰 인증, 토큰 재발급)은 토큰 없이 공개.
                        .requestMatchers("/api/health",
                                "/api/auth/oauth/**", "/api/auth/phone/**", "/api/auth/refresh").permitAll()
                        // 온보딩 단계(약관 동의·가입 완료)는 온보딩 토큰 또는 정식 USER 모두 허용.
                        .requestMatchers("/api/auth/terms", "/api/auth/complete").hasAnyRole("ONBOARDING", "USER")
                        // 닉네임 중복확인은 온보딩 단계(ProfileSetup)에서도 호출하므로 ONBOARDING도 허용.
                        .requestMatchers(HttpMethod.GET, "/api/users/nickname-check").hasAnyRole("ONBOARDING", "USER")
                        // 그 외 모든 요청은 정식 가입 사용자(USER)만 허용.
                        // authenticated() 대신 hasRole("USER")로 게이팅하는 이유: authenticated()면 온보딩 토큰도
                        // "인증됨"으로 통과하므로, 가입 미완료 온보딩 토큰이 일반 API를 호출하는 것을 막기 위함.
                        .anyRequest().hasRole("USER"))
                // 이 앱을 OAuth2 리소스 서버로 동작시켜 Authorization: Bearer 토큰을 위 디코더로 검증하고,
                // 아래 컨버터로 typ 클레임을 ROLE_* 권한으로 변환한다.
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(e -> e
                        // 인증 실패(토큰 없음/위조/만료) → 401, 권한 부족(역할 불충분) → 403.
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(forbiddenHandler()));
        return http.build();
    }

    /**
     * JWT의 {@code typ} 클레임을 Spring Security 권한(ROLE_*)으로 매핑하는 컨버터를 만든다.
     * typ=onboarding이면 ROLE_ONBOARDING, 그 외(access 포함)는 ROLE_USER를 부여한다.
     * 이 매핑이 위 인가 규칙(hasRole/hasAnyRole)의 판정 근거가 된다.
     *
     * @return typ→ROLE 매핑이 적용된 JwtAuthenticationConverter
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // 토큰 종류 식별용 typ 클레임을 읽어 부여할 역할을 정한다.
            String type = jwt.getClaimAsString(JwtProvider.CLAIM_TYPE);
            // onboarding 토큰만 ROLE_ONBOARDING, 나머지는 ROLE_USER로 취급한다.
            String role = JwtProvider.TYPE_ONBOARDING.equals(type) ? "ROLE_ONBOARDING" : "ROLE_USER";
            return List.<GrantedAuthority>of(new SimpleGrantedAuthority(role));
        });
        return converter;
    }

    /**
     * 인증 실패(미인증) 시 호출되는 진입점. 401과 함께 공통 에러 엔벨로프 JSON을 직접 쓴다.
     * (스프링 기본 응답 대신 우리 {@code {success:false,error:{...}}} 포맷으로 통일하기 위함.)
     *
     * @return UNAUTHORIZED(401)을 내려주는 AuthenticationEntryPoint
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, ex) -> writeError(response, ErrorCode.UNAUTHORIZED);
    }

    /**
     * 인가 실패(권한 부족) 시 호출되는 핸들러. 403과 함께 공통 에러 엔벨로프 JSON을 직접 쓴다.
     *
     * @return FORBIDDEN(403)을 내려주는 AccessDeniedHandler
     */
    private AccessDeniedHandler forbiddenHandler() {
        return (request, response, ex) -> writeError(response, ErrorCode.FORBIDDEN);
    }

    /**
     * 보안 필터 단계(컨트롤러 진입 전)에서 발생한 401/403을 공통 응답 엔벨로프 JSON으로 직접 직렬화한다.
     * 이 시점엔 {@code GlobalExceptionHandler}(@RestControllerAdvice)가 동작하지 않으므로 문자열로 직접 작성한다.
     *
     * @param response 응답 객체(상태·콘텐츠타입·인코딩을 세팅하고 본문을 기록)
     * @param code 내려줄 에러 코드(상태·코드명·메시지의 출처)
     * @throws IOException 응답 본문 기록 중 입출력 예외
     */
    private static void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        HttpStatus status = code.status();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // ApiResponse.error(...)와 동일한 형태를 손으로 만든 JSON. 필터 계층이라 메시지 컨버터를 거치지 않는다.
        response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"" + code.code() + "\",\"message\":\"" + code.message() + "\"}}");
    }
}
