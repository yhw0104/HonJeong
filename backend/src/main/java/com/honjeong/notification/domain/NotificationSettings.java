package com.honjeong.notification.domain;

import com.honjeong.global.common.BaseTimeEntity;
import jakarta.persistence.*;

/**
 * 사용자별 알림 수신 설정(1인 1행). 행이 없으면 {@link #of(Long)}의 기본값으로 간주한다(lazy).
 *
 * <p>meal/mate는 현재 발행되는 알림을 게이팅하고, notice/marketing은 해당 알림 기능이 생길 때 소비되는
 * 미래용 컬럼이다(marketing=이벤트·혜택 광고성 수신 동의, 옵트인 기본 false).
 */
@Entity
@Table(name = "notification_settings")
public class NotificationSettings extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "meal_enabled", nullable = false)
    private boolean mealEnabled;

    @Column(name = "mate_enabled", nullable = false)
    private boolean mateEnabled;

    @Column(name = "notice_enabled", nullable = false)
    private boolean noticeEnabled;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled;

    @Column(name = "badge_enabled", nullable = false)
    private boolean badgeEnabled;

    protected NotificationSettings() {
    }

    private NotificationSettings(Long userId, boolean meal, boolean mate, boolean notice, boolean marketing,
            boolean badge) {
        this.userId = userId;
        this.mealEnabled = meal;
        this.mateEnabled = mate;
        this.noticeEnabled = notice;
        this.marketingEnabled = marketing;
        this.badgeEnabled = badge;
    }

    /** 기본값 행: 같이먹기·메이트·공지·뱃지 ON, 이벤트·혜택(marketing) OFF(옵트인). */
    public static NotificationSettings of(Long userId) {
        return new NotificationSettings(userId, true, true, true, false, true);
    }

    /**
     * 수신 설정을 갱신한다.
     *
     * <p>★{@code badge}만 {@code Boolean}이다. 이 필드는 나중에 생겼고, 이미 배포된 앱
     * (1.0.0 빌드 26)은 이 값을 보내지 않는다. 원시 boolean으로 받으면 Jackson이 false로 채워
     * <b>구버전 앱에서 토글을 아무거나 하나 건드리는 순간 뱃지 알림이 조용히 꺼진다</b>.
     * 그래서 null이면 "안 보낸 것"으로 보고 기존 값을 그대로 둔다.
     * 앱이 전부 새 버전으로 올라가면 원시 boolean으로 좁혀도 된다.
     */
    public void update(boolean meal, boolean mate, boolean notice, boolean marketing, Boolean badge) {
        this.mealEnabled = meal;
        this.mateEnabled = mate;
        this.noticeEnabled = notice;
        this.marketingEnabled = marketing;
        if (badge != null) {
            this.badgeEnabled = badge;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isMealEnabled() {
        return mealEnabled;
    }

    public boolean isMateEnabled() {
        return mateEnabled;
    }

    public boolean isNoticeEnabled() {
        return noticeEnabled;
    }

    public boolean isMarketingEnabled() {
        return marketingEnabled;
    }

    public boolean isBadgeEnabled() {
        return badgeEnabled;
    }
}
