package com.honjeong.notification.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.notification.dto.NotificationResponse;
import com.honjeong.notification.dto.NotificationSettingsRequest;
import com.honjeong.notification.dto.NotificationSettingsResponse;
import com.honjeong.notification.dto.UnreadCountResponse;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.notification.service.NotificationSettingsService;

/**
 * 인앱 알림함(목록·안읽음 개수·읽음 처리) 컨트롤러.
 *
 * <p>기본 경로: /api/notifications
 *
 * <p>[기존 주석] 인앱 알림함 REST 컨트롤러(NOTI-003). 전부 정식 USER 전용(SecurityConfig 기본 규칙).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSettingsService notificationSettingsService;

    public NotificationController(NotificationService notificationService,
            NotificationSettingsService notificationSettingsService) {
        this.notificationService = notificationService;
        this.notificationSettingsService = notificationSettingsService;
    }

    /**
     * 1. API 주소: GET /api/notifications
     * 2. 사용 화면: 알림함(NotificationsScreen) — 알림 목록 표시(15초 폴링)
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: List&lt;NotificationResponse&gt; — 알림 목록(id, 종류, 상대 닉네임, 읽음 여부, 발생 시각)
     *
     * <p>[기존 주석] 내 알림 목록 — 최근 30일·최신순·최대 30건.
     */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationService.getNotifications(userId));
    }

    /**
     * 1. API 주소: GET /api/notifications/unread-count
     * 2. 사용 화면: 홈 지도(MapHome)·더보기(More) 헤더의 종 아이콘(BellButton) — 안읽음 뱃지 표시(15초 폴링)
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: UnreadCountResponse — 안읽은 알림 수(count)
     *
     * <p>[기존 주석] 안읽음 개수(종 뱃지 폴링용 경량 엔드포인트).
     */
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 1. API 주소: PATCH /api/notifications/{id}/read
     * 2. 사용 화면: 알림함(NotificationsScreen) — 안읽은 알림 항목 탭 시 읽음 처리
     * 3. Request: id(경로) — 읽음 처리할 알림 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void)
     *
     * <p>[기존 주석] 개별 읽음. 본인 알림이 아니면 404(NOTIFICATION_NOT_FOUND).
     */
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@CurrentUserId Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ApiResponse.<Void>success(null);
    }

    /**
     * 1. API 주소: PATCH /api/notifications/read-all
     * 2. 사용 화면: 알림함(NotificationsScreen) — "모두 읽음" 버튼
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void)
     *
     * <p>[기존 주석] 모두 읽음.
     */
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@CurrentUserId Long userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.<Void>success(null);
    }

    /**
     * 1. API 주소: GET /api/notifications/settings
     * 2. 사용 화면: 더보기 > 알림 설정(NotificationSettingsScreen) — 토글 초기값
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: NotificationSettingsResponse — 같이먹기·메이트·공지·이벤트혜택 수신 여부(행 없으면 기본값)
     */
    @GetMapping("/settings")
    public ApiResponse<NotificationSettingsResponse> getSettings(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationSettingsService.getSettings(userId));
    }

    /**
     * 1. API 주소: PATCH /api/notifications/settings
     * 2. 사용 화면: 알림 설정(NotificationSettingsScreen) — 토글 변경 시 저장
     * 3. Request: NotificationSettingsRequest(4필드 전체 교체) / 인증 사용자(@CurrentUserId)
     * 4. Response: NotificationSettingsResponse — 갱신된 설정
     */
    @PatchMapping("/settings")
    public ApiResponse<NotificationSettingsResponse> updateSettings(@CurrentUserId Long userId,
            @RequestBody NotificationSettingsRequest request) {
        return ApiResponse.success(notificationSettingsService.updateSettings(userId, request));
    }
}
