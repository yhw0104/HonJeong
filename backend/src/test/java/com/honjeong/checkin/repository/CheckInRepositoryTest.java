package com.honjeong.checkin.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/**
 * CheckInRepository 슬라이스 테스트. 실제 Postgres(Testcontainers)에서 매핑·제약·쿼리를 검증한다.
 * 특히 단일 활성 부분 유니크 인덱스는 H2로 검증 불가라 Postgres가 필수다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class CheckInRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 15, 12, 0);

    /** 닉네임을 가진 ACTIVE 사용자 1명을 영속화해 반환한다. */
    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        // completeProfile(nickname, gender, ageGroup, introduction, region, regionLat, regionLng, diningStyle, profileImageUrl) — 9개
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    private Place persistPlace(String sourceId, double lat, double lng) {
        return em.persist(Place.ofPublicData(sourceId, sourceId + "식당", "한식", "서울 어딘가", "서울 도로명",
                lat, lng, null, "영업"));
    }

    /** ENDED(종료된) 체크인을 만들어 반환한다(영속화는 호출 측 책임). startedAt으로 시작해 곧바로 종료 처리한다. */
    private CheckIn endedCheckIn(User user, Place place, LocalDateTime startedAt) {
        CheckIn c = CheckIn.start(user, place, startedAt);
        c.end(startedAt.plusMinutes(30));
        return c;
    }

    @Test
    @DisplayName("findByUser_IdAndStatus: 사용자의 ACTIVE 체크인을 찾고, 없으면 빈 Optional")
    void findActiveByUser() {
        // given: user1이 한 식당에 ACTIVE 체크인
        User user = persistUser("01000000001", "혼밥러A");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        em.persist(CheckIn.start(user, place, NOW));
        em.flush();

        // when & then
        assertThat(checkInRepository.findByUser_IdAndStatus(user.getId(), CheckInStatus.ACTIVE)).isPresent();
        assertThat(checkInRepository.findByUser_IdAndStatus(user.getId(), CheckInStatus.ENDED)).isEmpty();
        assertThat(checkInRepository.findByUser_IdAndStatus(999L, CheckInStatus.ACTIVE)).isEmpty();
    }

    @Test
    @DisplayName("countByStatus: ACTIVE 체크인 수를 센다")
    void countActive() {
        User u1 = persistUser("01000000001", "A");
        User u2 = persistUser("01000000002", "B");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        em.persist(CheckIn.start(u1, place, NOW));
        em.persist(CheckIn.start(u2, place, NOW));
        em.flush();

        assertThat(checkInRepository.countByStatus(CheckInStatus.ACTIVE)).isEqualTo(2);
        assertThat(checkInRepository.countByStatus(CheckInStatus.ENDED)).isZero();
    }

    @Test
    @DisplayName("단일 활성 제약: 같은 사용자가 ACTIVE 2개면 부분 유니크 인덱스 위반")
    void singleActiveConstraint() {
        // given: user1이 이미 ACTIVE 체크인
        User user = persistUser("01000000001", "혼밥러A");
        Place place1 = persistPlace("ext-1", 37.5, 127.0);
        Place place2 = persistPlace("ext-2", 37.6, 127.1);
        em.persist(CheckIn.start(user, place1, NOW));
        em.flush();

        // when & then: 같은 user의 두 번째 ACTIVE는 인덱스 위반(리포지토리 경유로 Spring 예외 변환을 탄다)
        CheckIn second = CheckIn.start(user, place2, NOW);
        assertThatThrownBy(() -> checkInRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("countDistinctUsersStartedSince: 기준 이후 시작된 distinct 사용자 수(어제·중복 제외)")
    void todayDistinctUsers() {
        User u1 = persistUser("01000000001", "A");
        User u2 = persistUser("01000000002", "B");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        LocalDateTime todayStart = LocalDateTime.of(2026, 6, 15, 0, 0);
        // u1: 오늘 ACTIVE 1건 + 오늘 ENDED 1건(같은 사용자 2건 → distinct 1)
        em.persist(CheckIn.start(u1, place, todayStart.plusHours(9)));
        CheckIn ended = CheckIn.start(u1, place, todayStart.plusHours(10));
        ended.end(todayStart.plusHours(11));
        em.persist(ended);
        // u2: 어제 시작 → 제외
        em.persist(CheckIn.start(u2, place, todayStart.minusHours(2)));
        em.flush();

        assertThat(checkInRepository.countDistinctUsersStartedSince(todayStart)).isEqualTo(1);
    }

    @Test
    @DisplayName("countActiveByPlaceWithinBounds: 박스 안 식당별 ACTIVE 수, 박스 밖·ENDED 제외")
    void mapAggregation() {
        User u1 = persistUser("01000000001", "A");
        User u2 = persistUser("01000000002", "B");
        User u3 = persistUser("01000000003", "C");
        Place inBox = persistPlace("in", 37.5000, 127.0000);
        Place outBox = persistPlace("out", 38.0000, 128.0000);
        em.persist(CheckIn.start(u1, inBox, NOW));   // inBox ACTIVE
        em.persist(CheckIn.start(u2, inBox, NOW));   // inBox ACTIVE → count 2
        em.persist(CheckIn.start(u3, outBox, NOW));  // 박스 밖
        em.flush();

        var markers = checkInRepository.countActiveByPlaceWithinBounds(37.49, 37.51, 126.99, 127.01);

        assertThat(markers).hasSize(1);
        assertThat(markers.get(0).placeId()).isEqualTo(inBox.getId());
        assertThat(markers.get(0).activeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("지도 마커는 식당별 ACTIVE·SEEKING 수를 각각 집계한다")
    void 마커_ACTIVE_SEEKING_분리집계() {
        User user = persistUser("01000000001", "A");
        User user2 = persistUser("01000000002", "B");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        CheckIn active = CheckIn.startSeeking(user, place, NOW);
        active.dineAlone(NOW);
        checkInRepository.save(active);
        checkInRepository.save(CheckIn.startSeeking(user2, place, NOW)); // 같은 place 모집중
        checkInRepository.flush();

        var markers = checkInRepository.countActiveByPlaceWithinBounds(
                place.getLatitude() - 0.01, place.getLatitude() + 0.01,
                place.getLongitude() - 0.01, place.getLongitude() + 0.01);

        assertThat(markers).singleElement().satisfies(m -> {
            assertThat(m.activeCount()).isEqualTo(1);
            assertThat(m.seekingCount()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("findActiveWithUserByPlace: 식당의 ACTIVE 혼밥러를 startedAt 오름차순, user fetch")
    void activeDiners() {
        User u1 = persistUser("01000000001", "먼저온사람");
        User u2 = persistUser("01000000002", "나중온사람");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        em.persist(CheckIn.start(u1, place, NOW));
        em.persist(CheckIn.start(u2, place, NOW.plusMinutes(10)));
        em.flush();
        em.clear(); // fetch join 실제 동작 확인(영속성 컨텍스트 비움)

        var diners = checkInRepository.findActiveWithUserByPlace(place.getId(), List.of(-1L));

        assertThat(diners).hasSize(2);
        assertThat(diners.get(0).getUser().getNickname()).isEqualTo("먼저온사람");
        assertThat(diners.get(1).getUser().getNickname()).isEqualTo("나중온사람");
    }

    @Test
    @DisplayName("혼밥러 목록: 제외 id 목록의 유저는 빠진다")
    void findActiveWithUserByPlace_excludesBlocked() {
        User userA = persistUser("01000000001", "A");
        User userB = persistUser("01000000002", "B");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        em.persist(CheckIn.start(userA, place, NOW));
        em.persist(CheckIn.start(userB, place, NOW.plusMinutes(1)));
        em.flush();
        em.clear();

        List<CheckIn> result = checkInRepository.findActiveWithUserByPlace(place.getId(), List.of(userB.getId()));

        assertThat(result).extracting(c -> c.getUser().getId()).containsExactly(userA.getId());
    }

    @Test
    @DisplayName("혼밥러 목록: 센티널(-1)만 있으면 전원 노출")
    void findActiveWithUserByPlace_sentinelShowsAll() {
        User userA = persistUser("01000000001", "A");
        User userB = persistUser("01000000002", "B");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        em.persist(CheckIn.start(userA, place, NOW));
        em.persist(CheckIn.start(userB, place, NOW.plusMinutes(1)));
        em.flush();
        em.clear();

        List<CheckIn> result = checkInRepository.findActiveWithUserByPlace(place.getId(), List.of(-1L));

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("endActiveStartedBefore: 임계 이전 ACTIVE만 ENDED, 이후는 보존")
    void ttlBulkEnd() {
        User u1 = persistUser("01000000001", "오래된");
        User u2 = persistUser("01000000002", "최근");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        LocalDateTime threshold = LocalDateTime.of(2026, 6, 15, 9, 0);
        em.persist(CheckIn.start(u1, place, threshold.minusHours(1))); // 08:00 → 만료 대상
        em.persist(CheckIn.start(u2, place, threshold.plusHours(1)));  // 10:00 → 보존
        em.flush();
        em.clear();

        int expired = checkInRepository.endActiveStartedBefore(threshold, LocalDateTime.of(2026, 6, 15, 12, 0));

        assertThat(expired).isEqualTo(1);
        assertThat(checkInRepository.countByStatus(CheckInStatus.ACTIVE)).isEqualTo(1);
        assertThat(checkInRepository.countByStatus(CheckInStatus.ENDED)).isEqualTo(1);
    }

    @Test
    @DisplayName("확장 유니크 인덱스: 한 사용자가 ACTIVE와 TOGETHER를 동시에 가질 수 없다")
    void currentUserUnique_blocksActivePlusTogether() {
        // given: 사용자 u가 ACTIVE 체크인 보유, meal_request_id FK를 만족할 실제 신청 1건
        User u = persistUser("01000000001", "혼밥러A");
        User other = persistUser("01000000002", "혼밥러B");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        checkInRepository.saveAndFlush(CheckIn.start(u, p, NOW));
        CheckIn otherCheckIn = em.persist(CheckIn.start(other, p, NOW));
        MealRequest mealRequest = em.persist(MealRequest.create(u, otherCheckIn, p, null, NOW));

        // when/then: 같은 사용자의 TOGETHER insert는 유니크 위반
        assertThatThrownBy(() ->
                checkInRepository.saveAndFlush(CheckIn.startTogether(u, p, mealRequest.getId(), NOW)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("matched_at·meal_request_id가 저장/조회된다")
    void persistsMatchColumns() {
        User u = persistUser("01000000001", "혼밥러A");
        User other = persistUser("01000000002", "혼밥러B");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn otherCheckIn = em.persist(CheckIn.start(other, p, NOW));
        MealRequest mealRequest = em.persist(MealRequest.create(u, otherCheckIn, p, null, NOW));
        em.flush();
        CheckIn saved = checkInRepository.saveAndFlush(CheckIn.startTogether(u, p, mealRequest.getId(), NOW));

        CheckIn found = checkInRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getMatchedAt()).isEqualTo(NOW);
        assertThat(found.getMealRequestId()).isEqualTo(mealRequest.getId());
    }

    @Test
    @DisplayName("오늘 혼밥 수: CANCELLED는 제외한다")
    void todayCount_excludesCancelled() {
        User u = persistUser("01000000001", "A");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn c = checkInRepository.saveAndFlush(CheckIn.start(u, p, NOW));
        c.cancel(NOW);
        checkInRepository.saveAndFlush(c);

        long today = checkInRepository.countDistinctUsersStartedSince(NOW.minusHours(1));
        assertThat(today).isZero();
    }

    @Test
    @DisplayName("findByUser_IdAndStatusIn: ACTIVE 또는 TOGETHER 현재 체크인 1건을 찾는다")
    void findCurrent_returnsActiveOrTogether() {
        User u = persistUser("01000000001", "A");
        User other = persistUser("01000000002", "B");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn otherCheckIn = em.persist(CheckIn.start(other, p, NOW));
        MealRequest mealRequest = em.persist(MealRequest.create(u, otherCheckIn, p, null, NOW));
        em.flush();
        checkInRepository.saveAndFlush(CheckIn.startTogether(u, p, mealRequest.getId(), NOW));

        Optional<CheckIn> cur = checkInRepository.findByUser_IdAndStatusIn(
                u.getId(), List.of(CheckInStatus.ACTIVE, CheckInStatus.TOGETHER));
        assertThat(cur).isPresent();
        assertThat(cur.get().getStatus()).isEqualTo(CheckInStatus.TOGETHER);
    }

    @Test
    @DisplayName("findByUser_IdAndStatusIn: ACTIVE/TOGETHER가 없으면(ENDED만) 빈 Optional")
    void findCurrent_emptyWhenOnlyEnded() {
        User u = persistUser("01000000001", "A");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn ended = CheckIn.start(u, p, NOW.minusHours(2));
        ended.end(NOW.minusHours(1));
        checkInRepository.saveAndFlush(ended);

        Optional<CheckIn> cur = checkInRepository.findByUser_IdAndStatusIn(
                u.getId(), List.of(CheckInStatus.ACTIVE, CheckInStatus.TOGETHER));
        assertThat(cur).isEmpty();
    }

    @Test
    @DisplayName("endTogetherMatchedBefore: 오래된 TOGETHER를 ENDED로 만료한다")
    void endTogether_expiresOld() {
        User u = persistUser("01000000001", "A");
        User other = persistUser("01000000002", "B");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn otherCheckIn = em.persist(CheckIn.start(other, p, NOW.minusHours(4)));
        MealRequest mealRequest = em.persist(MealRequest.create(u, otherCheckIn, p, null, NOW.minusHours(4)));
        em.flush();
        CheckIn c = CheckIn.startTogether(u, p, mealRequest.getId(), NOW.minusHours(4)); // matchedAt=4h 전
        checkInRepository.saveAndFlush(c);
        em.clear(); // 벌크 @Modifying 후 1차 캐시 stale 방지(findById 재조회 보장)

        int n = checkInRepository.endTogetherMatchedBefore(NOW.minusHours(3), NOW);
        assertThat(n).isEqualTo(1);
        assertThat(checkInRepository.findById(c.getId()).orElseThrow().getStatus())
                .isEqualTo(CheckInStatus.ENDED);
    }

    @Test
    @DisplayName("endTogetherMatchedBefore: 임계 이후(최근) TOGETHER는 보존한다")
    void endTogether_preservesRecent() {
        User u = persistUser("01000000001", "A");
        User other = persistUser("01000000002", "B");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn otherCheckIn = em.persist(CheckIn.start(other, p, NOW.minusHours(1)));
        MealRequest mealRequest = em.persist(MealRequest.create(u, otherCheckIn, p, null, NOW.minusHours(1)));
        em.flush();
        CheckIn c = CheckIn.startTogether(u, p, mealRequest.getId(), NOW.minusHours(1)); // matchedAt=1h 전
        checkInRepository.saveAndFlush(c);
        em.clear(); // 벌크 @Modifying 후 1차 캐시 stale 방지(findById 재조회 보장)

        int n = checkInRepository.endTogetherMatchedBefore(NOW.minusHours(3), NOW);
        assertThat(n).isZero();
        assertThat(checkInRepository.findById(c.getId()).orElseThrow().getStatus())
                .isEqualTo(CheckInStatus.TOGETHER);
    }

    @Test
    @DisplayName("countCompletedByUser: CANCELLED를 제외한 총 체크인 수")
    void countExcludingCancelled() {
        User u = persistUser("01000000001", "A");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn cancelled = CheckIn.start(u, p, NOW.minusDays(2));
        cancelled.cancel(NOW.minusDays(2));
        em.persist(cancelled); // CANCELLED로 insert되므로 유니크 슬롯을 점유하지 않음
        CheckIn ended = CheckIn.start(u, p, NOW.minusHours(2));
        ended.end(NOW.minusHours(1));
        em.persist(ended);
        CheckIn active = CheckIn.start(u, p, NOW);
        em.persist(active);
        em.flush();

        assertThat(checkInRepository.countCompletedByUser(u.getId())).isEqualTo(2);
        assertThat(checkInRepository.countByUser_Id(u.getId())).isEqualTo(3); // 기존 메서드는 그대로 총건수
    }

    @Test
    @DisplayName("혼밥 횟수 집계는 SEEKING과 CANCELLED를 모두 제외한다")
    void 이력집계_SEEKING_제외() {
        User user = persistUser("01000000001", "A");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        Place place2 = persistPlace("ext-2", 37.6, 127.1);
        em.persist(endedCheckIn(user, place, NOW));            // 종료된 식사(집계 대상)
        em.persist(CheckIn.startSeeking(user, place2, NOW));   // 모집중(제외돼야)
        em.flush();

        long count = checkInRepository.countCompletedByUser(user.getId());

        assertThat(count).isEqualTo(1); // SEEKING 제외 → 1
    }

    @Test
    @DisplayName("오늘 N명 집계도 SEEKING을 제외한다")
    void 오늘집계_SEEKING_제외() {
        User user = persistUser("01000000001", "A");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        em.persist(CheckIn.startSeeking(user, place, NOW));    // 모집만
        em.flush();

        long today = checkInRepository.countDistinctUsersStartedSince(NOW.minusHours(1));

        assertThat(today).isZero();
    }

    @Test
    @DisplayName("countByUserIds: 배치 집계도 CANCELLED를 제외한다(본인 프로필 카운트와 일치)")
    void countByUserIdsExcludesCancelled() {
        // given: A는 ENDED 2 + CANCELLED 1, B는 CANCELLED 1건뿐
        User a = persistUser("01000000001", "A");
        User b = persistUser("01000000002", "B");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn ended1 = CheckIn.start(a, p, NOW.minusHours(5));
        ended1.end(NOW.minusHours(4));
        em.persist(ended1);
        CheckIn ended2 = CheckIn.start(a, p, NOW.minusHours(3));
        ended2.end(NOW.minusHours(2));
        em.persist(ended2);
        CheckIn cancelledA = CheckIn.start(a, p, NOW.minusHours(1));
        cancelledA.cancel(NOW.minusHours(1));
        em.persist(cancelledA);
        CheckIn cancelledB = CheckIn.start(b, p, NOW.minusHours(1));
        cancelledB.cancel(NOW.minusHours(1));
        em.persist(cancelledB);
        em.flush();

        // when
        List<CheckInRepository.CheckInCountRow> rows = checkInRepository.countByUserIds(List.of(a.getId(), b.getId()));

        // then: A=2(CANCELLED 제외), B는 유효 체크인 0건이라 행 자체가 없음
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getUserId()).isEqualTo(a.getId());
        assertThat(rows.get(0).getCnt()).isEqualTo(2);
    }

    @Test
    @DisplayName("findTogetherByMealRequestId: 같은 매칭의 TOGETHER 쌍을 user와 함께 반환한다")
    void findTogetherPair() {
        User sender = persistUser("01000000001", "신청자");
        User receiver = persistUser("01000000002", "수신자");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn receiverCheckIn = em.persist(CheckIn.startSeeking(receiver, p, NOW));
        MealRequest mealRequest = em.persist(MealRequest.create(sender, receiverCheckIn, p, null, NOW));
        em.flush();

        checkInRepository.saveAndFlush(CheckIn.startTogether(sender, p, mealRequest.getId(), NOW));
        receiverCheckIn.matchTogether(mealRequest.getId(), NOW);
        checkInRepository.saveAndFlush(receiverCheckIn);
        em.clear();

        List<CheckIn> pair = checkInRepository.findTogetherByMealRequestId(mealRequest.getId());

        assertThat(pair).hasSize(2);
        assertThat(pair).extracting(c -> c.getUser().getNickname())
                .containsExactlyInAnyOrder("신청자", "수신자");
        assertThat(pair).allMatch(c -> c.getStatus() == CheckInStatus.TOGETHER);
    }

    @Test
    @DisplayName("findHistoryWithPlaceByUser: CANCELLED는 이력에서 제외한다")
    void history_excludesCancelled() {
        User u = persistUser("01000000001", "A");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn cancelled = CheckIn.start(u, p, NOW.minusDays(1));
        cancelled.cancel(NOW.minusDays(1));
        em.persist(cancelled);
        CheckIn ended = CheckIn.start(u, p, NOW.minusHours(2));
        ended.end(NOW.minusHours(1));
        em.persist(ended);
        em.flush();
        em.clear();

        List<CheckIn> history = checkInRepository.findHistoryWithPlaceByUser(u.getId());

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getStatus()).isEqualTo(CheckInStatus.ENDED);
    }

    @Test
    @DisplayName("countDistinctPlacesByUser: CANCELLED만 있는 방문지는 제외한다")
    void distinctPlaces_excludesCancelled() {
        User u = persistUser("01000000001", "A");
        Place p1 = persistPlace("ext-1", 37.5, 127.0);
        Place p2 = persistPlace("ext-2", 37.6, 127.1);
        CheckIn cancelledAtP2 = CheckIn.start(u, p2, NOW.minusDays(1));
        cancelledAtP2.cancel(NOW.minusDays(1));
        em.persist(cancelledAtP2);
        CheckIn endedAtP1 = CheckIn.start(u, p1, NOW.minusHours(2));
        endedAtP1.end(NOW.minusHours(1));
        em.persist(endedAtP1);
        em.flush();

        assertThat(checkInRepository.countDistinctPlacesByUser(u.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("countByUserSince: 기준 이후 체크인 중 CANCELLED는 제외한다")
    void countSince_excludesCancelled() {
        User u = persistUser("01000000001", "A");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        LocalDateTime monthStart = NOW.minusDays(5);
        CheckIn cancelled = CheckIn.start(u, p, NOW.minusDays(1));
        cancelled.cancel(NOW.minusDays(1));
        em.persist(cancelled);
        CheckIn ended = CheckIn.start(u, p, NOW.minusHours(2));
        ended.end(NOW.minusHours(1));
        em.persist(ended);
        em.flush();

        assertThat(checkInRepository.countByUserSince(u.getId(), monthStart)).isEqualTo(1);
    }

    @Test
    @DisplayName("findVisitedPlaceIds: CANCELLED만 있는 장소는 방문지로 치지 않는다")
    void visited_excludesCancelled() {
        User u = persistUser("01000000001", "A");
        Place onlyCancelled = persistPlace("ext-1", 37.5, 127.0);
        Place visited = persistPlace("ext-2", 37.6, 127.1);
        CheckIn cancelled = CheckIn.start(u, onlyCancelled, NOW.minusDays(1));
        cancelled.cancel(NOW.minusDays(1));
        em.persist(cancelled);
        em.persist(CheckIn.start(u, visited, NOW));
        em.flush();

        List<Long> ids = checkInRepository.findVisitedPlaceIds(
                u.getId(), List.of(onlyCancelled.getId(), visited.getId()));

        assertThat(ids).containsExactly(visited.getId());
    }

    @Test
    @DisplayName("findRecentForReview: 같이먹기로 매칭됐던 체크인(matchedAt not null)은 리뷰 자동연결에서 제외한다")
    void recentForReview_excludesMatched() {
        User u = persistUser("01000000001", "A");
        User other = persistUser("01000000002", "B");
        Place p = persistPlace("ext-1", 37.5, 127.0);
        CheckIn otherCheckIn = em.persist(CheckIn.start(other, p, NOW.minusHours(2)));
        MealRequest mealRequest = em.persist(MealRequest.create(u, otherCheckIn, p, null, NOW.minusHours(2)));
        em.flush();
        CheckIn matched = CheckIn.startTogether(u, p, mealRequest.getId(), NOW.minusHours(2));
        matched.end(NOW.minusHours(1));
        checkInRepository.saveAndFlush(matched);

        assertThat(checkInRepository.findRecentForReview(u.getId(), p.getId(), NOW.minusHours(3)))
                .isEmpty();

        // 매칭 없는(솔로) 체크인은 여전히 자동연결된다
        CheckIn solo = CheckIn.start(u, p, NOW.minusMinutes(30));
        solo.end(NOW.minusMinutes(10));
        checkInRepository.saveAndFlush(solo);

        Optional<CheckIn> found = checkInRepository.findRecentForReview(u.getId(), p.getId(), NOW.minusHours(3));
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(solo.getId());
    }
}
