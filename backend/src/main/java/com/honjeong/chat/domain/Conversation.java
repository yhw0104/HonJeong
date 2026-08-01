package com.honjeong.chat.domain;

import java.time.LocalDateTime;

import com.honjeong.global.common.BaseTimeEntity;
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

/** 매칭(meal_request)당 1개 대화방. ACTIVE=전송가능, CLOSED=읽기전용(영구보관). */
@Entity
@Table(name = "conversations")
public class Conversation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_request_id", nullable = false, unique = true)
    private Long mealRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationStatus status;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "from_last_read_at")
    private LocalDateTime fromLastReadAt;

    @Column(name = "to_last_read_at")
    private LocalDateTime toLastReadAt;

    /** from_user가 내 목록에서 이 대화를 지운 시각. NULL이면 안 지움. 메시지는 지우지 않는다. */
    @Column(name = "from_deleted_at")
    private LocalDateTime fromDeletedAt;

    /** to_user가 내 목록에서 이 대화를 지운 시각. NULL이면 안 지움. 메시지는 지우지 않는다. */
    @Column(name = "to_deleted_at")
    private LocalDateTime toDeletedAt;

    protected Conversation() {
    }

    private Conversation(Long mealRequestId, Place place, User fromUser, User toUser) {
        this.mealRequestId = mealRequestId;
        this.place = place;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.status = ConversationStatus.ACTIVE;
    }

    public static Conversation open(Long mealRequestId, Place place, User fromUser, User toUser) {
        return new Conversation(mealRequestId, place, fromUser, toUser);
    }

    public void close() {
        this.status = ConversationStatus.CLOSED;
    }

    public boolean isActive() {
        return status == ConversationStatus.ACTIVE;
    }

    public boolean isParticipant(Long userId) {
        return fromUser.getId().equals(userId) || toUser.getId().equals(userId);
    }

    public void touch(LocalDateTime now) {
        this.lastMessageAt = now;
    }

    public void markRead(Long userId, LocalDateTime now) {
        if (fromUser.getId().equals(userId)) {
            this.fromLastReadAt = now;
        } else if (toUser.getId().equals(userId)) {
            this.toLastReadAt = now;
        }
    }

    public LocalDateTime lastReadAtFor(Long userId) {
        return fromUser.getId().equals(userId) ? fromLastReadAt : toLastReadAt;
    }

    /**
     * 이 대화를 요청자의 목록에서만 숨긴다(소프트 삭제). 상대 목록과 메시지는 그대로 남는다.
     *
     * <p>이미 지운 경우 최초 삭제 시각을 덮어쓰지 않는다(멱등).
     *
     * @param userId 지우는 참여자 id
     * @param now    삭제 시각
     */
    public void deleteBy(Long userId, LocalDateTime now) {
        if (fromUser.getId().equals(userId)) {
            if (fromDeletedAt == null) {
                this.fromDeletedAt = now;
            }
        } else if (toUser.getId().equals(userId)) {
            if (toDeletedAt == null) {
                this.toDeletedAt = now;
            }
        }
    }

    /**
     * 요청자가 이 대화를 자기 목록에서 지웠는지 여부.
     *
     * @param userId 참여자 id
     * @return 지웠으면 true, 참여자가 아니면 false
     */
    public boolean isDeletedBy(Long userId) {
        if (fromUser.getId().equals(userId)) {
            return fromDeletedAt != null;
        } else if (toUser.getId().equals(userId)) {
            return toDeletedAt != null;
        }
        return false;
    }

    public Long partnerOf(Long userId) {
        return fromUser.getId().equals(userId) ? toUser.getId() : fromUser.getId();
    }

    public Long getId() {
        return id;
    }

    public Long getMealRequestId() {
        return mealRequestId;
    }

    public Place getPlace() {
        return place;
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    /**
     * from_user가 이 대화를 자기 목록에서 지운 시각.
     *
     * @return 삭제 시각(안 지웠으면 null)
     */
    public LocalDateTime getFromDeletedAt() {
        return fromDeletedAt;
    }

    /**
     * to_user가 이 대화를 자기 목록에서 지운 시각.
     *
     * @return 삭제 시각(안 지웠으면 null)
     */
    public LocalDateTime getToDeletedAt() {
        return toDeletedAt;
    }
}
