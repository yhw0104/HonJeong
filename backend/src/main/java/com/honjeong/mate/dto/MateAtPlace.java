package com.honjeong.mate.dto;

import java.time.LocalDateTime;

/**
 * 이 식당과 관계된 내 메이트 1명.
 * soloFriendlyRating·reviewContent는 그 메이트의 이 식당 리뷰(없으면 null, reviewContent=Review.content 전문→프론트 1줄 말줄임).
 * togetherCount=우리가 전체 같이 먹은 횟수(관계 친밀도), visitCount=이 식당 방문수, lastVisitedAt=마지막 방문(방문 없으면 null).
 */
public record MateAtPlace(long userId, String nickname, boolean hereNow, Integer soloFriendlyRating,
        String reviewContent, int togetherCount, int visitCount, LocalDateTime lastVisitedAt) {}
