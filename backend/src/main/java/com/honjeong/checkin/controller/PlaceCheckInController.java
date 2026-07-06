package com.honjeong.checkin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.checkin.dto.CheckInUserResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

/**
 * 식당별 현재 혼밥러 목록 조회 컨트롤러.
 *
 * <p>기본 경로: /api/places
 *
 * <p>[기존 주석] 식당별 혼밥러 목록 엔드포인트(/api/places/{placeId}/check-ins). 경로가 places 네임스페이스라
 * {@link CheckInController}와 분리하고, 로직은 {@link CheckInService}에 위임한다. 인가는 USER 전용(기본 규칙).
 */
@RestController
@RequestMapping("/api/places")
public class PlaceCheckInController {

    private final CheckInService checkInService;

    public PlaceCheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /**
     * 1. API 주소: GET /api/places/{placeId}/check-ins
     * 2. 사용 화면: 식당 상세(RestaurantDetail), 같이먹기 신청(MealRequest) — 같은 식당 혼밥러 목록
     * 3. Request: placeId(경로) — 식당 ID / 인증 사용자(@CurrentUserId, 차단 상호 은닉 기준)
     * 4. Response: List&lt;CheckInUserResponse&gt; — 체크인 ID, 사용자 ID, 닉네임, 시작 시각, 경과 분 목록
     *
     * <p>[기존 주석] 같은 식당 현재 혼밥러 목록(프라이버시: 닉네임·경과만, 차단 상대는 상호 은닉).
     */
    @GetMapping("/{placeId}/check-ins")
    public ApiResponse<List<CheckInUserResponse>> activeDiners(@CurrentUserId Long userId,
            @PathVariable Long placeId) {
        return ApiResponse.success(checkInService.getActiveDiners(userId, placeId));
    }
}
