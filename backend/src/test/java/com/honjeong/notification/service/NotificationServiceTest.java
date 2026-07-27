package com.honjeong.notification.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.notification.domain.Notification;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.dto.NotificationResponse;
import com.honjeong.notification.repository.NotificationRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/** NotificationService 단위 테스트(Mockito + 고정 Clock). 발행·목록 변환·본인확인 가드를 검증한다. */
class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationSettingsService notificationSettingsService = mock(NotificationSettingsService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-04T03:00:00Z"), ZoneOffset.UTC);
    private final NotificationService service =
            new NotificationService(notificationRepository, userRepository, clock, notificationSettingsService);

    private User user(Long id, String nickname) {
        User u = mock(User.class);
        lenient().when(u.getId()).thenReturn(id);
        lenient().when(u.getNickname()).thenReturn(nickname);
        return u;
    }

    @Test
    @DisplayName("publish: 수신자·주체 참조로 알림을 저장한다")
    void publish_saves() {
        when(notificationSettingsService.isEnabled(1L, NotificationType.MEAL_REQUEST_RECEIVED)).thenReturn(true);
        User recipient = user(1L, "나");
        User actor = user(2L, "상대");
        when(userRepository.getReferenceById(1L)).thenReturn(recipient);
        when(userRepository.getReferenceById(2L)).thenReturn(actor);

        service.publish(1L, NotificationType.MEAL_REQUEST_RECEIVED, 2L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(recipient);
        assertThat(captor.getValue().getActor()).isSameAs(actor);
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.MEAL_REQUEST_RECEIVED);
        assertThat(captor.getValue().isRead()).isFalse();
    }

    @Test
    @DisplayName("publish: 수신자가 그 종류 알림을 껐으면 저장하지 않는다")
    void publish_gatedOff_doesNotSave() {
        when(notificationSettingsService.isEnabled(1L, NotificationType.MEAL_REQUEST_RECEIVED)).thenReturn(false);

        service.publish(1L, NotificationType.MEAL_REQUEST_RECEIVED, 2L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("publish: 설정이 켜져 있으면 저장한다(메이트)")
    void publish_gatedOn_saves() {
        when(notificationSettingsService.isEnabled(1L, NotificationType.MATE_REQUEST_RECEIVED)).thenReturn(true);
        User recipient = user(1L, "나");
        User actor = user(2L, "상대");
        when(userRepository.getReferenceById(1L)).thenReturn(recipient);
        when(userRepository.getReferenceById(2L)).thenReturn(actor);

        service.publish(1L, NotificationType.MATE_REQUEST_RECEIVED, 2L);

        verify(notificationRepository).save(any());
    }

    @Test
    @DisplayName("getNotifications: 30일 컷오프로 조회해 DTO로 변환한다")
    void list_maps() {
        User actor = user(2L, "상대");
        Notification n = Notification.create(user(1L, "나"), actor,
                NotificationType.MATE_REQUEST_ACCEPTED, LocalDateTime.of(2026, 7, 4, 11, 0));
        when(notificationRepository.findTop30ByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class))).thenReturn(List.of(n));

        List<NotificationResponse> got = service.getNotifications(1L);

        assertThat(got).hasSize(1);
        assertThat(got.get(0).type()).isEqualTo("MATE_REQUEST_ACCEPTED");
        assertThat(got.get(0).actorNickname()).isEqualTo("상대");
    }

    @Test
    @DisplayName("getNotifications: 탈퇴한 주체(닉네임 null)는 actorNickname이 '알 수 없음'")
    void list_withdrawnActor_showsUnknown() {
        User withdrawnActor = user(2L, null); // 주체는 있지만 탈퇴로 닉네임이 사라짐
        Notification n = Notification.create(user(1L, "나"), withdrawnActor,
                NotificationType.MATE_REQUEST_ACCEPTED, LocalDateTime.of(2026, 7, 4, 11, 0));
        when(notificationRepository.findTop30ByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class))).thenReturn(List.of(n));

        List<NotificationResponse> got = service.getNotifications(1L);

        assertThat(got.get(0).actorNickname()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("getNotifications: 주체 자체가 없는 알림(예: BADGE_EARNED)은 actorNickname이 null 그대로 유지된다"
            + "(탈퇴자의 '알 수 없음'과 혼동하면 안 됨)")
    void list_noActor_staysNull() {
        Notification n = Notification.create(user(1L, "나"), null,
                NotificationType.BADGE_EARNED, LocalDateTime.of(2026, 7, 4, 11, 0));
        when(notificationRepository.findTop30ByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(1L), any(LocalDateTime.class))).thenReturn(List.of(n));

        List<NotificationResponse> got = service.getNotifications(1L);

        assertThat(got.get(0).actorNickname()).isNull();
    }

    @Test
    @DisplayName("markRead: 본인 알림이 아니면 NOTIFICATION_NOT_FOUND")
    void markRead_ownershipGuard() {
        Notification others = Notification.create(user(99L, "남"), user(2L, "상대"),
                NotificationType.MEAL_REQUEST_RECEIVED, LocalDateTime.of(2026, 7, 4, 11, 0));
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(others));

        assertThatThrownBy(() -> service.markRead(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("markRead: 본인 알림이면 읽음 처리")
    void markRead_marks() {
        Notification mine = Notification.create(user(1L, "나"), user(2L, "상대"),
                NotificationType.MEAL_REQUEST_RECEIVED, LocalDateTime.of(2026, 7, 4, 11, 0));
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(mine));

        service.markRead(1L, 10L);

        assertThat(mine.isRead()).isTrue();
    }
}
