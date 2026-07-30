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
 * <p>인앱 알림함 REST 컨트롤러(NOTI-003). 전부 정식 USER 전용(SecurityConfig 기본 규칙).
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
     * 내 알림 목록을 조회한다 — 최근 30일·최신순·최대 30건.
     *
     * <p>사용 화면: 알림함(NotificationsScreen) — 15초 주기로 폴링한다.
     *
     * @param userId 인증 사용자 ID
     * @return 알림 목록(id, 종류, 상대 닉네임, 읽음 여부, 발생 시각)
     */
    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationService.getNotifications(userId));
    }

    /**
     * 안읽은 알림 개수를 조회한다 — 종 뱃지 폴링용 경량 엔드포인트.
     *
     * <p>사용 화면: 홈 지도(MapHome)·더보기(More) 헤더의 종 아이콘(BellButton) — 15초 주기로 폴링한다.
     *
     * @param userId 인증 사용자 ID
     * @return 안읽은 알림 수
     */
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 알림 한 건을 읽음 처리한다. 본인 알림이 아니면 404(NOTIFICATION_NOT_FOUND)다.
     *
     * <p>사용 화면: 알림함(NotificationsScreen)에서 안읽은 알림 항목을 탭했을 때.
     *
     * @param userId 인증 사용자 ID
     * @param id 읽음 처리할 알림 ID
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markRead(@CurrentUserId Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ApiResponse.<Void>success(null);
    }

    /**
     * 내 알림을 모두 읽음 처리한다.
     *
     * <p>사용 화면: 알림함(NotificationsScreen)의 "모두 읽음" 버튼.
     *
     * @param userId 인증 사용자 ID
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllRead(@CurrentUserId Long userId) {
        notificationService.markAllRead(userId);
        return ApiResponse.<Void>success(null);
    }

    /**
     * 내 알림 설정을 조회한다.
     *
     * <p>사용 화면: 더보기 > 알림 설정(NotificationSettingsScreen)의 토글 초기값.
     *
     * @param userId 인증 사용자 ID
     * @return 같이먹기·메이트·공지·이벤트혜택 수신 여부(설정 행이 없으면 기본값)
     */
    @GetMapping("/settings")
    public ApiResponse<NotificationSettingsResponse> getSettings(@CurrentUserId Long userId) {
        return ApiResponse.success(notificationSettingsService.getSettings(userId));
    }

    /**
     * 내 알림 설정을 저장한다.
     *
     * <p>사용 화면: 알림 설정(NotificationSettingsScreen)의 토글 변경.
     *
     * @param userId 인증 사용자 ID
     * @param request 4개 필드를 전체 교체하는 설정 값
     * @return 갱신된 설정
     */
    @PatchMapping("/settings")
    public ApiResponse<NotificationSettingsResponse> updateSettings(@CurrentUserId Long userId,
            @RequestBody NotificationSettingsRequest request) {
        return ApiResponse.success(notificationSettingsService.updateSettings(userId, request));
    }
}
