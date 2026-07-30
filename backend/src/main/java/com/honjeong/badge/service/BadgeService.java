package com.honjeong.badge.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.badge.domain.BadgeCatalog;
import com.honjeong.badge.domain.BadgeMetric;
import com.honjeong.badge.dto.BadgeStatusResponse;
import com.honjeong.badge.repository.UserBadgeRepository;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.review.repository.ReviewRepository;

/**
 * 뱃지 지급 판정(재계산 방식)·조회. 카운트가 임계 달성+미보유면 저장, 획득 시 인앱 알림. 2. 사용: 도메인 액션(체크인/리뷰/즐겨찾기/메이트/같이먹기)이 checkAndAward 호출, BadgeController가 getMyBadges 호출.
 */
@Service
public class BadgeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserBadgeRepository badgeRepository;
    private final CheckInRepository checkInRepository;
    private final ReviewRepository reviewRepository;
    private final MateRepository mateRepository;
    private final FavoriteRepository favoriteRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public BadgeService(UserBadgeRepository badgeRepository, CheckInRepository checkInRepository,
            ReviewRepository reviewRepository, MateRepository mateRepository,
            FavoriteRepository favoriteRepository, NotificationService notificationService, Clock clock) {
        this.badgeRepository = badgeRepository;
        this.checkInRepository = checkInRepository;
        this.reviewRepository = reviewRepository;
        this.mateRepository = mateRepository;
        this.favoriteRepository = favoriteRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * 획득 판정·지급. 카운트를 다시 세서(재계산) 임계 달성 + 미보유 뱃지를 저장한다.
     * notify=true면 새로 지급한 뱃지마다 BADGE_EARNED 알림. 이미 보유는 스킵(멱등),
     * 동시 지급 경합은 DB 네이티브 ON CONFLICT DO NOTHING으로 예외 없이 흡수한다
     * (예외 기반 dedup은 실PostgreSQL에서 트랜잭션을 중단시켜 후속 문장까지 실패시키므로 금지).
     */
    @Transactional
    public void checkAndAward(long userId, boolean notify) {
        Set<String> owned = new HashSet<>(badgeRepository.findKeysByUserId(userId));
        Map<BadgeMetric, Long> counts = new EnumMap<>(BadgeMetric.class);
        for (BadgeCatalog b : BadgeCatalog.values()) {
            if (owned.contains(b.key())) {
                continue;
            }
            long cur = counts.computeIfAbsent(b.metric(), m -> countOf(m, userId));
            if (cur >= b.threshold()) {
                int inserted = badgeRepository.insertIfAbsent(userId, b.key(), now());
                if (inserted == 1) {
                    owned.add(b.key());
                    if (notify) {
                        notificationService.publish(userId, NotificationType.BADGE_EARNED, null);
                    }
                }
            }
        }
    }

    /**
     * 내 뱃지 현황(10종 전부, 획득 플래그+시각). 조회 시 밀린 지급을 조용히 보정한다
     * (소급·일관성 — 무알림). 화면이 항상 실제 카운트와 일치하도록 보장.
     */
    @Transactional
    public List<BadgeStatusResponse> getMyBadges(long userId) {
        checkAndAward(userId, false);
        Map<String, LocalDateTime> earned = new HashMap<>();
        badgeRepository.findByUserId(userId).forEach(b -> earned.put(b.getBadgeKey(), b.getEarnedAt()));
        List<BadgeStatusResponse> out = new ArrayList<>();
        for (BadgeCatalog b : BadgeCatalog.values()) {
            out.add(new BadgeStatusResponse(b.key(), earned.containsKey(b.key()), earned.get(b.key())));
        }
        return out;
    }

    private long countOf(BadgeMetric metric, long userId) {
        return switch (metric) {
            case SOLO_CHECKIN -> checkInRepository.countSoloCompletedByUser(userId);
            case REVIEW -> reviewRepository.countSoloAuthenticatedByUser(userId);
            case TOGETHER -> checkInRepository.countTogetherByUser(userId);
            case MATE -> mateRepository.countByUser_Id(userId);
            case FAVORITE -> favoriteRepository.countDistinctPlaceByUserId(userId);
        };
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
