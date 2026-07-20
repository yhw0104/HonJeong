package com.honjeong.mate.dto;

/**
 * 이 식당을 즐겨찾기(저장)한 내 메이트 1명 — 식당 상세 메이트 탭의 아바타 스택 표시용.
 * profileImageUrl=프로필 사진 URL(없으면 null → 프론트에서 이니셜 폴백).
 */
public record SavedMate(long userId, String nickname, String profileImageUrl) {}
