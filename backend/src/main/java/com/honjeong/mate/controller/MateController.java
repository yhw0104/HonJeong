package com.honjeong.mate.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.mate.dto.MateResponse;
import com.honjeong.mate.service.MateService;

@RestController
@RequestMapping("/api/mates")
public class MateController {

    private final MateService mateService;

    public MateController(MateService mateService) {
        this.mateService = mateService;
    }

    @GetMapping
    public ApiResponse<List<MateResponse>> myMates(@CurrentUserId Long userId) {
        return ApiResponse.success(mateService.getMyMates(userId));
    }

    @DeleteMapping("/{mateUserId}")
    public ApiResponse<SuccessBody> delete(@CurrentUserId Long userId, @PathVariable Long mateUserId) {
        mateService.deleteMate(userId, mateUserId);
        return ApiResponse.success(new SuccessBody(true));
    }

    public record SuccessBody(boolean success) {
    }
}
