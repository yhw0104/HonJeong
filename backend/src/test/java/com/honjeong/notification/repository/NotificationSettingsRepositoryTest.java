package com.honjeong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.notification.domain.NotificationSettings;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/** NotificationSettingsRepository 슬라이스 테스트(실 Postgres). 기본값 라운드트립·유니크 제약을 검증한다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class NotificationSettingsRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private NotificationSettingsRepository settingsRepository;

    @Autowired
    private TestEntityManager em;

    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    @Test
    @DisplayName("findByUserId: 없으면 empty, of()+update 저장 후 값이 라운드트립된다")
    void saveAndFind() {
        User me = persistUser("01000000001", "나");
        assertThat(settingsRepository.findByUserId(me.getId())).isEmpty();

        NotificationSettings s = NotificationSettings.of(me.getId());
        s.update(false, true, true, true); // meal off, mate on, notice on, marketing on
        settingsRepository.saveAndFlush(s);
        em.clear();

        NotificationSettings got = settingsRepository.findByUserId(me.getId()).orElseThrow();
        assertThat(got.isMealEnabled()).isFalse();
        assertThat(got.isMateEnabled()).isTrue();
        assertThat(got.isNoticeEnabled()).isTrue();
        assertThat(got.isMarketingEnabled()).isTrue();
    }

    @Test
    @DisplayName("of(): 기본값은 meal·mate·notice ON, marketing OFF")
    void defaults() {
        NotificationSettings s = NotificationSettings.of(42L);
        assertThat(s.isMealEnabled()).isTrue();
        assertThat(s.isMateEnabled()).isTrue();
        assertThat(s.isNoticeEnabled()).isTrue();
        assertThat(s.isMarketingEnabled()).isFalse();
    }

    @Test
    @DisplayName("user_id 유니크: 같은 사용자 두 행은 제약 위반")
    void uniquePerUser() {
        User me = persistUser("01000000001", "나");
        settingsRepository.saveAndFlush(NotificationSettings.of(me.getId()));

        assertThatThrownBy(() -> settingsRepository.saveAndFlush(NotificationSettings.of(me.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
