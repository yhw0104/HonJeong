package com.honjeong.place.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.common.PageResponse;
import com.honjeong.place.dto.PlaceDetailResponse;
import com.honjeong.place.dto.PlaceNearbyResponse;
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

    /**
     * 현재 위치 주변의 영업 중인 식당을 거리순으로 반환하고 ACTIVE 혼밥러 수를 오버레이한다.
     *
     * <p><b>요청:</b> {@code GET /api/places/nearby?lat=37.5&lng=127.0&radius=1000&page=0&size=20}.
     * {@code lat}/{@code lng}는 필수 — 누락 시 400({@code INVALID_INPUT}).
     *
     * <p><b>응답:</b> {@code ApiResponse<PageResponse<PlaceNearbyResponse>>} — 거리순 장소 목록(혼밥러수 포함).
     *
     * @param lat    요청 위도(필수)
     * @param lng    요청 경도(필수)
     * @param radius 반경(m, 기본 1000, 최대 10000)
     * @param page   0-base 페이지 번호(기본 0)
     * @param size   페이지 크기(기본 20)
     * @return 주변 식당 페이지 엔벨로프
     */
    @GetMapping("/nearby")
    public ApiResponse<PageResponse<PlaceNearbyResponse>> nearby(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "1000") int radius,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(placeService.nearby(lat, lng, radius, page, size));
    }

    /**
     * 식당 상세(기본 정보) 단건 조회.
     *
     * <p><b>요청:</b> {@code GET /api/places/{placeId}}. {@code placeId}는 우리 DB의 장소 PK.
     *
     * <p><b>응답:</b> {@code ApiResponse<PlaceDetailResponse>} — 식별·위치·영업상태 등 기본 정보.
     * 해당 장소가 없으면 {@code PLACE_NOT_FOUND}(404).
     *
     * <p><b>인가:</b> 검색·주변과 동일하게 정식 회원(USER)만 호출할 수 있다.
     *
     * @param placeId 우리 DB의 장소 PK
     * @return 식당 상세 응답
     */
    @GetMapping("/{placeId}")
    public ApiResponse<PlaceDetailResponse> detail(@PathVariable Long placeId) {
        return ApiResponse.success(placeService.getDetail(placeId));
    }
}
