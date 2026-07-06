package com.honjeong.meal.controller;

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

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestListItemResponse;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.service.MealRequestService;

import jakarta.validation.Valid;

/**
 * 같이먹기 신청(생성·목록·수락·거절) 컨트롤러.
 *
 * <p>기본 경로: /api/meal-requests
 *
 * <p>[기존 주석] 같이먹기 신청 REST 컨트롤러(/api/meal-requests). 얇게 유지 — {@code @CurrentUserId}·{@code @Valid}·DTO 변환만 하고
 * 검증·매핑은 {@link MealRequestService}에 위임한다.
 *
 * <p><b>인가:</b> 모든 경로가 정식 USER 전용이다. SecurityConfig의 {@code anyRequest().hasRole("USER")} 기본 규칙이
 * 커버하므로 별도 매처가 필요 없다(토큰 없으면 401, 온보딩 토큰이면 403).
 */
@RestController
@RequestMapping("/api/meal-requests")
public class MealRequestController {

    private final MealRequestService mealRequestService;

    public MealRequestController(MealRequestService mealRequestService) {
        this.mealRequestService = mealRequestService;
    }

    /**
     * 1. API 주소: POST /api/meal-requests
     * 2. 사용 화면: 같이먹기 신청(MealRequestScreen) — 같은 식당 혼밥러에게 신청 보내기
     * 3. Request: MealRequestCreateRequest(바디) — toCheckInId(대상 혼밥러의 체크인 id, 필수)·message(인사 한마디, 선택·최대 200자) / 인증 사용자(@CurrentUserId)
     * 4. Response: MealRequestResponse — 신청 id, 대상 체크인 id, 인사말, 상태(PENDING) (201 Created)
     *
     * <p>[기존 주석] 같이먹기 신청.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MealRequestResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody MealRequestCreateRequest request) {
        return ApiResponse.success(mealRequestService.create(userId, request));
    }

    /**
     * 1. API 주소: GET /api/meal-requests?role={received|sent}&status={PENDING|ACCEPTED|DECLINED}
     * 2. 사용 화면: 같이먹기 피드(TogetherFeedScreen) — 받은/보낸 탭 목록, 받은 신청(ReceivedRequestsScreen) — 받은 목록, 더보기(MoreScreen) — 받은 PENDING 건수 표시
     * 3. Request: role(쿼리, 기본 received) — received(받은)|sent(보낸), status(쿼리, 선택) — 상태 필터 / 인증 사용자(@CurrentUserId)
     * 4. Response: List&lt;MealRequestListItemResponse&gt; — 신청 id, 신청자/수신자(userId·닉네임), 장소 id·이름, 인사말, 상태, 신청 시각 (createdAt 내림차순)
     *
     * <p>[기존 주석] 받은/보낸 신청 목록. role 기본 received, status 선택 필터.
     */
    @GetMapping
    public ApiResponse<List<MealRequestListItemResponse>> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "received") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(mealRequestService.getMealRequests(userId, role, status));
    }

    /**
     * 1. API 주소: PATCH /api/meal-requests/{id}/accept
     * 2. 사용 화면: 같이먹기 피드(TogetherFeedScreen)·받은 신청(ReceivedRequestsScreen) — 받은 신청 수락 버튼
     * 3. Request: id(경로) — 신청 id / 인증 사용자(@CurrentUserId, 수신자여야 함)
     * 4. Response: MealRequestStatusResponse — 신청 id, 전이된 상태(ACCEPTED), 응답 시각
     *
     * <p>[기존 주석] 신청 수락.
     */
    @PatchMapping("/{id}/accept")
    public ApiResponse<MealRequestStatusResponse> accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.accept(userId, id));
    }

    /**
     * 1. API 주소: PATCH /api/meal-requests/{id}/decline
     * 2. 사용 화면: 같이먹기 피드(TogetherFeedScreen)·받은 신청(ReceivedRequestsScreen) — 받은 신청 거절 버튼
     * 3. Request: id(경로) — 신청 id / 인증 사용자(@CurrentUserId, 수신자여야 함)
     * 4. Response: MealRequestStatusResponse — 신청 id, 전이된 상태(DECLINED), 응답 시각
     *
     * <p>[기존 주석] 신청 거절.
     */
    @PatchMapping("/{id}/decline")
    public ApiResponse<MealRequestStatusResponse> decline(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.decline(userId, id));
    }
}
