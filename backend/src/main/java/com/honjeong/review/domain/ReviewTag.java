package com.honjeong.review.domain;

import com.honjeong.place.domain.Place;
import jakarta.persistence.*;

/** 리뷰의 친화 태그. place_id는 reviews에서 역정규화(식당별 빈도 집계를 JOIN 없이). */
@Entity
@Table(name = "review_tags")
public class ReviewTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

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
