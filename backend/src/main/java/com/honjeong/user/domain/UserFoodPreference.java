package com.honjeong.user.domain;

import java.util.ArrayList;
import java.util.List;

import com.honjeong.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 회원의 선호 음식(최대 3개)을 담는 데이터.
 * (매핑 테이블: user_food_preferences)
 *
 * <p>사용자당 1행이며 user_id에 UNIQUE 제약이 걸린다. 최대 3개를 고정 컬럼(food1/food2/food3)으로 보관하고,
 * 목록(List&lt;String&gt;) ↔ 3개 컬럼 변환을 엔티티가 책임진다. created_at/updated_at은 BaseTimeEntity.
 */
@Entity
@Table(name = "user_food_preferences")
public class UserFoodPreference extends BaseTimeEntity {

    /** 보관 가능한 선호 음식 최대 개수(3). */
    private static final int MAX_FOODS = 3;

    /** PK. IDENTITY 전략 — DB auto-increment로 채워진다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유 회원 id. 사용자당 1행이라 user_id에 UNIQUE(Flyway 스키마). 연관관계 없이 Long.
    @Column(nullable = false)
    private Long userId;

    /** 선호 음식 1번째(없으면 null, 최대 50자). */
    @Column(length = 50)
    private String food1;
    /** 선호 음식 2번째(없으면 null, 최대 50자). */
    @Column(length = 50)
    private String food2;
    /** 선호 음식 3번째(없으면 null, 최대 50자). */
    @Column(length = 50)
    private String food3;

    /** JPA용 기본 생성자(외부 직접 사용 금지). */
    protected UserFoodPreference() {
    }

    /** 소유 회원 id만 채운 빈 행을 만든다 — {@link #of(Long, List)} 팩토리 내부 전용. */
    private UserFoodPreference(Long userId) {
        this.userId = userId;
    }

    /** 회원 id와 선호 음식 목록으로 새 행을 만든다(앞 3개만 반영). */
    public static UserFoodPreference of(Long userId, List<String> foods) {
        UserFoodPreference pref = new UserFoodPreference(userId);
        pref.updateFoods(foods);
        return pref;
    }

    /** 선호 음식을 통째로 교체한다(앞 3개만, 모자라면 null). */
    public void updateFoods(List<String> foods) {
        List<String> safe = foods == null ? List.of() : foods;
        this.food1 = safe.size() > 0 ? safe.get(0) : null;
        this.food2 = safe.size() > 1 ? safe.get(1) : null;
        this.food3 = safe.size() > 2 ? safe.get(2) : null;
    }

    /** 비어있지 않은 음식만 순서대로 반환한다(0~3개). */
    public List<String> toFoods() {
        List<String> foods = new ArrayList<>(MAX_FOODS);
        if (food1 != null) foods.add(food1);
        if (food2 != null) foods.add(food2);
        if (food3 != null) foods.add(food3);
        return foods;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }
}
