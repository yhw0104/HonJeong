package com.honjeong.checkin.controller;

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

import com.honjeong.checkin.dto.CheckInUserResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;

/**
 * {@link PlaceCheckInController} 웹 슬라이스 테스트 — 혼밥러 목록 엔드포인트의 매핑·인가를 검증한다.
 */
@WebMvcTest(controllers = PlaceCheckInController.class)
@Import({SecurityConfig.class, WebConfig.class})
class PlaceCheckInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private CheckInService checkInService;

    @Test
    @DisplayName("GET /api/places/{id}/check-ins: USER면 200 + 혼밥러 목록(닉네임·경과)")
    void diners_200() throws Exception {
        when(checkInService.getActiveDiners(3L)).thenReturn(List.of(
                new CheckInUserResponse(10L, 5L, "혼밥러", LocalDateTime.of(2026, 6, 15, 12, 0), 15L)));

        mockMvc.perform(get("/api/places/3/check-ins")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(5))
                .andExpect(jsonPath("$.data[0].nickname").value("혼밥러"))
                .andExpect(jsonPath("$.data[0].elapsedMinutes").value(15));
    }

    @Test
    @DisplayName("GET /api/places/{id}/check-ins: 토큰 없으면 401")
    void diners_401() throws Exception {
        mockMvc.perform(get("/api/places/3/check-ins"))
                .andExpect(status().isUnauthorized());
    }
}
