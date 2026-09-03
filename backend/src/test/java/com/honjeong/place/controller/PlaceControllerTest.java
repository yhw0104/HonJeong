package com.honjeong.place.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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

import com.honjeong.global.common.ListResponse;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.place.dto.PlaceDetailResponse;
import com.honjeong.place.dto.PlaceNearbyResponse;
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.service.PlaceService;
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * {@link PlaceController}의 웹 계층 슬라이스 테스트.
 *
 * <p>Task 6: 검색이 우리 DB 기반으로 전환됐다. PlaceSearchResponse 형태가 바뀌었고(placeId 추가,
 * lat/lng 파라미터 제거) PlaceService.search 시그니처가 (query, page, size) 3인자로 단순화됐다.
 */
@WebMvcTest(controllers = PlaceController.class)
@Import({SecurityConfig.class, WebConfig.class})
class PlaceControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private PlaceService placeService;

    private ListResponse<PlaceSearchResponse> sampleList() {
        List<PlaceSearchResponse> content = IntStream.range(0, 5)
                .mapToObj(i -> new PlaceSearchResponse(
                        (long) (i + 1), "김밥 맛집 " + (i + 1), "분식",
                        "서울 어딘가", "서울 도로명", 37.5 + i * 0.001, 127.0, "02-111", 120L + i))
                .toList();
        return ListResponse.of(content);
    }

    @Test
    @DisplayName("GET /search: USER 토큰이면 200 + content 목록 엔벨로프")
    void search_ok_listEnvelope() throws Exception {
        // given: 서비스가 5건을 돌려주도록 스텁 + USER access 토큰
        when(placeService.search(any(), any(), any(), anyInt())).thenReturn(sampleList());
        String token = jwtProvider.createAccessToken(1L);

        // when & then
        mockMvc.perform(get("/api/places/search")
                        .param("query", "김밥")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.content[0].placeId").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("김밥 맛집 1"));
    }

    /**
     * ★배포된 앱이 {@code data.content}를 읽는다. 봉투를 벗겨 배열을 그대로 내려보내면 구버전
     * 앱에서 목록이 통째로 비어 보이는데, 서버는 200을 주므로 아무 신호가 없다. 그 회귀를 막는다.
     */
    @Test
    @DisplayName("★GET /search: 응답이 배열이 아니라 content 봉투다(구버전 앱 호환)")
    void search_keepsContentEnvelope() throws Exception {
        when(placeService.search(any(), any(), any(), anyInt())).thenReturn(sampleList());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/search")
                        .param("query", "김밥")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    /** page/size를 보내도 400이 아니다 — 구버전 앱이 붙여 보내는 값이라 무시하고 받아야 한다. */
    @Test
    @DisplayName("★GET /search: 구버전 앱이 page/size를 붙여 보내도 200이다")
    void search_ignoresLegacyPagingParams() throws Exception {
        when(placeService.search(any(), any(), any(), anyInt())).thenReturn(sampleList());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/search")
                        .param("query", "김밥").param("page", "0").param("size", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(5));
    }

    /**
     * ★lat/lng가 서비스까지 실제로 전달되는지 못 박는다.
     *
     * <p>위 {@code search_ok_listEnvelope}는 {@code any()}로 스텁해서, 컨트롤러가 lat/lng를
     * 받지 않도록 되돌려도(또는 null로 넘겨도) 그대로 초록이다. 그러면 "내 위치 기준 검색"이
     * 조용히 전국 이름순으로 돌아가는데, 서버는 200을 주고 결과도 나오므로 아무 신호가 없다.
     * 값 자체를 캡처해 확인해야 그 회귀가 잡힌다.
     */
    @Test
    @DisplayName("★GET /search: lat/lng/radius를 주면 그 값이 서비스로 전달된다")
    void search_forwardsCoordinates() throws Exception {
        when(placeService.search(any(), any(), any(), anyInt())).thenReturn(sampleList());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/search")
                        .param("query", "김밥")
                        .param("lat", "37.5").param("lng", "127.0").param("radius", "3000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(placeService).search(eq("김밥"), eq(37.5), eq(127.0), eq(3000));
    }

    /** 좌표를 안 주면 null로 내려간다 — 서비스가 그 null을 보고 전국 이름순으로 간다. */
    @Test
    @DisplayName("GET /search: lat/lng가 없으면 서비스에 null로 전달된다")
    void search_withoutCoordinates_passesNull() throws Exception {
        when(placeService.search(any(), any(), any(), anyInt())).thenReturn(sampleList());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/search")
                        .param("query", "김밥")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(placeService).search(eq("김밥"), isNull(), isNull(), anyInt());
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

    private ListResponse<PlaceNearbyResponse> sampleNearbyList() {
        List<PlaceNearbyResponse> content = List.of(
                new PlaceNearbyResponse(10L, "혼밥집", "한식", "서울 도로명", 37.5001, 127.0001, 15L, 3L, 2L,
                        List.of("https://img/1.jpg", "https://img/2.jpg"), 12L, 4.5, 4.0),
                new PlaceNearbyResponse(11L, "먼집", "분식", "서울 도로명", 37.5050, 127.0050, 680L, 0L, 0L,
                        List.of(), 0L, null, null));
        return ListResponse.of(content);
    }

    @Test
    @DisplayName("GET /nearby: USER 토큰 + lat/lng 있으면 200 + content 엔벨로프(혼밥러수·모집중수 포함)")
    void nearby_ok() throws Exception {
        when(placeService.nearby(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(sampleNearbyList());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/nearby")
                        .param("lat", "37.5").param("lng", "127.0").param("radius", "1000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].placeId").value(10))
                .andExpect(jsonPath("$.data.content[0].activeCount").value(3))
                .andExpect(jsonPath("$.data.content[0].seekingCount").value(2))
                .andExpect(jsonPath("$.data.content[0].reviewCount").value(12))
                .andExpect(jsonPath("$.data.content[0].avgTasteRating").value(4.5))
                .andExpect(jsonPath("$.data.content[1].reviewCount").value(0))
                .andExpect(jsonPath("$.data.content[1].activeCount").value(0))
                .andExpect(jsonPath("$.data.content[1].seekingCount").value(0));
    }

    @Test
    @DisplayName("GET /nearby: lat/lng 누락이면 서비스가 INVALID_INPUT 예외를 던져 400")
    void nearby_missingLatLng_400() throws Exception {
        when(placeService.nearby(any(), any(), anyInt()))
                .thenThrow(new com.honjeong.global.exception.BusinessException(
                        com.honjeong.global.exception.ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다."));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/nearby")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    // ─── detail ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /{placeId}: USER 토큰이면 200 + 식당 상세")
    void detail_ok() throws Exception {
        when(placeService.getDetail(7L)).thenReturn(new PlaceDetailResponse(
                7L, "혼밥식당", "한식", "서울 지번", "서울 도로명", 37.5, 127.0, "02-123", "영업"));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/{placeId}", 7L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.placeId").value(7))
                .andExpect(jsonPath("$.data.name").value("혼밥식당"))
                .andExpect(jsonPath("$.data.category").value("한식"))
                .andExpect(jsonPath("$.data.roadAddress").value("서울 도로명"))
                .andExpect(jsonPath("$.data.businessStatus").value("영업"));
    }

    @Test
    @DisplayName("GET /{placeId}: 없는 식당이면 404 PLACE_NOT_FOUND")
    void detail_notFound_404() throws Exception {
        when(placeService.getDetail(999L)).thenThrow(new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/places/{placeId}", 999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /{placeId}: 토큰이 없으면 401")
    void detail_noToken_401() throws Exception {
        mockMvc.perform(get("/api/places/{placeId}", 7L))
                .andExpect(status().isUnauthorized());
    }
}
