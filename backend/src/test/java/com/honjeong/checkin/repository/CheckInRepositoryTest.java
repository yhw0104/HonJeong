package com.honjeong.checkin.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

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
    @DisplayName("findActiveWithUserByPlace: 식당의 ACTIVE 혼밥러를 startedAt 오름차순, user fetch")
    void activeDiners() {
        User u1 = persistUser("01000000001", "먼저온사람");
        User u2 = persistUser("01000000002", "나중온사람");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        em.persist(CheckIn.start(u1, place, NOW));
        em.persist(CheckIn.start(u2, place, NOW.plusMinutes(10)));
        em.flush();
        em.clear(); // fetch join 실제 동작 확인(영속성 컨텍스트 비움)

        var diners = checkInRepository.findActiveWithUserByPlace(place.getId());

        assertThat(diners).hasSize(2);
        assertThat(diners.get(0).getUser().getNickname()).isEqualTo("먼저온사람");
        assertThat(diners.get(1).getUser().getNickname()).isEqualTo("나중온사람");
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
}
