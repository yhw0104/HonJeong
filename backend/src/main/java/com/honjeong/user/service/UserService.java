package com.honjeong.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.user.domain.User;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.repository.UserRepository;

/**
 * 회원 프로필 조회·수정·닉네임 중복확인을 담당하는 서비스. 조회는 readOnly 트랜잭션, 수정은 쓰기 트랜잭션
 * 경계를 가진다. UserRepository(findById·existsByNickname)만 의존한다.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 내 프로필 조회. 회원이 없으면 {@code USER_NOT_FOUND}. */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    /** 닉네임 사용 가능 여부. 존재하지 않으면 available=true. */
    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname) {
        return new NicknameCheckResponse(nickname, !userRepository.existsByNickname(nickname));
    }

    /** userId로 회원을 찾고 없으면 {@code USER_NOT_FOUND}를 던진다. */
    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
