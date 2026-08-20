package com.honjeong.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.service.AuthResult;
import com.honjeong.auth.service.AuthService;
import com.honjeong.auth.service.TokenPair;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.support.ActiveUserSliceSupport;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

/**
 * {@link AuthController}의 웹 계층 슬라이스 테스트.
 *
 * <p>검증 목적은 컨트롤러의 HTTP 관심사 — 요청 매핑/상태코드, {@code @Valid} 검증, JSON 응답 형태, 그리고 보안 규칙
 * (토큰 필요 여부와 {@code @CurrentUserId} 주입) — 이며, 서비스의 비즈니스 로직은 여기서 다루지 않는다(그건
 * {@code AuthServiceTest}의 몫).
 *
 * <p>{@code @WebMvcTest(controllers = AuthController.class)}로 <b>범위를 이 컨트롤러 하나로 한정</b>한다 — 전체
 * {@code ApplicationContext}를 띄우지 않고 MVC 인프라(MockMvc·컨트롤러·필터 등)만 로드하므로 가볍고 빠르며, 서비스·리포지토리·DB는
 * 끌어오지 않는다. 다만 슬라이스 테스트는 보안/MVC 설정을 자동으로 포함하지 않으므로, 실제 인가 규칙과 인자 리졸버를 함께 검증하려고
 * {@code @Import}로 {@link SecurityConfig}·{@link WebConfig}를 명시적으로 끌어온다(덕분에 SecurityConfig가 만드는
 * {@code JwtProvider} 빈도 주입받아 테스트용 토큰을 발급할 수 있다).
 *
 * <p>{@code @WebMvcTest}는 서비스 빈을 만들지 않으므로, 컨트롤러가 의존하는 {@link AuthService}는 {@code @MockitoBean}으로
 * 가짜 빈을 등록해 컨텍스트에 채워 넣고, 각 테스트에서 {@code when(...)}으로 반환값을 정하거나 {@code verify(...)}로 호출을 확인한다.
 *
 * <p><b>{@code honjeong.sms.mode=real}로 강제 오버라이드하는 이유</b>: {@link SecurityConfig}는 SMS가
 * mock인 동안 {@code /api/auth/phone/**}를 보안 계층에서 차단한다(휴대폰 인증번호가 고정값이라 공개
 * 서버에서 열어두면 무인증 계정 생성이 가능하기 때문 — 자세한 근거는 {@code SecurityPingTest}). 그
 * 차단 자체는 이 클래스가 아니라 {@code SecurityPingTest}에서 검증하므로, 여기서는 이 컨트롤러 고유의
 * 관심사(검증·위임·인가 규칙)를 그 차단에 가리지 않고 그대로 확인하기 위해 real로 켜 둔다.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, WebConfig.class})
@TestPropertySource(properties = "honjeong.sms.mode=real")
class AuthControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthService authService;

    /**
     * given: phone이 담긴 정상 요청 본문.
     * when: {@code POST /phone/send-code} 호출.
     * then: 200 + {@code success:true}로 응답하고, 컨트롤러가 받은 phone 그대로 {@code authService.sendPhoneCode}에 위임한다.
     */
    @Test
    @DisplayName("send-code: 200이고 서비스에 위임한다")
    void sendCode_ok() throws Exception {
        mockMvc.perform(post("/api/auth/phone/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).sendPhoneCode("01012345678");
    }

    /**
     * given: phone이 빠진 빈 본문({@code {}}).
     * when: {@code POST /phone/send-code} 호출.
     * then: {@code @NotBlank @Valid} 검증에 걸려 400 + 에러코드 {@code INVALID_INPUT}으로 응답한다(서비스까지 가지 않는다).
     */
    @Test
    @DisplayName("send-code: phone 누락 시 400")
    void sendCode_validation() throws Exception {
        mockMvc.perform(post("/api/auth/phone/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    /**
     * given: 서비스가 신규 회원을 뜻하는 {@code AuthResult.onboarding("onb-token")}을 돌려주도록 스텁.
     * when: phone+code로 {@code POST /phone/verify} 호출.
     * then: 200 + {@code data.onboarding=true}, {@code data.onboardingToken="onb-token"}으로 응답한다
     *       (토큰 3종은 null이라 NON_NULL 규칙으로 응답에서 빠진다).
     */
    @Test
    @DisplayName("verify: 신규 회원이면 온보딩 토큰을 반환한다")
    void verify_newUser_returnsOnboarding() throws Exception {
        when(authService.verifyPhone("01012345678", "000000")).thenReturn(AuthResult.onboarding("onb-token"));

        mockMvc.perform(post("/api/auth/phone/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\",\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboarding").value(true))
                .andExpect(jsonPath("$.data.onboardingToken").value("onb-token"));
    }

    // --- 소셜 로그인 진입: 앱이 보낸 값이 서비스까지 그대로 도달하는지 ---
    //
    // 이 컨트롤러가 애플 로그인에서 하는 일은 사실상 authorizationCode를 서비스로 넘기는 것 하나뿐이라,
    // 그 한 줄이 끊겨도 서비스·배선 테스트는 전부 초록으로 남는다(요청만 조용히 코드 없이 처리된다).
    // 그래서 "위임 확인"을 자명한 코드로 보지 않고 여기서 못 박는다.

    /**
     * given: 서비스가 온보딩 결과를 돌려주도록 스텁.
     * when: idToken과 authorizationCode를 함께 담아 {@code POST /oauth/apple} 호출(애플 로그인의 실제 모양).
     * then: 200으로 응답하고, 경로변수에서 뽑은 {@code APPLE}과 본문의 두 값이 그대로 서비스에 위임된다.
     */
    @Test
    @DisplayName("oauth: 애플이 보낸 authorizationCode가 서비스까지 그대로 전달된다")
    void oauth_apple_passesAuthorizationCode() throws Exception {
        when(authService.oauthLogin(any(), any(), any())).thenReturn(AuthResult.onboarding("onb-token"));

        mockMvc.perform(post("/api/auth/oauth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"t\",\"authorizationCode\":\"c\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboarding").value(true))
                .andExpect(jsonPath("$.data.onboardingToken").value("onb-token"));

        verify(authService).oauthLogin(Provider.APPLE, "t", "c");
    }

    /**
     * given: 서비스가 온보딩 결과를 돌려주도록 스텁.
     * when: authorizationCode 없이 idToken만 담아 {@code POST /oauth/kakao} 호출(카카오는 코드를 보내지 않고,
     *       애플을 붙이기 전 앱이 보내던 본문도 정확히 이 모양이다).
     * then: 검증에 걸리지 않고 200이며, 세 번째 인자로 null이 넘어간다 — 필드를 더해도 기존 클라가
     *       그대로 동작한다는 하위 호환 주장을 증거로 만든다.
     */
    @Test
    @DisplayName("oauth: authorizationCode가 없으면 null로 위임된다 — 기존 클라 본문도 그대로 통한다")
    void oauth_withoutAuthorizationCode_passesNull() throws Exception {
        when(authService.oauthLogin(any(), any(), any())).thenReturn(AuthResult.onboarding("onb-token"));

        mockMvc.perform(post("/api/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"t\"}"))
                .andExpect(status().isOk());

        verify(authService).oauthLogin(Provider.KAKAO, "t", null);
    }

    /**
     * given: Authorization 헤더(토큰) 없는 요청.
     * when: 온보딩 토큰이 필요한 {@code POST /terms} 호출.
     * then: SecurityConfig의 인가 규칙에 막혀 401(Unauthorized)로 응답한다 — 컨트롤러 본문에 도달하지 않는다.
     */
    @Test
    @DisplayName("terms: 토큰 없이 호출하면 401")
    void terms_withoutToken_401() throws Exception {
        mockMvc.perform(post("/api/auth/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\":true,\"service\":true,\"privacy\":true,\"location\":true,\"marketing\":false}"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * given: userId 1L에 대한 온보딩 토큰을 발급해 Authorization 헤더에 실음.
     * when: 약관 동의 본문과 함께 {@code POST /terms} 호출.
     * then: 200으로 통과하고, 토큰의 sub가 {@code @CurrentUserId}로 1L로 주입돼 {@code agreeTerms(1L, true,true,true,false)}로
     *       위임됨을 확인한다 — 인가 통과 + 동의값 전달 + userId 주입을 한 번에 검증.
     */
    @Test
    @DisplayName("terms: 온보딩 토큰이면 200이고 userId가 주입된다")
    void terms_withOnboardingToken_ok() throws Exception {
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(post("/api/auth/terms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"age\":true,\"service\":true,\"privacy\":true,\"location\":true,\"marketing\":false}"))
                .andExpect(status().isOk());

        verify(authService).agreeTerms(1L, true, true, true, true, false);
    }

    /**
     * given: 서비스가 정식 토큰쌍 {@code TokenPair("acc","ref",3600)}을 돌려주도록 스텁하고, userId 1L의 온보딩 토큰을 발급.
     * when: 닉네임만 담은 본문과 함께 온보딩 토큰으로 {@code POST /complete} 호출.
     * then: 200 + {@code data.accessToken="acc"}, {@code data.refreshToken="ref"}로 응답한다
     *       — 온보딩 토큰으로 가입 확정 후 정식 토큰이 내려가는 경로를 검증한다.
     */
    @Test
    @DisplayName("complete: 온보딩 토큰이면 정식 토큰을 반환한다")
    void complete_withOnboardingToken_returnsTokens() throws Exception {
        when(authService.complete(eq(1L), any())).thenReturn(new TokenPair("acc", "ref", 3600));
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(post("/api/auth/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"혼밥러\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("acc"))
                .andExpect(jsonPath("$.data.refreshToken").value("ref"));
    }

    /**
     * given: 20자를 초과하는 닉네임(21자)을 담은 가입 확정 요청 + userId 1L의 온보딩 토큰.
     * when: {@code POST /complete} 호출.
     * then: @Size(max=20) 검증에 걸려 400 + INVALID_INPUT으로 응답한다.
     *
     * <p>PATCH /users/me와 같은 컬럼(users.nickname)을 쓰는데 이쪽만 상한이 없으면, 프로필 수정으로는
     * 만들 수 없는 긴 닉네임이 가입 경로로 들어온다 — 그 닉네임을 가진 사용자를 신고하면
     * {@code reports.target_nickname VARCHAR(20)}에 걸려 신고가 500으로 실패한다.
     */
    @Test
    @DisplayName("complete: 닉네임이 20자를 초과하면 400")
    void complete_nicknameTooLong_400() throws Exception {
        String token = jwtProvider.createOnboardingToken(1L);
        String tooLong = "가".repeat(21);

        mockMvc.perform(post("/api/auth/complete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
