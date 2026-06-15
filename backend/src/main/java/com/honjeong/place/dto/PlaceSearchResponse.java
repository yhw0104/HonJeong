package com.honjeong.place.dto;

import com.honjeong.place.client.PlaceCandidate;

/**
 * 장소 검색 결과 1건의 응답 DTO. 클라이언트 후보({@link PlaceCandidate})에서 노출용 6개 필드만 그대로 옮긴다.
 *
 * @param externalId 카카오 place id(체크인 시 이 값을 다시 보내 캐싱 upsert에 쓴다)
 * @param name       가게명
 * @param address    주소(nullable)
 * @param latitude   위도
 * @param longitude  경도
 * @param category   카테고리(nullable)
 */
public record PlaceSearchResponse(String externalId, String name, String address,
        double latitude, double longitude, String category) {

    /**
     * 검색 클라이언트 후보를 응답 DTO로 변환한다.
     *
     * @param candidate 클라이언트가 돌려준 후보
     * @return 응답 DTO
     */
    public static PlaceSearchResponse from(PlaceCandidate candidate) {
        return new PlaceSearchResponse(candidate.externalId(), candidate.name(), candidate.address(),
                candidate.latitude(), candidate.longitude(), candidate.category());
    }
}
