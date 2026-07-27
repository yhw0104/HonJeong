package com.honjeong.geo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.geo.dto.ReverseGeocodeResponse;
import com.honjeong.geo.service.GeoService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * {@link GeoController}의 웹 계층 슬라이스 테스트. 역지오코딩 응답·입력검증·인가(온보딩 허용)를 확인한다.
 */
@WebMvcTest(controllers = GeoController.class)
@Import({SecurityConfig.class, WebConfig.class})
class GeoControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private GeoService geoService;

    @Test
    @DisplayName("GET /reverse: USER 토큰 + lat/lng면 200 + 동네 응답")
    void reverse_ok() throws Exception {
        when(geoService.reverseGeocode(37.5, 127.0))
                .thenReturn(new ReverseGeocodeResponse("서울특별시 강남구 역삼동", 37.5, 127.0));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/geo/reverse").param("lat", "37.5").param("lng", "127.0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.region").value("서울특별시 강남구 역삼동"))
                .andExpect(jsonPath("$.data.regionLat").value(37.5))
                .andExpect(jsonPath("$.data.regionLng").value(127.0));
    }

    @Test
    @DisplayName("GET /reverse: 온보딩 토큰이면 200(동네 설정은 온보딩 단계에서 호출)")
    void reverse_onboardingToken_ok() throws Exception {
        when(geoService.reverseGeocode(37.5, 127.0))
                .thenReturn(new ReverseGeocodeResponse("서울특별시 강남구 역삼동", 37.5, 127.0));
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(get("/api/geo/reverse").param("lat", "37.5").param("lng", "127.0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.region").value("서울특별시 강남구 역삼동"));
    }

    @Test
    @DisplayName("GET /reverse: lat/lng 누락이면 서비스가 INVALID_INPUT을 던져 400")
    void reverse_missingCoords_400() throws Exception {
        when(geoService.reverseGeocode(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다."));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/geo/reverse").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /reverse: 토큰이 없으면 401")
    void reverse_noToken_401() throws Exception {
        mockMvc.perform(get("/api/geo/reverse").param("lat", "37.5").param("lng", "127.0"))
                .andExpect(status().isUnauthorized());
    }
}
