package com.honjeong.mate.dto;

import java.util.List;

/** 식당 상세 메이트 탭. visitedCount=이 식당에 다녀간 내 메이트 수, mates=hereNow 우선·마지막방문 최신순. */
public record PlaceMatesResponse(int visitedCount, List<MateAtPlace> mates) {}
