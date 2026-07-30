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
 * <p>유저 차단 REST(FR-108). 전부 정식 USER 전용(SecurityConfig 기본 규칙).
 */
@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    /**
     * 사용자를 차단한다(201 Created). 메이트 관계·대기 신청·TOGETHER 매칭도 함께 자동 정리한다.
     *
     * <p>사용 화면: 메이트 프로필(MateProfile)의 케밥 메뉴 '차단', 신고하기(ReportForm)에서 USER 신고
     * 접수 후 이어지는 차단 제안.
     *
     * @param userId 인증 사용자 ID
     * @param request targetUserId — 차단할 유저 ID
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> block(@CurrentUserId Long userId, @Valid @RequestBody BlockCreateRequest request) {
        blockService.block(userId, request.targetUserId());
        return ApiResponse.<Void>success(null); // 반환 데이터 없음 → <Void> 명시(기존 관례)
    }

    /**
     * 내 차단 목록을 최신순으로 조회한다.
     *
     * <p>사용 화면: 차단/신고 관리(BlockReport)의 '차단 목록' 탭.
     *
     * @param userId 인증 사용자 ID
     * @return 차단한 유저 요약(ID·닉네임·프로필 이미지)과 차단 시각
     */
    @GetMapping
    public ApiResponse<List<BlockedUserResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(blockService.getMyBlocks(userId));
    }

    /**
     * 차단을 해제한다 — DELETE 규약대로 200 + {@code success:true}를 반환한다.
     *
     * <p>사용 화면: 차단/신고 관리(BlockReport)의 차단 목록 항목 '차단 해제' 버튼.
     *
     * @param userId 인증 사용자 ID
     * @param targetUserId 차단 해제할 유저 ID
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @DeleteMapping("/{targetUserId}")
    public ApiResponse<Void> unblock(@CurrentUserId Long userId, @PathVariable Long targetUserId) {
        blockService.unblock(userId, targetUserId);
        return ApiResponse.<Void>success(null);
    }
}
