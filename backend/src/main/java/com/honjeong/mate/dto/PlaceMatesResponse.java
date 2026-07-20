package com.honjeong.mate.dto;

import java.util.List;

/**
 * 식당 상세 메이트 탭. visitedCount=이 식당에 다녀간 내 메이트 수, mates=hereNow 우선·마지막방문 최신순,
 * savedCount=이 식당을 저장(즐겨찾기)한 전체 사용자 수, savedMateCount=그중 내 메이트 수(사회적 증거).
 */
public record PlaceMatesResponse(int visitedCount, List<MateAtPlace> mates, int savedCount, int savedMateCount) {}
