package com.honjeong.place.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.common.PageResponse;
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.service.PlaceService;

/**
 * 장소 검색 REST 컨트롤러 — 카카오 로컬 검색 프록시 엔드포인트를 담당한다.
 *
 * <p>모든 경로는 {@code @RequestMapping("/api/places")} 접두사라서 {@code /api/places/...} 형태가 된다.
 * 컨트롤러는 얇게 유지한다 — 쿼리 파라미터 바인딩과 DTO 변환만 하고 검증·페이지네이션·매핑은 {@link PlaceService}에 위임한다.
 *
 * <p><b>인가:</b> 검색은 정식 회원(USER)만 호출할 수 있다. SecurityConfig의 {@code anyRequest().hasRole("USER")}
 * 기본 규칙이 이 경로를 커버하므로(별도 매처 불필요) 온보딩 토큰은 403, 토큰 없으면 401로 막힌다. userId 자체는
 * 검색 로직에 쓰이지 않아 주입받지 않는다.
 */
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    /**
     * 가게 검색(카카오 로컬 프록시).
     *
     * <p><b>요청:</b> {@code GET /api/places/search?query=김밥&lat=..&lng=..&page=0&size=20}.
     * {@code query}는 필수 — 누락 시 {@code MissingServletRequestParameterException} 핸들러가 400({@code INVALID_INPUT})을
     * 돌려준다. {@code lat}/{@code lng}는 선택(거리순 정렬용), {@code page}(기본 0)·{@code size}(기본 20)는 페이지네이션.
     *
     * <p><b>응답:</b> {@code ApiResponse<PageResponse<PlaceSearchResponse>>} — {@code content}/{@code page}/{@code size}/{@code totalElements}.
     *
     * <p><b>인증:</b> 정식 USER 토큰 필요.
     *
     * @param query 검색어(필수)
     * @param lat   중심 위도(선택, 거리순 정렬용)
     * @param lng   중심 경도(선택, 거리순 정렬용)
     * @param page  0-base 페이지 번호(기본 0)
     * @param size  페이지 크기(기본 20)
     * @return 검색 결과 페이지 엔벨로프
     */
    @GetMapping("/search")
    public ApiResponse<PageResponse<PlaceSearchResponse>> search(
            @RequestParam String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(placeService.search(query, lat, lng, page, size));
    }
}
