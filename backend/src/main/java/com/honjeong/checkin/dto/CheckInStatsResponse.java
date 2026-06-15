package com.honjeong.checkin.dto;

/**
 * 사회적 증거 통계. todayCount=오늘(KST 자정 기준) 체크인한 distinct 사용자 수, activeCount=현재 ACTIVE 수.
 *
 * @param todayCount  오늘 혼밥한 사람 수(중복 제거)
 * @param activeCount 현재 혼밥 중인 수
 */
public record CheckInStatsResponse(long todayCount, long activeCount) {
}
