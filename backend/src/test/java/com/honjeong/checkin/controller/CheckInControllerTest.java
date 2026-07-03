package com.honjeong.checkin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;

/**
 * {@link CheckInController} 웹 슬라이스 테스트. HTTP 매핑·상태코드·인가·{@code @Valid}를 검증하고 로직은 서비스 모킹.
 */
@WebMvcTest(controllers = CheckInController.class)
@Import({SecurityConfig.class, WebConfig.class})
class CheckInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private CheckInService checkInService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    private String body() {
        return """
                {"placeId": 3}
                """;
    }

    @Test
    @DisplayName("POST /api/check-ins: USER면 201 + 체크인 응답")
    void create_201() throws Exception {
        when(checkInService.createCheckIn(eq(1L), any())).thenReturn(
                new CheckInResponse(10L, 3L, "ACTIVE", LocalDateTime.of(2026, 6, 15, 12, 0), null));

        mockMvc.perform(post("/api/check-ins").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.checkInId").value(10))
                .andExpect(jsonPath("$.data.placeId").value(3))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/check-ins: placeId 누락이면 400")
    void create_invalid() throws Exception {
        String bad = """
                {}
                """;
        mockMvc.perform(post("/api/check-ins").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /api/check-ins: 토큰 없으면 401")
    void create_401() throws Exception {
        mockMvc.perform(post("/api/check-ins")
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/check-ins: 온보딩 토큰이면 403")
    void create_403() throws Exception {
        mockMvc.perform(post("/api/check-ins")
                        .header("Authorization", "Bearer " + jwtProvider.createOnboardingToken(1L))
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/check-ins: 다른 장소 ACTIVE면 409")
    void create_409() throws Exception {
        when(checkInService.createCheckIn(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.CHECKIN_ALREADY_ACTIVE));

        mockMvc.perform(post("/api/check-ins").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHECKIN_ALREADY_ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/check-ins: 존재하지 않는 placeId면 404")
    void create_placeNotFound() throws Exception {
        when(checkInService.createCheckIn(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        mockMvc.perform(post("/api/check-ins").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /api/check-ins/{id}/end: 200")
    void end_200() throws Exception {
        when(checkInService.endCheckIn(1L, 10L)).thenReturn(
                new CheckInResponse(10L, 3L, "ENDED", LocalDateTime.of(2026, 6, 15, 12, 0),
                        LocalDateTime.of(2026, 6, 15, 13, 0)));

        mockMvc.perform(patch("/api/check-ins/10/end").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"));
    }

    @Test
    @DisplayName("PATCH .../end: 없으면 404")
    void end_404() throws Exception {
        when(checkInService.endCheckIn(1L, 99L))
                .thenThrow(new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));

        mockMvc.perform(patch("/api/check-ins/99/end").header("Authorization", userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CHECKIN_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /check-ins/{id}/cancel → 200, CANCELLED 응답")
    void cancel_ok() throws Exception {
        when(checkInService.cancelCheckIn(eq(1L), eq(3L))).thenReturn(
                new CheckInResponse(3L, 10L, "CANCELLED", LocalDateTime.of(2026, 6, 15, 12, 0),
                        LocalDateTime.of(2026, 6, 15, 12, 0)));

        mockMvc.perform(patch("/api/check-ins/3/cancel").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH .../cancel: ACTIVE가 아니면 409")
    void cancel_409() throws Exception {
        when(checkInService.cancelCheckIn(eq(1L), eq(3L)))
                .thenThrow(new BusinessException(ErrorCode.CHECKIN_NOT_ACTIVE));

        mockMvc.perform(patch("/api/check-ins/3/cancel").header("Authorization", userToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHECKIN_NOT_ACTIVE"));
    }

    @Test
    @DisplayName("PATCH .../cancel: 없으면 404")
    void cancel_404() throws Exception {
        when(checkInService.cancelCheckIn(eq(1L), eq(99L)))
                .thenThrow(new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));

        mockMvc.perform(patch("/api/check-ins/99/cancel").header("Authorization", userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CHECKIN_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH .../cancel: 타인 체크인이면 403")
    void cancel_403() throws Exception {
        when(checkInService.cancelCheckIn(eq(1L), eq(3L)))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mockMvc.perform(patch("/api/check-ins/3/cancel").header("Authorization", userToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /me: ACTIVE 없으면 data:null")
    void me_null() throws Exception {
        when(checkInService.getMyActiveCheckIn(1L)).thenReturn(null);

        mockMvc.perform(get("/api/check-ins/me").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GET /stats: 200 + todayCount/activeCount")
    void stats_200() throws Exception {
        when(checkInService.getStats()).thenReturn(new CheckInStatsResponse(124L, 17L));

        mockMvc.perform(get("/api/check-ins/stats").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayCount").value(124))
                .andExpect(jsonPath("$.data.activeCount").value(17));
    }

    @Test
    @DisplayName("GET /stats: 토큰 없이도 200 — 사회적 증거는 비로그인 첫 화면에 노출(공개)")
    void stats_noToken_200() throws Exception {
        when(checkInService.getStats()).thenReturn(new CheckInStatsResponse(124L, 17L));

        mockMvc.perform(get("/api/check-ins/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayCount").value(124))
                .andExpect(jsonPath("$.data.activeCount").value(17));
    }

    @Test
    @DisplayName("GET /map: 200 + 마커 배열")
    void map_200() throws Exception {
        when(checkInService.getMap(any(), any(), anyInt()))
                .thenReturn(List.of(new MapMarkerResponse(3L, "혼밥식당", 37.5, 127.0, 3)));

        mockMvc.perform(get("/api/check-ins/map").header("Authorization", userToken())
                        .param("lat", "37.5").param("lng", "127.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].placeId").value(3))
                .andExpect(jsonPath("$.data[0].activeCount").value(3));
    }

    @Test
    @DisplayName("GET /map: lat 누락이면 400")
    void map_missingLat() throws Exception {
        mockMvc.perform(get("/api/check-ins/map").header("Authorization", userToken())
                        .param("lng", "127.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
