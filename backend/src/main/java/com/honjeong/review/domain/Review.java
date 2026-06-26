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
 * 리뷰 = 혼밥일기(ERD F-1, C3 통합). 공개 식당 리뷰이자 개인 방문기록.
 * check_in 연결 시 "인증". 같은 식당 다회 작성 허용(유니크 없음). 별점 2종은 NOT NULL.
 */
@Entity
@Table(name = "reviews")
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 인증(있으면). NULL 허용 — 일반 리뷰.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_in_id")
    private CheckIn checkIn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "taste_rating", nullable = false)
    private short tasteRating;

    @Column(name = "solo_friendly_rating", nullable = false)
    private short soloFriendlyRating;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewTag> tags = new ArrayList<>();

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
}
