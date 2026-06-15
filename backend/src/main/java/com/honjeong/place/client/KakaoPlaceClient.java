package com.honjeong.place.client;

/**
 * 외부 장소 검색(카카오 로컬)을 격리하는 인터페이스. 검색만 담당하며, 실제 호출 수단은 구현에 맡긴다.
 * 개발/테스트용 {@link MockKakaoPlaceClient}는 네트워크 없이 결정적 결과를 만들고, 실 운영용 구현은
 * 카카오 로컬 API를 호출한다. 구현 전환은 설정 {@code honjeong.place.mode}로 한다(SMS·OAuth와 동일 패턴).
 */
public interface KakaoPlaceClient {

    /**
     * 주어진 조건으로 장소를 검색한다. 페이지 슬라이싱과 (좌표가 있으면) 거리순 정렬은 구현이 책임진다.
     *
     * @param query 검색어·중심좌표·페이지 정보
     * @return 한 페이지의 후보 목록과 전체 건수
     */
    PlaceSearchPage search(PlaceSearchQuery query);
}
