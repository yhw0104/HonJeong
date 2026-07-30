package com.honjeong.review.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.honjeong.review.domain.SoloFriendlyTags;
import com.honjeong.review.dto.DiningHistoryResponse;
import com.honjeong.review.dto.MyReviewsResponse;
import com.honjeong.review.dto.PlacePhotoResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.dto.PlaceReviewSummaryResponse;
import com.honjeong.review.dto.ReviewCreateRequest;
import com.honjeong.review.dto.ReviewResponse;
import com.honjeong.review.dto.ReviewUpdateRequest;
import com.honjeong.review.repository.ReviewPhotoRepository;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 리뷰(혼밥일기) 도메인 서비스 — 작성(인증 자동연결·태그 검증)·수정·삭제와 식당별 리뷰 조회·집계,
 * 내 방문 기록·내 리뷰 조립을 담당한다.
 *
 * <p>사용 Controller: ReviewController, PlaceReviewController, DiningHistoryController.
 */
@Service
public class ReviewService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int AUTH_WINDOW_HOURS = 24;
    private static final int TOP_TAGS_LIMIT = 5;

    private final ReviewRepository reviewRepository;
    private final CheckInRepository checkInRepository;
    private final PlaceService placeService;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final Clock clock;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final BadgeService badgeService;

    public ReviewService(ReviewRepository reviewRepository, CheckInRepository checkInRepository,
            PlaceService placeService, UserRepository userRepository, BlockRepository blockRepository, Clock clock,
            ReviewPhotoRepository reviewPhotoRepository, BadgeService badgeService) {
        this.reviewRepository = reviewRepository;
        this.checkInRepository = checkInRepository;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.clock = clock;
        this.reviewPhotoRepository = reviewPhotoRepository;
        this.badgeService = badgeService;
    }

    /**
     * 식당 리뷰를 집계한다 — 리뷰 수, 별점 평균 2종(소수 1자리), 상위 태그 5개.
     *
     * @param placeId 식당 ID
     * @return 리뷰 수, 맛·혼밥 적합도 평균(리뷰 없으면 null), 상위 태그
     */
    @Transactional(readOnly = true)
    public PlaceReviewSummaryResponse getPlaceReviewSummary(Long placeId) {
        Object[] agg = reviewRepository.summarizeByPlace(placeId).get(0);
        long count = ((Number) agg[2]).longValue();
        Double avgTaste = count == 0 ? null : round1(((Number) agg[0]).doubleValue());
        Double avgSolo = count == 0 ? null : round1(((Number) agg[1]).doubleValue());

        List<PlaceReviewSummaryResponse.TagCount> topTags = reviewRepository.countTagsByPlace(placeId).stream()
                .limit(TOP_TAGS_LIMIT)
                .map(row -> new PlaceReviewSummaryResponse.TagCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        return new PlaceReviewSummaryResponse(placeId, count, avgTaste, avgSolo, topTags);
    }

    /** 소수 첫째 자리로 반올림한다. */
    private static Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /**
     * 식당 리뷰 목록을 조회한다 — 사진은 별도 쿼리로 조립하고, 차단 상대 리뷰는 서버에서 제외한다.
     *
     * @param placeId 식당 ID
     * @param currentUserId 요청 사용자 ID(차단 필터·내 리뷰 표시용)
     * @return 리뷰 목록(작성 최신순)
     */
    @Transactional(readOnly = true)
    public List<PlaceReviewResponse> getPlaceReviews(Long placeId, Long currentUserId) {
        Map<Long, List<String>> photosByReview = reviewPhotoRepository.findByPlaceFlattened(placeId).stream()
                .collect(Collectors.groupingBy(
                        ReviewPhotoRepository.ReviewPhotoRow::getReviewId,
                        LinkedHashMap::new,
                        Collectors.mapping(ReviewPhotoRepository.ReviewPhotoRow::getImageUrl, Collectors.toList())));
        // 차단 상대의 리뷰는 숨긴다 — 응답 DTO에 작성자 id가 없어 서버 필터가 유일한 방법(FR-108).
        List<Long> excluded = blockRepository.findExclusionIds(currentUserId);
        return reviewRepository.findByPlaceWithUserAndTags(placeId, excluded).stream()
                .map(r -> PlaceReviewResponse.from(r, currentUserId,
                        photosByReview.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    /**
     * 식당의 모든 리뷰 사진 목록을 조회한다(리뷰 최신순 평탄화).
     *
     * @param placeId 식당 ID
     * @return 사진 URL + 출처 리뷰 ID 목록
     */
    @Transactional(readOnly = true)
    public List<PlacePhotoResponse> getPlacePhotos(Long placeId) {
        return reviewPhotoRepository.findByPlaceFlattened(placeId).stream()
                .map(row -> new PlacePhotoResponse(row.getImageUrl(), row.getReviewId()))
                .toList();
    }

    /**
     * 리뷰를 작성한다. 별점 2종은 필수이며(컨트롤러 {@code @Valid}), 태그는 허용 프리셋만 받는다(아니면 400).
     *
     * <p>인증 연결: checkInId가 주어지면 내 소유이고 place가 일치할 때 연결하고(타인 소유면 403),
     * 없으면 그 place의 24h 내 최근 체크인을 자동 연결한다.
     *
     * @param userId 작성자 ID
     * @param req placeId, checkInId(선택), tasteRating, soloFriendlyRating, content, tags, imageUrls
     * @return 생성된 리뷰 ID·식당 ID·연결 체크인 ID·인증 여부
     */
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewCreateRequest req) {
        Place place = placeService.getById(req.placeId());
        validateTags(req.tags());

        LocalDateTime now = now();
        CheckIn linked = resolveCheckIn(userId, req.placeId(), req.checkInId(), now);
        if (linked != null && reviewRepository.existsByCheckIn_Id(linked.getId())) {
            linked = null;   // 그 방문엔 이미 인증 리뷰가 있음 → 차단하지 않고 인증 없는 일반 리뷰로 저장
        }
        LocalDateTime visitedAt = linked != null ? linked.getStartedAt() : now;

        User userRef = userRepository.getReferenceById(userId);
        Review review = Review.create(userRef, linked, place, visitedAt,
                req.tasteRating(), req.soloFriendlyRating(), req.content());
        if (req.tags() != null) {
            req.tags().forEach(tag -> review.addTag(place, tag));
        }
        review.replacePhotos(req.imageUrls());
        try {
            Review saved = reviewRepository.saveAndFlush(review);
            badgeService.checkAndAward(userId, true); // 일기 뱃지 지급 체크
            return ReviewResponse.from(saved);
        } catch (DataIntegrityViolationException e) {   // 동시 작성 경쟁 → 부분 유니크 위반
            throw new BusinessException(ErrorCode.REVIEW_DUPLICATE_CHECKIN);
        }
    }

    /**
     * 리뷰를 수정한다. 본인만 가능하고(아니면 403), 없으면 404다.
     *
     * <p>별점·본문을 갱신하고 태그·사진을 전량 교체한다.
     * place·checkIn·visitedAt은 불변이다(인증 무결성 유지).
     *
     * @param userId 요청 사용자 ID
     * @param reviewId 수정할 리뷰 ID
     * @param req tasteRating, soloFriendlyRating, content, tags, imageUrls
     * @return 수정된 리뷰 ID·식당 ID·연결 체크인 ID·인증 여부
     */
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewUpdateRequest req) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        validateTags(req.tags());
        review.update(req.tasteRating(), req.soloFriendlyRating(), req.content());
        review.replaceTags(req.tags());
        review.replacePhotos(req.imageUrls());
        return ReviewResponse.from(review);
    }

    /**
     * 리뷰를 완전 삭제한다 — 태그·사진은 cascade로 함께 삭제된다.
     *
     * <p>본인만 가능하고(아니면 403), 없으면 404다.
     *
     * @param userId 요청 사용자 ID
     * @param reviewId 삭제할 리뷰 ID
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        reviewRepository.delete(review);
    }

    /** 태그가 허용 프리셋({@link SoloFriendlyTags#ALLOWED})에 속하는지 검증한다 — 위반 시 400(INVALID_INPUT). */
    private void validateTags(List<String> tags) {
        if (tags == null) {
            return;
        }
        for (String tag : tags) {
            if (!SoloFriendlyTags.ALLOWED.contains(tag)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "허용되지 않은 태그입니다: " + tag);
            }
        }
    }

    /**
     * 리뷰에 연결할 체크인을 결정한다.
     *
     * <p>checkInId를 명시하면 소유·식당·솔로 여부를 검증하고(타인 소유 403, 불일치나 매칭된 체크인이면 null),
     * 미지정이면 24h 내 최근 솔로 체크인을 자동 탐색한다. 인증(혼밥 뱃지)은
     * <b>혼자 먹은(matchedAt IS NULL) 체크인</b>만 대상이다.
     */
    private CheckIn resolveCheckIn(Long userId, Long placeId, Long checkInId, LocalDateTime now) {
        if (checkInId != null) {
            CheckIn ci = checkInRepository.findById(checkInId).orElse(null);
            if (ci == null) {
                return null;
            }
            if (!ci.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            // 같이 먹은(matched) 체크인은 혼밥 인증이 아니다 → 연결 안 함(일반 리뷰로 저장). 자동연결(findRecentForReview)과 기준 일치.
            if (ci.getMatchedAt() != null) {
                return null;
            }
            return ci.getPlace().getId().equals(placeId) ? ci : null;   // place 불일치면 일반 리뷰
        }
        LocalDateTime since = now.minusHours(AUTH_WINDOW_HOURS);
        return checkInRepository.findRecentForReview(userId, placeId, since).orElse(null);
    }

    /**
     * 내 방문 타임라인을 조회한다 — 체크인 이력(최신순)에 각 체크인의 리뷰(있으면)를 조립하고
     * 요약 통계(총 체크인·일기·방문 식당·이달 체크인)를 함께 반환한다.
     *
     * @param userId 회원 id
     * @return 방문 타임라인 + 요약 통계
     */
    @Transactional(readOnly = true)
    public DiningHistoryResponse getDiningHistory(Long userId) {
        Map<Long, Review> reviewByCheckIn = reviewRepository.findByUserWithCheckIn(userId).stream()
                .collect(Collectors.toMap(r -> r.getCheckIn().getId(), r -> r, (a, b) -> a)); // 최신 우선(쿼리 DESC)

        // 타임라인은 혼밥(솔로) 체크인만(findHistoryWithPlaceByUser가 matchedAt IS NULL 강제). 요약도 이 목록에서 직접 계산해
        // 리스트와 요약이 절대 어긋나지 않게 한다 — 같이 먹은 방문은 '내 혼밥 기록'에서 완전히 제외된다.
        List<CheckIn> history = checkInRepository.findHistoryWithPlaceByUser(userId);
        List<DiningHistoryResponse.Entry> entries = history.stream()
                .map(c -> {
                    Review r = reviewByCheckIn.get(c.getId());
                    DiningHistoryResponse.ReviewBrief brief = r == null ? null
                            : new DiningHistoryResponse.ReviewBrief(r.getId(), r.getContent(),
                                    r.getTasteRating(), r.getSoloFriendlyRating(),
                                    r.getTags().stream().map(t -> t.getTag()).toList(),
                                    r.getPhotos().stream().map(ReviewPhoto::getImageUrl).toList());
                    return new DiningHistoryResponse.Entry(c.getId(), c.getPlace().getId(),
                            c.getPlace().getName(), c.getStartedAt(), c.getStatus().name(), brief);
                })
                .toList();

        LocalDateTime monthStart = LocalDate.ofInstant(clock.instant(), KST).withDayOfMonth(1).atStartOfDay();
        DiningHistoryResponse.Summary summary = new DiningHistoryResponse.Summary(
                history.size(),                                                               // 총 혼밥(솔로 완료 체크인)
                history.stream().filter(c -> reviewByCheckIn.containsKey(c.getId())).count(),  // 일기(솔로 체크인에 달린 리뷰)
                history.stream().map(c -> c.getPlace().getId()).distinct().count(),           // 방문 식당(솔로 기준)
                history.stream().filter(c -> !c.getStartedAt().isBefore(monthStart)).count()); // 이번달 혼밥(솔로)

        return new DiningHistoryResponse(summary, entries);
    }

    /**
     * 내가 쓴 리뷰 전체(인증+일반)를 작성 최신순으로 반환한다. '내가 쓴 리뷰' 화면용이며 식당명·별점·
     * 태그·사진·인증 여부를 포함한다.
     *
     * <p>사진은 tags와의 MultipleBagFetch를 피해 별도 쿼리로 조립한다(getPlaceReviews와 동일 패턴).
     *
     * @param userId 회원 id
     * @return 내 리뷰 목록
     */
    @Transactional(readOnly = true)
    public MyReviewsResponse getMyReviews(Long userId) {
        Map<Long, List<String>> photosByReview = reviewPhotoRepository.findByUserFlattened(userId).stream()
                .collect(Collectors.groupingBy(
                        ReviewPhotoRepository.ReviewPhotoRow::getReviewId,
                        LinkedHashMap::new,
                        Collectors.mapping(ReviewPhotoRepository.ReviewPhotoRow::getImageUrl, Collectors.toList())));
        List<MyReviewsResponse.Item> items = reviewRepository.findAllByUserWithPlaceAndTags(userId).stream()
                .map(r -> new MyReviewsResponse.Item(r.getId(), r.getPlace().getId(), r.getPlace().getName(),
                        r.getVisitedAt(), r.getContent(), r.getTasteRating(), r.getSoloFriendlyRating(),
                        r.getTags().stream().map(t -> t.getTag()).toList(),
                        photosByReview.getOrDefault(r.getId(), List.of()),
                        r.getCheckIn() != null, r.getCreatedAt()))
                .toList();
        return new MyReviewsResponse(items);
    }

    /** 현재 시각을 KST로 반환한다(주입된 Clock 기준). */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
