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

@RestController
@RequestMapping("/api/mate-requests")
public class MateRequestController {

    private final MateRequestService mateRequestService;

    public MateRequestController(MateRequestService mateRequestService) {
        this.mateRequestService = mateRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MateRequestResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody MateRequestCreateRequest request) {
        return ApiResponse.success(mateRequestService.create(userId, request));
    }

    @GetMapping
    public ApiResponse<List<MateRequestListItemResponse>> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "received") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(mateRequestService.getMateRequests(userId, role, status));
    }

    @PatchMapping("/{id}/accept")
    public ApiResponse<MateRequestStatusResponse> accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.accept(userId, id));
    }

    @PatchMapping("/{id}/decline")
    public ApiResponse<MateRequestStatusResponse> decline(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.decline(userId, id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<MateRequestStatusResponse> cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateRequestService.cancel(userId, id));
    }
}
