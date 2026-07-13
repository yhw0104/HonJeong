package com.honjeong.place.dto;

import java.util.List;

/**
 * 주변 식당 1건 응답. distanceMeters는 요청 위치에서의 Haversine 거리(반올림), activeCount는 현재 ACTIVE 혼밥러 수,
 * seekingCount는 현재 SEEKING(모집중) 수. photoUrls는 이 식당의 대표 사진(리뷰 사진 최신순 최대 N장; 없으면 빈 배열).
 *
 * @param placeId       장소 PK
 * @param name          가게명
 * @param category      카테고리(없으면 null)
 * @param roadAddress   도로명 주소(없으면 null)
 * @param latitude      위도
 * @param longitude     경도
 * @param distanceMeters 요청 좌표에서 이 장소까지의 거리(m, 반올림)
 * @param activeCount   현재 ACTIVE 혼밥러 수(없으면 0)
 * @param seekingCount  현재 SEEKING(모집중) 수(없으면 0)
 * @param photoUrls     대표 사진 URL 목록(리뷰 사진 최신순 최대 N장; 없으면 빈 배열)
 */
public record PlaceNearbyResponse(
        Long placeId,
        String name,
        String category,
        String roadAddress,
        double latitude,
        double longitude,
        long distanceMeters,
        long activeCount,
        long seekingCount,
        List<String> photoUrls) {
}
