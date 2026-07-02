package com.honjeong.mate.repository;

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
import com.honjeong.global.config.JpaConfig;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class MateRepositoryTest extends AbstractPostgresTest {

    @Autowired private MateRequestRepository mateRequestRepository;
    @Autowired private MateRepository mateRepository;
    @Autowired private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 2, 12, 0);

    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    @Test
    @DisplayName("PENDING 중복: 같은 (from,to)로 두 번째 PENDING이면 유니크 위반")
    void pendingDuplicate() {
        User from = persistUser("01000000001", "신청자");
        User to = persistUser("01000000002", "대상");
        em.persist(MateRequest.create(from, to, NOW));
        em.flush();

        MateRequest dup = MateRequest.create(from, to, NOW);
        assertThatThrownBy(() -> mateRequestRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("재신청 허용: 이전 신청이 CANCELED면 같은 쌍 새 PENDING 저장 가능")
    void reRequestAfterCanceled() {
        User from = persistUser("01000000001", "신청자");
        User to = persistUser("01000000002", "대상");
        MateRequest first = MateRequest.create(from, to, NOW);
        first.cancel(NOW);
        em.persist(first);
        em.flush();

        MateRequest second = MateRequest.create(from, to, NOW);
        assertThatCode(() -> mateRequestRepository.saveAndFlush(second)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("findMatesWithUserByUserId: 내 user_id 행만 mateUser fetch")
    void findMates() {
        User me = persistUser("01000000001", "나");
        User mate = persistUser("01000000002", "메이트");
        em.persist(Mate.create(me, mate, NOW));
        em.persist(Mate.create(mate, me, NOW));
        em.flush();
        em.clear();

        var mine = mateRepository.findMatesWithUserByUserId(me.getId());
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getMateUser().getId()).isEqualTo(mate.getId());
    }
}
