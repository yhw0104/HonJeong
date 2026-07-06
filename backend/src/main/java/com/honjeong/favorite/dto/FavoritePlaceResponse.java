package com.honjeong.favorite.dto;

/**
 * 그룹 상세 안에서 즐겨찾기된 장소 1건을 나타내는 응답 요소 (FavoriteGroupDetailResponse.places).
 *
 * @param placeId     장소 ID
 * @param name        장소(식당) 이름
 * @param category    업종 카테고리
 * @param address     지번 주소
 * @param roadAddress 도로명 주소
 * @param latitude    위도
 * @param longitude   경도
 * @param visited     방문 여부 (해당 장소에 사용자의 체크인 이력이 있으면 true)
 */
public record FavoritePlaceResponse(
        Long placeId, String name, String category, String address, String roadAddress,
        double latitude, double longitude, boolean visited) {
}
