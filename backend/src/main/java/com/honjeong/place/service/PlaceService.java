package com.honjeong.place.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.dto.PlaceActiveCount;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.common.PageResponse;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.dto.PlaceDetailResponse;
import com.honjeong.place.dto.PlaceNearbyResponse;
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.repository.PlaceRepository;

/**
 * 1. 기능: 장소(식당) 단건 조회·이름 검색·주변 반경 조회(ACTIVE 혼밥러 수 오버레이) 비즈니스 로직
 * 2. 사용 Controller: PlaceController (그 외 CheckInService·FavoriteService·ReviewService가 getById로 사용)
 *
 * <p>[기존 주석] 장소 도메인 서비스. 세 가지 책임을 가진다.
 *
 * <ul>
 *   <li><b>단건 조회</b> — {@link #getById}로 placeId를 내부 엔티티로 변환한다(없으면 PLACE_NOT_FOUND).</li>
 *   <li><b>검색(Task 6~)</b> — 우리 DB({@link PlaceRepository#searchOpenByName})에서 영업 중인 장소를
 *       이름 부분일치로 조회해 페이지 엔벨로프로 반환한다.</li>
 *   <li><b>주변 장소(Task 7~)</b> — 반경 내 영업 장소를 거리순으로 반환하고 혼밥러 수를 오버레이한다.</li>
 * </ul>
 */
@Service
public class PlaceService {

    // 한 번에 가져올 수 있는 검색 결과 상한. 이보다 큰 size 요청은 이 값으로 줄인다(방어적 클램프).
    static final int MAX_SIZE = 50;

    // nearby 반경 상한(m). 이보다 큰 radius는 이 값으로 클램프한다.
    static final int MAX_RADIUS = 10_000;

    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private final PlaceRepository placeRepository;
    private final CheckInRepository checkInRepository;

    public PlaceService(PlaceRepository placeRepository, CheckInRepository checkInRepository) {
        this.placeRepository = placeRepository;
        this.checkInRepository = checkInRepository;
    }

