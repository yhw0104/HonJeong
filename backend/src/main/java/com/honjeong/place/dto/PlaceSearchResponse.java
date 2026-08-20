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
 * @param distanceMeters 요청 좌표로부터의 거리(m). <b>좌표를 주지 않은 검색이면 null</b>이다 —
 *                       0이 아니라 null인 이유는 "0m 거리"와 "거리를 모른다"가 다른 사실이기 때문이다.
 *                       0으로 채우면 앱이 모든 결과를 "0m"로 표시하게 된다.
 */
public record PlaceSearchResponse(Long placeId, String name, String category, String address,
        String roadAddress, double latitude, double longitude, String phone, Long distanceMeters) {

    /**
     * DB 장소 엔티티를 응답 DTO로 변환한다.
     *
     * @param p              DB에서 조회한 장소 엔티티
     * @param distanceMeters 요청 좌표로부터의 거리(m), 좌표를 모르면 null
     * @return 응답 DTO
     */
    public static PlaceSearchResponse from(Place p, Long distanceMeters) {
        return new PlaceSearchResponse(p.getId(), p.getName(), p.getCategory(), p.getAddress(),
                p.getRoadAddress(), p.getLatitude(), p.getLongitude(), p.getPhone(), distanceMeters);
    }
}
