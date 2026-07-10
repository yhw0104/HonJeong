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

    protected NotificationSettings() {
    }

    private NotificationSettings(Long userId, boolean meal, boolean mate, boolean notice, boolean marketing) {
        this.userId = userId;
        this.mealEnabled = meal;
        this.mateEnabled = mate;
        this.noticeEnabled = notice;
        this.marketingEnabled = marketing;
    }

    /** 기본값 행: 같이먹기·메이트·공지 ON, 이벤트·혜택(marketing) OFF(옵트인). */
    public static NotificationSettings of(Long userId) {
        return new NotificationSettings(userId, true, true, true, false);
    }

    public void update(boolean meal, boolean mate, boolean notice, boolean marketing) {
        this.mealEnabled = meal;
        this.mateEnabled = mate;
        this.noticeEnabled = notice;
        this.marketingEnabled = marketing;
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
}
