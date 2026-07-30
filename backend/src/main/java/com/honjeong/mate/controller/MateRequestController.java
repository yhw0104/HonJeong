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
     * 메이트 신청을 보낸다(201 Created).
     *
     * <p>사용 화면: 메이트 목록(MatesScreen)의 검색 결과 신청 버튼, 메이트 프로필(MateProfileScreen)의
     * 메이트 신청 버튼.
     *
     * @param userId 인증 사용자 ID(신청자)
     * @param request toUserId(신청 상대 사용자 ID)
     * @return 생성된 신청 ID, 상대 사용자 ID, 상태(PENDING)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MateRequestResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody MateRequestCreateRequest request) {
        return ApiResponse.success(mateRequestService.create(userId, request));
    }

    /**
     * 받은/보낸 메이트 신청 목록을 최신순으로 조회한다.
     *
     * <p>사용 화면: 메이트 목록(MatesScreen)의 받은 신청(PENDING)·보낸 신청 목록.
     *
     * @param userId 인증 사용자 ID
     * @param role received(기본)|sent
     * @param status 상태 필터(선택 — PENDING 등)
     * @return 신청 ID·발신/수신 사용자 요약·상태·생성 시각
     */
    @GetMapping
    public ApiResponse<List<MateRequestListItemResponse>> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "received") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(mateRequestService.getMateRequests(userId, role, status));
    }

    /**
     * 받은 메이트 신청을 수락한다.
     *
     * <p>사용 화면: 메이트 목록(MatesScreen)의 수락 버튼.
     *
     * @param userId 인증 사용자 ID(수신자만 가능)
     * @param id 신청 ID
     * @return 신청 ID, 상태(ACCEPTED), 응답 시각
     */
    @PatchMapping("/{id}/accept")
    public ApiResponse<MateRequestStatusResponse> accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.accept(userId, id));
    }

    /**
     * 받은 메이트 신청을 거절한다.
     *
     * <p>사용 화면: 메이트 목록(MatesScreen)의 거절 버튼.
     *
     * @param userId 인증 사용자 ID(수신자만 가능)
     * @param id 신청 ID
     * @return 신청 ID, 상태(DECLINED), 응답 시각
     */
    @PatchMapping("/{id}/decline")
    public ApiResponse<MateRequestStatusResponse> decline(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.decline(userId, id));
    }

    /**
     * 내가 보낸 메이트 신청을 취소한다.
     *
     * <p>사용 화면: 앱에서는 아직 쓰지 않는다 — 프론트에 API 함수
     * (features/mate/api.ts cancelMateRequest)만 정의돼 있고 호출하는 화면이 없다.
     *
     * @param userId 인증 사용자 ID(발신자만 가능)
     * @param id 신청 ID
     * @return 신청 ID, 상태(CANCELED), 응답 시각
     */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<MateRequestStatusResponse> cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.cancel(userId, id));
    }
}
