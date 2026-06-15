package com.honjeong.place.service;

/**
 * 장소 캐시 upsert 입력. 체크인/리뷰 시 클라이언트가 선택한 가게 정보(검색 결과에서 받은 값)를 그대로 담아
 * {@link PlaceService#findOrCreateByExternalId} 에 전달한다. {@code external_id}가 캐싱 키다.
 *
 * @param externalId 카카오 place id(캐싱 키)
 * @param name       가게명
 * @param address    주소(nullable)
 * @param latitude   위도
 * @param longitude  경도
 * @param category   카테고리(nullable)
 */
public record PlaceUpsertCommand(String externalId, String name, String address,
        double latitude, double longitude, String category) {
}
