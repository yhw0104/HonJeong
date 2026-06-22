package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

/**
 * {@link UserService}의 단위 테스트. 프로필 조회·부분수정·닉네임 중복확인의 비즈니스 규칙을 검증한다.
 * 저장소({@link UserRepository})는 Mockito로 모킹해 DB 없이 순수 로직만 확인한다.
 *
 * <p>가장 중요한 케이스는 부분수정의 닉네임 엣지다 — 본인 현재 닉네임을 그대로 두면 중복 검사를 건너뛰고
 * (existsByNickname 미호출), 다른 닉네임이 이미 사용 중이면 거부한다.
 */
class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserFoodPreferenceService foodPreferenceService = mock(UserFoodPreferenceService.class);
    private final UserService userService = new UserService(userRepository, foodPreferenceService);

    /** 프로필이 채워진 ACTIVE 회원을 만들고, 자동 생성되는 id를 리플렉션으로 강제 주입한다(모킹 반환값으로 쓰려고). */
    private User userWithId(long id) {
        User user = User.pending("01012345678", null);
        user.completeProfile("기존닉", Gender.MALE, "20s", "기존소개", "서울", 37.5, 127.0, DiningStyle.QUIET, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /**
     * given: id 1L 회원이 조회되도록 모킹.
     * when: 내 프로필 조회.
     * then: 엔티티 값이 응답 DTO로 매핑되고, phone은 원문 그대로(마스킹 없음) 노출된다.
     */
    @Test
    @DisplayName("getMyProfile: 회원이 있으면 프로필을 반환한다")
    void getMyProfile_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId(1L)));

        UserProfileResponse res = userService.getMyProfile(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.nickname()).isEqualTo("기존닉");
        assertThat(res.phone()).isEqualTo("01012345678"); // 원문 반환(마스킹 없음)
    }

    /**
     * given: id 1L 회원 + 선호 음식 스텁.
     * when: 내 프로필 조회.
     * then: 응답에 선호 음식이 포함된다.
     */
    @Test
    @DisplayName("getMyProfile: 응답에 선호 음식이 포함된다")
    void getMyProfileIncludesFoods() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId(1L)));
        when(foodPreferenceService.getFoods(1L)).thenReturn(List.of("한식", "일식"));

        assertThat(userService.getMyProfile(1L).favoriteFoods()).containsExactly("한식", "일식");
    }

    /**
     * given: 해당 id 회원이 없도록(Optional.empty) 모킹.
     * when/then: 조회하면 USER_NOT_FOUND 코드의 BusinessException이 발생한다.
     */
    @Test
    @DisplayName("getMyProfile: 회원이 없으면 USER_NOT_FOUND")
    void getMyProfile_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    /**
     * given: 닉네임 "쓰임"이 이미 사용 중(existsByNickname=true)이도록 모킹.
     * when: 닉네임 사용 가능 여부 확인.
     * then: 요청 닉네임을 echo하고 available=false를 돌려준다.
     */
    @Test
    @DisplayName("checkNickname: 사용 중이면 available=false")
    void checkNickname_taken() {
        when(userRepository.existsByNickname("쓰임")).thenReturn(true);

        NicknameCheckResponse res = userService.checkNickname("쓰임");

        assertThat(res.nickname()).isEqualTo("쓰임");
        assertThat(res.available()).isFalse();
    }

    /**
     * given: 닉네임 "빈닉"이 미사용(existsByNickname=false)이도록 모킹.
     * when: 닉네임 사용 가능 여부 확인.
     * then: available=true이고 요청 닉네임을 echo한다.
     */
    @Test
    @DisplayName("checkNickname: 미사용이면 available=true")
    void checkNickname_free() {
        when(userRepository.existsByNickname("빈닉")).thenReturn(false);

        NicknameCheckResponse res = userService.checkNickname("빈닉");

        assertThat(res.available()).isTrue();
        assertThat(res.nickname()).isEqualTo("빈닉");
    }

    /**
     * given: 현재 닉네임이 "기존닉"인 회원.
     * when: 닉네임을 "기존닉"(동일) 그대로 두고 소개만 바꿔 부분수정.
     * then: 소개가 반영되고 닉네임은 유지되며, 본인 동일 닉네임이라 중복 검사(existsByNickname)는 호출되지 않는다.
     */
    @Test
    @DisplayName("updateProfile: 닉네임을 본인 현재값과 동일하게 두면 중복검사 없이 통과한다")
    void updateProfile_sameNickname_skipsDupCheck() {
        User user = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UpdateProfileCommand cmd = new UpdateProfileCommand("기존닉", null, "새소개", null, null, null, null, null);

        UserProfileResponse res = userService.updateProfile(1L, cmd);

        assertThat(res.introduction()).isEqualTo("새소개");
        assertThat(res.nickname()).isEqualTo("기존닉");
        verify(userRepository, never()).existsByNickname(anyString());
    }

    /**
     * given: 회원이 조회되고, 바꾸려는 닉네임 "중복닉"이 이미 사용 중(existsByNickname=true)이도록 모킹.
     * when/then: 다른 닉네임으로의 변경이라 중복 검사에 걸려 NICKNAME_DUPLICATE 예외가 발생한다.
     */
    @Test
    @DisplayName("updateProfile: 닉네임을 타인과 중복되게 바꾸면 NICKNAME_DUPLICATE")
    void updateProfile_duplicateNickname_throws() {
        User user = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("중복닉")).thenReturn(true);
        UpdateProfileCommand cmd = new UpdateProfileCommand("중복닉", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> userService.updateProfile(1L, cmd))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NICKNAME_DUPLICATE);
    }

    /**
     * given: 같이먹기 수신이 기본 허용인 회원.
     * when: allowMealRequest=false만 담아 부분수정.
     * then: 수신 토글이 false로 반영된 프로필이 반환된다.
     */
    @Test
    @DisplayName("updateProfile: allowMealRequest=false 토글이 반영된다")
    void updateProfile_toggle() {
        User user = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UpdateProfileCommand cmd = new UpdateProfileCommand(null, null, null, null, null, null, null, Boolean.FALSE);

        UserProfileResponse res = userService.updateProfile(1L, cmd);

        assertThat(res.allowMealRequest()).isFalse();
    }
}
