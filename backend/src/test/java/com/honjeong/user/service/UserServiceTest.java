package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;
import com.honjeong.user.domain.User;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.repository.UserRepository;

/** {@link UserService} 단위 테스트(Repository는 Mockito 모킹, DB 불필요). */
class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    /** ACTIVE 회원 + id 강제 주입. */
    private User userWithId(long id) {
        User user = User.pending("01012345678", null);
        user.completeProfile("기존닉", Gender.MALE, "20s", "기존소개", "서울", 37.5, 127.0, DiningStyle.QUIET, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("getMyProfile: 회원이 있으면 프로필을 반환한다")
    void getMyProfile_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId(1L)));

        UserProfileResponse res = userService.getMyProfile(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.nickname()).isEqualTo("기존닉");
        assertThat(res.phone()).isEqualTo("01012345678"); // 원문 반환(마스킹 없음)
    }

    @Test
    @DisplayName("getMyProfile: 회원이 없으면 USER_NOT_FOUND")
    void getMyProfile_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("checkNickname: 사용 중이면 available=false")
    void checkNickname_taken() {
        when(userRepository.existsByNickname("쓰임")).thenReturn(true);

        NicknameCheckResponse res = userService.checkNickname("쓰임");

        assertThat(res.nickname()).isEqualTo("쓰임");
        assertThat(res.available()).isFalse();
    }

    @Test
    @DisplayName("checkNickname: 미사용이면 available=true")
    void checkNickname_free() {
        when(userRepository.existsByNickname("빈닉")).thenReturn(false);

        NicknameCheckResponse res = userService.checkNickname("빈닉");

        assertThat(res.available()).isTrue();
        assertThat(res.nickname()).isEqualTo("빈닉");
    }
}
