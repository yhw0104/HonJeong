package com.honjeong.geo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 외부 API 호출 없이 좌표 해시로 5개 후보 중 결정론적 동네를 돌려주는 개발용 Mock 역지오코더(ReverseGeocoder 구현체).
 *
 * <p>사용처: GeoService — honjeong.geo.mode=mock 또는 미지정(기본) 시 빈 등록, 운영 시 카카오 REST 구현으로 교체 예정.
 * <p>개발용 Mock 역지오코더. 외부 역지오코딩 API를 호출하지 않고, 받은 좌표만으로 <b>결정론적</b> 동네를
 * 만들어 낸다(같은 좌표 → 항상 같은 동네). 같은 입력이 항상 같은 결과를 주므로 온보딩 흐름을 반복 테스트하기 쉽다.
 *
 * <p>{@code @ConditionalOnProperty(..., matchIfMissing = true)}: {@code honjeong.geo.mode}가 "mock"이거나
 * <b>아예 지정되지 않았을 때</b>(기본) 이 빈이 등록된다. 실 연동은 {@code honjeong.geo.mode=real}로 두고
 * 별도 구현으로 교체한다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.geo.mode", havingValue = "mock", matchIfMissing = true)
public class MockReverseGeocoder implements ReverseGeocoder {

    // 결정론적 후보 동네 — 좌표 해시로 하나를 고른다(외부 호출 없이 재현 가능).
    private static final List<String> SAMPLE_REGIONS = List.of(
            "서울특별시 강남구 역삼동",
            "서울특별시 마포구 서교동",
            "서울특별시 종로구 사직동",
            "부산광역시 해운대구 우동",
            "대구광역시 중구 삼덕동");

    /**
     * 좌표를 소수 둘째자리로 반올림해 해시한 값으로 5개 후보 동네 중 하나를 결정론적으로 선택한다.
     *
     * @param lat 위도
     * @param lng 경도
     * @return 후보 동네 표시명 + 입력 좌표 그대로(mock이라 중심 스냅 없음)
     */
    @Override
    public ReverseGeocodeResult reverse(double lat, double lng) {
        // 좌표를 소수 둘째자리로 묶어 해시 → 같은 동네 범위는 같은 결과가 나온다.
        int idx = Math.floorMod(Objects.hash(Math.round(lat * 100), Math.round(lng * 100)), SAMPLE_REGIONS.size());
        // 중심 좌표는 mock이라 입력 좌표를 그대로 돌려준다(실 구현은 동 중심으로 스냅).
        return new ReverseGeocodeResult(SAMPLE_REGIONS.get(idx), lat, lng);
    }
}
