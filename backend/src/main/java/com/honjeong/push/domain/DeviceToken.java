package com.honjeong.push.domain;

import java.time.LocalDateTime;

import com.honjeong.global.common.BaseTimeEntity;
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
 * 푸시 발송 대상 기기 토큰(행-per-기기).
 *
 * <p>토큰은 사용자가 아니라 <b>기기</b>에 붙는 값이다. 한 휴대폰을 두 사람이 번갈아 쓰면
 * 같은 토큰의 주인이 바뀌므로, 등록은 {@link #reassignTo}로 주인을 갱신한다.
 * 안 그러면 이전 사용자의 알림이 다음 사용자 폰에 계속 뜬다.
 *
 * <p>사용처: DeviceTokenService(등록·삭제), PushDispatcher(발송 대상 조회).
 */
@Entity
@Table(name = "device_tokens")
public class DeviceToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    private Platform platform;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /** JPA가 리플렉션으로 엔티티를 생성할 때 쓰는 기본 생성자. */
    protected DeviceToken() {
    }

    private DeviceToken(User user, String token, Platform platform, LocalDateTime now) {
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.lastUsedAt = now;
    }

    /**
     * 신규 등록.
     *
     * @param user     토큰의 주인
     * @param token    FCM 등록 토큰
     * @param platform 기기 플랫폼
     * @param now      등록 시각(마지막 사용 시각의 초기값)
     * @return 아직 저장되지 않은 새 DeviceToken
     */
    public static DeviceToken of(User user, String token, Platform platform, LocalDateTime now) {
        return new DeviceToken(user, token, platform, now);
    }

    /**
     * 이미 있는 토큰의 주인을 바꾼다(같은 기기에 다른 계정이 로그인한 경우).
     *
     * @param user 토큰의 새 주인
     * @param now  갱신 시각
     */
    public void reassignTo(User user, LocalDateTime now) {
        this.user = user;
        this.lastUsedAt = now;
    }

    /**
     * 발송 성공 시각 갱신.
     *
     * @param now 발송에 성공한 시각
     */
    public void markUsed(LocalDateTime now) {
        this.lastUsedAt = now;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public Platform getPlatform() {
        return platform;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
}
