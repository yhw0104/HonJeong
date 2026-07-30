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
 * 같이먹기 신청 REST 컨트롤러 — 생성·목록·수락·거절·철회.
 *
 * <p>기본 경로: /api/meal-requests
 *
 * <p>얇게 유지한다 — {@code @CurrentUserId}·{@code @Valid}·DTO 변환만 하고 검증·매핑은
 * {@link MealRequestService}에 위임한다.
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
     * 같이먹기 신청을 보낸다(201 Created).
     *
     * <p>사용 화면: 같이먹기 신청(MealRequestScreen) — 같은 식당 혼밥러에게 신청 보내기.
     *
     * @param userId 인증 사용자 ID(신청자)
     * @param request toCheckInId(대상 혼밥러의 체크인 id, 필수)·message(인사 한마디, 선택·최대 200자)
     * @return 신청 id, 대상 체크인 id, 인사말, 상태(PENDING)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MealRequestResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody MealRequestCreateRequest request) {
        return ApiResponse.success(mealRequestService.create(userId, request));
    }

    /**
     * 받은/보낸 신청 목록을 조회한다(createdAt 내림차순).
     *
     * <p>사용 화면: 같이먹기 피드(TogetherFeedScreen)의 받은/보낸 탭, 받은 신청(ReceivedRequestsScreen),
     * 더보기(MoreScreen)의 받은 PENDING 건수 표시.
     *
     * @param userId 인증 사용자 ID
     * @param role received(받은, 기본)|sent(보낸)
     * @param status 상태 필터(선택) — PENDING|ACCEPTED|DECLINED|EXPIRED|WITHDRAWN
     * @return 신청 id, 신청자/수신자(userId·닉네임·사진), 장소 id·이름, 인사말, 상태, 신청 시각
     */
    @GetMapping
    public ApiResponse<List<MealRequestListItemResponse>> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "received") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(mealRequestService.getMealRequests(userId, role, status));
    }

    /**
     * 받은 신청을 수락한다.
     *
     * <p>사용 화면: 같이먹기 피드(TogetherFeedScreen)·받은 신청(ReceivedRequestsScreen)의 수락 버튼.
     *
     * @param userId 인증 사용자 ID(수신자여야 함)
     * @param id 신청 id
     * @return 신청 id, 전이된 상태(ACCEPTED), 응답 시각
     */
    @PatchMapping("/{id}/accept")
    public ApiResponse<MealRequestStatusResponse> accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.accept(userId, id));
    }

    /**
     * 받은 신청을 거절한다.
     *
     * <p>사용 화면: 같이먹기 피드(TogetherFeedScreen)·받은 신청(ReceivedRequestsScreen)의 거절 버튼.
     *
     * @param userId 인증 사용자 ID(수신자여야 함)
     * @param id 신청 id
     * @return 신청 id, 전이된 상태(DECLINED), 응답 시각
     */
    @PatchMapping("/{id}/decline")
    public ApiResponse<MealRequestStatusResponse> decline(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.decline(userId, id));
    }

    /**
     * 신청자가 보낸 PENDING 신청을 스스로 철회한다.
     *
     * <p>사용 화면: 같이먹기 피드·받은 신청 화면의 보낸 신청 탭 — '철회' 버튼.
     *
     * @param userId 인증 사용자 ID(발신자여야 함)
     * @param id 신청 id
     * @return 신청 id, 전이된 상태(WITHDRAWN), 응답 시각
     */
    @PatchMapping("/{id}/withdraw")
    public ApiResponse<MealRequestStatusResponse> withdraw(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.withdraw(userId, id));
    }
}
