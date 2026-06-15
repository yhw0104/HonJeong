package com.honjeong.place.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.service.PlaceService;

/**
 * {@link PlaceController}의 웹 계층 슬라이스 테스트.
 *
 * <p>검증 목적은 컨트롤러의 HTTP 관심사 — 요청 매핑/상태코드, 페이지 엔벨로프 JSON 형태, 그리고 보안 규칙
 * (검색은 정식 USER 토큰 필요) — 이며, 검색 비즈니스 로직은 {@code PlaceServiceTest}의 몫이다.
 *
 * <p>{@code @WebMvcTest(controllers = PlaceController.class)} + {@code @Import({SecurityConfig, WebConfig})}로
 * MVC·보안·인자 리졸버만 로드하고, 의존 {@link PlaceService}는 {@code @MockitoBean}으로 대체한다(UserControllerTest와 동일 구성).
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
                        "mock-1-" + i, "김밥 맛집 " + (i + 1), "서울 어딘가", 37.5 + i * 0.001, 127.0, "한식"))
                .toList();
        return PageResponse.of(content, 0, 5, 23L);
    }

    @Test
    @DisplayName("GET /search: USER 토큰이면 200 + 페이지 엔벨로프(content/page/size/totalElements)")
    void search_ok_pagedEnvelope() throws Exception {
        // given: 서비스가 5건·전체 23건 페이지를 돌려주도록 스텁 + USER access 토큰
        when(placeService.search(any(), any(), any(), anyInt(), anyInt())).thenReturn(samplePage());
        String token = jwtProvider.createAccessToken(1L);

        // when & then
        mockMvc.perform(get("/api/places/search")
                        .param("query", "김밥").param("page", "0").param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.content[0].externalId").value("mock-1-0"))
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
}
