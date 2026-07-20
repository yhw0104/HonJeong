package com.honjeong.mate.dto;

import java.util.List;

/**
 * 식당 상세 메이트 탭. visitedCount=이 식당에 다녀간 내 메이트 수, mates=hereNow 우선·마지막방문 최신순,
 * savedCount=이 식당을 저장(즐겨찾기)한 전체 사용자 수, savedMateCount=그중 내 메이트 수(=savedMates.size()),
 * savedMates=저장한 내 메이트 목록(아바타 스택용, userId 오름차순).
 */
public record PlaceMatesResponse(int visitedCount, List<MateAtPlace> mates, int savedCount, int savedMateCount,
        List<SavedMate> savedMates) {}
