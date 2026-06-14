package com.honjeong.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.service.UserService;

/**
 * {@link UserController}의 웹 계층 슬라이스 테스트.
 *
 * <p>검증 목적은 컨트롤러의 HTTP 관심사 — 요청 매핑/상태코드, {@code @Valid} 검증, JSON 응답 형태, 그리고 보안 규칙
 * (토큰 필요 여부와 {@code @CurrentUserId} 주입) — 이며, 서비스의 비즈니스 로직은 여기서 다루지 않는다(그건
 * {@code UserServiceTest}의 몫).
 *
 * <p>{@code @WebMvcTest(controllers = UserController.class)}로 <b>범위를 이 컨트롤러 하나로 한정</b>한다 — 전체
 * {@code ApplicationContext}를 띄우지 않고 MVC 인프라(MockMvc·컨트롤러·필터 등)만 로드하므로 가볍고 빠르며, 서비스·리포지토리·DB는
 * 끌어오지 않는다. 다만 슬라이스 테스트는 보안/MVC 설정을 자동으로 포함하지 않으므로, 실제 인가 규칙과 인자 리졸버를 함께 검증하려고
 * {@code @Import}로 {@link SecurityConfig}·{@link WebConfig}를 명시적으로 끌어온다(덕분에 SecurityConfig가 만드는
 * {@code JwtProvider} 빈도 주입받아 테스트용 토큰을 발급할 수 있다).
 *
 * <p>{@code @WebMvcTest}는 서비스 빈을 만들지 않으므로, 컨트롤러가 의존하는 {@link UserService}는 {@code @MockitoBean}으로
 * 가짜 빈을 등록해 컨텍스트에 채워 넣고, 각 테스트에서 {@code when(...)}으로 반환값을 정하거나 {@code verify(...)}로 호출을 확인한다.
 */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, WebConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserService userService;

    private UserProfileResponse sampleProfile() {
        return new UserProfileResponse(1L, "01012345678", null, "혼밥러", null,
                null, null, null, null, null, null, null, true, UserStatus.ACTIVE);
    }

    /**
     * given: userId 1L의 정식 access 토큰 발급 + 서비스가 sampleProfile()을 돌려주도록 스텁.
     * when: {@code GET /api/users/me} 호출.
     * then: 200 + {@code data.nickname="혼밥러"}, {@code data.phone="01012345678"}으로 응답한다
     *       — {@code @CurrentUserId}로 주입된 userId=1L이 getMyProfile에 전달됨을 간접 확인한다.
     */
    @Test
    @DisplayName("GET /me: access 토큰이면 200 + 프로필")
    void getMe_ok() throws Exception {
        when(userService.getMyProfile(1L)).thenReturn(sampleProfile());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("혼밥러"))
                .andExpect(jsonPath("$.data.phone").value("01012345678"));
    }

    /**
     * given: Authorization 헤더(토큰) 없는 요청.
     * when: {@code GET /api/users/me} 호출.
     * then: SecurityConfig의 인가 규칙에 막혀 401(Unauthorized)로 응답한다 — 컨트롤러 본문에 도달하지 않는다.
     */
    @Test
    @DisplayName("GET /me: 토큰 없으면 401")
    void getMe_noToken_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * given: userId 1L의 정식 access 토큰 발급 + 서비스가 sampleProfile()을 돌려주도록 스텁.
     * when: 닉네임·allowMealRequest를 담은 본문으로 {@code PATCH /api/users/me} 호출.
     * then: 200 + {@code data.nickname="혼밥러"}로 응답하고, 컨트롤러가
     *       {@code userService.updateProfile(1L, ...)}에 위임함을 확인한다
     *       — {@code @CurrentUserId}로 주입된 userId=1L 전달과 toCommand() 변환 경로를 함께 검증한다.
     */
    @Test
    @DisplayName("PATCH /me: access 토큰이면 200이고 서비스에 위임한다")
    void patchMe_ok() throws Exception {
        when(userService.updateProfile(eq(1L), any())).thenReturn(sampleProfile());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉\",\"allowMealRequest\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("혼밥러"));

        verify(userService).updateProfile(eq(1L), any());
    }

    /**
     * given: 20자를 초과하는 닉네임(21자)을 담은 수정 요청 + 유효한 access 토큰.
     * when: PATCH /me 호출.
     * then: @Size(max=20) @Valid 검증에 걸려 400 + INVALID_INPUT으로 응답한다(서비스까지 가지 않는다).
     */
    @Test
    @DisplayName("PATCH /me: 닉네임이 20자를 초과하면 400")
    void patchMe_nicknameTooLong_400() throws Exception {
        String token = jwtProvider.createAccessToken(1L);
        String tooLong = "가".repeat(21);

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    /**
     * given: userId 1L의 정식 access 토큰 발급 + 서비스가 NicknameCheckResponse("새닉", true)를 돌려주도록 스텁.
     * when: {@code nickname=새닉} 파라미터로 {@code GET /api/users/nickname-check} 호출.
     * then: 200 + {@code data.available=true}로 응답한다.
     */
    @Test
    @DisplayName("GET /nickname-check: access 토큰이면 200 + available")
    void checkNickname_ok() throws Exception {
        when(userService.checkNickname("새닉")).thenReturn(new NicknameCheckResponse("새닉", true));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/users/nickname-check").param("nickname", "새닉")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    /**
     * given: nickname 파라미터가 없는 요청 + 유효한 access 토큰.
     * when: {@code GET /api/users/nickname-check} 호출.
     * then: Spring MVC의 {@code MissingServletRequestParameterException} 핸들러가 400 + {@code INVALID_INPUT}으로
     *       응답한다 — 컨트롤러 본문에 도달하지 않는다.
     */
    @Test
    @DisplayName("GET /nickname-check: nickname 파라미터 누락 시 400")
    void checkNickname_missingParam_400() throws Exception {
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/users/nickname-check").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    /**
     * given: userId 1L의 온보딩 토큰(typ=onboarding → ROLE_ONBOARDING).
     * when: USER 전용인 GET /me 호출.
     * then: 권한 부족으로 403(Forbidden). /me는 정식 가입(USER)만 허용됨을 확인한다.
     */
    @Test
    @DisplayName("GET /me: 온보딩 토큰이면 403(USER 전용)")
    void getMe_onboardingToken_403() throws Exception {
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    /**
     * given: 온보딩 토큰 + checkNickname 스텁(available=true).
     * when: GET /nickname-check 호출.
     * then: 200으로 통과한다 — nickname-check은 온보딩 ProfileSetup 단계에서도 호출되므로 ONBOARDING도 허용됨을 확인한다.
     */
    @Test
    @DisplayName("GET /nickname-check: 온보딩 토큰으로도 200(온보딩 ProfileSetup 지원)")
    void checkNickname_onboardingToken_ok() throws Exception {
        when(userService.checkNickname("새닉")).thenReturn(new NicknameCheckResponse("새닉", true));
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(get("/api/users/nickname-check").param("nickname", "새닉")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }
}
