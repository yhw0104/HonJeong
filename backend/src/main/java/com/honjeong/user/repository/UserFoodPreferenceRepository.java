package com.honjeong.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.user.domain.UserFoodPreference;

/**
 * 선호 음식 영속성 접근. 사용자당 1행이므로 user_id 기준 단건 조회를 제공한다.
 */
public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, Long> {

    /** 회원 id로 선호 음식 행을 조회: WHERE user_id = ?. 없으면 Optional.empty(). */
    Optional<UserFoodPreference> findByUserId(Long userId);
}
