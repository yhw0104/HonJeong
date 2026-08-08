package com.honjeong.push.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.honjeong.push.domain.Platform;
import com.honjeong.push.service.DeviceTokenService;
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * {@link DeviceTokenController}의 웹 계층 슬라이스 테스트.
 *
 * <p><b>검증 목적은 하위호환 하나다.</b> 등록 본문에 {@code installationId}가 2026-08-08에 추가됐는데,
 * 서버는 앱보다 먼저 배포된다 — 한동안 그 값을 보내지 않는 구버전 앱(현재 TestFlight 빌드)이 계속
 * 등록한다. 그 요청이 400으로 죽으면 <b>그 사용자들의 푸시가 통째로 끊긴다.</b> 필드에 {@code @NotNull}을
 * 실수로 붙이는 순간 그렇게 되는데, 서비스·리포지토리 테스트로는 잡히지 않는다(검증은 웹 계층에서 돈다).
 *
 * <p>하네스는 {@code BadgeControllerTest}와 같다 — SecurityConfig·WebConfig를 함께 올려 실제 인가와
 * {@code @CurrentUserId} 리졸버를 통과시키고, 인증은 JwtProvider가 발급한 진짜 access 토큰으로 한다.
 */
@WebMvcTest(controllers = DeviceTokenController.class)
@Import({SecurityConfig.class, WebConfig.class})
class DeviceTokenControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private DeviceTokenService deviceTokenService;

    /**
     * given: installationId가 없는 <b>구버전 앱</b>의 등록 본문.
     * when: {@code POST /api/device-tokens}.
     * then: 200이고 서비스에 installationId=null이 전달된다 — 필수로 바뀌면 여기서 400으로 깨진다.
     */
    @Test
    @DisplayName("★ installationId 없이 등록해도 200 — 구버전 앱의 푸시가 끊기면 안 된다")
    void 설치ID_없는_구버전_본문도_통과한다() throws Exception {
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(post("/api/device-tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"fcm-tok\",\"platform\":\"IOS\"}"))
                .andExpect(status().isOk());

        verify(deviceTokenService).register(eq(1L), eq("fcm-tok"), eq(Platform.IOS), isNull());
    }

    /**
     * given: installationId를 실은 새 앱의 등록 본문.
     * when: {@code POST /api/device-tokens}.
     * then: 200이고 그 값이 서비스까지 그대로 전달된다.
     */
    @Test
    @DisplayName("installationId를 보내면 서비스까지 그대로 전달된다")
    void 설치ID를_보내면_전달된다() throws Exception {
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(post("/api/device-tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"fcm-tok\",\"platform\":\"IOS\",\"installationId\":\"install-1\"}"))
                .andExpect(status().isOk());

        verify(deviceTokenService).register(eq(1L), eq("fcm-tok"), eq(Platform.IOS), eq("install-1"));
    }

    /**
     * given: 64자를 넘는 installationId.
     * when: {@code POST /api/device-tokens}.
     * then: 400 — DB 컬럼(VARCHAR 64) 위반이 500으로 내려가지 않게 웹 계층에서 막는다.
     */
    @Test
    @DisplayName("installationId가 64자를 넘으면 400 — DB 제약 위반이 500으로 새지 않게")
    void 설치ID가_너무_길면_400() throws Exception {
        String token = jwtProvider.createAccessToken(1L);
        String tooLong = "x".repeat(65);

        mockMvc.perform(post("/api/device-tokens")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"fcm-tok\",\"platform\":\"IOS\",\"installationId\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
