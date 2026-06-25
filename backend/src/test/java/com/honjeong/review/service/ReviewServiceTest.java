package com.honjeong.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.review.domain.Review;
import com.honjeong.review.dto.PlaceReviewSummaryResponse;
import com.honjeong.review.dto.ReviewCreateRequest;
import com.honjeong.review.dto.ReviewResponse;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

class ReviewServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final PlaceService placeService = mock(PlaceService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-25T03:00:00Z"), ZoneOffset.UTC);
    private final ReviewService service =
            new ReviewService(reviewRepository, checkInRepository, placeService, userRepository, clock);

    private Place place(long id) {
        Place p = mock(Place.class);
        when(p.getId()).thenReturn(id);
        return p;
    }

    private ReviewCreateRequest req(Long checkInId, List<String> tags) {
        return new ReviewCreateRequest(3L, checkInId, 5, 4, "편히 먹었다", tags);
    }

    @Test
    @DisplayName("checkInId 없으면 24h 내 최근 체크인을 자동 인증 연결")
    void create_autoLinksRecentCheckIn() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        CheckIn recent = mock(CheckIn.class);
        when(recent.getId()).thenReturn(7L);
        when(recent.getStartedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 11, 0));
        when(checkInRepository.findRecentForReview(eqL(1L), eqL(3L), any())).thenReturn(Optional.of(recent));
        when(reviewRepository.existsByCheckIn_Id(7L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse res = service.createReview(1L, req(null, List.of("1인석 많음")));

        assertThat(res.placeId()).isEqualTo(3L);
        assertThat(res.authenticated()).isTrue();
        assertThat(res.checkInId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("미허용 태그면 INVALID_INPUT")
    void create_rejectsUnknownTag() {
        Place p = place(3L);  // place()는 when() 내부를 쓰므로 thenReturn 인자로 직접 쓰면 nested-when 오류 발생
        when(placeService.getById(3L)).thenReturn(p);

        assertThatThrownBy(() -> service.createReview(1L, req(null, List.of("우주최고"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("checkInId가 타인 소유면 FORBIDDEN")
    void create_forbidsOthersCheckIn() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        CheckIn others = mock(CheckIn.class);
        when(others.isOwnedBy(1L)).thenReturn(false);
        when(checkInRepository.findById(99L)).thenReturn(Optional.of(others));

        assertThatThrownBy(() -> service.createReview(1L, req(99L, List.of())))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("checkInId 지정했으나 place 불일치면 인증 없는 일반 리뷰로 저장")
    void create_ignoresCheckInWhenPlaceMismatch() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Place otherPlace = mock(Place.class);
        when(otherPlace.getId()).thenReturn(999L);
        CheckIn ci = mock(CheckIn.class);
        when(ci.isOwnedBy(1L)).thenReturn(true);
        when(ci.getPlace()).thenReturn(otherPlace);
        when(checkInRepository.findById(99L)).thenReturn(Optional.of(ci));

        ReviewResponse res = service.createReview(1L, req(99L, List.of()));

        assertThat(res.authenticated()).isFalse();
        assertThat(res.checkInId()).isNull();
    }

    @Test
    @DisplayName("checkInId 지정했으나 DB에 없으면 인증 없는 일반 리뷰로 저장")
    void create_ignoresCheckInWhenNotFound() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.findById(99L)).thenReturn(Optional.empty());

        ReviewResponse res = service.createReview(1L, req(99L, List.of()));

        assertThat(res.authenticated()).isFalse();
        assertThat(res.checkInId()).isNull();
    }

    @Test
    @DisplayName("그 체크인에 이미 인증 리뷰가 있으면 차단 않고 인증 없는 일반 리뷰로 저장")
    void create_degradesToGeneralWhenCheckInAlreadyReviewed() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        CheckIn recent = mock(CheckIn.class);
        when(recent.getId()).thenReturn(7L);
        when(checkInRepository.findRecentForReview(eqL(1L), eqL(3L), any())).thenReturn(Optional.of(recent));
        when(reviewRepository.existsByCheckIn_Id(7L)).thenReturn(true);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse res = service.createReview(1L, req(null, List.of()));

        assertThat(res.authenticated()).isFalse();   // 인증 강등
        assertThat(res.checkInId()).isNull();
    }

    @Test
    @DisplayName("getPlaceReviewSummary — 리뷰 0건이면 count=0, 평균=null, topTags 비어있음")
    void getPlaceReviewSummary_empty() {
        when(reviewRepository.summarizeByPlace(3L))
                .thenReturn(java.util.Collections.singletonList(new Object[]{null, null, 0L}));
        when(reviewRepository.countTagsByPlace(3L))
                .thenReturn(java.util.Collections.emptyList());

        PlaceReviewSummaryResponse res = service.getPlaceReviewSummary(3L);

        assertThat(res.reviewCount()).isEqualTo(0L);
        assertThat(res.avgTasteRating()).isNull();
        assertThat(res.avgSoloFriendlyRating()).isNull();
        assertThat(res.topTags()).isEmpty();
    }

    @Test
    @DisplayName("getPlaceReviewSummary — 비0건: 평균 반올림, topTags 상위 5개 제한, 첫 태그 순서 확인")
    void getPlaceReviewSummary_nonEmpty() {
        when(reviewRepository.summarizeByPlace(3L))
                .thenReturn(java.util.Collections.singletonList(new Object[]{4.25, 4.6, 4L}));
        when(reviewRepository.countTagsByPlace(3L))
                .thenReturn(java.util.Arrays.asList(
                        new Object[]{"1인석 많음", 3L},
                        new Object[]{"바테이블", 2L},
                        new Object[]{"눈치 없음", 2L},
                        new Object[]{"조용함", 1L},
                        new Object[]{"금연", 1L},
                        new Object[]{"주차 가능", 1L}
                ));

        PlaceReviewSummaryResponse res = service.getPlaceReviewSummary(3L);

        assertThat(res.reviewCount()).isEqualTo(4L);
        assertThat(res.avgTasteRating()).isCloseTo(4.3, org.assertj.core.data.Offset.offset(0.001));
        assertThat(res.avgSoloFriendlyRating()).isEqualTo(4.6);
        assertThat(res.topTags()).hasSize(5);
        assertThat(res.topTags().get(0).tag()).isEqualTo("1인석 많음");
    }

    @Test
    @DisplayName("타임라인: 체크인 이력에 리뷰를 매칭, 리뷰 없는 체크인은 review=null")
    void diningHistory_joinsReviewsToCheckIns() {
        com.honjeong.place.domain.Place p3 = place(3L);
        CheckIn c1 = mock(CheckIn.class);
        when(c1.getId()).thenReturn(10L);
        when(c1.getPlace()).thenReturn(p3);
        when(c1.getStartedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 11, 0));
        when(c1.getStatus()).thenReturn(com.honjeong.checkin.domain.CheckInStatus.ENDED);
        when(p3.getName()).thenReturn("큰순두부");
        CheckIn c2 = mock(CheckIn.class);
        when(c2.getId()).thenReturn(8L);
        when(c2.getPlace()).thenReturn(p3);
        when(c2.getStartedAt()).thenReturn(LocalDateTime.of(2026, 6, 24, 11, 0));
        when(c2.getStatus()).thenReturn(com.honjeong.checkin.domain.CheckInStatus.ENDED);

        Review r1 = mock(Review.class);
        when(r1.getId()).thenReturn(42L);
        CheckIn c1ref = c1;
        when(r1.getCheckIn()).thenReturn(c1ref);
        when(r1.getContent()).thenReturn("편히");
        when(r1.getTasteRating()).thenReturn(5);
        when(r1.getSoloFriendlyRating()).thenReturn(4);
        when(r1.getTags()).thenReturn(java.util.List.of());

        when(checkInRepository.findHistoryWithPlaceByUser(1L)).thenReturn(java.util.List.of(c1, c2));
        when(reviewRepository.findByUserWithCheckIn(1L)).thenReturn(java.util.List.of(r1));
        when(checkInRepository.countByUser_Id(1L)).thenReturn(5L);
        when(reviewRepository.countByUser_Id(1L)).thenReturn(3L);
        when(checkInRepository.countDistinctPlacesByUser(1L)).thenReturn(2L);
        when(checkInRepository.countByUserSince(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(4L);

        var res = service.getDiningHistory(1L);

        assertThat(res.summary().totalCheckIns()).isEqualTo(5L);
        assertThat(res.summary().totalReviews()).isEqualTo(3L);
        assertThat(res.summary().distinctPlaces()).isEqualTo(2L);
        assertThat(res.summary().thisMonthCheckIns()).isEqualTo(4L);
        assertThat(res.entries()).hasSize(2);
        assertThat(res.entries().get(0).review().reviewId()).isEqualTo(42L); // c1 has review
        assertThat(res.entries().get(1).review()).isNull();                  // c2 none
    }

    private static Long eqL(long v) { return org.mockito.ArgumentMatchers.eq(v); }
}
