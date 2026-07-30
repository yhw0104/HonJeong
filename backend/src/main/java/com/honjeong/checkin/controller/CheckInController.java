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
import com.honjeong.checkin.dto.LeaveMatchRequest;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

import jakarta.validation.Valid;

/**
 * 혼밥 체크인 REST 컨트롤러 — 시작·종료·취소·혼자먹기 전이·매칭 해제·내 체크인·통계·지도 마커.
 *
 * <p>기본 경로: /api/check-ins
 *
 * <p>얇게 유지한다 — {@code @CurrentUserId}·{@code @Valid}·DTO 변환만 하고
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
     * 혼밥 체크인을 시작한다. 같은 장소 멱등 재요청도 201로 통일한다.
     *
     * <p>사용 화면: 홈 지도(MapHome)·식당 상세(RestaurantDetail)의 "혼밥 시작" 버튼.
     *
     * @param userId 인증 사용자 ID
     * @param request placeId(체크인할 장소 ID, 필수)
     * @return 체크인 ID, 장소 ID, 상태, 시작·종료·매칭 시각, 파트너 닉네임
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckInResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success(checkInService.createCheckIn(userId, request));
    }

    /**
     * 체크인을 종료한다.
     *
     * <p>사용 화면: 홈 지도(MapHome)·식당 상세(RestaurantDetail)의 혼밥 종료 확인(usePromptEndCheckIn).
     *
     * @param userId 인증 사용자 ID
     * @param id 종료할 체크인 ID
     * @return 종료(ENDED) 처리된 체크인 정보
     */
    @PatchMapping("/{id}/end")
    public ApiResponse<CheckInResponse> end(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(checkInService.endCheckIn(userId, id));
    }

    /**
     * 짧은 혼밥을 취소한다(오집계 제외). 소유자의 SEEKING·ACTIVE만 취소할 수 있다.
     *
     * <p>사용 화면: 홈 지도(MapHome)·식당 상세(RestaurantDetail)의 짧은 혼밥 종료 시 취소 분기
     * (usePromptEndCheckIn).
     *
     * @param userId 인증 사용자 ID
     * @param id 취소할 체크인 ID
     * @return 취소(CANCELLED) 처리된 체크인 정보
     */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<CheckInResponse> cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(checkInService.cancelCheckIn(userId, id));
    }

    /**
     * 모집중(SEEKING)을 혼밥중(ACTIVE)으로 전이한다 — 매칭 실패/포기 후 혼자 먹기 시작.
     *
     * <p>사용 화면: 홈 지도·식당 상세 — 같이 먹을 사람을 못 구해 "혼자 먹기"를 선택했을 때.
     *
     * @param userId 인증 사용자 ID
     * @param id 전이할 체크인 ID
     * @return ACTIVE(혼밥중)로 전이된 체크인
     */
    @PatchMapping("/{id}/dine-alone")
    public ApiResponse<CheckInResponse> dineAlone(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(checkInService.dineAlone(userId, id));
    }

    /**
     * 같이먹기 매칭을 깨고 내 체크인을 지정 상태로 전이한다. 상대는 항상 SEEKING으로 복귀시키고 알림을
     * 발행한다.
     *
     * <p>사용 화면: 같이먹기 종료 시트 — 상대 노쇼/취소 시 "혼밥 계속/다시 모집/취소" 선택.
     *
     * @param userId 인증 사용자 ID
     * @param id 내 TOGETHER 체크인 ID
     * @param request to — ACTIVE|SEEKING|CANCELLED 중 전이할 내 상태
     * @return 전이된 내 체크인
     */
    @PatchMapping("/{id}/leave-match")
    public ApiResponse<CheckInResponse> leaveMatch(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody LeaveMatchRequest request) {
        return ApiResponse.success(checkInService.leaveMatch(userId, id, request.to()));
    }

    /**
     * 내 현재 체크인(SEEKING/ACTIVE/TOGETHER)을 조회한다.
     *
     * <p>사용 화면: 홈 지도(MapHome)·식당 상세(RestaurantDetail)·같이먹기 피드(TogetherFeed)의
     * 현재 혼밥 상태 표시.
     *
     * @param userId 인증 사용자 ID
     * @return 현재 진행 중 체크인, 없으면 {@code data:null}
     */
    @GetMapping("/me")
    public ApiResponse<CheckInResponse> me(@CurrentUserId Long userId) {
        return ApiResponse.success(checkInService.getMyCurrentCheckIn(userId));
    }

    /**
     * 사회적 증거 통계를 조회한다("오늘 N명 / 현재 N명").
     *
     * <p>사용 화면: 웰컴(Welcome)·홈 지도(MapHome)의 사회적 증거 문구.
     *
     * @return todayCount(오늘 혼밥한 사람 수)·activeCount(현재 혼밥 중 수)·seekingCount(현재 모집중 수)
     */
    @GetMapping("/stats")
    public ApiResponse<CheckInStatsResponse> stats() {
        return ApiResponse.success(checkInService.getStats());
    }

    /**
     * 반경 내 식당별 현재 혼밥러 수 마커를 조회한다.
     *
     * <p>사용 화면: 홈 지도(MapHome)의 마커 렌더링.
     *
     * @param lat 중심 위도(필수)
     * @param lng 중심 경도(필수)
     * @param radius 반경(m, 기본 1000)
     * @return 식당 위치·현재 혼밥러 수 마커 목록(거리순)
     */
    @GetMapping("/map")
    public ApiResponse<List<MapMarkerResponse>> map(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "1000") int radius) {
        return ApiResponse.success(checkInService.getMap(lat, lng, radius));
    }
}
