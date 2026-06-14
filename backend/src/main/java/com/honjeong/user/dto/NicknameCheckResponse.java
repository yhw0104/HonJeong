package com.honjeong.user.dto;

/** 닉네임 사용 가능 여부 응답. {@code GET /api/users/nickname-check}. */
public record NicknameCheckResponse(String nickname, boolean available) {
}
