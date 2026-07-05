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

import com.honjeong.block.repository.BlockRepository;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
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

/** 리뷰 도메인 서비스. 작성(인증 자동연결·태그 검증)과 식당 리뷰 조회를 담당한다. */
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

    public ReviewService(ReviewRepository reviewRepository, CheckInRepository checkInRepository,
            PlaceService placeService, UserRepository userRepository, BlockRepository blockRepository, Clock clock,
            ReviewPhotoRepository reviewPhotoRepository) {
        this.reviewRepository = reviewRepository;
        this.checkInRepository = checkInRepository;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.clock = clock;
        this.reviewPhotoRepository = reviewPhotoRepository;
    }

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

    private static Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

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

    @Transactional(readOnly = true)
    public List<PlacePhotoResponse> getPlacePhotos(Long placeId) {
        return reviewPhotoRepository.findByPlaceFlattened(placeId).stream()
                .map(row -> new PlacePhotoResponse(row.getImageUrl(), row.getReviewId()))
                .toList();
    }

    /**
     * 리뷰를 작성한다. 별점 2종 필수(컨트롤러 @Valid). 태그는 허용 프리셋만(아니면 400).
     * 인증: checkInId 주어지면 내 소유+place 일치 시 연결(타인 소유 403), 없으면 place의 24h 내 최근 체크인 자동 연결.
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
            return ReviewResponse.from(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException e) {   // 동시 작성 경쟁 → 부분 유니크 위반
            throw new BusinessException(ErrorCode.REVIEW_DUPLICATE_CHECKIN);
        }
    }

    /**
     * 리뷰를 수정한다. 본인만(아니면 403), 없으면 404. 별점·본문 갱신 + 태그 전량 교체.
     * place·checkIn·visitedAt은 불변(인증 무결성 유지).
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

    /** 리뷰를 완전 삭제한다(태그 cascade). 본인만(아니면 403), 없으면 404. */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        reviewRepository.delete(review);
    }

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

    private CheckIn resolveCheckIn(Long userId, Long placeId, Long checkInId, LocalDateTime now) {
        if (checkInId != null) {
            CheckIn ci = checkInRepository.findById(checkInId).orElse(null);
            if (ci == null) {
                return null;
            }
            if (!ci.isOwnedBy(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return ci.getPlace().getId().equals(placeId) ? ci : null;   // place 불일치면 일반 리뷰
        }
        LocalDateTime since = now.minusHours(AUTH_WINDOW_HOURS);
        return checkInRepository.findRecentForReview(userId, placeId, since).orElse(null);
    }

    /**
     * 내 방문 타임라인. 체크인 이력(최신순) + 각 체크인의 리뷰(있으면)를 조립하고 요약 통계를 함께 반환한다.
     *
     * @param userId 회원 id
     * @return 방문 타임라인 + 요약 통계
     */
    @Transactional(readOnly = true)
    public DiningHistoryResponse getDiningHistory(Long userId) {
        Map<Long, Review> reviewByCheckIn = reviewRepository.findByUserWithCheckIn(userId).stream()
                .collect(Collectors.toMap(r -> r.getCheckIn().getId(), r -> r, (a, b) -> a)); // 최신 우선(쿼리 DESC)

        List<DiningHistoryResponse.Entry> entries = checkInRepository.findHistoryWithPlaceByUser(userId).stream()
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
                checkInRepository.countByUser_IdAndStatusNot(userId, CheckInStatus.CANCELLED),
                reviewRepository.countByUser_IdAndCheckInIsNotNull(userId),  // 화면 목록(인증 일기만)과 기준 일치
                checkInRepository.countDistinctPlacesByUser(userId),
                checkInRepository.countByUserSince(userId, monthStart));

        return new DiningHistoryResponse(summary, entries);
    }

    /**
     * 내가 쓴 리뷰 전체(인증+일반)를 작성 최신순으로 반환한다. '내가 쓴 리뷰' 화면용.
     * 사진은 tags와의 MultipleBagFetch를 피해 별도 쿼리로 조립한다(getPlaceReviews와 동일 패턴).
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

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
