package com.honjeong.badge.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.honjeong.badge.dto.BadgeStatusResponse;
import com.honjeong.badge.service.BadgeService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

/**
 * 내 뱃지 현황 조회.
 *
 * <p>사용처: 뱃지 화면(ChallengeBadges)·마이페이지(MyProfile)·더보기(More).
 */
@RestController
@RequestMapping("/api/users")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /**
     * 내 뱃지 현황을 조회한다 — 10종 전부를 획득 여부와 함께 반환한다.
     *
     * <p>사용 화면: 혼밥 뱃지(ChallengeBadges)·마이페이지(MyProfile)·더보기(More).
     *
     * @param userId 인증 사용자 ID
     * @return 뱃지 10종의 key·earned·earnedAt
     */
    @GetMapping("/me/badges")
    public ApiResponse<List<BadgeStatusResponse>> getMyBadges(@CurrentUserId Long userId) {
        return ApiResponse.success(badgeService.getMyBadges(userId));
    }
}
