package com.honjeong.mate.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.mate.dto.MateAtPlace;
import com.honjeong.mate.dto.PlaceMatesResponse;
import com.honjeong.mate.service.PlaceMateService;

/**
 * {@link PlaceMateController} 웹 슬라이스 테스트 — 식당 상세 메이트 탭 엔드포인트의 매핑·인가를 검증한다.
 */
@WebMvcTest(controllers = PlaceMateController.class)
@Import({SecurityConfig.class, WebConfig.class})
class PlaceMateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private PlaceMateService placeMateService;

    @Test
    @DisplayName("GET /api/places/{id}/mates: 인증 사용자로 서비스 위임, 200 + 응답 래핑")
    void 메이트탭_조회() throws Exception {
        when(placeMateService.getMatesAtPlace(1L, 9L))
                .thenReturn(new PlaceMatesResponse(1, List.of(
                        new MateAtPlace(11L, "에이", true, 5, "조용해요", 3, 2,
                                java.time.LocalDateTime.of(2026, 7, 18, 12, 0), "https://img/11")), 10, 2));

        mockMvc.perform(get("/api/places/9/mates")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visitedCount").value(1))
                .andExpect(jsonPath("$.data.savedCount").value(10))
                .andExpect(jsonPath("$.data.savedMateCount").value(2))
                .andExpect(jsonPath("$.data.mates[0].nickname").value("에이"))
                .andExpect(jsonPath("$.data.mates[0].profileImageUrl").value("https://img/11"))
                .andExpect(jsonPath("$.data.mates[0].hereNow").value(true));
    }

    @Test
    @DisplayName("GET /api/places/{id}/mates: 토큰 없으면 401")
    void 메이트탭_401() throws Exception {
        mockMvc.perform(get("/api/places/9/mates"))
                .andExpect(status().isUnauthorized());
    }
}
