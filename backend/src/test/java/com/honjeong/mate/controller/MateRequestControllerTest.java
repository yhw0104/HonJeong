package com.honjeong.mate.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.mate.dto.MateRequestListItemResponse;
import com.honjeong.mate.dto.MateRequestResponse;
import com.honjeong.mate.dto.MateRequestStatusResponse;
import com.honjeong.mate.service.MateRequestService;

@WebMvcTest(controllers = MateRequestController.class)
@Import({SecurityConfig.class, WebConfig.class})
class MateRequestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private MateRequestService mateRequestService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("POST /api/mate-requests: 201 + PENDING")
    void create_201() throws Exception {
        when(mateRequestService.create(eq(1L), any()))
                .thenReturn(new MateRequestResponse(7L, 2L, "PENDING"));
        mockMvc.perform(post("/api/mate-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"toUserId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/mate-requests/9/accept: 200")
    void accept_200() throws Exception {
        when(mateRequestService.accept(1L, 9L))
                .thenReturn(new MateRequestStatusResponse(9L, "ACCEPTED", null));
        mockMvc.perform(patch("/api/mate-requests/9/accept").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("PATCH /api/mate-requests/9/decline: 200")
    void decline_200() throws Exception {
        when(mateRequestService.decline(1L, 9L))
                .thenReturn(new MateRequestStatusResponse(9L, "DECLINED", null));
        mockMvc.perform(patch("/api/mate-requests/9/decline").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }

    @Test
    @DisplayName("PATCH /api/mate-requests/9/cancel: 200")
    void cancel_200() throws Exception {
        when(mateRequestService.cancel(1L, 9L))
                .thenReturn(new MateRequestStatusResponse(9L, "CANCELED", null));
        mockMvc.perform(patch("/api/mate-requests/9/cancel").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    @Test
    @DisplayName("GET /api/mate-requests?role=received: 200 + 목록")
    void list_200() throws Exception {
        when(mateRequestService.getMateRequests(eq(1L), any(), any()))
                .thenReturn(List.of(new MateRequestListItemResponse(
                        7L,
                        new MateRequestListItemResponse.MateUser(2L, "옆자리", null),
                        new MateRequestListItemResponse.MateUser(1L, "수신자", null),
                        "PENDING",
                        LocalDateTime.of(2026, 6, 18, 12, 0))));
        mockMvc.perform(get("/api/mate-requests").param("role", "received")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
