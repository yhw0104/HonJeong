package com.honjeong.notification.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.notification.dto.NotificationResponse;
import com.honjeong.notification.dto.UnreadCountResponse;
import com.honjeong.notification.service.NotificationService;

/**
 * 인앱 알림함 REST 컨트롤러(NOTI-003). 전부 정식 USER 전용(SecurityConfig 기본 규칙).
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** 내 알림 목록 — 최근 30일·최신순·최대 30건. */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationService.getNotifications(userId));
    }

    /** 안읽음 개수(종 뱃지 폴링용 경량 엔드포인트). */
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    /** 개별 읽음. 본인 알림이 아니면 404(NOTIFICATION_NOT_FOUND). */
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@CurrentUserId Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ApiResponse.<Void>success(null);
    }

    /** 모두 읽음. */
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@CurrentUserId Long userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.<Void>success(null);
    }
}
