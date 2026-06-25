package com.honjeong.review.repository;

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

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.place.domain.Place;
import com.honjeong.review.domain.Review;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ReviewRepositoryTest extends AbstractPostgresTest {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 25, 12, 0);

    @Test
    @DisplayName("리뷰 저장 시 태그 cascade 저장, 식당별 조회로 user·tags 로드")
    void save_and_findByPlace() {
        User user = persistUser("01000000001", "연남러");
        Place place = persistPlace("ext-1");
        CheckIn checkIn = persistCheckIn(user, place);

        Review review = Review.create(user, checkIn, place, NOW, 5, 4, "마음 편히 먹었다");
        review.addTag(place, "1인석 많음");
        review.addTag(place, "눈치 없음");
        em.persist(review);
        em.flush();
        em.clear();

        List<Review> found = reviewRepository.findByPlaceWithUserAndTags(place.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUser().getNickname()).isEqualTo("연남러");
        assertThat(found.get(0).getTags()).extracting("tag")
                .containsExactlyInAnyOrder("1인석 많음", "눈치 없음");
        assertThat(found.get(0).isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("체크인 없이 작성한 리뷰는 isAuthenticated()가 false")
    void notAuthenticated_whenCheckInIsNull() {
        User user = persistUser("01000000002", "강남러");
        Place place = persistPlace("ext-2");

        Review review = Review.create(user, null, place, NOW, 3, 3, null);
        em.persist(review);
        em.flush();
        em.clear();

        List<Review> found = reviewRepository.findByPlaceWithUserAndTags(place.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("식당 집계: 평균 별점·태그 빈도, 리뷰 0건 경계")
    void summarize_and_tagCounts() {
        User u1 = persistUser("01000000003", "a");
        User u2 = persistUser("01000000004", "b");
        Place place = persistPlace("ext-agg");

        Review r1 = Review.create(u1, null, place, NOW, 5, 5, "x");
        r1.addTag(place, "1인석 많음");
        Review r2 = Review.create(u2, null, place, NOW, 3, 4, "y");
        r2.addTag(place, "1인석 많음");
        r2.addTag(place, "바테이블");
        em.persist(r1); em.persist(r2); em.flush(); em.clear();

        java.util.List<Object[]> agg = reviewRepository.summarizeByPlace(place.getId());
        assertThat(((Number) agg.get(0)[2]).longValue()).isEqualTo(2L);
        assertThat(((Number) agg.get(0)[0]).doubleValue()).isEqualTo(4.0); // (5+3)/2

        assertThat(((Number) agg.get(0)[1]).doubleValue()).isEqualTo(4.5); // avgSoloFriendly: (5+4)/2

        java.util.List<Object[]> tags = reviewRepository.countTagsByPlace(place.getId());
        assertThat(tags.get(0)[0]).isEqualTo("1인석 많음");        // 최빈
        assertThat(((Number) tags.get(0)[1]).longValue()).isEqualTo(2L);
    }

    @Test
    @DisplayName("리뷰 0건인 새 place에 대해 summarizeByPlace는 1행(count=0, AVG=null) 반환, countTagsByPlace는 빈 리스트")
    void summarize_emptyPlace() {
        Place emptyPlace = persistPlace("ext-empty");
        em.flush(); em.clear();

        java.util.List<Object[]> emptyAgg = reviewRepository.summarizeByPlace(emptyPlace.getId());
        assertThat(emptyAgg).hasSize(1);                                              // 스칼라 집계는 0건이어도 한 행
        assertThat(((Number) emptyAgg.get(0)[2]).longValue()).isEqualTo(0L);          // count = 0
        assertThat(emptyAgg.get(0)[0]).isNull();                                      // AVG(tasteRating) = NULL
        assertThat(emptyAgg.get(0)[1]).isNull();                                      // AVG(soloFriendlyRating) = NULL

        assertThat(reviewRepository.countTagsByPlace(emptyPlace.getId())).isEmpty();
    }

    // --- helpers (MealRequestRepositoryTest와 동일 도메인 팩토리 사용) ---
    private User persistUser(String phone, String nickname) {
        User u = User.pending(phone, null);
        u.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(u);
    }

    private Place persistPlace(String sourceId) {
        return em.persist(Place.ofPublicData(sourceId, sourceId + "식당", "한식", "서울", "서울 도로명",
                37.5, 127.0, null, "영업"));
    }

    private CheckIn persistCheckIn(User user, Place place) {
        return em.persist(CheckIn.start(user, place, NOW.minusHours(1)));
    }
}
