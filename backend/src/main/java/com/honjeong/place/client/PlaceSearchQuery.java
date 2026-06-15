package com.honjeong.place.client;

/**
 * 장소 검색 클라이언트 호출 파라미터. 검색어와 (선택) 중심 좌표, 그리고 페이지네이션 정보를 담는다.
 * 중심 좌표가 주어지면 결과를 거리순으로 정렬하는 데 쓰인다.
 *
 * @param query 검색어(가게명/지역) — 비어있지 않음(블랭크 검증은 서비스가 선처리)
 * @param lat   중심 위도(nullable, 거리순 정렬용)
 * @param lng   중심 경도(nullable, 거리순 정렬용)
 * @param page  0-base 페이지 번호
 * @param size  페이지 크기
 */
public record PlaceSearchQuery(String query, Double lat, Double lng, int page, int size) {
}
