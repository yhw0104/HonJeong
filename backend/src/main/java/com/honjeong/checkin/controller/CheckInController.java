package com.honjeong.checkin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

import jakarta.validation.Valid;

/**
 * 혼밥 체크인(시작·종료·취소·내 체크인·통계·지도 마커) 컨트롤러.
 *
 * <p>기본 경로: /api/check-ins
 *
 * <p>[기존 주석] 체크인 REST 컨트롤러(/api/check-ins). 얇게 유지 — {@code @CurrentUserId}·{@code @Valid}·DTO 변환만 하고
 * 검증·집계·매핑은 {@link CheckInService}에 위임한다.
 *
 * <p><b>인가:</b> 모든 경로가 정식 USER 전용이다. SecurityConfig의 {@code anyRequest().hasRole("USER")} 기본 규칙이
 * 커버하므로 별도 매처가 필요 없다(토큰 없으면 401, 온보딩 토큰이면 403).
 */
@RestController
@RequestMapping("/api/check-ins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /**
     * 1. API 주소: POST /api/check-ins
     * 2. 사용 화면: 홈 지도(MapHome), 식당 상세(RestaurantDetail) — "혼밥 시작" 체크인 버튼
     * 3. Request: CheckInRequest(요청바디) — placeId(체크인할 장소 ID, 필수) / 인증 사용자(@CurrentUserId)
     * 4. Response: CheckInResponse — 체크인 ID, 장소 ID, 상태, 시작·종료·매칭 시각, 파트너 닉네임
     *
     * <p>[기존 주석] 혼밥 체크인 시작. 같은 장소 멱등 재요청도 201로 통일한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckInResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success(checkInService.createCheckIn(userId, request));
    }

    /**
     * 1. API 주소: PATCH /api/check-ins/{id}/end
     * 2. 사용 화면: 홈 지도(MapHome), 식당 상세(RestaurantDetail) — 혼밥 종료 확인(usePromptEndCheckIn)
     * 3. Request: id(경로) — 종료할 체크인 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: CheckInResponse — 종료(ENDED) 처리된 체크인 정보
     *
     * <p>[기존 주석] 체크인 종료.
     */
    @PatchMapping("/{id}/end")
    public ApiResponse<CheckInResponse> end(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(checkInService.endCheckIn(userId, id));
    }

    /**
     * 1. API 주소: PATCH /api/check-ins/{id}/cancel
     * 2. 사용 화면: 홈 지도(MapHome), 식당 상세(RestaurantDetail) — 짧은 혼밥 종료 시 취소 분기(usePromptEndCheckIn)
     * 3. Request: id(경로) — 취소할 체크인 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: CheckInResponse — 취소(CANCELLED) 처리된 체크인 정보
     *
     * <p>[기존 주석] 짧은 혼밥 취소(오집계 제외). 소유자의 ACTIVE만.
     */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<CheckInResponse> cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(checkInService.cancelCheckIn(userId, id));
    }

    /**
     * 1. API 주소: GET /api/check-ins/me
     * 2. 사용 화면: 홈 지도(MapHome), 식당 상세(RestaurantDetail), 같이먹기 피드(TogetherFeed) — 현재 혼밥 상태 표시
     * 3. Request: 인증 사용자(@CurrentUserId) 외 없음
     * 4. Response: CheckInResponse — 현재 진행 중 체크인(ACTIVE/TOGETHER), 없으면 data:null
     *
     * <p>[기존 주석] 내 현재 체크인(ACTIVE 또는 TOGETHER, 없으면 data:null).
     */
    @GetMapping("/me")
    public ApiResponse<CheckInResponse> me(@CurrentUserId Long userId) {
        return ApiResponse.success(checkInService.getMyCurrentCheckIn(userId));
    }

    /**
     * 1. API 주소: GET /api/check-ins/stats
     * 2. 사용 화면: 웰컴(Welcome), 홈 지도(MapHome) — "오늘 N명 / 현재 N명" 사회적 증거 문구
     * 3. Request: 없음
     * 4. Response: CheckInStatsResponse — todayCount(오늘 혼밥한 사람 수), activeCount(현재 혼밥 중 수)
     *
     * <p>[기존 주석] 사회적 증거 통계("오늘 N명 / 현재 N명").
     */
    @GetMapping("/stats")
    public ApiResponse<CheckInStatsResponse> stats() {
        return ApiResponse.success(checkInService.getStats());
    }

    /**
     * 1. API 주소: GET /api/check-ins/map
     * 2. 사용 화면: 홈 지도(MapHome) — 반경 내 식당별 혼밥러 수 마커 렌더링
     * 3. Request: lat(쿼리, 필수) — 중심 위도, lng(쿼리, 필수) — 중심 경도, radius(쿼리, 기본 1000) — 반경(m)
     * 4. Response: List&lt;MapMarkerResponse&gt; — 식당 위치·현재 혼밥러 수 마커 목록(거리순)
     *
     * <p>[기존 주석] 반경 내 식당별 현재 혼밥러 수 마커. lat/lng 필수, radius 기본 1000m.
     */
    @GetMapping("/map")
    public ApiResponse<List<MapMarkerResponse>> map(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "1000") int radius) {
        return ApiResponse.success(checkInService.getMap(lat, lng, radius));
    }
}
