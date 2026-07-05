package com.honjeong.block.dto;

import java.time.LocalDateTime;
import com.honjeong.block.domain.Block;

/** 차단 목록 항목 — 차단당한 유저 요약 + 차단 시각. */
public record BlockedUserResponse(Long userId, String nickname, String profileImageUrl, LocalDateTime createdAt) {

    public static BlockedUserResponse from(Block b) {
        return new BlockedUserResponse(b.getBlocked().getId(), b.getBlocked().getNickname(),
                b.getBlocked().getProfileImageUrl(), b.getCreatedAt());
    }
}
