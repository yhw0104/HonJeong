package com.honjeong.meal.domain;

import java.time.LocalDateTime;

import com.honjeong.checkin.domain.CheckIn;
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
 * 같이먹기 신청. 신청자(fromUser)가 대상 혼밥러의 체크인(toCheckIn)에 보낸다. 수신자는 {@code toCheckIn.user}로 식별한다.
 * {@code created_at}·{@code responded_at}만 있고 {@code updated_at}이 없어 {@code BaseTimeEntity}를 상속하지 않는다(CheckIn 패턴).
 * 단일 신청 중복은 DB 유니크(from_user_id, to_check_in_id)가 강제한다.
 */
@Entity
@Table(name = "meal_requests")
public class MealRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청자. LAZY — 목록에서 fetch join으로 닉네임만 가져온다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    // 대상 혼밥러의 체크인. 수신자(소유자)·장소의 원천. LAZY.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_check_in_id", nullable = false)
    private CheckIn toCheckIn;

    // 신청 발생 장소(대상 체크인의 place에서 파생·역정규화). LAZY — 응답엔 id만 필요(프록시 getId는 로딩 없음).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    // 인사 한마디(선택, 최대 200자).
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealRequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수락/거절 시각. PENDING 동안 null.
    private LocalDateTime respondedAt;

    /** JPA용 기본 생성자. 외부 직접 호출은 막으려고 protected. */
    protected MealRequest() {
    }

    private MealRequest(User fromUser, CheckIn toCheckIn, Place place, String message, LocalDateTime now) {
        this.fromUser = fromUser;
        this.toCheckIn = toCheckIn;
        this.place = place;
        this.message = message;
        this.status = MealRequestStatus.PENDING;
        this.createdAt = now;
    }

    /**
     * 새 PENDING 신청을 만든다.
     *
     * @param fromUser  신청자(영속 또는 프록시 참조)
     * @param toCheckIn 대상 혼밥러의 체크인
     * @param place     대상 체크인의 장소(역정규화 저장)
     * @param message   인사 한마디(nullable)
     * @param now       생성 시각
     * @return PENDING 신청
     */
    public static MealRequest create(User fromUser, CheckIn toCheckIn, Place place, String message, LocalDateTime now) {
        return new MealRequest(fromUser, toCheckIn, place, message, now);
    }

    /** 신청을 수락 처리한다(ACCEPTED + respondedAt). PENDING 가드는 서비스가 한다. */
    public void accept(LocalDateTime now) {
        this.status = MealRequestStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /** 신청을 거절 처리한다(DECLINED + respondedAt). */
    public void decline(LocalDateTime now) {
        this.status = MealRequestStatus.DECLINED;
        this.respondedAt = now;
    }

    /** 아직 응답 전(PENDING)인지. */
    public boolean isPending() {
        return status == MealRequestStatus.PENDING;
    }

    /** 이 신청의 수신자(대상 체크인 주인)가 주어진 사용자인지. */
    public boolean isReceivedBy(Long userId) {
        return toCheckIn.getUser().getId().equals(userId);
    }

    /** 내부 식별자(PK)를 반환한다. */
    public Long getId() {
        return id;
    }

    /** 신청자를 반환한다(LAZY). */
    public User getFromUser() {
        return fromUser;
    }

    /** 대상 혼밥러의 체크인을 반환한다(LAZY). */
    public CheckIn getToCheckIn() {
        return toCheckIn;
    }

    /** 신청 발생 장소를 반환한다(LAZY). */
    public Place getPlace() {
        return place;
    }

    /** 인사 한마디를 반환한다(없으면 null). */
    public String getMessage() {
        return message;
    }

    /** 현재 상태(PENDING|ACCEPTED|DECLINED)를 반환한다. */
    public MealRequestStatus getStatus() {
        return status;
    }

    /** 신청 생성 시각을 반환한다. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 수락/거절 시각을 반환한다(PENDING이면 null). */
    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
}
