package com.honjeong.badge.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.badge.domain.UserBadge;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserId(Long userId);

    @Query("SELECT b.badgeKey FROM UserBadge b WHERE b.userId = :userId")
    List<String> findKeysByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    /**
     * 멱등 지급: (user_id, badge_key) 유니크 충돌 시 예외 없이 0행 반환.
     * 실PostgreSQL에서 예외 기반 dedup(save+catch)은 트랜잭션을 중단(25P02)시켜
     * 이후 문장까지 실패하므로, DB 네이티브 ON CONFLICT로 대체한다.
     * @return 1=새로 지급, 0=이미 보유(경합)
     */
    @Modifying
    @Query(value = "INSERT INTO user_badges (user_id, badge_key, earned_at) "
            + "VALUES (:userId, :badgeKey, :earnedAt) ON CONFLICT (user_id, badge_key) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("badgeKey") String badgeKey,
            @Param("earnedAt") LocalDateTime earnedAt);

    /**
     * 기능: 사용자가 보유한 뱃지를 전부 삭제(탈퇴 시 개인정보 정리용)
     * 쿼리: DELETE FROM user_badges WHERE user_id = :userId
     * Request: userId — 대상 사용자 ID / Response: int — 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM UserBadge ub WHERE ub.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
