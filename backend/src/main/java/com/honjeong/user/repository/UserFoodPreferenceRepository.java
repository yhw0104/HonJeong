package com.honjeong.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.user.domain.UserFoodPreference;

/**
 * 회원 선호 음식 행 데이터 접근. (대상 테이블: user_food_preferences)
 *
 * <p>사용자당 1행이므로 user_id 기준 단건 조회를 제공한다.
 */
public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, Long> {

    /**
     * 회원 id로 선호 음식 행을 조회한다(사용자당 1행).
     *
     * @param userId 회원 ID
     * @return 선호 음식 행(없으면 빈 Optional)
     */
    Optional<UserFoodPreference> findByUserId(Long userId);

    /**
     * 사용자의 선호 음식 행을 삭제한다(탈퇴 시 개인정보 정리용).
     *
     * @param userId 대상 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM UserFoodPreference p WHERE p.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
