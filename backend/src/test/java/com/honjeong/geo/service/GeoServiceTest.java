package com.honjeong.geo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.geo.dto.ReverseGeocodeResponse;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * GeoService 단위 테스트(순수 Mockito). 좌표 검증과 {@link ReverseGeocoder} 위임·매핑을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class GeoServiceTest {

    @Mock
    ReverseGeocoder reverseGeocoder;

    @InjectMocks
    GeoService service;

    @Test
    @DisplayName("좌표를 역지오코더에 위임하고 결과를 응답 DTO로 매핑한다")
    void reverseGeocode_delegatesAndMaps() {
        when(reverseGeocoder.reverse(37.5, 127.0))
                .thenReturn(new ReverseGeocodeResult("서울특별시 강남구 역삼동", 37.5, 127.0));

        ReverseGeocodeResponse res = service.reverseGeocode(37.5, 127.0);

        assertThat(res.region()).isEqualTo("서울특별시 강남구 역삼동");
        assertThat(res.regionLat()).isEqualTo(37.5);
        assertThat(res.regionLng()).isEqualTo(127.0);
    }

    @Test
    @DisplayName("lat이 null이면 INVALID_INPUT이고 역지오코더를 호출하지 않는다")
    void reverseGeocode_nullLat() {
        assertThatThrownBy(() -> service.reverseGeocode(null, 127.0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(reverseGeocoder);
    }

    @Test
    @DisplayName("lng이 null이면 INVALID_INPUT")
    void reverseGeocode_nullLng() {
        assertThatThrownBy(() -> service.reverseGeocode(37.5, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(reverseGeocoder);
    }

    @Test
    @DisplayName("좌표 범위를 벗어나면 INVALID_INPUT(위도 ±90, 경도 ±180)")
    void reverseGeocode_outOfRange() {
        assertThatThrownBy(() -> service.reverseGeocode(200.0, 127.0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.reverseGeocode(37.5, 999.0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(reverseGeocoder);
    }
}
