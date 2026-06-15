package com.honjeong.place.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * MockKakaoPlaceClient 단위 테스트.
 *
 * <p>검증 목적: 실제 카카오 호출 없이 동작하는 Mock이 (1) 같은 입력에 항상 같은 결과를 주는 결정성,
 * (2) page/size로 올바르게 슬라이싱하고 totalCount는 페이지와 무관하게 일정한지, (3) 범위를 넘는 페이지는
 * 빈 목록을 주는지, (4) lat/lng가 있으면 거리 오름차순으로 정렬하는지, (5) 생성한 external_id가
 * places.external_id 컬럼 길이(VARCHAR(64))를 넘지 않는지를 본다. (Random·시계 미사용 → 결정적.)
 */
class MockKakaoPlaceClientTest {

    private final MockKakaoPlaceClient client = new MockKakaoPlaceClient();

    @Test
    @DisplayName("같은 쿼리는 항상 같은 totalCount와 같은 첫 결과를 준다(결정적)")
    void deterministicForSameQuery() {
        // given & when: 같은 쿼리/페이지로 두 번 호출하면
        PlaceSearchPage first = client.search(new PlaceSearchQuery("김밥", null, null, 0, 5));
        PlaceSearchPage second = client.search(new PlaceSearchQuery("김밥", null, null, 0, 5));

        // then: totalCount와 첫 후보(externalId·name)가 동일하다
        assertThat(first.totalCount()).isEqualTo(second.totalCount());
        assertThat(first.candidates().get(0).externalId())
                .isEqualTo(second.candidates().get(0).externalId());
        assertThat(first.candidates().get(0).name())
                .isEqualTo(second.candidates().get(0).name());
        // total은 쿼리별 고정 범위[3,20] 안에 있다
        assertThat(first.totalCount()).isBetween(3L, 20L);
    }

    @Test
    @DisplayName("page/size로 슬라이싱한다 — 인접 페이지는 서로 겹치지 않고 totalCount는 일정하다")
    void paginatesAndSlices() {
        // given: 같은 쿼리를 size=2로, page 0과 1을 각각 조회
        PlaceSearchPage page0 = client.search(new PlaceSearchQuery("국밥", null, null, 0, 2));
        PlaceSearchPage page1 = client.search(new PlaceSearchQuery("국밥", null, null, 1, 2));

        // then: 각 페이지 크기는 요청 size 이하, totalCount는 동일, 두 페이지의 externalId는 서로소
        assertThat(page0.candidates()).isNotEmpty().hasSizeLessThanOrEqualTo(2);
        assertThat(page1.candidates()).isNotEmpty().hasSizeLessThanOrEqualTo(2);
        assertThat(page0.totalCount()).isEqualTo(page1.totalCount());

        List<String> ids0 = page0.candidates().stream().map(PlaceCandidate::externalId).toList();
        List<String> ids1 = page1.candidates().stream().map(PlaceCandidate::externalId).toList();
        assertThat(ids0).doesNotContainAnyElementsOf(ids1);
    }

    @Test
    @DisplayName("totalCount를 넘는 페이지는 빈 목록을 주되 totalCount는 유지한다")
    void pageBeyondTotalIsEmpty() {
        // given & when: 아주 큰 페이지 번호로 조회하면
        PlaceSearchPage page = client.search(new PlaceSearchQuery("라멘", null, null, 100, 10));

        // then: 후보는 비지만 totalCount는 그대로다
        assertThat(page.candidates()).isEmpty();
        assertThat(page.totalCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("lat/lng가 주어지면 거리 오름차순으로 정렬한다")
    void sortsByDistanceWhenLatLngGiven() {
        // given: 중심 좌표를 주고 조회
        double lat = 37.5665;
        double lng = 126.9780;
        PlaceSearchPage page = client.search(new PlaceSearchQuery("카페", lat, lng, 0, 10));

        // then: 후보들의 (중심까지) 거리가 비내림차순이다
        List<PlaceCandidate> c = page.candidates();
        assertThat(c).isNotEmpty();
        double prev = -1;
        for (PlaceCandidate p : c) {
            double d = sq(p.latitude() - lat) + sq(p.longitude() - lng);
            assertThat(d).isGreaterThanOrEqualTo(prev);
            prev = d;
        }
    }

    @Test
    @DisplayName("생성한 external_id는 컬럼 길이(64)를 넘지 않는다")
    void externalIdFitsColumnLength() {
        // given & when: 긴 한글 쿼리로 조회해도
        PlaceSearchPage page = client.search(new PlaceSearchQuery("아주아주긴검색어테스트값", null, null, 0, 20));

        // then: 모든 external_id 길이가 64 이하다
        assertThat(page.candidates())
                .allSatisfy(p -> assertThat(p.externalId().length()).isLessThanOrEqualTo(64));
    }

    private static double sq(double x) {
        return x * x;
    }
}
