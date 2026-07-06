package com.honjeong.mate.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.mate.dto.MateResponse;
import com.honjeong.mate.service.MateService;

/**
 * 혼밥 메이트(친구) 관계 조회·해제 컨트롤러.
 *
 * <p>기본 경로: /api/mates
 */
@RestController
@RequestMapping("/api/mates")
public class MateController {

    private final MateService mateService;

    public MateController(MateService mateService) {
        this.mateService = mateService;
    }

    /**
     * 1. API 주소: GET /api/mates
     * 2. 사용 화면: 메이트 목록(MatesScreen) — 내 메이트 탭 리스트(온라인 상태·통계 포함) 표시
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: List<MateResponse> — 메이트별 닉네임·프로필·식사성향·온라인(체크인) 상태·누적 체크인·함께 먹은 횟수·메이트 시작일
     */
    @GetMapping
    public ApiResponse<List<MateResponse>> myMates(@CurrentUserId Long userId) {
        return ApiResponse.success(mateService.getMyMates(userId));
    }

    /**
     * 1. API 주소: DELETE /api/mates/{mateUserId}
     * 2. 사용 화면: 메이트 프로필(MateProfileScreen) — 메이트 해제 버튼
     * 3. Request: mateUserId(경로) — 해제할 상대 사용자 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: SuccessBody — success(처리 성공 여부)
     */
    @DeleteMapping("/{mateUserId}")
    public ApiResponse<SuccessBody> delete(@CurrentUserId Long userId, @PathVariable Long mateUserId) {
        mateService.deleteMate(userId, mateUserId);
        return ApiResponse.success(new SuccessBody(true));
    }

    /**
     * 메이트 해제 결과 응답 바디.
     *
     * @param success 처리 성공 여부(성공 시 true)
     */
    public record SuccessBody(boolean success) {
    }
}
