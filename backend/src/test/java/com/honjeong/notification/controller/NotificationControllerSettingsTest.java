package com.honjeong.notification.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.honjeong.notification.dto.NotificationSettingsRequest;
import com.honjeong.notification.dto.NotificationSettingsResponse;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.notification.service.NotificationSettingsService;

/**
 * NotificationController 알림 설정 엔드포인트 슬라이스 테스트(@WebMvcTest).
 * 인증 주입(@CurrentUserId)은 형제 FileControllerTest와 동일: SecurityConfig+WebConfig import + 실 JwtProvider 토큰.
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, WebConfig.class})
class NotificationControllerSettingsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationSettingsService notificationSettingsService;

    @Test
    @DisplayName("GET /settings: USER 토큰이면 현재 설정을 반환한다")
    void getSettings() throws Exception {
        when(notificationSettingsService.getSettings(anyLong()))
                .thenReturn(new NotificationSettingsResponse(true, false, true, false));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/notifications/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meal").value(true))
                .andExpect(jsonPath("$.data.mate").value(false))
                .andExpect(jsonPath("$.data.notice").value(true))
                .andExpect(jsonPath("$.data.marketing").value(false));
    }

    @Test
    @DisplayName("PATCH /settings: 요청 4필드로 갱신하고 갱신값을 반환한다")
    void updateSettings() throws Exception {
        when(notificationSettingsService.updateSettings(anyLong(),
                eq(new NotificationSettingsRequest(false, true, false, true))))
                .thenReturn(new NotificationSettingsResponse(false, true, false, true));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(patch("/api/notifications/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meal\":false,\"mate\":true,\"notice\":false,\"marketing\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meal").value(false))
                .andExpect(jsonPath("$.data.mate").value(true))
                .andExpect(jsonPath("$.data.marketing").value(true));
    }

    @Test
    @DisplayName("GET /settings: 토큰 없으면 401")
    void getSettings_noToken_401() throws Exception {
        mockMvc.perform(get("/api/notifications/settings"))
                .andExpect(status().isUnauthorized());
    }
}
