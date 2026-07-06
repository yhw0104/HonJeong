package com.honjeong.geo.service;

import org.springframework.stereotype.Service;

import com.honjeong.geo.dto.ReverseGeocodeResponse;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * 1. 기능: 좌표 입력을 검증하고 ReverseGeocoder에 역지오코딩(좌표→동네)을 위임한 뒤 응답 DTO로 변환
 * 2. 사용 Controller: GeoController
 *
 * <p>[기존 주석] 역지오코딩 서비스. 좌표 입력을 검증하고 {@link ReverseGeocoder}에 변환을 위임한 뒤 응답 DTO로 매핑한다.
 * 변환 구현(mock/real)은 주입받은 {@link ReverseGeocoder}에 달려 있어 이 서비스는 교체에 영향받지 않는다.
 */
@Service
public class GeoService {

    private final ReverseGeocoder reverseGeocoder;

    public GeoService(ReverseGeocoder reverseGeocoder) {
        this.reverseGeocoder = reverseGeocoder;
    }

    /**
     * 기능: 좌표를 동네(행정구역)로 역지오코딩한다 — null·좌표 범위 검증 후 ReverseGeocoder에 위임
     * Request: lat — 위도(필수, -90~90), lng — 경도(필수, -180~180) / 위반 시 BusinessException(INVALID_INPUT)
     * Response: ReverseGeocodeResponse — 동네 표시명·중심 좌표
     *
     * <p>[기존 주석] 좌표를 동네로 역지오코딩한다.
     *
     * @param lat 위도(필수)
     * @param lng 경도(필수)
     * @return 동네 표시명·중심 좌표 응답
     */
    public ReverseGeocodeResponse reverseGeocode(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다.");
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "좌표 범위가 올바르지 않습니다.");
        }
        return ReverseGeocodeResponse.from(reverseGeocoder.reverse(lat, lng));
    }
}
