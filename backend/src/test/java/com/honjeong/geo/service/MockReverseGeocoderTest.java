package com.honjeong.geo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * MockReverseGeocoder 단위 테스트. 외부 호출 없이 결정론적 동네를 만들고, 입력 좌표를 그대로 중심으로 돌려주는지 확인한다.
 */
class MockReverseGeocoderTest {

    private final MockReverseGeocoder geocoder = new MockReverseGeocoder();

    @Test
    @DisplayName("동네 표시명을 비어있지 않게 반환하고 입력 좌표를 중심으로 그대로 돌려준다")
    void reverse_returnsRegionAndEchoesCoords() {
        ReverseGeocodeResult r = geocoder.reverse(37.5, 127.0);

        assertThat(r.region()).isNotBlank();
        assertThat(r.regionLat()).isEqualTo(37.5);
        assertThat(r.regionLng()).isEqualTo(127.0);
    }

    @Test
    @DisplayName("같은 좌표는 항상 같은 동네로 변환한다(결정론적)")
    void reverse_isDeterministic() {
        ReverseGeocodeResult a = geocoder.reverse(37.512, 127.034);
        ReverseGeocodeResult b = geocoder.reverse(37.512, 127.034);

        assertThat(a.region()).isEqualTo(b.region());
    }
}
