package com.honjeong.geo.dto;

import com.honjeong.geo.service.ReverseGeocodeResult;

/**
 * 역지오코딩(좌표→동네 변환) 결과를 나타내는 응답 DTO.
 *
 * <p>역지오코딩 응답 DTO. {@code GET /api/geo/reverse}에서 좌표→동네 변환 결과로 반환한다.
 * 앱은 이 값을 온보딩 '동네 설정'에 채워 {@code POST /api/auth/complete}의 region/regionLat/regionLng로 보낸다.
 *
 * @param region    동네 표시명(시·군·구·동)
 * @param regionLat 동네 중심 위도
 * @param regionLng 동네 중심 경도
 */
public record ReverseGeocodeResponse(String region, double regionLat, double regionLng) {

    /**
     * 서비스 계층 값 객체(ReverseGeocodeResult)를 응답 DTO로 변환한다.
     * <p>서비스 계층 결과 값 객체를 응답 DTO로 변환한다.
     *
     * @param r 역지오코딩 결과
     * @return 응답 DTO
     */
    public static ReverseGeocodeResponse from(ReverseGeocodeResult r) {
        return new ReverseGeocodeResponse(r.region(), r.regionLat(), r.regionLng());
    }
}
