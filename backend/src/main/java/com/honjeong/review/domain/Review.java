package com.honjeong.review.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.global.common.BaseTimeEntity;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

import jakarta.persistence.*;

/**
 * 식당 리뷰(혼밥일기) 한 건을 나타내는 엔티티 — 공개 식당 리뷰이자 개인 방문기록(ERD F-1, C3 통합).
 * (매핑 테이블: reviews)
 *
 * <p>check_in에 연결되면 "인증" 리뷰가 된다. 같은 식당에 여러 번 작성할 수 있고(유니크 없음)
 * 별점 2종은 NOT NULL이다.
 */
@Entity
@Table(name = "reviews")
public class Review extends BaseTimeEntity {

    /** 리뷰 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 리뷰 작성자 (FK: user_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 연결된 체크인 — 있으면 인증 리뷰 (FK: check_in_id, NULL 허용) */
    // 인증(있으면). NULL 허용 — 일반 리뷰.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_in_id")
    private CheckIn checkIn;

    /** 리뷰 대상 식당 (FK: place_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** 방문 시각 — 인증 리뷰면 체크인 시작 시각, 일반 리뷰면 작성 시점 */
    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    /** 리뷰 본문(TEXT, NULL 허용) */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 맛 별점(1~5, NOT NULL) */
    @Column(name = "taste_rating", nullable = false)
    private short tasteRating;

    /** 혼밥 적합도 별점(1~5, NOT NULL) */
    @Column(name = "solo_friendly_rating", nullable = false)
    private short soloFriendlyRating;

    /** 혼밥 친화 태그 목록 (review_tags 1:N, 교체 시 orphanRemoval로 기존 삭제) */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewTag> tags = new ArrayList<>();

    /** 리뷰 사진 목록 (review_photos 1:N, sortOrder 오름차순) */
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ReviewPhoto> photos = new ArrayList<>();

    protected Review() {}

    private Review(User user, CheckIn checkIn, Place place, LocalDateTime visitedAt,
            int tasteRating, int soloFriendlyRating, String content) {
        this.user = user;
        this.checkIn = checkIn;
        this.place = place;
        this.visitedAt = visitedAt;
        this.tasteRating = (short) tasteRating;
        this.soloFriendlyRating = (short) soloFriendlyRating;
        this.content = content;
    }

    /** 새 리뷰 생성. checkIn은 인증(없으면 null). 태그는 {@link #addTag}로 부착. */
    public static Review create(User user, CheckIn checkIn, Place place, LocalDateTime visitedAt,
            int tasteRating, int soloFriendlyRating, String content) {
        return new Review(user, checkIn, place, visitedAt, tasteRating, soloFriendlyRating, content);
    }

    /** 친화 태그 부착(place는 이 리뷰의 place와 동일하게 역정규화). */
    public void addTag(Place place, String tag) {
        this.tags.add(new ReviewTag(this, place, tag));
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public CheckIn getCheckIn() { return checkIn; }
    public Place getPlace() { return place; }
    public LocalDateTime getVisitedAt() { return visitedAt; }
    public String getContent() { return content; }
    public int getTasteRating() { return tasteRating; }
    public int getSoloFriendlyRating() { return soloFriendlyRating; }
    public List<ReviewTag> getTags() { return Collections.unmodifiableList(tags); }
    /** 인증 여부(연계 체크인 존재). */
    public boolean isAuthenticated() { return checkIn != null; }

    /** 이 리뷰가 주어진 사용자 소유인지. */
    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    /** 별점·본문 수정(태그는 {@link #replaceTags}로 별도 교체). place·checkIn·visitedAt은 불변. */
    public void update(int tasteRating, int soloFriendlyRating, String content) {
        this.tasteRating = (short) tasteRating;
        this.soloFriendlyRating = (short) soloFriendlyRating;
        this.content = content;
    }

    /** 친화 태그 전량 교체(기존 태그는 orphanRemoval로 삭제). null이면 빈 목록. */
    public void replaceTags(List<String> tags) {
        this.tags.clear();
        if (tags != null) {
            tags.forEach(tag -> this.tags.add(new ReviewTag(this, this.place, tag)));
        }
    }

    /** 사진 전량 교체(기존 사진은 orphanRemoval로 삭제). null이면 빈 목록. sortOrder는 리스트 순서. */
    public void replacePhotos(List<String> urls) {
        this.photos.clear();
        if (urls != null) {
            for (int i = 0; i < urls.size(); i++) {
                this.photos.add(new ReviewPhoto(this, urls.get(i), i));
            }
        }
    }

    public List<ReviewPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }
}
