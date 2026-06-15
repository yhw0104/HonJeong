package com.honjeong.place.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발/테스트용 Mock 구현. 실제 카카오 호출 없이 검색어에서 <b>결정적으로</b> 가짜 장소 목록을 만든다(Random·시계 미사용).
 * 덕분에 API 키·네트워크 없이 검색·페이지네이션·거리정렬 흐름을 앱과 e2e에서 확인할 수 있다.
 *
 * <p>{@code @ConditionalOnProperty(..., matchIfMissing = true)}: 설정 {@code honjeong.place.mode}가 "mock"이거나
 * <b>지정되지 않았을 때</b>(기본) 이 빈이 등록된다. 실 연동은 {@code honjeong.place.mode=real}로 교체한다(SMS·OAuth와 동일).
 *
 * <p>생성 규칙: 검색어 해시로 전체 건수를 [3,20]에 고정하고, i번째 후보의 좌표를 중심에서 일정 간격 멀어지게 둬
 * 거리정렬 결과가 결정적이 되게 한다. external_id는 {@code mock-<해시>-<i>} 형태로 짧게 만들어
 * {@code places.external_id VARCHAR(64)} 제약을 항상 만족한다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.place.mode", havingValue = "mock", matchIfMissing = true)
public class MockKakaoPlaceClient implements KakaoPlaceClient {

    private static final String[] CATEGORIES = {"한식", "분식", "카페", "일식"};
    private static final double DEFAULT_LAT = 37.5665; // 좌표 미지정 시 기준점(서울시청 부근)
    private static final double DEFAULT_LNG = 126.9780;
    private static final double LAT_STEP = 0.0009; // i가 커질수록 중심에서 멀어지는 간격(거리정렬 검증용)
    private static final double LNG_STEP = 0.0007;

    @Override
    public PlaceSearchPage search(PlaceSearchQuery query) {
        int total = 3 + Math.floorMod(query.query().hashCode(), 18); // 검색어별 [3,20]로 고정
        double baseLat = query.lat() != null ? query.lat() : DEFAULT_LAT;
        double baseLng = query.lng() != null ? query.lng() : DEFAULT_LNG;
        int idBase = Math.floorMod(query.query().hashCode(), 100000); // external_id를 짧게 유지

        List<PlaceCandidate> all = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            all.add(new PlaceCandidate(
                    "mock-" + idBase + "-" + i,
                    query.query() + " 맛집 " + (i + 1),
                    "서울특별시 어딘가구 " + (i + 1) + "로",
                    baseLat + i * LAT_STEP,
                    baseLng + i * LNG_STEP,
                    CATEGORIES[i % CATEGORIES.length]));
        }

        // 중심 좌표가 있으면 거리 오름차순 정렬(없으면 생성 순서 유지)
        if (query.lat() != null && query.lng() != null) {
            double lat = query.lat();
            double lng = query.lng();
            all.sort(Comparator.comparingDouble(p -> sq(p.latitude() - lat) + sq(p.longitude() - lng)));
        }

        // page/size 슬라이싱. 큰 page에서의 int 오버플로를 막으려 long으로 계산한다.
        long from = (long) query.page() * query.size();
        if (from >= total) {
            return new PlaceSearchPage(List.of(), total);
        }
        int to = (int) Math.min(from + query.size(), total);
        return new PlaceSearchPage(List.copyOf(all.subList((int) from, to)), total);
    }

    private static double sq(double x) {
        return x * x;
    }
}
