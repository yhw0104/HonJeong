package com.honjeong.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.notification.domain.Notification;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.dto.NotificationResponse;
import com.honjeong.notification.dto.UnreadCountResponse;
import com.honjeong.notification.repository.NotificationRepository;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: 인앱 알림의 발행(publish)·목록 조회·안읽음 개수 조회·읽음 처리 비즈니스 로직
 * 2. 사용 Controller: NotificationController (발행은 MealRequestService·MateRequestService가 호출)
 *
 * <p>[기존 주석] 인앱 알림함(NOTI-003). 발행은 도메인 서비스(같이먹기/메이트)가 각자의 트랜잭션 안에서
 * {@link #publish}를 호출하는 방식 — 신청/수락과 알림이 원자적으로 함께 저장된다.
 * 미래 푸시(NOTI-001/002)는 publish에 기기 발송을 얹는 것으로 확장한다.
 */
@Service
public class NotificationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 목록 보존(노출) 기간 — 이보다 오래된 알림은 조회에서 제외(물리 삭제는 하지 않음). */
    private static final int RETENTION_DAYS = 30;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * 기능: 알림 한 건 생성·저장(발행)
     * Request: recipientId — 받는 사람 ID, type — 알림 종류, actorId — 알림을 일으킨 상대 ID(없으면 null)
     * Response: 없음(void)
     *
     * <p>[기존 주석] 알림 발행. 호출한 도메인 트랜잭션에 참여한다(별도 트랜잭션 아님 — 신청 실패 시 알림도 롤백).
     *
     * @param recipientId 받는 사람 id
     * @param type        알림 종류
     * @param actorId     알림을 일으킨 상대 id(닉네임 표시용)
     */
    @Transactional
    public void publish(Long recipientId, NotificationType type, Long actorId) {
        notificationRepository.save(Notification.create(
                userRepository.getReferenceById(recipientId),
                actorId == null ? null : userRepository.getReferenceById(actorId),
                type, now()));
    }

    /**
     * 기능: 내 알림 목록 조회(최근 30일·최신순·최대 30건)
     * Request: userId — 요청 사용자 ID
     * Response: List&lt;NotificationResponse&gt; — 알림 목록(id, 종류, 상대 닉네임, 읽음 여부, 발생 시각)
     *
     * <p>[기존 주석] 내 알림 목록 — 최근 30일·최신순·최대 30건.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository
                .findTop30ByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(userId, now().minusDays(RETENTION_DAYS))
                .stream().map(NotificationResponse::from).toList();
    }

    /**
     * 기능: 내 안읽은 알림 개수 조회
     * Request: userId — 요청 사용자 ID
     * Response: UnreadCountResponse — 안읽은 알림 수(count)
     *
     * <p>[기존 주석] 안읽음 개수(종 뱃지 폴링용).
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByUser_IdAndIsReadFalse(userId));
    }

    /**
     * 기능: 알림 한 건 읽음 처리
     * Request: userId — 요청 사용자 ID, id — 읽음 처리할 알림 ID
     * Response: 없음(void)
     *
     * <p>[기존 주석] 개별 읽음. 본인 소유가 아니면 존재 여부를 숨기기 위해 NOT_FOUND로 처리한다.
     */
    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!n.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        n.markRead();
    }

    /**
     * 기능: 내 안읽은 알림 전체 읽음 처리
     * Request: userId — 요청 사용자 ID
     * Response: 없음(void)
     *
     * <p>[기존 주석] 내 안읽음 전체 읽음 처리.
     */
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    /** 기능: 현재 시각을 KST(Asia/Seoul) 기준 LocalDateTime으로 반환 */
    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(KST));
    }
}
