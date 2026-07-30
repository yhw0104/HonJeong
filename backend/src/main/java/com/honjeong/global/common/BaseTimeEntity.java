package com.honjeong.global.common;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * 생성/수정 시각 감사 컬럼(created_at·updated_at)을 자동으로 채워 주는 공통 매핑 상위 클래스.
 *
 * <p>사용처: 두 컬럼을 모두 가진 엔티티가 상속한다 — User·UserFoodPreference·Place·Review·
 * FavoriteGroup·Notice·RefreshToken·SocialAccount. 자동 채움은 JpaConfig의
 * {@code @EnableJpaAuditing}이 있어야 동작한다.
 *
 * <p>created_at만 있는 엔티티(check_ins·meal_requests 등)는 개별 매핑한다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /** 엔티티 생성(최초 저장) 시각 (컬럼: created_at) */
    // 최초 INSERT 시점에 자동으로 채워진다. updatable=false라 이후 수정되지 않는다.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 엔티티 마지막 수정 시각 (컬럼: updated_at) */
    // INSERT 및 매 UPDATE마다 현재 시각으로 자동 갱신된다.
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 엔티티가 처음 저장된 시각을 반환한다.
     *
     * @return created_at 값(생성 후 변경되지 않음)
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 엔티티가 마지막으로 수정된 시각을 반환한다.
     *
     * @return updated_at 값(매 변경 시 갱신)
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
