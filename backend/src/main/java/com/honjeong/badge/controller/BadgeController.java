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
 * 1. 기능: 내 뱃지 현황 조회
 * 2. 사용 화면: 뱃지 화면(ChallengeBadges)·마이페이지(MyProfile)·더보기(More)
 */
@RestController
@RequestMapping("/api/users")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /**
     * 1. API 주소: GET /api/users/me/badges
     * 2. 사용 화면: ChallengeBadges·MyProfile·More
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: List&lt;BadgeStatusResponse&gt; — 10종 전부(key, earned, earnedAt)
     */
    @GetMapping("/me/badges")
    public ApiResponse<List<BadgeStatusResponse>> getMyBadges(@CurrentUserId Long userId) {
        return ApiResponse.success(badgeService.getMyBadges(userId));
    }
}
