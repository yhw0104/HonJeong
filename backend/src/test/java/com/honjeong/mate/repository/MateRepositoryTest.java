package com.honjeong.mate.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
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
    @DisplayName("findReceived: status=null이면 전체(PENDING+DECLINED), status 지정 시 필터")
    void findReceivedStatusNullPath() {
        User from = persistUser("01000000001", "신청자");
        User to = persistUser("01000000002", "대상");
        // 같은 쌍이라도 부분 유니크는 PENDING만 막으므로 PENDING 1 + DECLINED 1 저장 가능.
        em.persist(MateRequest.create(from, to, NOW));
        MateRequest declined = MateRequest.create(from, to, NOW);
        declined.decline(NOW.plusMinutes(5));
        em.persist(declined);
        em.flush();
        em.clear();

        // status=null 바인딩: 두 상태 모두 반환되어야 JPQL null 경로 정상.
        assertThat(mateRequestRepository.findReceived(to.getId(), null, List.of(-1L))).hasSize(2);
        assertThat(mateRequestRepository.findReceived(to.getId(), MateRequestStatus.PENDING, List.of(-1L)))
                .hasSize(1);
        assertThat(mateRequestRepository.findReceived(to.getId(), MateRequestStatus.DECLINED, List.of(-1L)))
                .hasSize(1);
        assertThat(mateRequestRepository.findReceived(to.getId(), MateRequestStatus.CANCELED, List.of(-1L)))
                .isEmpty();
    }

    @Test
    @DisplayName("findReceived: 제외 id 목록의 fromUser(신청자)는 빠진다")
    void findReceived_excludesBlocked() {
        User from = persistUser("01000000001", "신청자");
        User blockedFrom = persistUser("01000000004", "차단된신청자");
        User to = persistUser("01000000002", "나");
        em.persist(MateRequest.create(from, to, NOW));
        em.persist(MateRequest.create(blockedFrom, to, NOW));
        em.flush();
        em.clear();

        List<MateRequest> received = mateRequestRepository.findReceived(to.getId(), null,
                List.of(blockedFrom.getId()));

        assertThat(received).hasSize(1);
        assertThat(received.get(0).getFromUser().getNickname()).isEqualTo("신청자");
    }

    @Test
    @DisplayName("findReceived: 센티널(-1)만 있으면 전원 노출")
    void findReceived_sentinelShowsAll() {
        User from = persistUser("01000000001", "신청자");
        User other = persistUser("01000000004", "다른신청자");
        User to = persistUser("01000000002", "나");
        em.persist(MateRequest.create(from, to, NOW));
        em.persist(MateRequest.create(other, to, NOW));
        em.flush();
        em.clear();

        assertThat(mateRequestRepository.findReceived(to.getId(), null, List.of(-1L))).hasSize(2);
    }

    @Test
    @DisplayName("findSent: status=null이면 내가 보낸 전체 반환")
    void findSentStatusNullPath() {
        User me = persistUser("01000000001", "나");
        User to = persistUser("01000000002", "대상");
        em.persist(MateRequest.create(me, to, NOW));
        MateRequest canceled = MateRequest.create(me, to, NOW);
        canceled.cancel(NOW.plusMinutes(5));
        em.persist(canceled);
        em.flush();
        em.clear();

        assertThat(mateRequestRepository.findSent(me.getId(), null, List.of(-1L))).hasSize(2);
        assertThat(mateRequestRepository.findSent(me.getId(), MateRequestStatus.PENDING, List.of(-1L))).hasSize(1);
    }

    @Test
    @DisplayName("findSent: 제외 id 목록의 toUser(상대)에게 보낸 신청은 빠진다")
    void findSent_excludesBlocked() {
        User me = persistUser("01000000001", "나");
        User to = persistUser("01000000002", "대상");
        User blockedTo = persistUser("01000000003", "차단된대상");
        em.persist(MateRequest.create(me, to, NOW));
        em.persist(MateRequest.create(me, blockedTo, NOW));
        em.flush();
        em.clear();

        List<MateRequest> sent = mateRequestRepository.findSent(me.getId(), null, List.of(blockedTo.getId()));

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getToUser().getNickname()).isEqualTo("대상");
    }

    @Test
    @DisplayName("findSent: 센티널(-1)만 있으면 전원 노출")
    void findSent_sentinelShowsAll() {
        User me = persistUser("01000000001", "나");
        User to = persistUser("01000000002", "대상");
        User to2 = persistUser("01000000003", "대상2");
        em.persist(MateRequest.create(me, to, NOW));
        em.persist(MateRequest.create(me, to2, NOW));
        em.flush();
        em.clear();

        assertThat(mateRequestRepository.findSent(me.getId(), null, List.of(-1L))).hasSize(2);
    }

    @Test
    @DisplayName("resolvePendingBetween: from→to 방향 PENDING만 지정 status로 바뀌고, 역방향·non-PENDING은 불변")
    void resolvePendingBetween_updatesOnlyForwardPending() {
        User a = persistUser("01000000001", "A");
        User b = persistUser("01000000002", "B");
        MateRequest forwardPending = em.persist(MateRequest.create(a, b, NOW));      // a→b PENDING(대상)
        MateRequest forwardAccepted = MateRequest.create(a, b, NOW);
        forwardAccepted.accept(NOW.plusMinutes(1));
        em.persist(forwardAccepted);                                                 // a→b ACCEPTED(불변 확인용)
        MateRequest backwardPending = em.persist(MateRequest.create(b, a, NOW));     // b→a PENDING(역방향, 불변)
        em.flush();
        em.clear(); // 벌크 @Modifying 후 1차 캐시 stale 방지(findById 재조회 보장)

        int updated = mateRequestRepository.resolvePendingBetween(
                a.getId(), b.getId(), MateRequestStatus.CANCELED, NOW.plusMinutes(5));

        assertThat(updated).isEqualTo(1);
        em.clear();
        assertThat(mateRequestRepository.findById(forwardPending.getId()).orElseThrow().getStatus())
                .isEqualTo(MateRequestStatus.CANCELED);
        assertThat(mateRequestRepository.findById(forwardAccepted.getId()).orElseThrow().getStatus())
                .isEqualTo(MateRequestStatus.ACCEPTED);
        assertThat(mateRequestRepository.findById(backwardPending.getId()).orElseThrow().getStatus())
                .isEqualTo(MateRequestStatus.PENDING);
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
