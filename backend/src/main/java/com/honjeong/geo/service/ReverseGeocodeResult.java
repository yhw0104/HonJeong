package com.honjeong.geo.service;

/**
 * 역지오코딩 결과(좌표→동네 변환 결과)를 나타내는 서비스 계층 값 객체.
 *
 * <p>[기존 주석] 역지오코딩 결과(서비스 계층 값 객체). {@link ReverseGeocoder}가 좌표를 변환해 돌려준다.
 * 웹 계층 DTO({@link com.honjeong.geo.dto.ReverseGeocodeResponse})와 분리해 서비스 경계 안에서만 쓴다.
 *
 * @param region    동네 표시명(예: "서울특별시 강남구 역삼동")
 * @param regionLat 동네 중심 위도
 * @param regionLng 동네 중심 경도
 */
public record ReverseGeocodeResult(String region, double regionLat, double regionLng) {
}
