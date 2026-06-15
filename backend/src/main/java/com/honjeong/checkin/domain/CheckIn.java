package com.honjeong.checkin.domain;

import java.time.LocalDateTime;

import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 혼밥 체크인(핵심 데이터). 사용자가 선택한 식당에 "혼밥 중"을 등록한 기록이며, 통계·지도·혼밥러 목록의 원천이다.
 * 사용자당 ACTIVE 1개 제약은 DB 부분 유니크 인덱스(uq_check_ins_active_user)가 보장한다.
 *
 * <p>{@code check_ins}는 {@code created_at}만 있고 {@code updated_at}이 없어 {@code BaseTimeEntity}를 상속하지 않고
 * 시각을 직접 매핑한다. User·Place는 LAZY {@code @ManyToOne}으로 두어, 혼밥러 목록·지도 집계를 프로젝션/페치조인으로 뽑는다.
 */
@Entity
@Table(name = "check_ins")
public class CheckIn {

    // PK. IDENTITY 전략 → DB의 auto-increment에 위임해 INSERT 시 채워진다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 체크인한 회원. LAZY — 혼밥러 목록에서 fetch join으로 닉네임만 가져온다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 체크인한 식당. LAZY — 지도 집계는 프로젝션, 응답엔 id만 필요.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    // 상태(ACTIVE|ENDED). 문자열 enum으로 저장.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckInStatus status;

    // 체크인 시작 시각. NOT NULL.
    @Column(nullable = false)
    private LocalDateTime startedAt;

    // 종료 시각. ACTIVE 동안 null.
    private LocalDateTime endedAt;

    // 생성 시각. INSERT 시 한 번 채워지고 갱신되지 않는다.
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** JPA가 리플렉션으로 엔티티를 생성할 때 쓰는 기본 생성자. 외부 직접 호출은 막으려고 protected. */
    protected CheckIn() {
    }

    private CheckIn(User user, Place place, LocalDateTime now) {
        this.user = user;
        this.place = place;
        this.status = CheckInStatus.ACTIVE;
        this.startedAt = now;
        this.createdAt = now;
    }

    /**
     * 새 ACTIVE 체크인을 시작한다. startedAt·createdAt은 동일한 now로 채운다.
     *
     * @param user  체크인하는 회원(영속 또는 프록시 참조)
     * @param place 체크인 대상 식당
     * @param now   현재 시각(서비스가 Clock에서 환산해 주입)
     * @return 새 ACTIVE 체크인
     */
    public static CheckIn start(User user, Place place, LocalDateTime now) {
        return new CheckIn(user, place, now);
    }

    /**
     * 체크인을 종료한다. 이미 ENDED면 아무 것도 하지 않는다(멱등).
     *
     * @param now 종료 시각
     */
    public void end(LocalDateTime now) {
        if (status == CheckInStatus.ACTIVE) {
            this.status = CheckInStatus.ENDED;
            this.endedAt = now;
        }
    }

    /** 이 체크인이 주어진 사용자 소유인지 여부. */
    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    /** 내부 식별자(PK)를 반환한다. */
    public Long getId() {
        return id;
    }

    /** 체크인한 회원을 반환한다(LAZY). */
    public User getUser() {
        return user;
    }

    /** 체크인한 식당을 반환한다(LAZY). */
    public Place getPlace() {
        return place;
    }

    /** 현재 상태(ACTIVE|ENDED)를 반환한다. */
    public CheckInStatus getStatus() {
        return status;
    }

    /** 체크인 시작 시각을 반환한다. */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /** 종료 시각을 반환한다(ACTIVE면 null). */
    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    /** 생성 시각을 반환한다. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
