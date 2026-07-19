package com.honjeong.checkin.dto;

import java.util.List;

/**
 * 식당 상세 사회적 증거. totalDiners=누적 혼밥 세션 수(중복 포함=periods 합, distinct 사람 아님), periods=시간대별 세션 수(아침→점심→저녁→밤),
 * peakPeriodKey=붐비는 시간대 key(데이터 적으면 null).
 */
public record PlaceCheckinSummaryResponse(long totalDiners, List<PeriodCount> periods, String peakPeriodKey) {}
