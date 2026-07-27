package com.honjeong.badge.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.badge.dto.BadgeStatusResponse;
import com.honjeong.badge.service.BadgeService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * {@link BadgeController}의 웹 계층 슬라이스 테스트.
 *
 * <p>{@code UserControllerTest}와 동일한 하네스 — {@code @WebMvcTest}로 이 컨트롤러만 로드하고,
 * {@code @Import}로 {@link SecurityConfig}·{@link WebConfig}를 끌어와 실제 인가 규칙과
 * {@code @CurrentUserId} 인자 리졸버까지 함께 검증한다. {@link BadgeService}는 {@code @MockitoBean}으로
 * 대체하고, 인증은 {@link JwtProvider}가 발급한 실제 access 토큰을 Authorization 헤더에 실어 검증한다
 * (컨트롤러 인자에 값을 직접 주입하는 방식이 아니라, SecurityConfig 필터 체인을 실제로 통과시킨다).
 */
@WebMvcTest(controllers = BadgeController.class)
@Import({SecurityConfig.class, WebConfig.class})
class BadgeControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private BadgeService badgeService;

    /**
     * given: userId 1L의 정식 access 토큰 + badgeService가 SOLO_1(획득)·SOLO_10(미획득) 2건을 돌려주도록 스텁.
     * when: {@code GET /api/users/me/badges} 호출.
     * then: 200 + 뱃지 현황 배열이 그대로 응답 본문에 실린다 — {@code @CurrentUserId}로 주입된 userId=1L이
     *       {@code getMyBadges}에 전달됨을 간접 확인한다.
     */
    @Test
    @DisplayName("GET /api/users/me/badges: 10종 현황 반환")
    void getMyBadges() throws Exception {
        when(badgeService.getMyBadges(anyLong())).thenReturn(List.of(
                new BadgeStatusResponse("SOLO_1", true, LocalDateTime.of(2026, 7, 20, 12, 0)),
                new BadgeStatusResponse("SOLO_10", false, null)));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/users/me/badges").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].key").value("SOLO_1"))
                .andExpect(jsonPath("$.data[0].earned").value(true))
                .andExpect(jsonPath("$.data[1].earned").value(false));
    }

    /**
     * given: Authorization 헤더(토큰) 없는 요청.
     * when: {@code GET /api/users/me/badges} 호출.
     * then: SecurityConfig의 인가 규칙(anyRequest().hasRole("USER"))에 막혀 401로 응답한다
     *       — 컨트롤러 본문에 도달하지 않는다(엔드포인트별 보안 예외가 없음을 확인).
     */
    @Test
    @DisplayName("GET /api/users/me/badges: 토큰 없으면 401")
    void getMyBadges_noToken_401() throws Exception {
        mockMvc.perform(get("/api/users/me/badges"))
                .andExpect(status().isUnauthorized());
    }
}
