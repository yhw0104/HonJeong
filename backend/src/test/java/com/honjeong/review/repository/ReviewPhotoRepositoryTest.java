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

import com.honjeong.global.config.JpaConfig;
import com.honjeong.place.domain.Place;
import com.honjeong.review.domain.Review;
import com.honjeong.review.repository.ReviewPhotoRepository.ReviewPhotoRow;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ReviewPhotoRepositoryTest extends AbstractPostgresTest {

    @Autowired private ReviewPhotoRepository reviewPhotoRepository;
    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("findByPlaceFlattened: 같은 place의 사진을 리뷰 최신순·sortOrder순으로 평탄화한다")
    void findByPlaceFlattened_orders() {
        // given: 같은 place에 리뷰 2개(오래된 것·최신 것), 각각 사진 2/1장 저장
        User user = persistUser("01000000030", "사진정렬러");
        Place place = persistPlace("ext-photo-flat");

        LocalDateTime oldVisit = LocalDateTime.of(2026, 6, 20, 12, 0);
        LocalDateTime newVisit = LocalDateTime.of(2026, 6, 25, 12, 0);

        // 오래된 리뷰 — 사진 2장 (sortOrder 0·1)
        Review oldReview = Review.create(user, null, place, oldVisit, 4, 4, "오래된 리뷰");
        oldReview.replacePhotos(List.of("old1", "old2"));
        em.persist(oldReview);

        // 최신 리뷰 — 사진 1장 (sortOrder 0)
        Review newReview = Review.create(user, null, place, newVisit, 5, 5, "최신 리뷰");
        newReview.replacePhotos(List.of("new1"));
        em.persist(newReview);

        em.flush();
        em.clear();

        // when
        List<ReviewPhotoRow> rows = reviewPhotoRepository.findByPlaceFlattened(place.getId());

        // then: 최신 리뷰 사진 먼저, 오래된 리뷰 사진은 sortOrder 순
        assertThat(rows).extracting(ReviewPhotoRow::getImageUrl)
                .containsExactly("new1", "old1", "old2");
        assertThat(rows).extracting(ReviewPhotoRow::getReviewId)
                .containsExactly(newReview.getId(), oldReview.getId(), oldReview.getId());
    }

    @Test
    @DisplayName("findByPlaceFlattened: 사진 없는 place면 빈 목록 반환")
    void findByPlaceFlattened_emptyWhenNoPhotos() {
        Place place = persistPlace("ext-photo-empty");
        em.flush();
        em.clear();

        List<ReviewPhotoRow> rows = reviewPhotoRepository.findByPlaceFlattened(place.getId());

        assertThat(rows).isEmpty();
    }

    // --- helpers ---
    private User persistUser(String phone, String nickname) {
        User u = User.pending(phone, null);
        u.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(u);
    }

    private Place persistPlace(String sourceId) {
        return em.persist(Place.ofPublicData(sourceId, sourceId + "식당", "한식", "서울", "서울 도로명",
                37.5, 127.0, null, "영업"));
    }
}
