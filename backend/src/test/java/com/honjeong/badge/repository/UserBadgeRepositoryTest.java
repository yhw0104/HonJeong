package com.honjeong.badge.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import com.honjeong.badge.domain.UserBadge;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class UserBadgeRepositoryTest extends AbstractPostgresTest {

    @Autowired private UserBadgeRepository badgeRepository;
    @Autowired private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 23, 12, 0);

    private Long persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user).getId();
    }

    @Test
    @DisplayName("findKeysByUserId·countByUserId: 유저의 획득 뱃지 키·개수")
    void findKeysAndCount() {
        Long userId = persistUser("01000000001", "혼밥러");
        badgeRepository.save(UserBadge.of(userId, "SOLO_1", NOW));
        badgeRepository.save(UserBadge.of(userId, "SOLO_10", NOW));
        em.flush();

        assertThat(badgeRepository.findKeysByUserId(userId)).containsExactlyInAnyOrder("SOLO_1", "SOLO_10");
        assertThat(badgeRepository.countByUserId(userId)).isEqualTo(2);
    }

    @Test
    @DisplayName("유니크 (user_id, badge_key): 같은 뱃지 두 번이면 제약 위반")
    void duplicateViolatesUnique() {
        Long userId = persistUser("01000000002", "중복");
        badgeRepository.saveAndFlush(UserBadge.of(userId, "SOLO_1", NOW));

        assertThatThrownBy(() -> badgeRepository.saveAndFlush(UserBadge.of(userId, "SOLO_1", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("insertIfAbsent: 첫 삽입 1행, 중복이면 0행(예외 없음)·행은 하나만")
    void insertIfAbsentIdempotent() {
        Long userId = persistUser("01000000003", "멱등");
        assertThat(badgeRepository.insertIfAbsent(userId, "SOLO_1", NOW)).isEqualTo(1);
        assertThat(badgeRepository.insertIfAbsent(userId, "SOLO_1", NOW)).isEqualTo(0);
        assertThat(badgeRepository.countByUserId(userId)).isEqualTo(1);
    }
}
