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
 * 장소 검색 REST 컨트롤러.
 *
 * <p>Task 6 이후 검색은 카카오 로컬 API 대신 우리 DB(공공데이터 마스터)를 사용한다.
 * lat/lng 파라미터는 제거됐다(거리 정렬이 필요할 경우 Task N에서 재추가 예정).
 *
 * <p><b>인가:</b> 검색은 정식 회원(USER)만 호출할 수 있다.
 */
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    /**
     * 가게 검색(우리 DB, 영업 중인 장소 이름 부분일치).
     *
     * <p><b>요청:</b> {@code GET /api/places/search?query=김밥&page=0&size=20}.
     * {@code query}는 필수 — 누락 시 400({@code INVALID_INPUT}).
     *
     * <p><b>응답:</b> {@code ApiResponse<PageResponse<PlaceSearchResponse>>} — {@code content}/{@code page}/{@code size}/{@code totalElements}.
     *
     * @param query 검색어(필수)
     * @param page  0-base 페이지 번호(기본 0)
     * @param size  페이지 크기(기본 20)
     * @return 검색 결과 페이지 엔벨로프
     */
    @GetMapping("/search")
    public ApiResponse<PageResponse<PlaceSearchResponse>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(placeService.search(query, page, size));
    }
}
