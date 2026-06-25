package com.honjeong.review.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.review.domain.Review;
import com.honjeong.review.domain.SoloFriendlyTags;
import com.honjeong.review.dto.DiningHistoryResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.dto.PlaceReviewSummaryResponse;
import com.honjeong.review.dto.ReviewCreateRequest;
import com.honjeong.review.dto.ReviewResponse;
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
    private final Clock clock;

    public ReviewService(ReviewRepository reviewRepository, CheckInRepository checkInRepository,
            PlaceService placeService, UserRepository userRepository, Clock clock) {
        this.reviewRepository = reviewRepository;
        this.checkInRepository = checkInRepository;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.clock = clock;
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
    public List<PlaceReviewResponse> getPlaceReviews(Long placeId) {
        return reviewRepository.findByPlaceWithUserAndTags(placeId).stream()
                .map(PlaceReviewResponse::from)
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
        LocalDateTime visitedAt = linked != null ? linked.getStartedAt() : now;

        User userRef = userRepository.getReferenceById(userId);
        Review review = Review.create(userRef, linked, place, visitedAt,
                req.tasteRating(), req.soloFriendlyRating(), req.content());
        if (req.tags() != null) {
            req.tags().forEach(tag -> review.addTag(place, tag));
        }
        return ReviewResponse.from(reviewRepository.save(review));
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
                                    r.getTags().stream().map(t -> t.getTag()).toList());
                    return new DiningHistoryResponse.Entry(c.getId(), c.getPlace().getId(),
                            c.getPlace().getName(), c.getStartedAt(), c.getStatus().name(), brief);
                })
                .toList();

        LocalDateTime monthStart = LocalDate.ofInstant(clock.instant(), KST).withDayOfMonth(1).atStartOfDay();
        DiningHistoryResponse.Summary summary = new DiningHistoryResponse.Summary(
                checkInRepository.countByUser_Id(userId),
                reviewRepository.countByUser_Id(userId),
                checkInRepository.countDistinctPlacesByUser(userId),
                checkInRepository.countByUserSince(userId, monthStart));

        return new DiningHistoryResponse(summary, entries);
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
