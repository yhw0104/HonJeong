package com.honjeong.geo.service;

/**
 * 1. 기능: 좌표(위도·경도)→동네(행정구역) 변환 책임을 정의하는 역지오코딩 인터페이스
 * 2. 사용처: GeoService(주입) — 구현체: MockReverseGeocoder(개발), 운영 시 카카오 REST 구현으로 교체 예정
 *
 * <p>[기존 주석] 좌표(위도·경도)를 사람이 읽는 동네(행정구역)로 바꾸는 역지오코딩 책임을 정의하는 인터페이스.
 * 온보딩 '동네 설정'(AUTH-012)에서 현재 위치를 동네 표시명 + 중심 좌표로 변환하는 데 쓴다.
 *
 * <p>실제 변환은 환경에 따라 구현이 갈린다 — 개발용 {@link MockReverseGeocoder}는 외부 호출 없이
 * 결정론적 동네를 만들고, 실 운영용 구현은 외부 역지오코딩 API와 통신한다. 이렇게 인터페이스로 추상화해
 * {@link GeoService}는 구현 교체에 영향받지 않는다(mock-first 패턴, 설정 {@code honjeong.geo.mode}).
 */
public interface ReverseGeocoder {

    /**
     * 기능: 좌표를 동네(행정구역)로 변환한다
     * Request: lat — 위도, lng — 경도
     * Response: ReverseGeocodeResult — 동네 표시명과 중심 좌표
     *
     * <p>[기존 주석] 좌표를 동네(행정구역)로 변환한다.
     *
     * @param lat 위도
     * @param lng 경도
     * @return 동네 표시명과 중심 좌표를 담은 결과
     */
    ReverseGeocodeResult reverse(double lat, double lng);
}
