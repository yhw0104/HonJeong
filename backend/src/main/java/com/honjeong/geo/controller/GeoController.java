package com.honjeong.geo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.geo.dto.ReverseGeocodeResponse;
import com.honjeong.geo.service.GeoService;
import com.honjeong.global.common.ApiResponse;

/**
 * 역지오코딩(좌표→동네 이름 변환, AUTH-012) 컨트롤러.
 *
 * <p>기본 경로: /api/geo
 *
 * <p>역지오코딩 REST 컨트롤러(AUTH-012).
 *
 * <p><b>인가:</b> 온보딩 '동네 설정'(ProfileSetup) 단계에서 호출하므로 온보딩 토큰 또는 정식 USER 모두 허용한다.
 */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    /**
     * 좌표를 동네(행정구역)로 변환한다.
     *
     * <p>사용 화면: 현재 앱에서는 쓰지 않는다 — '내 동네' 기능 제거 결정(2026-07-04) 이후 호출하는 화면이 없다.
     *
     * <p>{@code lat}/{@code lng}는 필수라 누락·범위초과 시 400({@code INVALID_INPUT})이다.
     *
     * @param lat 요청 위도(필수)
     * @param lng 요청 경도(필수)
     * @return 역지오코딩 응답
     */
    @GetMapping("/reverse")
    public ApiResponse<ReverseGeocodeResponse> reverse(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return ApiResponse.success(geoService.reverseGeocode(lat, lng));
    }
}
