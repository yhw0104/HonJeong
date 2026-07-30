package com.honjeong.favorite.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.honjeong.place.domain.Place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 즐겨찾기 그룹에 담긴 장소 1건(그룹-장소 매핑)을 나타내는 엔티티.
 * (엔티티: 매핑 테이블 favorites)
 */
@Entity
@Table(name = "favorites")
@EntityListeners(AuditingEntityListener.class)
public class Favorite {

    /** 즐겨찾기 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 즐겨찾기가 속한 그룹 (FK: group_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private FavoriteGroup group;

    /** 즐겨찾기된 장소(식당) (FK: place_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** 담은 시각 (JPA Auditing 자동 기록, 수정 불가) */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Favorite() {}

    private Favorite(FavoriteGroup group, Place place) {
        this.group = group;
        this.place = place;
    }

    /**
     * 그룹-장소 매핑 즐겨찾기 인스턴스 생성(정적 팩토리).
     *
     * @param group 담을 그룹
     * @param place 담을 장소
     * @return 저장 전 새 즐겨찾기 엔티티
     */
    public static Favorite of(FavoriteGroup group, Place place) {
        return new Favorite(group, place);
    }

    public Long getId() { return id; }
    public FavoriteGroup getGroup() { return group; }
    public Place getPlace() { return place; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
