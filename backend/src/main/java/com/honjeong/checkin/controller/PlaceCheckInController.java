package com.honjeong.checkin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.checkin.dto.CheckInUserResponse;
import com.honjeong.checkin.dto.PlaceCheckinSummaryResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

/**
 * 식당별 현재 모집중(SEEKING) 목록과 체크인 요약 조회 컨트롤러.
 *
 * <p>기본 경로: /api/places
 *
 * <p>경로가 places 네임스페이스라 {@link CheckInController}와 분리하고, 로직은 {@link CheckInService}에
 * 위임한다.
 *
 * <p><b>인가:</b> USER 전용(기본 규칙).
 */
@RestController
@RequestMapping("/api/places")
public class PlaceCheckInController {

    private final CheckInService checkInService;

    public PlaceCheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /**
     * 같은 식당의 현재 모집중(SEEKING) 목록을 조회한다 — 같이먹기 신청 대상.
     *
     * <p>사용 화면: 식당 상세(RestaurantDetail)·같이먹기 신청(MealRequest)의 매칭 대상 목록.
     *
     * <p>프라이버시상 닉네임·사진·시작시각·경과만 노출하고, 차단 상대는 상호 은닉한다.
     *
     * @param userId 인증 사용자 ID(차단 상호 은닉 기준)
     * @param placeId 식당 ID
     * @return 체크인 ID, 사용자 ID, 닉네임, 프로필 사진, 시작 시각, 경과 분 목록
     */
    @GetMapping("/{placeId}/check-ins")
    public ApiResponse<List<CheckInUserResponse>> seekers(@CurrentUserId Long userId,
            @PathVariable Long placeId) {
        return ApiResponse.success(checkInService.getSeekers(userId, placeId));
    }

    /**
     * 누적 혼밥러 수와 붐비는 시간대를 조회한다(사회적 증거).
     *
     * <p>사용 화면: 식당 상세(RestaurantDetail).
     *
     * @param placeId 식당 ID
     * @return totalDiners(누적 혼밥 세션 수)·periods(시간대별 세션 수)·peakPeriodKey(붐비는 시간대, 없으면 null)
     */
    @GetMapping("/{placeId}/checkin-summary")
    public ApiResponse<PlaceCheckinSummaryResponse> checkinSummary(@PathVariable Long placeId) {
        return ApiResponse.success(checkInService.getCheckinSummary(placeId));
    }
}
