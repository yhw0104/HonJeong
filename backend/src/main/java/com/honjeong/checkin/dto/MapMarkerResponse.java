package com.honjeong.checkin.dto;

/**
 * 지도 마커 1개: 식당 위치 + 현재 ACTIVE·SEEKING(모집중) 혼밥러 수. 둘 다 조건부 SUM 집계 결과라 long이다.
 *
 * @param placeId      식당 id
 * @param name         가게명
 * @param latitude     위도
 * @param longitude    경도
 * @param activeCount  현재 ACTIVE 혼밥러 수
 * @param seekingCount 현재 SEEKING(모집중) 혼밥러 수
 */
public record MapMarkerResponse(Long placeId, String name, double latitude, double longitude,
        long activeCount, long seekingCount) {
}
