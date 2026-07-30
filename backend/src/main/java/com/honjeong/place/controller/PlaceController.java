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
 * 장소(식당) 검색·주변 조회·상세 조회 컨트롤러.
 *
 * <p>기본 경로: /api/places
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
     * 가게를 검색한다 — 우리 DB에서 영업 중인 장소의 이름 부분일치로 찾는다.
     *
     * <p>사용 화면: 식당 검색(PlaceSearch) — 검색어 입력 시 결과 목록.
     *
     * <p>{@code query}는 필수라 누락 시 400({@code INVALID_INPUT})이다.
     * 응답은 {@code content}/{@code page}/{@code size}/{@code totalElements} 구조다.
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
     * <p>사용 화면: 홈 지도(MapHome)의 이 지역 재검색, 같이먹기 피드(TogetherFeed)의 주변 식당 목록,
     * 식당 상세(RestaurantDetail)의 주변 지도 섹션.
     *
     * <p>{@code lat}/{@code lng}는 필수라 누락 시 400({@code INVALID_INPUT})이다.
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
     * 식당 상세(기본 정보)를 단건 조회한다 — 식별·위치·연락처·영업상태 등.
     *
     * <p>사용 화면: 식당 상세(RestaurantDetail)의 상단 기본 정보(이름·카테고리·주소·전화).
     *
     * <p>해당 장소가 없으면 {@code PLACE_NOT_FOUND}(404)다.
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
