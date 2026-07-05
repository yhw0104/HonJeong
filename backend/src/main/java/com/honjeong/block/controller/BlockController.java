package com.honjeong.block.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.honjeong.block.dto.BlockCreateRequest;
import com.honjeong.block.dto.BlockedUserResponse;
import com.honjeong.block.service.BlockService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import jakarta.validation.Valid;

/** 유저 차단 REST(FR-108). 전부 정식 USER 전용(SecurityConfig 기본 규칙). */
@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    /** 차단 생성(+메이트/신청/TOGETHER 자동 정리). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> block(@CurrentUserId Long userId, @Valid @RequestBody BlockCreateRequest request) {
        blockService.block(userId, request.targetUserId());
        return ApiResponse.<Void>success(null); // 반환 데이터 없음 → <Void> 명시(기존 관례)
    }

    /** 내 차단 목록(최신순). */
    @GetMapping
    public ApiResponse<List<BlockedUserResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(blockService.getMyBlocks(userId));
    }

    /** 차단 해제 — DELETE 규약(200 + success:true). */
    @DeleteMapping("/{targetUserId}")
    public ApiResponse<Void> unblock(@CurrentUserId Long userId, @PathVariable Long targetUserId) {
        blockService.unblock(userId, targetUserId);
        return ApiResponse.<Void>success(null);
    }
}
