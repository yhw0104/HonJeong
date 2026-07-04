package com.honjeong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.notification.domain.Notification;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/** NotificationRepository 슬라이스 테스트(실 Postgres). 30일 필터·최신순·안읽음 카운트·모두읽음을 검증한다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class NotificationRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 4, 12, 0);

    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    @Test
    @DisplayName("findTop30...: 30일 이내만·최신순·다른 사용자 제외")
    void listRecent() {
        User me = persistUser("01000000001", "나");
        User other = persistUser("01000000002", "상대");
        em.persist(Notification.create(me, other, NotificationType.MEAL_REQUEST_RECEIVED, NOW.minusDays(31))); // 보존기간 밖
        em.persist(Notification.create(me, other, NotificationType.MEAL_REQUEST_RECEIVED, NOW.minusDays(1)));
        em.persist(Notification.create(me, other, NotificationType.MATE_REQUEST_RECEIVED, NOW.minusHours(1)));
        em.persist(Notification.create(other, me, NotificationType.MEAL_REQUEST_RECEIVED, NOW)); // 남의 알림
        em.flush();

        List<Notification> got = notificationRepository
                .findTop30ByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(me.getId(), NOW.minusDays(30));

        assertThat(got).hasSize(2);
        assertThat(got.get(0).getType()).isEqualTo(NotificationType.MATE_REQUEST_RECEIVED); // 최신 먼저
        assertThat(got.get(1).getType()).isEqualTo(NotificationType.MEAL_REQUEST_RECEIVED);
        assertThat(got.get(0).getActor().getNickname()).isEqualTo("상대"); // EntityGraph 로드
    }

    @Test
    @DisplayName("countByUser_IdAndIsReadFalse + markAllRead: 안읽음만 세고, 모두읽음이 전부 끈다")
    void unreadCountAndMarkAll() {
        User me = persistUser("01000000001", "나");
        User other = persistUser("01000000002", "상대");
        Notification read = Notification.create(me, other, NotificationType.MEAL_REQUEST_RECEIVED, NOW.minusHours(2));
        read.markRead();
        em.persist(read);
        em.persist(Notification.create(me, other, NotificationType.MEAL_REQUEST_ACCEPTED, NOW.minusHours(1)));
        em.persist(Notification.create(me, other, NotificationType.MATE_REQUEST_ACCEPTED, NOW));
        em.flush();

        assertThat(notificationRepository.countByUser_IdAndIsReadFalse(me.getId())).isEqualTo(2);

        int updated = notificationRepository.markAllRead(me.getId());

        assertThat(updated).isEqualTo(2);
        assertThat(notificationRepository.countByUser_IdAndIsReadFalse(me.getId())).isZero();
    }
}
