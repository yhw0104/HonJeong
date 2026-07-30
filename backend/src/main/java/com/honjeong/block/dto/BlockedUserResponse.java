package com.honjeong.block.dto;

import java.time.LocalDateTime;
import com.honjeong.block.domain.Block;

/**
 * 내 차단 목록의 한 항목을 나타내는 응답 데이터 (GET /api/blocks 응답 원소)
 *
 * <p>차단 목록 항목 — 차단당한 유저 요약 + 차단 시각.
 *
 * @param userId 차단당한 유저 ID
 * @param nickname 차단당한 유저 닉네임
 * @param profileImageUrl 차단당한 유저 프로필 이미지 URL (없으면 null)
 * @param createdAt 차단 시각
 */
public record BlockedUserResponse(Long userId, String nickname, String profileImageUrl, LocalDateTime createdAt) {

    /** 기능: Block 엔티티 → 차단 목록 응답 DTO 변환 (blocked 유저 요약 + 차단 시각 추출) */
    public static BlockedUserResponse from(Block b) {
        return new BlockedUserResponse(b.getBlocked().getId(), b.getBlocked().getNickname(),
                b.getBlocked().getProfileImageUrl(), b.getCreatedAt());
    }
}
