package com.honjeong.place.client;

/**
 * 장소 검색 클라이언트가 돌려주는 단일 후보. 카카오 로컬 문서 1건에 대응하며, 우리 응답/캐시에 필요한
 * 6개 필드만 담는다(phone·homepage 등 P2 필드는 제외).
 *
 * @param externalId 카카오 place id(캐싱 키)
 * @param name       가게명
 * @param address    주소(nullable)
 * @param latitude   위도
 * @param longitude  경도
 * @param category   카테고리(nullable)
 */
public record PlaceCandidate(String externalId, String name, String address,
        double latitude, double longitude, String category) {
}
