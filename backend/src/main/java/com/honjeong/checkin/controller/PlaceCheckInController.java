package com.honjeong.checkin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.checkin.dto.CheckInUserResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.common.ApiResponse;

/**
 * 식당별 혼밥러 목록 엔드포인트(/api/places/{placeId}/check-ins). 경로가 places 네임스페이스라
 * {@link CheckInController}와 분리하고, 로직은 {@link CheckInService}에 위임한다. 인가는 USER 전용(기본 규칙).
 */
@RestController
@RequestMapping("/api/places")
public class PlaceCheckInController {

    private final CheckInService checkInService;

    public PlaceCheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /** 같은 식당 현재 혼밥러 목록(프라이버시: 닉네임·경과만). */
    @GetMapping("/{placeId}/check-ins")
    public ApiResponse<List<CheckInUserResponse>> activeDiners(@PathVariable Long placeId) {
        return ApiResponse.success(checkInService.getActiveDiners(placeId));
    }
}
