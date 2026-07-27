package com.honjeong.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.auth.service.TokenService;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserRepository;

/**
 * 계정 상태 강제가 실제 HTTP 요청에서 동작하는지 검증한다.
 * 단위 테스트로는 필터 등록 위치(BearerTokenAuthenticationFilter 뒤)를 확인할 수 없어 MockMvc를 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActiveUserFilterTest extends AbstractPostgresTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;
    @Autowired private JwtProvider jwtProvider;

    @Test
    @DisplayName("ACTIVE 회원의 토큰은 통과한다")
    void activeUserPasses() throws Exception {
        String token = tokenService.issue(activeUser().getId()).accessToken();
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("탈퇴한 회원의 남은 토큰은 401 ACCOUNT_INACTIVE로 막힌다")
    void withdrawnUserBlocked() throws Exception {
        User user = activeUser();
        String token = tokenService.issue(user.getId()).accessToken();
        // Task 1 시점에는 User.withdraw()가 아직 없다(Task 2에서 추가). 상태만 직접 주입해 재현한다.
        ReflectionTestUtils.setField(user, "status", UserStatus.WITHDRAWN);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("users에 없는 사용자 id의 (정상 서명된) 토큰은 401 ACCOUNT_INACTIVE로 막힌다 — 500이 아니다")
    void unknownUserBlocked() throws Exception {
        // findStatusById가 empty를 돌려주는 fail-closed 분기: 서명은 유효하지만 그 sub의 회원 행이 없다
        // (탈퇴 후 완전 삭제·데이터 정합성 붕괴 등). orElse(null) 경로가 실제로 401로 이어지는지 확인한다.
        String token = jwtProvider.createAccessToken(999_999_999L);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));
    }

    private User activeUser() {
        User u = User.pending("01099998888", null);
        u.completeProfile("상태테스트", null, null, null, null, null, null, null, null);
        return userRepository.saveAndFlush(u);
    }
}
