package com.honjeong.user.dto;

/**
 * 닉네임 사용 가능 여부 응답 본문. {@code GET /api/users/nickname-check}에서 반환한다.
 *
 * @param nickname  확인한 닉네임(요청 값을 그대로 echo)
 * @param available 사용 가능 여부 — {@code true}면 미사용(사용 가능), {@code false}면 이미 사용 중
 */
public record NicknameCheckResponse(String nickname, boolean available) {
}
