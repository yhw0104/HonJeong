package com.honjeong.mate.dto;

import java.time.LocalDateTime;

/**
 * 이 식당과 관계된 내 메이트 1명.
 * seekingNow=지금 이 식당에서 같이 먹을 사람을 모집중(SEEKING)인지 — 신청을 받을 수 있는 상태만 true다
 * (혼자 먹는 중(ACTIVE)·이미 매칭됨(TOGETHER)은 false).
 * soloFriendlyRating·reviewContent는 그 메이트의 이 식당 리뷰(없으면 null, reviewContent=Review.content 전문→프론트 1줄 말줄임).
 * togetherCount=우리가 전체 같이 먹은 횟수(관계 친밀도), visitCount=이 식당 방문수, lastVisitedAt=마지막 방문(방문 없으면 null),
 * profileImageUrl=프로필 사진 URL(없으면 null → 프론트에서 이니셜 폴백).
 */
public record MateAtPlace(long userId, String nickname, boolean seekingNow, Integer soloFriendlyRating,
        String reviewContent, int togetherCount, int visitCount, LocalDateTime lastVisitedAt, String profileImageUrl) {}
