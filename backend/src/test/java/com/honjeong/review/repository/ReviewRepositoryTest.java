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
import com.honjeong.review.domain.ReviewPhoto;
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

        List<Review> found = reviewRepository.findByPlaceWithUserAndTags(place.getId(), List.of(-1L));

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

        List<Review> found = reviewRepository.findByPlaceWithUserAndTags(place.getId(), List.of(-1L));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("식당 리뷰 목록: 제외 id 목록의 작성자 리뷰는 빠진다")
    void findByPlaceWithUserAndTags_excludesBlocked() {
        User userA = persistUser("01000000030", "정상러");
        User userB = persistUser("01000000031", "차단러");
        Place place = persistPlace("ext-block");
        Review r1 = Review.create(userA, null, place, NOW, 5, 5, "a");
        Review r2 = Review.create(userB, null, place, NOW, 4, 4, "b");
        em.persist(r1);
        em.persist(r2);
        em.flush();
        em.clear();

        List<Review> found = reviewRepository.findByPlaceWithUserAndTags(place.getId(), List.of(userB.getId()));

        assertThat(found).extracting(r -> r.getUser().getId()).containsExactly(userA.getId());
    }

    @Test
    @DisplayName("식당 리뷰 목록: 센티널(-1)만 있으면 전원 노출")
    void findByPlaceWithUserAndTags_sentinelShowsAll() {
        User userA = persistUser("01000000032", "정상러2");
        User userB = persistUser("01000000033", "차단러2");
        Place place = persistPlace("ext-block2");
        Review r1 = Review.create(userA, null, place, NOW, 5, 5, "a");
        Review r2 = Review.create(userB, null, place, NOW, 4, 4, "b");
        em.persist(r1);
        em.persist(r2);
        em.flush();
        em.clear();

        List<Review> found = reviewRepository.findByPlaceWithUserAndTags(place.getId(), List.of(-1L));

        assertThat(found).hasSize(2);
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

    @Test
    @DisplayName("update + replaceTags: 별점·본문 갱신과 태그 전량 교체가 영속화(기존 태그 orphan 삭제)")
    void update_and_replaceTags_persist() {
        User u = persistUser("01000000010", "수정러");
        Place p = persistPlace("ext-upd");
        Review r = Review.create(u, null, p, NOW, 5, 5, "old");
        r.addTag(p, "1인석 많음");
        em.persist(r);
        em.flush();
        em.clear();

        Review loaded = reviewRepository.findById(r.getId()).orElseThrow();
        loaded.update(3, 2, "new");
        loaded.replaceTags(java.util.List.of("바테이블"));
        reviewRepository.saveAndFlush(loaded);
        em.clear();

        Review after = reviewRepository.findByPlaceWithUserAndTags(p.getId(), List.of(-1L)).get(0);
        assertThat(after.getTasteRating()).isEqualTo(3);
        assertThat(after.getSoloFriendlyRating()).isEqualTo(2);
        assertThat(after.getContent()).isEqualTo("new");
        assertThat(after.getTags()).extracting("tag").containsExactly("바테이블");
    }

    @Test
    @DisplayName("delete: 리뷰 삭제 시 태그 cascade 삭제 + 체크인 슬롯 해제(existsByCheckIn=false)")
    void delete_cascadesTags_freesCheckInSlot() {
        User u = persistUser("01000000011", "삭제러");
        Place p = persistPlace("ext-del");
        CheckIn ci = persistCheckIn(u, p);
        Review r = Review.create(u, ci, p, NOW, 5, 5, "x");
        r.addTag(p, "1인석 많음");
        em.persist(r);
        em.flush();
        em.clear();
        Long ciId = ci.getId();
        assertThat(reviewRepository.existsByCheckIn_Id(ciId)).isTrue();

        Review loaded = reviewRepository.findById(r.getId()).orElseThrow();
        reviewRepository.delete(loaded);
        em.flush();
        em.clear();

        assertThat(reviewRepository.findByPlaceWithUserAndTags(p.getId(), List.of(-1L))).isEmpty();
        assertThat(reviewRepository.existsByCheckIn_Id(ciId)).isFalse();
    }

    @Test
    @DisplayName("replacePhotos: url 목록을 sortOrder 순서대로 저장하고, 재호출 시 전량 교체된다")
    void replacePhotos_replacesAllInOrder() {
        User user = persistUser("01000000020", "사진러");
        Place place = persistPlace("ext-photo");
        Review review = Review.create(user, null, place, NOW, 4, 4, "사진 테스트");
        review.replacePhotos(List.of("u1", "u2", "u3"));
        Review saved = reviewRepository.saveAndFlush(review);
        em.clear();

        Review reloaded = reviewRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPhotos()).extracting(ReviewPhoto::getImageUrl)
                .containsExactly("u1", "u2", "u3");
        assertThat(reloaded.getPhotos()).extracting(ReviewPhoto::getSortOrder)
                .containsExactly(0, 1, 2);

        reloaded.replacePhotos(List.of("only"));
        reviewRepository.saveAndFlush(reloaded);
        em.clear();
        Review again = reviewRepository.findById(saved.getId()).orElseThrow();
        assertThat(again.getPhotos()).extracting(ReviewPhoto::getImageUrl).containsExactly("only");
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
