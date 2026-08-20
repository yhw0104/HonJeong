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
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * NotificationController 알림 설정 엔드포인트 슬라이스 테스트(@WebMvcTest).
 * 인증 주입(@CurrentUserId)은 형제 FileControllerTest와 동일: SecurityConfig+WebConfig import + 실 JwtProvider 토큰.
 */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, WebConfig.class})
class NotificationControllerSettingsTest extends ActiveUserSliceSupport {

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
                .thenReturn(new NotificationSettingsResponse(true, false, true, false, true));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/notifications/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meal").value(true))
                .andExpect(jsonPath("$.data.mate").value(false))
                .andExpect(jsonPath("$.data.notice").value(true))
                .andExpect(jsonPath("$.data.marketing").value(false))
                .andExpect(jsonPath("$.data.badge").value(true));
    }

    /**
     * ★이 테스트의 본문은 <b>badge가 없는 4필드</b>다 — 이미 배포된 앱(1.0.0 빌드 26)이 보내는
     * 모양 그대로다. 그래서 여기서 고정하는 명제는 "구버전 앱의 요청이 badge=null로 도착한다"이고,
     * 그 null을 서비스가 "안 보냈다"로 읽어 기존 값을 지킨다(NotificationSettingsServiceTest).
     *
     * <p>badge를 원시 boolean으로 되돌리면 이 요청이 badge=false로 도착해 여기서 빨개진다 —
     * 그때 실제로 일어나는 일은 구버전 앱 사용자가 토글을 아무거나 하나 건드리는 순간
     * 뱃지 알림이 조용히 꺼지는 것이다.
     */
    @Test
    @DisplayName("★PATCH /settings: badge 없는 구버전 앱 요청은 badge=null로 도착한다")
    void updateSettings_legacyBodyWithoutBadge() throws Exception {
        when(notificationSettingsService.updateSettings(anyLong(),
                eq(new NotificationSettingsRequest(false, true, false, true, null))))
                .thenReturn(new NotificationSettingsResponse(false, true, false, true, true));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(patch("/api/notifications/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meal\":false,\"mate\":true,\"notice\":false,\"marketing\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meal").value(false))
                .andExpect(jsonPath("$.data.mate").value(true))
                .andExpect(jsonPath("$.data.marketing").value(true))
                .andExpect(jsonPath("$.data.badge").value(true)); // 건드리지 않았으므로 기존 값 그대로
    }

    /** 새 앱이 보내는 모양 — badge를 담으면 그대로 서비스에 전달된다. */
    @Test
    @DisplayName("PATCH /settings: badge를 담아 보내면 그 값이 서비스로 전달된다")
    void updateSettings_withBadge() throws Exception {
        when(notificationSettingsService.updateSettings(anyLong(),
                eq(new NotificationSettingsRequest(true, true, true, false, false))))
                .thenReturn(new NotificationSettingsResponse(true, true, true, false, false));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(patch("/api/notifications/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"meal\":true,\"mate\":true,\"notice\":true,\"marketing\":false,\"badge\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.badge").value(false));
    }

    @Test
    @DisplayName("GET /settings: 토큰 없으면 401")
    void getSettings_noToken_401() throws Exception {
        mockMvc.perform(get("/api/notifications/settings"))
                .andExpect(status().isUnauthorized());
    }
}
