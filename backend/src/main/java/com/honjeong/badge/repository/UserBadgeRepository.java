package com.honjeong.badge.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.badge.domain.UserBadge;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    List<UserBadge> findByUserId(Long userId);

    @Query("SELECT b.badgeKey FROM UserBadge b WHERE b.userId = :userId")
    List<String> findKeysByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
