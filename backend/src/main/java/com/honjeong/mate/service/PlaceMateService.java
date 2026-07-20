package com.honjeong.mate.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.dto.MateAtPlace;
import com.honjeong.mate.dto.PlaceMatesResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.review.domain.Review;
import com.honjeong.review.repository.ReviewRepository;

/**
 * 1. 기능: 식당 상세 메이트 탭 — 내 메이트 중 이 식당 다녀갔거나 지금 있는 사람을 방문·리뷰·같이먹음 데이터와 조립(읽기 전용)
 * 2. 사용 Controller: (연동 예정) PlaceController — 식당상세 메이트 탭
 */
@Service
public class PlaceMateService {

    private final MateRepository mateRepository;
    private final CheckInRepository checkInRepository;
    private final ReviewRepository reviewRepository;

    public PlaceMateService(MateRepository mateRepository, CheckInRepository checkInRepository,
            ReviewRepository reviewRepository) {
        this.mateRepository = mateRepository;
        this.checkInRepository = checkInRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * 기능: 이 식당에 다녀갔거나(방문 이력) 지금 있는(hereNow) 내 메이트 목록을 조립한다.
     * 후보 = 방문(aggregate) ∪ 현재체크인(hereNow). 정렬 = hereNow 우선 → lastVisitedAt 최신(null 마지막).
     * Request: viewerId — 조회하는 사용자(나) ID, placeId — 식당 ID
     * Response: PlaceMatesResponse — visitedCount(방문>0 메이트 수) + 정렬된 메이트 목록
     */
    @Transactional(readOnly = true)
    public PlaceMatesResponse getMatesAtPlace(Long viewerId, Long placeId) {
        List<Mate> mates = mateRepository.findMatesWithUserByUserId(viewerId);
        if (mates.isEmpty()) {
            return new PlaceMatesResponse(0, List.of());
        }

        Map<Long, String> nicknameById = new LinkedHashMap<>();
        for (Mate m : mates) {
            nicknameById.put(m.getMateUser().getId(), m.getMateUser().getNickname());
        }
        List<Long> mateIds = new ArrayList<>(nicknameById.keySet());

        Map<Long, CheckInRepository.MateVisitRow> visitByUser = checkInRepository
                .aggregateMateVisitsAtPlace(placeId, mateIds).stream()
                .collect(Collectors.toMap(CheckInRepository.MateVisitRow::getUserId, r -> r));
        Set<Long> hereNow = new HashSet<>(checkInRepository.findMateIdsHereNow(placeId, mateIds));

        Map<Long, Review> latestReviewByUser = new LinkedHashMap<>();
        for (Review r : reviewRepository.findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(placeId, mateIds)) {
            latestReviewByUser.putIfAbsent(r.getUser().getId(), r); // 최신순이라 첫 건이 최신
        }

        Map<Long, Integer> togetherByPartner = checkInRepository.countTogetherPairsForUser(viewerId).stream()
                .collect(Collectors.toMap(CheckInRepository.TogetherPairRow::getPartnerId,
                        p -> (int) p.getCnt()));

        // 후보 = 방문했거나(visitByUser) 지금 여기 있는(hereNow) 메이트
        Set<Long> candidateIds = new LinkedHashSet<>(visitByUser.keySet());
        candidateIds.addAll(hereNow);

        List<MateAtPlace> list = new ArrayList<>();
        int visitedCount = 0;
        for (Long uid : candidateIds) {
            CheckInRepository.MateVisitRow v = visitByUser.get(uid);
            int visitCount = v != null ? (int) v.getVisitCount() : 0;
            if (visitCount > 0) {
                visitedCount++;
            }
            Review rv = latestReviewByUser.get(uid);
            list.add(new MateAtPlace(
                    uid,
                    nicknameById.get(uid),
                    hereNow.contains(uid),
                    rv != null ? rv.getSoloFriendlyRating() : null,
                    rv != null ? rv.getContent() : null,
                    togetherByPartner.getOrDefault(uid, 0),
                    visitCount,
                    v != null ? v.getLastVisitedAt() : null));
        }
        // 정렬: hereNow 우선 → lastVisitedAt 최신(null 마지막) → userId(결정적 tie-break)
        list.sort(Comparator
                .comparing(MateAtPlace::hereNow, Comparator.reverseOrder())
                .thenComparing(MateAtPlace::lastVisitedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MateAtPlace::userId));
        return new PlaceMatesResponse(visitedCount, list);
    }
}
