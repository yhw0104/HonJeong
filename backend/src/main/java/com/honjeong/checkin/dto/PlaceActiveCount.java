package com.honjeong.checkin.dto;

/**
 * JPQL 생성자 표현식용 DTO. 특정 장소 ID의 현재 ACTIVE 혼밥러 수를 담는다.
 *
 * @param placeId     장소 PK
 * @param activeCount ACTIVE 상태 체크인 수
 */
public record PlaceActiveCount(Long placeId, long activeCount) {
}
