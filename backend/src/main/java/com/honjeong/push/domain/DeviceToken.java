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
 * 같은 토큰의 주인이 바뀌므로, 등록은 주인·플랫폼을 함께 갱신하는 UPSERT다
 * ({@code DeviceTokenRepository.upsert} — 앱 시작 시 등록이 거의 동시에 두 번 뜨는 경합이 있어
 * "조회 후 갱신"이 아니라 DB의 ON CONFLICT로 처리한다). 안 그러면 이전 사용자의 알림이
 * 다음 사용자 폰에 계속 뜬다.
 *
 * <p>사용처: DeviceTokenService(등록·삭제), PushAudienceReader(발송 대상 조회).
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

    @Column(name = "last_registered_at", nullable = false)
    private LocalDateTime lastRegisteredAt;

    /** JPA가 리플렉션으로 엔티티를 생성할 때 쓰는 기본 생성자. */
    protected DeviceToken() {
    }

    private DeviceToken(User user, String token, Platform platform, LocalDateTime now) {
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.lastUsedAt = now;
        this.lastRegisteredAt = now;
    }

    /**
     * 신규 등록.
     *
     * <p>★ <b>운영 코드에는 이 경로로 들어오는 INSERT가 없다.</b> 등록은 전부
     * {@code DeviceTokenRepository.upsert}(네이티브 ON CONFLICT)가 처리한다 — 동시 등록 경합 때문이다.
     * 지금 이 팩토리는 테스트 픽스처 전용이다. 새 등록 경로를 만들 일이 생기면 여기가 아니라
     * upsert를 쓴다(그래야 경합이 다시 열리지 않는다).
     *
     * @param user     토큰의 주인
     * @param token    FCM 등록 토큰
     * @param platform 기기 플랫폼
     * @param now      등록 시각(마지막 사용 시각·마지막 등록 시각의 초기값)
     * @return 아직 저장되지 않은 새 DeviceToken
     */
    public static DeviceToken of(User user, String token, Platform platform, LocalDateTime now) {
        return new DeviceToken(user, token, platform, now);
    }

    /**
     * 마지막 발송 <b>시도</b> 시각 갱신.
     *
     * <p>성공만 찍히는 값이 아니다 — 사유는 {@code PushDeliveryRecorder.recordResult} Javadoc 참조.
     *
     * <p>★ <b>{@code lastRegisteredAt}은 절대 건드리지 않는다.</b> 그 칸은 "앱이 등록한 시각"이고
     * staleness 청소의 유일한 판단 기준이다. 여기서 함께 갱신하면 지우려는 고아 토큰이
     * (계속 발송되고 있으므로) 영원히 신선해 보여 청소가 무력해진다. 이 성질은
     * {@code DeviceTokenRepositoryTest.발송은_등록시각을_갱신하지_않는다}가 고정하고 있다.
     *
     * @param now 발송을 시도한 시각
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

    public LocalDateTime getLastRegisteredAt() {
        return lastRegisteredAt;
    }
}
