package com.honjeong.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import com.honjeong.badge.service.BadgeService;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.review.domain.Review;
import com.honjeong.review.domain.ReviewPhoto;
import com.honjeong.review.dto.MyReviewsResponse;
import com.honjeong.review.dto.PlacePhotoResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.dto.PlaceReviewSummaryResponse;
import com.honjeong.review.dto.ReviewCreateRequest;
import com.honjeong.review.dto.ReviewResponse;
import com.honjeong.review.repository.ReviewPhotoRepository;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

class ReviewServiceTest {

    private static final Long PLACE_ID = 3L;
    private static final Long USER_ID = 1L;

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final PlaceService placeService = mock(PlaceService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BlockRepository blockRepository = mock(BlockRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-25T03:00:00Z"), ZoneOffset.UTC);
    private final ReviewPhotoRepository reviewPhotoRepository = mock(ReviewPhotoRepository.class);
    private final BadgeService badgeService = mock(BadgeService.class);
    private final ReviewService service =
            new ReviewService(reviewRepository, checkInRepository, placeService, userRepository, blockRepository,
                    clock, reviewPhotoRepository, badgeService);

    private Place place(long id) {
        Place p = mock(Place.class);
        when(p.getId()).thenReturn(id);
        return p;
    }

    /** 혼밥 화면이 보내는 모양 — 체크인을 명시하고 혼밥 별점을 함께 보낸다. */
    private ReviewCreateRequest req(Long checkInId, List<String> tags) {
        return new ReviewCreateRequest(3L, checkInId, 5, 4, "편히 먹었다", tags, null);
    }

    /** 일반 리뷰 화면이 보내는 모양 — 체크인도 혼밥 별점도 태그도 없다. */
    private ReviewCreateRequest plainReq() {
        return new ReviewCreateRequest(3L, null, 5, null, "둘이 와도 좋아요", List.of(), null);
    }

    /** 내 소유·솔로·place 일치인, 쓸 수 있는 체크인 mock을 세팅한다. */
    private CheckIn usableCheckIn(long checkInId, Place p) {
        CheckIn ci = mock(CheckIn.class);
        when(ci.getId()).thenReturn(checkInId);
        when(ci.isOwnedBy(1L)).thenReturn(true);
        when(ci.getPlace()).thenReturn(p);
        when(ci.getStartedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 11, 0));
        when(checkInRepository.findById(checkInId)).thenReturn(Optional.of(ci));
        return ci;
    }

    @Test
    @DisplayName("앱이 넘긴 checkInId로 인증 연결한다 — 서버는 스스로 찾지 않는다")
    void create_linksGivenCheckIn() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        usableCheckIn(7L, p);
        when(reviewRepository.existsByCheckIn_Id(7L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse res = service.createReview(1L, req(7L, List.of("1인석 많음")));

        assertThat(res.placeId()).isEqualTo(3L);
        assertThat(res.authenticated()).isTrue();
        assertThat(res.checkInId()).isEqualTo(7L);
        verify(badgeService).checkAndAward(1L, true);
    }

    @Test
    @DisplayName("★checkInId가 없으면 자동으로 찾지 않는다 — 판단은 앱이 미리 받아간 답 하나뿐이다")
    void create_doesNotAutoLink() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse res = service.createReview(1L, plainReq());

        assertThat(res.authenticated()).isFalse();
        // 자동 탐색 쿼리를 아예 부르지 않는다 — 서버의 두 번째 판단이 사라졌다는 뜻이다.
        verify(checkInRepository, never()).findRecentForReview(any(), any(), any());
    }

    @Test
    @DisplayName("★review-context가 '쓸 수 있다'고 한 체크인은 createReview도 연결한다 — 화면과 결과가 같아야 한다")
    void reviewContext_agreesWithCreate() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        CheckIn ci = usableCheckIn(7L, p);
        when(checkInRepository.findRecentForReview(eqL(1L), eqL(3L), any())).thenReturn(Optional.of(ci));
        when(reviewRepository.existsByCheckIn_Id(7L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        // 앱이 받는 답
        Long fromContext = service.getReviewContext(1L, 3L).linkableCheckInId();
        assertThat(fromContext).isEqualTo(7L);

        // 앱이 그 답을 그대로 되돌려 보냈을 때 실제로 연결되는가
        ReviewResponse res = service.createReview(1L, req(fromContext, List.of()));
        assertThat(res.checkInId()).isEqualTo(7L);
        assertThat(res.authenticated()).isTrue();
    }

    @Test
    @DisplayName("★이미 리뷰가 있는 체크인은 review-context도 null을 준다 — createReview의 강등 조건과 같다")
    void reviewContext_nullWhenCheckInAlreadyReviewed() {
        CheckIn ci = mock(CheckIn.class);
        when(ci.getId()).thenReturn(7L);
        when(checkInRepository.findRecentForReview(eqL(1L), eqL(3L), any())).thenReturn(Optional.of(ci));
        when(reviewRepository.existsByCheckIn_Id(7L)).thenReturn(true);

        assertThat(service.getReviewContext(1L, 3L).linkableCheckInId()).isNull();
    }

    @Test
    @DisplayName("연결할 체크인이 없으면 review-context는 null — 앱은 일반 리뷰 화면을 연다")
    void reviewContext_nullWhenNoCheckIn() {
        when(checkInRepository.findRecentForReview(eqL(1L), eqL(3L), any())).thenReturn(Optional.empty());

        assertThat(service.getReviewContext(1L, 3L).linkableCheckInId()).isNull();
    }

    @Test
    @DisplayName("★인증 없는 리뷰에 혼밥 별점을 보내면 400 — 일반 리뷰 화면을 우회해 지표를 오염시킬 수 없다")
    void create_rejectsSoloRatingWithoutCheckIn() {
        Place p = place(3L);   // place()가 when()을 쓰므로 thenReturn 인자로 바로 넣으면 nested-when 오류
        when(placeService.getById(3L)).thenReturn(p);

        assertThatThrownBy(() -> service.createReview(1L, req(null, List.of())))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("인증 없는 리뷰에 친화 태그를 보내도 400 — 태그도 혼밥한 사람만 붙일 수 있다")
    void create_rejectsTagsWithoutCheckIn() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        ReviewCreateRequest withTags = new ReviewCreateRequest(3L, null, 5, null, "본문", List.of("바테이블"), null);

        assertThatThrownBy(() -> service.createReview(1L, withTags))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("인증 리뷰인데 혼밥 별점이 없으면 400 — 혼밥 화면은 반드시 별점을 보낸다")
    void create_rejectsMissingSoloRatingWhenLinked() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        usableCheckIn(7L, p);
        when(reviewRepository.existsByCheckIn_Id(7L)).thenReturn(false);
        ReviewCreateRequest noRating = new ReviewCreateRequest(3L, 7L, 5, null, "본문", List.of(), null);

        assertThatThrownBy(() -> service.createReview(1L, noRating))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("일반 리뷰는 혼밥 별점 없이 저장된다 — 혼밥 친화도 평균에 안 들어간다")
    void create_savesPlainReviewWithNullSoloRating() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createReview(1L, plainReq());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSoloFriendlyRating()).isNull();
        assertThat(captor.getValue().getTags()).isEmpty();
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
    @DisplayName("checkInId가 같이 먹은(matched) 체크인이면 인증 없는 일반 리뷰로 저장 — 혼밥 뱃지는 솔로만")
    void create_ignoresMatchedCheckIn() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckIn matched = mock(CheckIn.class);
        when(matched.isOwnedBy(1L)).thenReturn(true);
        when(matched.getMatchedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 12, 0)); // 같이 먹음(matched)
        when(checkInRepository.findById(99L)).thenReturn(Optional.of(matched));

        ReviewResponse res = service.createReview(1L, req(99L, List.of()));

        assertThat(res.authenticated()).isFalse(); // matched → 연결 안 함(일반 리뷰)
        assertThat(res.checkInId()).isNull();
    }

