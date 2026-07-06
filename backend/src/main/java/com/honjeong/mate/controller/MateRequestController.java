package com.honjeong.mate.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.mate.dto.MateRequestCreateRequest;
import com.honjeong.mate.dto.MateRequestListItemResponse;
import com.honjeong.mate.dto.MateRequestResponse;
import com.honjeong.mate.dto.MateRequestStatusResponse;
import com.honjeong.mate.service.MateRequestService;
import jakarta.validation.Valid;

/**
 * 메이트 신청(보내기·목록·수락·거절·취소) 컨트롤러.
 *
 * <p>기본 경로: /api/mate-requests
 */
@RestController
@RequestMapping("/api/mate-requests")
public class MateRequestController {

    private final MateRequestService mateRequestService;

    public MateRequestController(MateRequestService mateRequestService) {
        this.mateRequestService = mateRequestService;
    }

    /**
     * 1. API 주소: POST /api/mate-requests
     * 2. 사용 화면: 메이트 목록(MatesScreen) — 사용자 검색 결과의 신청 버튼 / 메이트 프로필(MateProfileScreen) — 메이트 신청 버튼
     * 3. Request: MateRequestCreateRequest(요청바디) — toUserId(신청 상대 사용자 ID) / 인증 사용자(@CurrentUserId)
     * 4. Response: MateRequestResponse — 생성된 신청 ID, 상대 사용자 ID, 상태(PENDING)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MateRequestResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody MateRequestCreateRequest request) {
        return ApiResponse.success(mateRequestService.create(userId, request));
    }

    /**
     * 1. API 주소: GET /api/mate-requests?role=received|sent&status=PENDING
     * 2. 사용 화면: 메이트 목록(MatesScreen) — 받은 신청(PENDING)·보낸 신청 목록 표시
     * 3. Request: role(쿼리, 기본 received) — received|sent, status(쿼리, 선택) — 상태 필터(PENDING 등) / 인증 사용자(@CurrentUserId)
     * 4. Response: List<MateRequestListItemResponse> — 신청 ID·발신/수신 사용자 요약·상태·생성 시각(최신순)
     */
    @GetMapping
    public ApiResponse<List<MateRequestListItemResponse>> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "received") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(mateRequestService.getMateRequests(userId, role, status));
    }

    /**
     * 1. API 주소: PATCH /api/mate-requests/{id}/accept
     * 2. 사용 화면: 메이트 목록(MatesScreen) — 받은 신청 수락 버튼
     * 3. Request: id(경로) — 신청 ID / 인증 사용자(@CurrentUserId, 수신자만 가능)
     * 4. Response: MateRequestStatusResponse — 신청 ID, 상태(ACCEPTED), 응답 시각
     */
    @PatchMapping("/{id}/accept")
    public ApiResponse<MateRequestStatusResponse> accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.accept(userId, id));
    }

    /**
     * 1. API 주소: PATCH /api/mate-requests/{id}/decline
     * 2. 사용 화면: 메이트 목록(MatesScreen) — 받은 신청 거절 버튼
     * 3. Request: id(경로) — 신청 ID / 인증 사용자(@CurrentUserId, 수신자만 가능)
     * 4. Response: MateRequestStatusResponse — 신청 ID, 상태(DECLINED), 응답 시각
     */
    @PatchMapping("/{id}/decline")
    public ApiResponse<MateRequestStatusResponse> decline(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.decline(userId, id));
    }

    /**
     * 1. API 주소: PATCH /api/mate-requests/{id}/cancel
     * 2. 사용 화면: (앱 미사용 — 프론트에 API 함수(features/mate/api.ts cancelMateRequest)만 정의돼 있고 호출 화면 없음)
     * 3. Request: id(경로) — 신청 ID / 인증 사용자(@CurrentUserId, 발신자만 가능)
     * 4. Response: MateRequestStatusResponse — 신청 ID, 상태(CANCELED), 응답 시각
     */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<MateRequestStatusResponse> cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.cancel(userId, id));
    }
}
