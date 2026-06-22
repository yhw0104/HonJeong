package com.honjeong.user.service;

import java.util.List;

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
    private final UserFoodPreferenceService foodPreferenceService;

    /**
     * 의존성을 주입받아 서비스를 구성한다(생성자 주입 + {@code final}).
     *
     * @param userRepository        회원 조회·닉네임 중복확인용 저장소
     * @param foodPreferenceService 선호 음식 upsert·조회 서비스
     */
    public UserService(UserRepository userRepository, UserFoodPreferenceService foodPreferenceService) {
        this.userRepository = userRepository;
        this.foodPreferenceService = foodPreferenceService;
    }

    /**
     * 내 프로필을 조회한다.
     *
     * @param userId 조회할 회원 식별자(JWT sub에서 주입됨)
     * @return 회원의 프로필 전 필드를 담은 {@link UserProfileResponse}
     * @throws BusinessException 해당 회원이 없을 때({@code USER_NOT_FOUND})
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(long userId) {
        return UserProfileResponse.from(findUser(userId), foodPreferenceService.getFoods(userId));
    }

    /**
     * 프로필을 부분 수정한다(PATCH). 명령에서 null이 아닌 필드만 반영하고, null 필드는 기존 값을 유지한다.
     *
     * <p>닉네임 처리: 닉네임이 들어오고 <b>현재 닉네임과 다를 때만</b> 중복 검사를 한다(본인 닉네임을 그대로 두면
     * 중복 검사를 건너뛴다). 다른 닉네임이 이미 사용 중이면 거부한다. 실제 필드 반영은 엔티티의
     * {@link User#updateProfile}이 담당하며, 영속성 컨텍스트의 dirty checking으로 커밋 시 UPDATE가 나간다.
     *
     * @param userId  수정할 회원 식별자(JWT sub에서 주입됨)
     * @param command 부분수정 입력값 묶음(null 필드는 미변경)
     * @return 수정이 반영된 최신 프로필 {@link UserProfileResponse}
     * @throws BusinessException 회원이 없거나({@code USER_NOT_FOUND}) 닉네임이 타인과 중복일 때({@code NICKNAME_DUPLICATE})
     */
    @Transactional
    public UserProfileResponse updateProfile(long userId, UpdateProfileCommand command) {
        User user = findUser(userId);
        if (command.nickname() != null && !command.nickname().equals(user.getNickname())
                && userRepository.existsByNickname(command.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
        user.updateProfile(command.nickname(), command.profileImageUrl(), command.introduction(),
                command.region(), command.regionLat(), command.regionLng(),
                command.diningStyle(), command.allowMealRequest());
        List<String> foods = foodPreferenceService.replaceFoods(userId, command.favoriteFoods());
        return UserProfileResponse.from(user, foods);
    }

    /**
     * 닉네임 사용 가능 여부를 확인한다(온보딩·프로필 수정 중 실시간 중복 체크).
     *
     * @param nickname 확인할 닉네임
     * @return 확인한 닉네임과 사용 가능 여부({@code available = 미사용})를 담은 {@link NicknameCheckResponse}
     */
    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname) {
        return new NicknameCheckResponse(nickname, !userRepository.existsByNickname(nickname));
    }

    /**
     * userId로 회원을 조회하고, 없으면 예외를 던진다. 조회·수정 메서드가 공유하는 헬퍼다.
     *
     * @param userId 조회할 회원 식별자
     * @return 조회된 {@link User}
     * @throws BusinessException 회원이 없을 때({@code USER_NOT_FOUND})
     */
    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
