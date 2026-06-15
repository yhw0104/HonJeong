package com.honjeong.place.client;

import java.util.List;

/**
 * 장소 검색 클라이언트의 한 페이지 결과. 이미 page/size로 잘린 후보 목록과, 페이지와 무관한 전체 건수를 담는다
 * (실제 카카오 응답의 {@code documents[]} + {@code meta.total_count} 구조를 본뜬 것). 페이지네이션·거리정렬은
 * 클라이언트가 책임지고, 서비스는 매핑·재포장만 한다.
 *
 * @param candidates 이 페이지의 후보 목록(범위를 벗어난 페이지면 빈 목록)
 * @param totalCount 검색어에 대한 전체 후보 수(페이지와 무관하게 일정)
 */
public record PlaceSearchPage(List<PlaceCandidate> candidates, long totalCount) {
}
