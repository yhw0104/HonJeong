package com.honjeong.place.dto;

import com.honjeong.place.domain.Place;

/**
 * 식당 상세(기본 정보) 응답 DTO. {@code GET /api/places/{placeId}}에서 단건 조회 결과로 반환한다.
 *
 * <p>검색 결과({@link PlaceSearchResponse})와 같은 기본 식별·위치 정보에 더해, 상세 화면이 활용하는
 * 영업 상태({@code businessStatus})까지 노출한다. 메뉴·편의시설·리뷰 등은 P2 범위라 포함하지 않는다.
 *
 * @param placeId        우리 DB의 장소 PK(체크인 등 후속 요청에 사용)
 * @param name           가게명
 * @param category       카테고리(nullable)
 * @param address        지번 주소(nullable)
 * @param roadAddress    도로명 주소(nullable)
 * @param latitude       위도
 * @param longitude      경도
 * @param phone          전화번호(nullable)
 * @param businessStatus 영업 상태(예: "영업")
 */
public record PlaceDetailResponse(Long placeId, String name, String category, String address,
        String roadAddress, double latitude, double longitude, String phone, String businessStatus) {

    /**
     * DB 장소 엔티티를 상세 응답 DTO로 변환한다.
     *
     * @param p DB에서 조회한 장소 엔티티
     * @return 상세 응답 DTO
     */
    public static PlaceDetailResponse from(Place p) {
        return new PlaceDetailResponse(p.getId(), p.getName(), p.getCategory(), p.getAddress(),
                p.getRoadAddress(), p.getLatitude(), p.getLongitude(), p.getPhone(), p.getBusinessStatus());
    }
}
