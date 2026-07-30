package com.honjeong.review.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * 리뷰에 첨부된 사진 한 장을 나타내는 엔티티(ERD F-2).
 * (매핑 테이블: review_photos)
 *
 * <p>리뷰와 1:N이며 sort_order로 표시 순서를 유지한다.
 */
@Entity
@Table(name = "review_photos")
public class ReviewPhoto {

    /** 사진 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사진이 속한 리뷰 (FK: review_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /** 사진 이미지 URL(최대 500자) */
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /** 표시 순서(0부터 오름차순) */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ReviewPhoto() {}

    ReviewPhoto(Review review, String imageUrl, int sortOrder) {
        this.review = review;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public int getSortOrder() { return sortOrder; }
}