    @Test
    @DisplayName("★강등되면 혼밥 별점·태그를 버린다 — 화면을 연 뒤 그 체크인에 리뷰가 생긴 드문 경우")
    void create_dropsSoloFieldsWhenDegraded() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        usableCheckIn(7L, p);
        when(reviewRepository.existsByCheckIn_Id(7L)).thenReturn(true);   // 그 사이 리뷰가 생겼다
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        // 400을 주지 않는다 — 사용자가 이미 쓴 글을 되돌릴 방법 없이 막히기 때문이다.
        ReviewResponse res = service.createReview(1L, req(7L, List.of("1인석 많음")));

        assertThat(res.authenticated()).isFalse();
        assertThat(res.checkInId()).isNull();

        // 대신 혼밥 값은 버려야 한다. 남기면 인증 없는 리뷰가 혼밥 친화도에 섞인다.
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getSoloFriendlyRating()).isNull();
        assertThat(captor.getValue().getTags()).isEmpty();
    }

    @Test
    @DisplayName("getPlaceReviewSummary — 리뷰 0건이면 count=0, 평균=null, topTags 비어있음")
    void getPlaceReviewSummary_empty() {
        when(reviewRepository.summarizeByPlace(3L))
                .thenReturn(java.util.Collections.singletonList(new Object[]{null, null, 0L, 0L}));
        when(reviewRepository.countTagsByPlace(3L))
                .thenReturn(java.util.Collections.emptyList());

        PlaceReviewSummaryResponse res = service.getPlaceReviewSummary(3L);

        assertThat(res.reviewCount()).isEqualTo(0L);
        assertThat(res.soloRatedCount()).isEqualTo(0L);
        assertThat(res.avgTasteRating()).isNull();
        assertThat(res.avgSoloFriendlyRating()).isNull();
        assertThat(res.topTags()).isEmpty();
    }

    @Test
    @DisplayName("★리뷰는 있는데 혼밥 평가가 하나도 없으면 혼밥 평균은 null — 0.0으로 보이면 안 된다")
    void getPlaceReviewSummary_noSoloRatings() {
        // 일반 리뷰만 3건 쌓인 식당. AVG(NULL만)는 NULL이고 COUNT(컬럼)은 0이다.
        when(reviewRepository.summarizeByPlace(3L))
                .thenReturn(java.util.Collections.singletonList(new Object[]{4.0, null, 3L, 0L}));
        when(reviewRepository.countTagsByPlace(3L)).thenReturn(java.util.Collections.emptyList());

        PlaceReviewSummaryResponse res = service.getPlaceReviewSummary(3L);

        assertThat(res.reviewCount()).isEqualTo(3L);
        assertThat(res.soloRatedCount()).isEqualTo(0L);
        assertThat(res.avgTasteRating()).isEqualTo(4.0);
        assertThat(res.avgSoloFriendlyRating()).isNull();   // 전체 리뷰 수로 판단했다면 여기서 NPE가 났다
    }

    @Test
    @DisplayName("getPlaceReviewSummary — 비0건: 평균 반올림, topTags 상위 5개 제한, 첫 태그 순서 확인")
    void getPlaceReviewSummary_nonEmpty() {
        when(reviewRepository.summarizeByPlace(3L))
                .thenReturn(java.util.Collections.singletonList(new Object[]{4.25, 4.6, 4L, 4L}));
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
        // 요약은 이력(솔로) 목록에서 직접 계산된다 — 별도 카운트 쿼리 스텁 불필요

        var res = service.getDiningHistory(1L);

        // c1(6/25, 리뷰 有)·c2(6/24, 리뷰 無), 둘 다 place3·6월(clock 6/25) → 총2·일기1·식당1·이번달2
        assertThat(res.summary().totalCheckIns()).isEqualTo(2L);
        assertThat(res.summary().totalReviews()).isEqualTo(1L);
        assertThat(res.summary().distinctPlaces()).isEqualTo(1L);
        assertThat(res.summary().thisMonthCheckIns()).isEqualTo(2L);
        assertThat(res.entries()).hasSize(2);
        assertThat(res.entries().get(0).review().reviewId()).isEqualTo(42L); // c1 has review
        assertThat(res.entries().get(1).review()).isNull();                  // c2 none
    }

    @Test
    @DisplayName("수정: 소유자면 별점·본문·태그 전량 교체")
    void update_byOwner() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Place p = place(3L);
        Review review = Review.create(owner, null, p, LocalDateTime.of(2026, 6, 25, 12, 0), 5, 5, "old");
        review.addTag(p, "1인석 많음");
        when(reviewRepository.findById(42L)).thenReturn(Optional.of(review));

        ReviewResponse res = service.updateReview(1L, 42L,
                new com.honjeong.review.dto.ReviewUpdateRequest(4, 3, "new", List.of("바테이블", "오래 OK"), null));

        assertThat(review.getTasteRating()).isEqualTo(4);
        assertThat(review.getSoloFriendlyRating()).isEqualTo(3);
        assertThat(review.getContent()).isEqualTo("new");
        assertThat(review.getTags()).extracting("tag").containsExactly("바테이블", "오래 OK");
        assertThat(res.authenticated()).isFalse();
    }

    @Test
    @DisplayName("수정: 타인 리뷰면 FORBIDDEN")
    void update_byNonOwner_forbidden() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Review review = Review.create(owner, null, place(3L), LocalDateTime.of(2026, 6, 25, 12, 0), 5, 5, "x");
        when(reviewRepository.findById(42L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.updateReview(1L, 42L,
                new com.honjeong.review.dto.ReviewUpdateRequest(4, 4, null, List.of(), null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("수정: 리뷰 없으면 REVIEW_NOT_FOUND")
    void update_notFound() {
        when(reviewRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateReview(1L, 42L,
                new com.honjeong.review.dto.ReviewUpdateRequest(4, 4, null, List.of(), null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    @DisplayName("수정: 미허용 태그면 INVALID_INPUT")
    void update_invalidTag() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Review review = Review.create(owner, null, place(3L), LocalDateTime.of(2026, 6, 25, 12, 0), 5, 5, "x");
        when(reviewRepository.findById(42L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.updateReview(1L, 42L,
                new com.honjeong.review.dto.ReviewUpdateRequest(4, 4, null, List.of("우주최고"), null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("삭제: 소유자면 repository.delete 호출")
    void delete_byOwner() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Review review = Review.create(owner, null, place(3L), LocalDateTime.of(2026, 6, 25, 12, 0), 5, 5, "x");
        when(reviewRepository.findById(42L)).thenReturn(Optional.of(review));

        service.deleteReview(1L, 42L);

        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("삭제: 타인 리뷰면 FORBIDDEN, delete 미호출")
    void delete_byNonOwner_forbidden() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);
        Review review = Review.create(owner, null, place(3L), LocalDateTime.of(2026, 6, 25, 12, 0), 5, 5, "x");
        when(reviewRepository.findById(42L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.deleteReview(1L, 42L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("삭제: 리뷰 없으면 REVIEW_NOT_FOUND")
    void delete_notFound() {
        when(reviewRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteReview(1L, 42L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    @Test
    @DisplayName("getPlacePhotos: 평탄화 사진 목록을 반환한다(0건이면 빈 목록)")
    void getPlacePhotos_flattens() {
        // given: 헬퍼를 when() 밖에서 미리 생성 — Mockito 중첩 stubbing 오류 방지
        ReviewPhotoRepository.ReviewPhotoRow r1 = rowOf(10L, "p1");
        ReviewPhotoRepository.ReviewPhotoRow r2 = rowOf(11L, "p2");
        when(reviewPhotoRepository.findByPlaceFlattened(PLACE_ID)).thenReturn(List.of(r1, r2));
        List<PlacePhotoResponse> result = service.getPlacePhotos(PLACE_ID);
        assertThat(result).extracting(PlacePhotoResponse::photoUrl).containsExactly("p1", "p2");

        when(reviewPhotoRepository.findByPlaceFlattened(99L)).thenReturn(List.of());
        assertThat(service.getPlacePhotos(99L)).isEmpty();
    }

    @Test
    @DisplayName("createReview: imageUrls를 리뷰 사진으로 저장한다")
    void createReview_savesPhotos() {
        // given: 기존 createReview happy-path 테스트와 동일 셋업(place·user·checkIn mock)
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        // 일반 리뷰(체크인 없음) — 혼밥 별점은 보내지 않는다. 사진은 두 화면 모두 붙일 수 있다.
        ReviewCreateRequest req = new ReviewCreateRequest(
                3L, null, 5, null, "좋아요", List.of(), List.of("url1", "url2"));

        service.createReview(1L, req);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPhotos()).extracting(ReviewPhoto::getImageUrl)
                .containsExactly("url1", "url2");
    }

    @Test
    @DisplayName("getPlaceReviews: 리뷰별 사진 url이 응답에 매핑된다")
    void getPlaceReviews_mapsPhotos() {
        // given: 헬퍼를 when() 밖에서 미리 생성 — Mockito 중첩 stubbing 오류 방지
        ReviewPhotoRepository.ReviewPhotoRow row = rowOf(10L, "p1");
        Review review = reviewWithId(10L);
        when(reviewPhotoRepository.findByPlaceFlattened(PLACE_ID)).thenReturn(List.of(row));
        when(blockRepository.findExclusionIds(USER_ID)).thenReturn(List.of(-1L));
        when(reviewRepository.findByPlaceWithUserAndTags(PLACE_ID, List.of(-1L))).thenReturn(List.of(review));

        // when
        List<PlaceReviewResponse> result = service.getPlaceReviews(PLACE_ID, USER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).imageUrls()).containsExactly("p1");
    }

    @Test
    @DisplayName("getPlaceReviews: 사진 없는 리뷰는 imageUrls가 빈 목록")
    void getPlaceReviews_emptyImagesWhenNoPhotos() {
        // given: 사진 없음 — 리뷰는 미리 생성
        Review review = reviewWithId(10L);
        when(reviewPhotoRepository.findByPlaceFlattened(PLACE_ID)).thenReturn(List.of());
        when(blockRepository.findExclusionIds(USER_ID)).thenReturn(List.of(-1L));
        when(reviewRepository.findByPlaceWithUserAndTags(PLACE_ID, List.of(-1L))).thenReturn(List.of(review));

        List<PlaceReviewResponse> result = service.getPlaceReviews(PLACE_ID, USER_ID);

        assertThat(result.get(0).imageUrls()).isEmpty();
    }

    @Test
    @DisplayName("getPlaceReviews: blockRepository의 제외 id 목록을 리포지토리 호출에 그대로 전달한다(FR-108)")
    void getPlaceReviews_passesExclusionIdsFromBlockRepository() {
        Review review = reviewWithId(10L);
        when(reviewPhotoRepository.findByPlaceFlattened(PLACE_ID)).thenReturn(List.of());
        when(blockRepository.findExclusionIds(USER_ID)).thenReturn(List.of(5L, 6L));
        when(reviewRepository.findByPlaceWithUserAndTags(PLACE_ID, List.of(5L, 6L))).thenReturn(List.of(review));

        List<PlaceReviewResponse> result = service.getPlaceReviews(PLACE_ID, USER_ID);

        assertThat(result).hasSize(1);
        verify(reviewRepository).findByPlaceWithUserAndTags(PLACE_ID, List.of(5L, 6L));
    }

    @Test
    @DisplayName("getPlaceReviews: 탈퇴한 작성자(닉네임 null)의 리뷰는 작성자명이 '알 수 없음'으로 표시된다")
    void getPlaceReviews_withdrawnAuthor_showsUnknown() {
        Review review = reviewWithId(10L, null);
        when(reviewPhotoRepository.findByPlaceFlattened(PLACE_ID)).thenReturn(List.of());
        when(blockRepository.findExclusionIds(USER_ID)).thenReturn(List.of(-1L));
        when(reviewRepository.findByPlaceWithUserAndTags(PLACE_ID, List.of(-1L))).thenReturn(List.of(review));

        List<PlaceReviewResponse> result = service.getPlaceReviews(PLACE_ID, USER_ID);

        assertThat(result.get(0).user().nickname()).isEqualTo("알 수 없음");
    }

    private static ReviewPhotoRepository.ReviewPhotoRow rowOf(Long reviewId, String url) {
        ReviewPhotoRepository.ReviewPhotoRow row = mock(ReviewPhotoRepository.ReviewPhotoRow.class);
        when(row.getReviewId()).thenReturn(reviewId);
        when(row.getImageUrl()).thenReturn(url);
        return row;
    }

    private static Review reviewWithId(Long id) {
        return reviewWithId(id, "닉네임");
    }

    private static Review reviewWithId(Long id, String authorNickname) {
        Review r = mock(Review.class);
        when(r.getId()).thenReturn(id);
        User user = mock(User.class);
        when(user.getNickname()).thenReturn(authorNickname);
        when(user.getId()).thenReturn(USER_ID);
        when(r.getUser()).thenReturn(user);
        when(r.getVisitedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 12, 0));
        when(r.getContent()).thenReturn("테스트");
        when(r.getTasteRating()).thenReturn(5);
        when(r.getSoloFriendlyRating()).thenReturn(4);
        when(r.getTags()).thenReturn(java.util.List.of());
        when(r.isAuthenticated()).thenReturn(false);
        return r;
    }

    @Test
    @DisplayName("수정: imageUrls를 기존과 동일하게 전달하면 사진이 보존된다(replacePhotos echo)")
    void updateReview_preservesPhotos() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        Place p = place(3L);
        Review review = Review.create(owner, null, p, LocalDateTime.of(2026, 6, 25, 12, 0), 5, 5, "old");
        review.replacePhotos(List.of("url1", "url2"));
        when(reviewRepository.findById(42L)).thenReturn(Optional.of(review));

        service.updateReview(1L, 42L,
                new com.honjeong.review.dto.ReviewUpdateRequest(4, 3, "new", List.of(), List.of("url1", "url2")));

        assertThat(review.getPhotos()).extracting(ReviewPhoto::getImageUrl)
                .containsExactly("url1", "url2");
    }

    @Test
    @DisplayName("타임라인: 사진 있는 리뷰의 imageUrls가 ReviewBrief에 포함된다")
    void getDiningHistory_returnsImageUrls() {
        // given: helpers built before when() to avoid Mockito nested-stubbing pitfall
        Place p3 = place(3L);
        when(p3.getName()).thenReturn("큰순두부");

        CheckIn c1 = mock(CheckIn.class);
        when(c1.getId()).thenReturn(10L);
        when(c1.getPlace()).thenReturn(p3);
        when(c1.getStartedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 11, 0));
        when(c1.getStatus()).thenReturn(com.honjeong.checkin.domain.CheckInStatus.ENDED);

        ReviewPhoto photo = mock(ReviewPhoto.class);
        when(photo.getImageUrl()).thenReturn("https://example.com/photo.jpg");

        Review r1 = mock(Review.class);
        when(r1.getId()).thenReturn(42L);
        when(r1.getCheckIn()).thenReturn(c1);
        when(r1.getContent()).thenReturn("편히");
        when(r1.getTasteRating()).thenReturn(5);
        when(r1.getSoloFriendlyRating()).thenReturn(4);
        when(r1.getTags()).thenReturn(List.of());
        when(r1.getPhotos()).thenReturn(List.of(photo));

        when(checkInRepository.findHistoryWithPlaceByUser(1L)).thenReturn(List.of(c1));
        when(reviewRepository.findByUserWithCheckIn(1L)).thenReturn(List.of(r1));

        // when
        var res = service.getDiningHistory(1L);

        // then
        assertThat(res.entries().get(0).review().imageUrls())
                .containsExactly("https://example.com/photo.jpg");
    }

    @Test
    @DisplayName("내 리뷰 전체: 인증 플래그·사진 조립·작성순 유지")
    void getMyReviews_mapsAuthenticatedAndPhotos() {
        // given: 일반 리뷰(checkIn null)와 인증 리뷰(checkIn 연결) 2건 — 스텁 순서(일반 먼저)가 응답 순서
        Place generalPlace = place(3L);
        when(generalPlace.getName()).thenReturn("국밥집");
        Review general = mock(Review.class);
        when(general.getId()).thenReturn(1L);
        when(general.getPlace()).thenReturn(generalPlace);
        when(general.getVisitedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 12, 0));
        when(general.getContent()).thenReturn("무난했다");
        when(general.getTasteRating()).thenReturn(4);
        when(general.getSoloFriendlyRating()).thenReturn(3);
        when(general.getTags()).thenReturn(List.of());
        when(general.getCheckIn()).thenReturn(null);
        when(general.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 12, 0));

        Place authPlace = place(4L);
        when(authPlace.getName()).thenReturn("혼밥맛집");
        CheckIn linkedCheckIn = mock(CheckIn.class);
        Review authenticated = mock(Review.class);
        when(authenticated.getId()).thenReturn(2L);
        when(authenticated.getPlace()).thenReturn(authPlace);
        when(authenticated.getVisitedAt()).thenReturn(LocalDateTime.of(2026, 6, 24, 12, 0));
        when(authenticated.getContent()).thenReturn("혼밥 인증 완료");
        when(authenticated.getTasteRating()).thenReturn(5);
        when(authenticated.getSoloFriendlyRating()).thenReturn(5);
        when(authenticated.getTags()).thenReturn(List.of());
        when(authenticated.getCheckIn()).thenReturn(linkedCheckIn);
        when(authenticated.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 24, 12, 0));

        ReviewPhotoRepository.ReviewPhotoRow row = rowOf(2L, "http://img/1.jpg");
        when(reviewRepository.findAllByUserWithPlaceAndTags(1L)).thenReturn(List.of(general, authenticated));
        when(reviewPhotoRepository.findByUserFlattened(1L)).thenReturn(List.of(row));

        var res = service.getMyReviews(1L);

        assertThat(res.reviews()).hasSize(2);
        assertThat(res.reviews().get(0).authenticated()).isFalse();  // 스텁 순서 유지(일반 먼저)
        assertThat(res.reviews().get(1).authenticated()).isTrue();
        assertThat(res.reviews().get(1).imageUrls()).containsExactly("http://img/1.jpg");
        assertThat(res.reviews().get(0).imageUrls()).isEmpty();
        assertThat(res.reviews().get(0).placeName()).isEqualTo("국밥집");
    }

    @Test
    @DisplayName("내 리뷰 없음 → 빈 목록")
    void getMyReviews_empty() {
        when(reviewRepository.findAllByUserWithPlaceAndTags(1L)).thenReturn(List.of());
        when(reviewPhotoRepository.findByUserFlattened(1L)).thenReturn(List.of());
        assertThat(service.getMyReviews(1L).reviews()).isEmpty();
    }

    @Test
    @DisplayName("혼밥 기록 요약의 일기 수는 인증 리뷰만 센다")
    void getDiningHistory_summaryCountsOnlyAuthenticatedReviews() {
        // given: 기존 getDiningHistory 해피패스(diningHistory_joinsReviewsToCheckIns)와 동일 셋업
        Place p3 = place(3L);
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
        when(r1.getCheckIn()).thenReturn(c1);
        when(r1.getContent()).thenReturn("편히");
        when(r1.getTasteRating()).thenReturn(5);
        when(r1.getSoloFriendlyRating()).thenReturn(4);
        when(r1.getTags()).thenReturn(List.of());

        when(checkInRepository.findHistoryWithPlaceByUser(1L)).thenReturn(List.of(c1, c2));
        when(reviewRepository.findByUserWithCheckIn(1L)).thenReturn(List.of(r1));

        var res = service.getDiningHistory(1L);

        // c1만 리뷰 有, c2는 리뷰 無 → 총 방문 2건이어도 일기(인증)는 1건만 센다
        assertThat(res.summary().totalCheckIns()).isEqualTo(2L);
        assertThat(res.summary().totalReviews()).isEqualTo(1L);
    }

    private static Long eqL(long v) { return org.mockito.ArgumentMatchers.eq(v); }
}
