package com.honjeong.place.dto;

import com.honjeong.place.domain.Place;

/**
 * 장소 검색 결과 1건의 응답 DTO. Task 6 이후 우리 DB({@link Place}) 기반으로 전환됐다.
 *
 * @param placeId     우리 DB의 장소 PK(체크인 등 후속 요청에 사용)
 * @param name        가게명
 * @param category    카테고리(nullable)
 * @param address     지번 주소(nullable)
 * @param roadAddress 도로명 주소(nullable)
 * @param latitude    위도
 * @param longitude   경도
 * @param phone       전화번호(nullable)
 */
public record PlaceSearchResponse(Long placeId, String name, String category, String address,
        String roadAddress, double latitude, double longitude, String phone) {

    /**
     * DB 장소 엔티티를 응답 DTO로 변환한다.
     *
     * @param p DB에서 조회한 장소 엔티티
     * @return 응답 DTO
     */
    public static PlaceSearchResponse from(Place p) {
        return new PlaceSearchResponse(p.getId(), p.getName(), p.getCategory(), p.getAddress(),
                p.getRoadAddress(), p.getLatitude(), p.getLongitude(), p.getPhone());
    }
}
