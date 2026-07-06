package com.honjeong.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.user.domain.UserFoodPreference;

/**
 * 1. 기능: 회원 선호 음식 행 데이터 접근 (대상 테이블: user_food_preferences)
 *
 * <p>[기존 주석] 선호 음식 영속성 접근. 사용자당 1행이므로 user_id 기준 단건 조회를 제공한다.
 */
public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, Long> {

    /**
     * 기능: 회원 id로 선호 음식 행 단건 조회(사용자당 1행)
     * 쿼리: SELECT * FROM user_food_preferences WHERE user_id = :userId
     * Request: userId — 회원 ID / Response: Optional&lt;UserFoodPreference&gt; — 선호 음식 행(없으면 empty)
     *
     * <p>[기존 주석] 회원 id로 선호 음식 행을 조회: WHERE user_id = ?. 없으면 Optional.empty().
     */
    Optional<UserFoodPreference> findByUserId(Long userId);
}
