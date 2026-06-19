package com.honjeong.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.global.common.PageResponse;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.place.dto.PlaceNearbyResponse;
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.service.PlaceService;

/**
 * {@link PlaceController}의 웹 계층 슬라이스 테스트.
 *
 * <p>Task 6: 검색이 우리 DB 기반으로 전환됐다. PlaceSearchResponse 형태가 바뀌었고(placeId 추가,
 * lat/lng 파라미터 제거) PlaceService.search 시그니처가 (query, page, size) 3인자로 단순화됐다.
 */
@WebMvcTest(controllers = PlaceController.class)
@Import({SecurityConfig.class, WebConfig.class})
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private PlaceService placeService;

    private PageResponse<PlaceSearchResponse> samplePage() {
        List<PlaceSearchResponse> content = IntStream.range(0, 5)
                .mapToObj(i -> new PlaceSearchResponse(
                        (long) (i + 1), "김밥 맛집 " + (i + 1), "분식",
                        "서울 어딘가", "서울 도로명", 37.5 + i * 0.001, 127.0, "02-111"))
                .toList();
        return PageResponse.of(content, 0, 5, 23L);
    }

    @Test
    @DisplayName("GET /search: USER 토큰이면 200 + 페이지 엔벨로프(content/page/size/totalElements)")
    void search_ok_pagedEnvelope() throws Exception {
        // given: 서비스가 5건·전체 23건 페이지를 돌려주도록 스텁 + USER access 토큰
        when(placeService.search(any(), anyInt(), anyInt())).thenReturn(samplePage());
        String token = jwtProvider.createAccessToken(1L);

        // when & then
        mockMvc.perform(get("/api/places/search")
                        .param("query", "김밥").param("page", "0").param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.content[0].placeId").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("김밥 맛집 1"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(23));
    }

    @Test
    @DisplayName("GET /search: 필수 query 파라미터가 없으면 400 INVALID_INPUT")
    void search_missingQuery_400() throws Exception {
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/search").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /search: 토큰이 없으면 401")
    void search_noToken_401() throws Exception {
        mockMvc.perform(get("/api/places/search").param("query", "김밥"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /search: 온보딩 토큰이면 403(검색은 USER 전용)")
    void search_onboardingToken_403() throws Exception {
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(get("/api/places/search").param("query", "김밥")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ─── nearby ──────────────────────────────────────────────────────────────

    private PageResponse<PlaceNearbyResponse> sampleNearbyPage() {
        List<PlaceNearbyResponse> content = List.of(
                new PlaceNearbyResponse(10L, "혼밥집", "한식", "서울 도로명", 37.5001, 127.0001, 15L, 3L),
                new PlaceNearbyResponse(11L, "먼집", "분식", "서울 도로명", 37.5050, 127.0050, 680L, 0L));
        return PageResponse.of(content, 0, 20, 2L);
    }

    @Test
    @DisplayName("GET /nearby: USER 토큰 + lat/lng 있으면 200 + 페이지 엔벨로프(혼밥러수 포함)")
    void nearby_ok() throws Exception {
        when(placeService.nearby(anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt()))
                .thenReturn(sampleNearbyPage());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/nearby")
                        .param("lat", "37.5").param("lng", "127.0").param("radius", "1000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].placeId").value(10))
                .andExpect(jsonPath("$.data.content[0].activeCount").value(3))
                .andExpect(jsonPath("$.data.content[1].activeCount").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /nearby: lat/lng 누락이면 서비스가 INVALID_INPUT 예외를 던져 400")
    void nearby_missingLatLng_400() throws Exception {
        when(placeService.nearby(any(), any(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new com.honjeong.global.exception.BusinessException(
                        com.honjeong.global.exception.ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다."));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/nearby")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