    /**
     * 기능: 내부 placeId로 장소 엔티티 단건 조회(다른 도메인 서비스에서 placeId 검증 겸용)
     * Request: placeId — 우리 DB의 장소 PK
     * Response: Place — 해당 장소 엔티티 (없으면 PLACE_NOT_FOUND 예외)
     *
     * <p>[기존 주석] 내부 placeId로 장소 엔티티를 조회한다. 없으면 {@link ErrorCode#PLACE_NOT_FOUND}(404)를 던진다.
     *
     * @param placeId 우리 DB의 장소 PK
     * @return 해당 장소 엔티티
     * @throws BusinessException 장소가 없으면 PLACE_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public Place getById(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }

    /**
     * 기능: 장소 상세(기본 정보)를 조회해 응답 DTO로 변환
     * Request: placeId — 우리 DB의 장소 PK
     * Response: PlaceDetailResponse — 식당 상세 기본 정보 (없으면 PLACE_NOT_FOUND 예외)
     *
     * <p>[기존 주석] 내부 placeId로 장소 상세(기본 정보)를 조회해 응답 DTO로 반환한다.
     *
     * @param placeId 우리 DB의 장소 PK
     * @return 식당 상세 응답 DTO
     * @throws BusinessException 장소가 없으면 PLACE_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public PlaceDetailResponse getDetail(Long placeId) {
        return PlaceDetailResponse.from(getById(placeId));
    }

    /**
     * 기능: 검색어로 영업 중인 장소를 이름 부분일치 조회(이름순 페이지)
     * Request: query — 검색어(공백 불가), page — 0-base 페이지 번호, size — 페이지 크기(최대 50 클램프)
     * Response: {@code PageResponse<PlaceSearchResponse>} — 검색 결과 페이지 엔벨로프
     *
     * <p>[기존 주석] 검색어로 우리 DB(영업 중인 장소)를 조회해 페이지 엔벨로프로 반환한다.
     *
     * @param query 검색어(공백 불가, trim 적용됨)
     * @param page  0-base 페이지 번호(0 이상)
     * @param size  페이지 크기(1 이상, {@link #MAX_SIZE} 초과 시 클램프)
     * @return content/page/size/totalElements를 담은 페이지 엔벨로프
     * @throws BusinessException 검색어가 공백이거나 page가 음수, size가 1 미만이면 {@link ErrorCode#INVALID_INPUT}
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceSearchResponse> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검색어를 입력해주세요.");
        }
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
        }
        if (size < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1 이상이어야 합니다.");
        }
        int clampedSize = Math.min(size, MAX_SIZE);

        Page<Place> result = placeRepository.searchOpenByName(
                query.trim(), PageRequest.of(page, clampedSize, Sort.by("name")));
        List<PlaceSearchResponse> content = result.getContent().stream()
                .map(PlaceSearchResponse::from)
                .toList();
        return PageResponse.of(content, page, clampedSize, result.getTotalElements());
    }

    /**
     * 기능: 반경 내 영업 중인 장소를 거리순으로 조회하고 장소별 ACTIVE 혼밥러 수를 오버레이
     * Request: lat — 요청 위도(필수), lng — 요청 경도(필수), radius — 반경(m, 1~10000 클램프), page — 0-base 페이지 번호, size — 페이지 크기(최대 50 클램프)
     * Response: {@code PageResponse<PlaceNearbyResponse>} — 거리순 주변 장소 페이지 엔벨로프(거리·혼밥러 수 포함)
     *
     * <p>[기존 주석] 요청 위치에서 반경 {@code radius}m 이내의 영업 중인 장소를 거리순으로 반환하고 ACTIVE 혼밥러 수를 오버레이한다.
     *
     * <p>바운딩박스 1차 필터 후 Haversine으로 원형 반경 보정·거리 정렬하고, 혼밥러 수는
     * {@link CheckInRepository#countActiveByPlaceIds}로 일괄 조회해 오버레이한다(없으면 0).
     *
     * @param lat    요청 위도(필수 — null이면 INVALID_INPUT)
     * @param lng    요청 경도(필수 — null이면 INVALID_INPUT)
     * @param radius 반경(m, 1~MAX_RADIUS 클램프)
     * @param page   0-base 페이지 번호
     * @param size   페이지 크기(MAX_SIZE 클램프)
     * @return 거리순 주변 장소 페이지 엔벨로프
     * @throws BusinessException lat 또는 lng가 null이거나 page/size가 올바르지 않으면 INVALID_INPUT
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceNearbyResponse> nearby(Double lat, Double lng, int radius, int page, int size) {
        if (lat == null || lng == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다.");
        }
        if (page < 0 || size < 1 || page > 1_000_000) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page/size가 올바르지 않습니다.");
        }
        int clampedSize = Math.min(size, MAX_SIZE);
        int r = Math.min(Math.max(radius, 1), MAX_RADIUS);

        double dLat = r / METERS_PER_DEGREE_LAT;
        double dLng = r / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));

        List<Place> inBox = placeRepository.findOpenWithinBounds(
                lat - dLat, lat + dLat, lng - dLng, lng + dLng);

        record PlaceDistance(Place place, double meters) {}

        List<PlaceDistance> within = inBox.stream()
                .map(p -> new PlaceDistance(p, haversine(lat, lng, p.getLatitude(), p.getLongitude())))
                .filter(pd -> pd.meters() <= r)
                .sorted(Comparator.comparingDouble(PlaceDistance::meters)
                        .thenComparingLong(pd -> pd.place().getId()))
                .toList();

        // 빈 리스트로 IN () 쿼리를 날리면 일부 JPQL 구현체에서 오류가 발생하므로 단락 처리한다.
        Map<Long, Long> counts;
        if (within.isEmpty()) {
            counts = Map.of();
        } else {
            List<Long> placeIds = within.stream().map(pd -> pd.place().getId()).toList();
            counts = checkInRepository.countActiveByPlaceIds(placeIds).stream()
                    .collect(Collectors.toMap(PlaceActiveCount::placeId, PlaceActiveCount::activeCount));
        }

        long total = within.size();
        int from = Math.min(page * clampedSize, within.size());
        int to = Math.min(from + clampedSize, within.size());

        List<PlaceNearbyResponse> content = within.subList(from, to).stream()
                .map(pd -> new PlaceNearbyResponse(
                        pd.place().getId(), pd.place().getName(), pd.place().getCategory(), pd.place().getRoadAddress(),
                        pd.place().getLatitude(), pd.place().getLongitude(),
                        Math.round(pd.meters()),
                        counts.getOrDefault(pd.place().getId(), 0L)))
                .toList();

        return PageResponse.of(content, page, clampedSize, total);
    }

    /**
     * 기능: 두 위경도 좌표 간 거리를 Haversine 공식으로 계산(m)
     *
     * <p>[기존 주석] Haversine 공식으로 두 위경도 좌표 간 거리를 미터 단위로 반환한다.
     */
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
