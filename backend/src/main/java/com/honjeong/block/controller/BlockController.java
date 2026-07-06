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

/**
 * 유저 차단(차단/해제/목록) 컨트롤러.
 *
 * <p>기본 경로: /api/blocks
 *
 * <p>[기존 주석] 유저 차단 REST(FR-108). 전부 정식 USER 전용(SecurityConfig 기본 규칙).
 */
@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    /**
     * 1. API 주소: POST /api/blocks
     * 2. 사용 화면: 메이트 프로필(MateProfile) — 케밥 메뉴 '차단' / 신고하기(ReportForm) — USER 신고 접수 후 차단 이어서 제안
     * 3. Request: BlockCreateRequest(targetUserId — 차단할 유저 ID) / 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void) — 201 Created
     *
     * <p>[기존 주석] 차단 생성(+메이트/신청/TOGETHER 자동 정리).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> block(@CurrentUserId Long userId, @Valid @RequestBody BlockCreateRequest request) {
        blockService.block(userId, request.targetUserId());
        return ApiResponse.<Void>success(null); // 반환 데이터 없음 → <Void> 명시(기존 관례)
    }

    /**
     * 1. API 주소: GET /api/blocks
     * 2. 사용 화면: 차단/신고 관리(BlockReport) — '차단 목록' 탭
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: List&lt;BlockedUserResponse&gt; — 차단당한 유저 요약(ID·닉네임·프로필 이미지)과 차단 시각, 최신순
     *
     * <p>[기존 주석] 내 차단 목록(최신순).
     */
    @GetMapping
    public ApiResponse<List<BlockedUserResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(blockService.getMyBlocks(userId));
    }

    /**
     * 1. API 주소: DELETE /api/blocks/{targetUserId}
     * 2. 사용 화면: 차단/신고 관리(BlockReport) — 차단 목록 항목의 '차단 해제' 버튼
     * 3. Request: targetUserId(경로) — 차단 해제할 유저 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void)
     *
     * <p>[기존 주석] 차단 해제 — DELETE 규약(200 + success:true).
     */
    @DeleteMapping("/{targetUserId}")
    public ApiResponse<Void> unblock(@CurrentUserId Long userId, @PathVariable Long targetUserId) {
        blockService.unblock(userId, targetUserId);
        return ApiResponse.<Void>success(null);
    }
}
