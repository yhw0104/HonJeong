package com.honjeong.global.config;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.ActiveUserFilter;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.global.security.SecurityErrorWriter;

/**
 * 무상태 JWT 보안 설정(Spring Security 7) — JwtProvider/JwtDecoder 빈 등록, 경로별 인가 규칙,
 * typ 클레임→ROLE 매핑, 401/403 공통 엔벨로프 응답.
 *
 * <p>사용처: 스프링 시큐리티 필터 체인으로 전 API 요청에 자동 적용된다(직접 참조 없음).
 * JwtProvider 빈은 AuthService·TokenService가 주입받는다.
 *
 * <p>토큰의 {@code typ} 클레임을 권한으로 매핑한다: typ=access → ROLE_USER,
 * typ=onboarding → ROLE_ONBOARDING.
 *
 * <p>인가: 인증/헬스는 공개, 온보딩 엔드포인트는 ONBOARDING|USER, 그 외 전부 USER.
 * (⚠️ {@code anyRequest().authenticated()}를 쓰면 온보딩 토큰이 일반 API를 통과하므로 USER로 게이팅.)
 *
 * <p><b>휴대폰 인증({@code /api/auth/phone/**})은 예외다</b> — {@code honjeong.sms.mode}가
 * {@code real}이 아닌 동안(현재 real 구현체 없음, 항상 mock)은 permitAll 대신 전면 차단한다.
 * mock SMS는 인증번호가 고정값 "000000"이라, 두 번의 무인증 호출(send-code → verify)만으로
 * 임의 전화번호로 ROLE_USER 계정을 만들 수 있기 때문이다. real SMS 게이트웨이가 붙어
 * {@code honjeong.sms.mode=real}이 되면 코드 변경 없이 자동으로 다시 열린다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(HonjeongJwtProperties.class)
public class SecurityConfig {

    /**
     * 자체 발급 JWT(access·onboarding)의 생성·검증을 담당하는 {@link JwtProvider}를 빈으로 등록한다.
     *
     * <p>시크릿·TTL은 {@link HonjeongJwtProperties}에서 받고, 시각은 UTC Clock으로 고정한다.
     *
     * @param props honjeong.jwt.* 설정값(시크릿·access/onboarding TTL)
     * @return 시크릿·TTL·UTC Clock으로 구성된 발급/검증기
     */
    @Bean
    JwtProvider jwtProvider(HonjeongJwtProperties props) {
        return new JwtProvider(props.secret(), props.accessTokenTtlSeconds(),
                props.onboardingTokenTtlSeconds(), Clock.systemUTC());
    }

    /**
     * 리소스 서버({@code oauth2ResourceServer().jwt()})가 들어온 Bearer 토큰을 검증할 때 쓸
     * {@link JwtDecoder}를 빈으로 등록한다.
     *
     * <p>JwtProvider가 가진 디코더(같은 대칭키·HS256)를 그대로 노출해 발급과 검증의 키를 일치시킨다.
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
     *
     * <ul>
     *   <li>CSRF 비활성화(쿠키·세션 미사용, Bearer 토큰만 받는 API라 불필요).</li>
     *   <li>세션을 만들지 않음(STATELESS) — 매 요청을 토큰으로만 인증한다.</li>
     *   <li>경로별 인가 규칙 적용 후, 토큰 검증과 401/403 처리기를 연결한다.</li>
     * </ul>
     *
     * @param http Security DSL 빌더
     * @param jwtDecoder 들어온 JWT를 검증할 디코더
     * @param activeUserFilter 인증된 요청마다 users.status가 ACTIVE인지 확인하는 필터
     * @param smsMode {@code honjeong.sms.mode} 값. {@code real}이 아니면(=SMS가 mock) 휴대폰 인증
     *        엔드포인트를 차단한다 — mock SMS는 인증번호가 항상 "000000"이라, 두 번의 무인증 호출
     *        (send-code → verify)만으로 임의 전화번호로 ROLE_USER 계정을 만들 수 있기 때문이다.
     * @return 빌드된 SecurityFilterChain
     * @throws Exception DSL 구성 중 발생할 수 있는 예외
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder, ActiveUserFilter activeUserFilter,
            @Value("${honjeong.sms.mode:mock}") String smsMode) throws Exception {
        // SMS가 real이 아닌 동안(현재 real 구현체가 없어 항상 mock)은 휴대폰 인증 경로를 전면 차단한다.
        // 이유: FixedVerificationCodeGenerator가 인증번호로 항상 "000000"을 돌려주므로, 공개 서버에
        // 이 경로를 열어두면 send-code → verify 두 번의 무인증 호출만으로 임의 전화번호를 ROLE_USER
        // 계정으로 만들 수 있다(무제한 무인증 계정 생성).
        //
        // 하드코딩된 denyAll이 아니라 sms.mode를 보는 이유는 "보안 규칙이 실제 능력을 따라가게" 하기
        // 위해서다 — real SMS 게이트웨이가 붙어 honjeong.sms.mode=real이 되는 순간 이 조건이 거짓이
        // 되어 아래 permitAll로 코드 변경 없이 자동 복귀한다.
        // ★단, 지금은 로컬에서도 막힌다: application.yml 공통 섹션이 sms.mode를 mock으로 고정하고
        //   있고(오버라이드 통로 없음), real로 바꾸면 mock 빈이 사라져 컨텍스트가 죽는다. 즉 "로컬
        //   개발용으로 열어둔다"는 선택지는 애초에 없다 — 휴대폰 온보딩을 테스트하려면 mock SMS가
        //   필요한데 그게 곧 차단 대상이기 때문이다. 앱의 휴대폰 진입점도 이미 숨겨져 있어 소비처가
        //   없다(Welcome.tsx 참고).
        boolean smsIsMock = !"real".equals(smsMode);
        http
                // 토큰 기반 무상태 API라 CSRF 보호가 불필요하므로 끈다.
                .csrf(AbstractHttpConfigurer::disable)
                // 서버 세션을 생성/사용하지 않고 매 요청을 JWT로만 인증한다.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth
                            // 헬스 체크와 로그인 전(前) 인증 흐름(소셜 인증, 토큰 재발급)은 토큰 없이 공개.
                            // 휴대폰(/api/auth/phone/**)은 위 smsIsMock 분기에서 별도로 다룬다.
                            // /ws는 시큐리티가 아니라 핸드셰이크 인터셉터가 지킨다 — 티켓이 없으면
                            // 연결 자체가 성립하지 않는다(WsHandshakeInterceptor). 여기서 막으면
                            // 티켓을 검사할 기회조차 없이 401이 나간다.
                            .requestMatchers("/api/health", "/api/auth/oauth/**", "/api/auth/refresh", "/ws").permitAll();
                    if (smsIsMock) {
                        // mock인 동안은 컨트롤러에 닿기 전에 보안 계층에서 끊는다. 익명 사용자의
                        // denyAll은 ExceptionTranslationFilter가 AuthenticationEntryPoint로 돌려 401로 응답한다.
                        auth.requestMatchers("/api/auth/phone/**").denyAll();
                    } else {
                        auth.requestMatchers("/api/auth/phone/**").permitAll();
                    }
                    auth
                            // 혼밥 통계(사회적 증거)는 비로그인 첫 화면에 노출되므로 토큰 없이 공개(FR-103). 집계 숫자만 반환.
                            .requestMatchers(HttpMethod.GET, "/api/check-ins/stats").permitAll()
                            // 업로드된 파일(프로필 사진 등) 정적 서빙은 공개로 둔다(이미지 표시는 인증 불필요).
                            .requestMatchers(HttpMethod.GET, "/files/**").permitAll()
                            // 온보딩 단계(약관 동의·가입 완료)는 온보딩 토큰 또는 정식 USER 모두 허용.
                            .requestMatchers("/api/auth/terms", "/api/auth/complete").hasAnyRole("ONBOARDING", "USER")
                            // 닉네임 중복확인은 온보딩 단계(ProfileSetup)에서도 호출하므로 ONBOARDING도 허용.
                            .requestMatchers(HttpMethod.GET, "/api/users/nickname-check").hasAnyRole("ONBOARDING", "USER")
                            // 파일 업로드(프로필 사진)는 온보딩(ProfileSetup) 단계에서도 호출하므로 ONBOARDING도 허용.
                            .requestMatchers(HttpMethod.POST, "/api/files").hasAnyRole("ONBOARDING", "USER")
                            // 그 외 모든 요청은 정식 가입 사용자(USER)만 허용.
                            // authenticated() 대신 hasRole("USER")로 게이팅하는 이유: authenticated()면 온보딩 토큰도
                            // "인증됨"으로 통과하므로, 가입 미완료 온보딩 토큰이 일반 API를 호출하는 것을 막기 위함.
                            .anyRequest().hasRole("USER");
                })
                // 이 앱을 OAuth2 리소스 서버로 동작시켜 Authorization: Bearer 토큰을 위 디코더로 검증하고,
                // 아래 컨버터로 typ 클레임을 ROLE_* 권한으로 변환한다.
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(e -> e
                        // 인증 실패(토큰 없음/위조/만료) → 401, 권한 부족(역할 불충분) → 403.
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(forbiddenHandler()))
                // 인증(Bearer 토큰 검증) 직후에 계정 상태를 확인한다. 이 위치여야 SecurityContext가 채워져 있다.
                .addFilterAfter(activeUserFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    /**
     * JWT의 {@code typ} 클레임을 Spring Security 권한(ROLE_*)으로 매핑하는 컨버터를 만든다.
     *
     * <p>typ=onboarding이면 ROLE_ONBOARDING, 그 외(access 포함)는 ROLE_USER를 부여한다.
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
     *
     * <p>스프링 기본 응답 대신 우리 {@code {success:false,error:{...}}} 포맷으로 통일하기 위함이다.
     *
     * @return UNAUTHORIZED(401)을 내려주는 AuthenticationEntryPoint
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, ex) -> SecurityErrorWriter.write(response, ErrorCode.UNAUTHORIZED);
    }

    /**
     * 인가 실패(권한 부족) 시 호출되는 핸들러. 403과 함께 공통 에러 엔벨로프 JSON을 직접 쓴다.
     *
     * @return FORBIDDEN(403)을 내려주는 AccessDeniedHandler
     */
    private AccessDeniedHandler forbiddenHandler() {
        return (request, response, ex) -> SecurityErrorWriter.write(response, ErrorCode.FORBIDDEN);
    }
}
