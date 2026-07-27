package com.honjeong.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.support.ActiveUserSliceSupport;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

/**
 * {@link SecurityConfig}와 {@link WebConfig}를 함께 올려 보안 필터 + @CurrentUserId 주입을 통합 검증하는 슬라이스 테스트.
 * 토큰 유무·종류에 따른 401/403/200 동작과 sub 주입을 임시 PingController로 확인한다.
 */
@WebMvcTest(controllers = SecurityPingTest.PingController.class)
@Import({SecurityConfig.class, WebConfig.class, SecurityPingTest.PingController.class})
class SecurityPingTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("토큰 없이 보호된 엔드포인트 호출 시 401")
    void noToken_returns401() throws Exception {
        // given: 토큰 없음
        // when: USER 전용 엔드포인트를 호출하면
        // then: 인증 진입점이 401을 반환한다
        mockMvc.perform(get("/api/__sec/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 액세스 토큰이면 200이고 @CurrentUserId로 sub가 주입된다")
    void accessToken_resolvesUserId() throws Exception {
        // given: userId=42인 액세스 토큰(ROLE_USER)
        String token = jwtProvider.createAccessToken(42L);

        // when: Bearer 토큰을 달아 호출하면
        // then: 200이고 본문에 주입된 sub(42)가 그대로 반환된다
        mockMvc.perform(get("/api/__sec/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    @DisplayName("온보딩 토큰으로 USER 전용 엔드포인트 호출 시 403")
    void onboardingToken_onUserEndpoint_returns403() throws Exception {
        // given: 온보딩 토큰(ROLE_ONBOARDING) — 인증은 되지만 USER 권한은 없음
        String token = jwtProvider.createOnboardingToken(42L);

        // when: USER 전용 엔드포인트를 호출하면
        // then: 권한 부족으로 접근 거부 핸들러가 403을 반환한다
        mockMvc.perform(get("/api/__sec/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    /** @CurrentUserId 주입과 USER 권한 게이팅을 확인하기 위한 테스트 전용 보호 엔드포인트. */
    @RestController
    static class PingController {

        /** 주입된 사용자 id를 그대로 돌려주는 USER 전용 엔드포인트. */
        @GetMapping("/api/__sec/me")
        Long me(@CurrentUserId Long userId) {
            return userId;
        }
    }
}
