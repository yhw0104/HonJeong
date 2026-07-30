package com.honjeong.review.domain;

import com.honjeong.place.domain.Place;
import jakarta.persistence.*;

/**
 * 리뷰에 부착된 혼밥 친화 태그 한 개를 나타내는 엔티티.
 * (매핑 테이블: review_tags)
 *
 * <p>place_id는 reviews에서 역정규화한 값이다 — 식당별 태그 빈도를 JOIN 없이 집계하기 위함이다.
 */
@Entity
@Table(name = "review_tags")
public class ReviewTag {

    /** 태그 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 태그가 속한 리뷰 (FK: review_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /** 태그가 달린 식당 — reviews에서 역정규화 (FK: place_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** 태그 문자열(최대 30자, SoloFriendlyTags 프리셋 중 하나) */
    @Column(nullable = false, length = 30)
    private String tag;

    protected ReviewTag() {}

    ReviewTag(Review review, Place place, String tag) {
        this.review = review;
        this.place = place;
        this.tag = tag;
    }

    public Long getId() { return id; }
    public Place getPlace() { return place; }
    public String getTag() { return tag; }
}
