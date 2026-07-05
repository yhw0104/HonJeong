package com.honjeong.meal.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/**
 * MealRequestRepository 슬라이스 테스트. 실제 Postgres(Testcontainers)에서 매핑·유니크 제약·조회 쿼리를 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class MealRequestRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private MealRequestRepository mealRequestRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 18, 12, 0);

    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    private Place persistPlace(String sourceId) {
        // lat/lng 고정 — meal 테스트는 지리 쿼리가 없다.
        return em.persist(Place.ofPublicData(sourceId, sourceId + "식당", "한식", "서울", "서울 도로명",
                37.5, 127.0, null, "영업"));
    }

    private CheckIn persistCheckIn(User user, Place place) {
        return em.persist(CheckIn.start(user, place, NOW));
    }

    @Test
    @DisplayName("중복 신청: 같은 (from_user, to_check_in)이면 유니크 위반")
    void duplicateConstraint() {
        User from = persistUser("01000000001", "신청자");
        User to = persistUser("01000000002", "수신자");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        em.persist(MealRequest.create(from, target, place, "1차", NOW));
        em.flush();

        MealRequest dup = MealRequest.create(from, target, place, "2차", NOW);
        assertThatThrownBy(() -> mealRequestRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findReceived: 내가 수신자인 신청만(타인 대상 제외), fromUser fetch, placeId 노출")
    void findReceived() {
        User from = persistUser("01000000001", "신청자");
        User me = persistUser("01000000002", "나");
        User other = persistUser("01000000003", "남");
        Place place = persistPlace("ext-1");
        CheckIn myCheckIn = persistCheckIn(me, place);
        CheckIn otherCheckIn = persistCheckIn(other, place);
        em.persist(MealRequest.create(from, myCheckIn, place, "받은신청", NOW));
        em.persist(MealRequest.create(from, otherCheckIn, place, "남에게간신청", NOW)); // 내 것 아님 → 제외돼야
        em.flush();
        em.clear();

        List<MealRequest> received = mealRequestRepository.findReceived(me.getId(), null, List.of(-1L));

        assertThat(received).hasSize(1);
        assertThat(received.get(0).getFromUser().getNickname()).isEqualTo("신청자");
        assertThat(received.get(0).getMessage()).isEqualTo("받은신청");
        assertThat(received.get(0).getPlace().getId()).isEqualTo(place.getId());
        assertThat(received.get(0).getPlace().getName()).isEqualTo("ext-1식당");
        assertThat(received.get(0).getToCheckIn().getUser().getNickname()).isEqualTo("나");
    }

    @Test
    @DisplayName("findReceived: 제외 id 목록의 fromUser(신청자)는 빠진다")
    void findReceived_excludesBlocked() {
        User from = persistUser("01000000001", "신청자");
        User blockedFrom = persistUser("01000000004", "차단된신청자");
        User me = persistUser("01000000002", "나");
        Place place = persistPlace("ext-1");
        CheckIn myCheckIn = persistCheckIn(me, place);
        em.persist(MealRequest.create(from, myCheckIn, place, "정상신청", NOW));
        em.persist(MealRequest.create(blockedFrom, myCheckIn, place, "차단대상신청", NOW));
        em.flush();
        em.clear();

        List<MealRequest> received = mealRequestRepository.findReceived(me.getId(), null,
                List.of(blockedFrom.getId()));

        assertThat(received).hasSize(1);
        assertThat(received.get(0).getFromUser().getNickname()).isEqualTo("신청자");
    }

    @Test
    @DisplayName("findReceived: 센티널(-1)만 있으면 전원 노출")
    void findReceived_sentinelShowsAll() {
        User from = persistUser("01000000001", "신청자");
        User other = persistUser("01000000004", "다른신청자");
        User me = persistUser("01000000002", "나");
        Place place = persistPlace("ext-1");
        CheckIn myCheckIn = persistCheckIn(me, place);
        em.persist(MealRequest.create(from, myCheckIn, place, "정상신청", NOW));
        em.persist(MealRequest.create(other, myCheckIn, place, "다른신청", NOW));
        em.flush();
        em.clear();

        assertThat(mealRequestRepository.findReceived(me.getId(), null, List.of(-1L))).hasSize(2);
    }

    @Test
    @DisplayName("findReceived: status 필터(PENDING·ACCEPTED 양방향, null=전체)")
    void findReceivedWithStatus() {
        User from = persistUser("01000000001", "신청자");
        User from2 = persistUser("01000000003", "신청자2");
        User me = persistUser("01000000002", "나");
        Place place = persistPlace("ext-1");
        CheckIn myCheckIn = persistCheckIn(me, place);
        em.persist(MealRequest.create(from, myCheckIn, place, "PENDING건", NOW));
        MealRequest accepted = MealRequest.create(from2, myCheckIn, place, "ACCEPTED건", NOW);
        accepted.accept(NOW.plusMinutes(5));
        em.persist(accepted);
        em.flush();
        em.clear();

        assertThat(mealRequestRepository.findReceived(me.getId(), MealRequestStatus.PENDING, List.of(-1L)))
                .hasSize(1);
        assertThat(mealRequestRepository.findReceived(me.getId(), MealRequestStatus.ACCEPTED, List.of(-1L)))
                .hasSize(1);
        assertThat(mealRequestRepository.findReceived(me.getId(), MealRequestStatus.DECLINED, List.of(-1L)))
                .isEmpty();
        assertThat(mealRequestRepository.findReceived(me.getId(), null, List.of(-1L))).hasSize(2);
    }

    @Test
    @DisplayName("findSent: 내가 신청자인 신청만(타인 발신 제외)")
    void findSent() {
        User me = persistUser("01000000001", "나");
        User to = persistUser("01000000002", "수신자");
        User other = persistUser("01000000003", "남신청자");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        em.persist(MealRequest.create(me, target, place, "보낸신청", NOW));
        em.persist(MealRequest.create(other, target, place, "남이보낸신청", NOW)); // 내 것 아님 → 제외돼야
        em.flush();
        em.clear();

        List<MealRequest> sent = mealRequestRepository.findSent(me.getId(), null, List.of(-1L));
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getFromUser().getNickname()).isEqualTo("나");
        assertThat(sent.get(0).getMessage()).isEqualTo("보낸신청");
        assertThat(sent.get(0).getPlace().getName()).isEqualTo("ext-1식당");
        assertThat(sent.get(0).getToCheckIn().getUser().getNickname()).isEqualTo("수신자");
    }

    @Test
    @DisplayName("findSent: 제외 id 목록의 수신자(toCheckIn.user)에게 보낸 신청은 빠진다")
    void findSent_excludesBlocked() {
        User me = persistUser("01000000001", "나");
        User to = persistUser("01000000002", "수신자");
        User blockedTo = persistUser("01000000003", "차단된수신자");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        CheckIn blockedTarget = persistCheckIn(blockedTo, place);
        em.persist(MealRequest.create(me, target, place, "정상발신", NOW));
        em.persist(MealRequest.create(me, blockedTarget, place, "차단대상발신", NOW));
        em.flush();
        em.clear();

        List<MealRequest> sent = mealRequestRepository.findSent(me.getId(), null, List.of(blockedTo.getId()));

        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getToCheckIn().getUser().getNickname()).isEqualTo("수신자");
    }

    @Test
    @DisplayName("findSent: 센티널(-1)만 있으면 전원 노출")
    void findSent_sentinelShowsAll() {
        User me = persistUser("01000000001", "나");
        User to = persistUser("01000000002", "수신자");
        User to2 = persistUser("01000000003", "수신자2");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        CheckIn target2 = persistCheckIn(to2, place);
        em.persist(MealRequest.create(me, target, place, "발신1", NOW));
        em.persist(MealRequest.create(me, target2, place, "발신2", NOW));
        em.flush();
        em.clear();

        assertThat(mealRequestRepository.findSent(me.getId(), null, List.of(-1L))).hasSize(2);
    }

    @Test
    @DisplayName("declinePendingBetween: 두 유저 사이 양방향 PENDING을 모두 DECLINED, 제3자는 불변")
    void declinePendingBetween_bulkDeclines() {
        User a = persistUser("01000000001", "A");
        User b = persistUser("01000000002", "B");
        User stranger = persistUser("01000000003", "제3자");
        Place place = persistPlace("ext-1");
        CheckIn aCheckIn = persistCheckIn(a, place);
        CheckIn bCheckIn = persistCheckIn(b, place);

        MealRequest aToB = em.persist(MealRequest.create(a, bCheckIn, place, "a->b", NOW));           // a→b PENDING
        MealRequest bToA = em.persist(MealRequest.create(b, aCheckIn, place, "b->a", NOW));           // b→a PENDING
        MealRequest strangerToA =
                em.persist(MealRequest.create(stranger, aCheckIn, place, "제3자->a", NOW));           // 불변 대상
        em.flush();
        em.clear();

        int updated = mealRequestRepository.declinePendingBetween(a.getId(), b.getId(), NOW.plusMinutes(5));

        assertThat(updated).isEqualTo(2);
        em.clear();
        assertThat(mealRequestRepository.findById(aToB.getId()).orElseThrow().getStatus())
                .isEqualTo(MealRequestStatus.DECLINED);
        assertThat(mealRequestRepository.findById(bToA.getId()).orElseThrow().getStatus())
                .isEqualTo(MealRequestStatus.DECLINED);
        assertThat(mealRequestRepository.findById(strangerToA.getId()).orElseThrow().getStatus())
                .isEqualTo(MealRequestStatus.PENDING);
    }

    @Test
    @DisplayName("countAcceptedBetween: 두 사람 사이 ACCEPTED만 방향 무관 집계(PENDING·DECLINED·제3자 제외)")
    void countAcceptedBetween() {
        User me = persistUser("01000000001", "나");
        User friend = persistUser("01000000002", "친구");
        User stranger = persistUser("01000000003", "제3자");
        Place place = persistPlace("ext-1");
        // 유니크 제약상 사용자당 ACTIVE 체크인 1개, (from,to_check_in) 쌍 1개 — 쌍이 겹치지 않게 구성
        CheckIn myCheckIn = persistCheckIn(me, place);
        CheckIn friendCheckIn = persistCheckIn(friend, place);
        CheckIn strangerCheckIn = persistCheckIn(stranger, place);

        persistAccepted(me, friendCheckIn, place);      // 나→친구 수락 (count)
        persistAccepted(friend, myCheckIn, place);      // 친구→나 수락 (역방향도 count)
        persistAccepted(me, strangerCheckIn, place);    // 나→제3자 수락 (친구와 무관)
        em.persist(MealRequest.create(stranger, friendCheckIn, place, "대기", NOW)); // 제3자→친구 PENDING (제외)
        persistDeclined(stranger, myCheckIn, place);    // 제3자→나 DECLINED (제외)
        em.flush();
        em.clear();

        assertThat(mealRequestRepository.countAcceptedBetween(me.getId(), friend.getId())).isEqualTo(2L);
        assertThat(mealRequestRepository.countAcceptedBetween(friend.getId(), me.getId())).isEqualTo(2L); // 대칭
        assertThat(mealRequestRepository.countAcceptedBetween(me.getId(), stranger.getId())).isEqualTo(1L); // declined 제외
        assertThat(mealRequestRepository.countAcceptedBetween(friend.getId(), stranger.getId())).isZero(); // pending만 → 0
    }

    @Test
    @DisplayName("findAcceptedPairsForUser: viewer가 신청자/수신자인 ACCEPTED 쌍만 반환(from/to id)")
    void findAcceptedPairsForUser() {
        User me = persistUser("01000000001", "나");
        User a = persistUser("01000000002", "A");
        User b = persistUser("01000000003", "B");
        Place place = persistPlace("ext-1");
        CheckIn myCheckIn = persistCheckIn(me, place);
        CheckIn aCheckIn = persistCheckIn(a, place);
        CheckIn bCheckIn = persistCheckIn(b, place);

        persistAccepted(me, aCheckIn, place);   // 나→A
        persistAccepted(b, myCheckIn, place);   // B→나
        persistAccepted(a, bCheckIn, place);    // A→B (나 무관, 제외)
        em.persist(MealRequest.create(me, bCheckIn, place, "대기", NOW)); // 나→B PENDING (제외)
        em.flush();
        em.clear();

        List<MealRequestRepository.MealPairRow> rows =
                mealRequestRepository.findAcceptedPairsForUser(me.getId());

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(r ->
                assertThat(r.getFromId().equals(me.getId()) || r.getToId().equals(me.getId())).isTrue());
        // (from, to) 쌍 집합 검증: (나→A), (B→나)
        assertThat(rows).extracting(r -> r.getFromId() + "->" + r.getToId())
                .containsExactlyInAnyOrder(me.getId() + "->" + a.getId(), b.getId() + "->" + me.getId());
    }

    private void persistAccepted(User from, CheckIn targetCheckIn, Place place) {
        MealRequest mr = MealRequest.create(from, targetCheckIn, place, "수락건", NOW);
        mr.accept(NOW.plusMinutes(5));
        em.persist(mr);
    }

    private void persistDeclined(User from, CheckIn targetCheckIn, Place place) {
        MealRequest mr = MealRequest.create(from, targetCheckIn, place, "거절건", NOW);
        mr.decline(NOW.plusMinutes(5));
        em.persist(mr);
    }

    @Test
    @DisplayName("findWithReceiverById: toCheckIn.user fetch로 수신자 식별 가능")
    void findWithReceiverById() {
        User from = persistUser("01000000001", "신청자");
        User to = persistUser("01000000002", "수신자");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        MealRequest saved = em.persist(MealRequest.create(from, target, place, "msg", NOW));
        em.flush();
        em.clear();

        MealRequest found = mealRequestRepository.findWithReceiverById(saved.getId()).orElseThrow();
        assertThat(found.isReceivedBy(to.getId())).isTrue();
        assertThat(found.isReceivedBy(from.getId())).isFalse();
        assertThat(mealRequestRepository.findWithReceiverById(-1L)).isEmpty();
    }
}
