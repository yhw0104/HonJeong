package com.honjeong.place.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.honjeong.review.repository.ReviewPhotoRepository;
import com.honjeong.review.repository.ReviewPhotoRepository.PlacePhotoRow;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.review.repository.ReviewRepository.PlaceReviewStatRow;

/**
 * 장소(식당) 도메인 서비스 — 단건 조회·이름 검색·주변 반경 조회(ACTIVE 혼밥러 수 오버레이).
 *
 * <p>사용 Controller: PlaceController. 그 외 CheckInService·FavoriteService·ReviewService가
 * getById로 사용한다.
 *
 * <p>세 가지 책임을 가진다.
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

    // 주변 목록 카드에 붙일 식당당 대표 사진(리뷰 사진) 최대 장수.
    static final int MAX_PHOTOS_PER_PLACE = 5;

    private final PlaceRepository placeRepository;
    private final CheckInRepository checkInRepository;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final ReviewRepository reviewRepository;

    public PlaceService(PlaceRepository placeRepository, CheckInRepository checkInRepository,
            ReviewPhotoRepository reviewPhotoRepository, ReviewRepository reviewRepository) {
        this.placeRepository = placeRepository;
        this.checkInRepository = checkInRepository;
        this.reviewPhotoRepository = reviewPhotoRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * 내부 placeId로 장소 엔티티를 조회한다. 없으면 {@link ErrorCode#PLACE_NOT_FOUND}(404)를 던진다.
     *
     * <p>다른 도메인 서비스에서 placeId 검증용으로도 쓴다.
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
     * 내부 placeId로 장소 상세(기본 정보)를 조회해 응답 DTO로 반환한다.
     *
     * <p>없으면 {@link ErrorCode#PLACE_NOT_FOUND}(404)를 던진다.
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
     * 검색어로 우리 DB(영업 중인 장소)를 이름 부분일치 조회해 페이지 엔벨로프로 반환한다.
     *
     * <p><b>좌표를 주면 내 위치 기준</b>으로 반경 안을 거리순 정렬해 돌려주고, 좌표가 없으면
     * 예전처럼 전국을 이름순으로 돌려준다. 좌표를 받기 전에는 "국밥"을 치면 부산 국밥집이 서울
     * 국밥집보다 위에 뜰 수 있었다.
     *
     * <p>★<b>반경 안이 비면 전국 이름 검색으로 떨어진다.</b> 반경만 두면 "멀리 있는 가게를 이름으로
     * 찾기"가 막히는데, 그건 좌표를 받기 전에는 되던 일이다. 기능을 더하면서 되던 것을 못 하게
     * 만들지 않으려는 장치다. 떨어진 경로에서도 좌표를 알고 있으므로 거리는 채워 준다(정렬만 이름순).
     *
     * @param query  검색어(공백 불가, trim 적용됨)
     * @param lat    내 위도(null이면 위치 기준 검색을 하지 않는다)
     * @param lng    내 경도(null이면 위치 기준 검색을 하지 않는다)
     * @param radius 반경(m, 1~{@link #MAX_RADIUS} 클램프). lat/lng가 없으면 무시된다.
     * @param page   0-base 페이지 번호(0 이상)
     * @param size   페이지 크기(1 이상, {@link #MAX_SIZE} 초과 시 클램프)
     * @return content/page/size/totalElements를 담은 페이지 엔벨로프
     * @throws BusinessException 검색어가 공백이거나 page가 범위를 벗어나거나 size가 1 미만이면
     *                           {@link ErrorCode#INVALID_INPUT}
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceSearchResponse> search(String query, Double lat, Double lng, int radius,
            int page, int size) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검색어를 입력해주세요.");
        }
        // 상한을 두는 이유는 아래 page * clampedSize다 — 둘 다 int라 큰 page에서 곱이 넘쳐
        // 음수가 되고, subList가 IndexOutOfBounds로 터진다(nearby가 같은 이유로 같은 상한을 쓴다).
        if (page < 0 || page > 1_000_000) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
        }
        if (size < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1 이상이어야 합니다.");
        }
        int clampedSize = Math.min(size, MAX_SIZE);
        String q = query.trim();

        if (lat != null && lng != null) {
            PageResponse<PlaceSearchResponse> nearbyHits = searchNearby(q, lat, lng, radius, page, clampedSize);
            if (nearbyHits != null) {
                return nearbyHits;
            }
            // null = 반경 안에 하나도 없었다 → 아래 전국 검색으로 떨어진다.
        }

        // ★정렬을 name이 아니라 id로 한다. name 정렬은 LIMIT이 조기 종료를 못 하게 만든다 —
        //   20건만 필요해도 조건에 맞는 행을 전부 찾아 정렬한 뒤 앞의 20건을 잘라야 하기 때문이다.
        //   '김밥'은 매칭이 10,135건이라 그 정렬 하나가 655,163행 전체 스캔이 된다(실측 182.6ms).
        //   id 정렬이면 옵티마이저가 기본키 인덱스를 id 순으로 훑다가 20건을 채우는 즉시 멈출 수
        //   있어 3.8ms로 끝난다. 매칭이 희귀한 검색어('파스타집' 17건)에서는 그 계획이 불리해지지만,
        //   그때는 옵티마이저가 알아서 trigram 인덱스로 돌아간다(실측 0.08ms) — 양쪽 다 빠르다.
        //   ★정렬을 아예 빼면 0.31ms로 더 빠르지만 그러면 순서가 보장되지 않아 페이지를 넘길 때
        //   같은 가게가 다시 나오거나 건너뛸 수 있다. id 정렬은 그 안정성을 지키면서 48배를 번다.
        //
        // 한 건 더 요청해서 다음 페이지 유무를 판단한다(카운트 쿼리를 없앤 대가로 치르는 비용이
        // 행 하나뿐이다). PageResponse.ofSlice 주석 참고.
        List<Place> rows = placeRepository.searchOpenByName(
                q, PageRequest.of(page, clampedSize + 1, Sort.by("id")));
        boolean hasNext = rows.size() > clampedSize;
        List<PlaceSearchResponse> content = (hasNext ? rows.subList(0, clampedSize) : rows).stream()
                .map(p -> PlaceSearchResponse.from(p, distanceOrNull(lat, lng, p)))
                .toList();
        return PageResponse.ofSlice(content, page, clampedSize, hasNext);
    }

    /**
     * 반경 안을 거리순으로 검색한다. {@code nearby}와 같은 방식이다 — 바운딩박스로 1차 필터한 뒤
     * Haversine으로 원형 보정·정렬하고 메모리에서 페이지를 자른다(DB가 거리를 못 계산한다).
     *
     * @return 반경 안에 하나도 없으면 <b>null</b>(호출자가 전국 검색으로 떨어뜨린다). 빈 페이지를
     *         돌려주면 "결과 없음"과 구분되지 않아 그 판단을 호출자가 할 수 없다.
     */
    private PageResponse<PlaceSearchResponse> searchNearby(String q, double lat, double lng, int radius,
            int page, int clampedSize) {
        int max = Math.min(Math.max(radius, 1), MAX_RADIUS);
        int needed = (page + 1) * clampedSize;   // 이 페이지를 채우는 데 필요한 최소 건수

        record PlaceDistance(Place place, double meters) {}

        // ★좁은 반경부터 시작해 결과가 충분해질 때까지만 넓힌다.
        //
        //   왜 필요한가: 비용이 반경의 "면적"에 비례한다. 강남역에서 '김밥'을 찾을 때
        //   10km 박스는 좌표 인덱스로 77,067행을 꺼내 76,091행을 이름으로 버리고 976행만 남긴다
        //   (실측 23.8ms). 같은 검색이 1km면 2,856행만 꺼내면 되고 1.2ms다 — 20배 차이다.
        //   이름 조건이 좌표 인덱스에 없어서, 넓게 잡을수록 "꺼냈다가 버리는 행"만 늘어난다.
        //
        //   ★왜 결과가 같은가: 어차피 거리순으로 정렬해 앞에서 N건만 쓴다. 1km 안에 이미 N건이
        //   있으면 그보다 먼 가게는 절대 그 N건 안에 못 든다 — 넓혀서 더 찾아봐야 순위가 바뀌지
        //   않는다. 그래서 조기 종료가 근사가 아니라 **정확히 같은 답**이다.
        //
        //   ★검색어 희소성으로는 못 줄인다: 이름 인덱스를 먼저 타게 해봤지만 흔한 검색어에서는
        //   오히려 느렸고(76.8ms), 이름+좌표 복합 GIN 인덱스도 더 느렸다(36.8ms). 남은 레버가
        //   범위뿐이었다.
        List<PlaceDistance> within = List.of();
        for (int r : radiusLadder(max)) {
            double dLat = r / METERS_PER_DEGREE_LAT;
            double dLng = r / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));

            List<Place> inBox = placeRepository.searchOpenByNameWithinBounds(
                    q, lat - dLat, lat + dLat, lng - dLng, lng + dLng);

            final int rr = r;
            within = inBox.stream()
                    .map(p -> new PlaceDistance(p, haversine(lat, lng, p.getLatitude(), p.getLongitude())))
                    .filter(pd -> pd.meters() <= rr) // 박스는 사각형이라 모서리가 반경 밖으로 삐져나온다
                    .sorted(Comparator.comparingDouble(PlaceDistance::meters)
                            .thenComparingLong(pd -> pd.place().getId())) // 동거리 동점은 id로 안정 정렬
                    .toList();

            if (within.size() >= needed) {
                break; // 이 페이지를 채우고도 남는다 → 더 넓혀도 답이 안 바뀐다
            }
        }

        if (within.isEmpty()) {
            return null;
        }
        int from = Math.min(page * clampedSize, within.size());
        int to = Math.min(from + clampedSize, within.size());
        List<PlaceSearchResponse> content = within.subList(from, to).stream()
                .map(pd -> PlaceSearchResponse.from(pd.place(), Math.round(pd.meters())))
                .toList();
        return PageResponse.of(content, page, clampedSize, within.size());
    }

    /**
     * 넓혀 갈 반경 목록을 만든다. 요청 반경(max)을 넘지 않고, 마지막은 반드시 max다.
     *
     * <p>1km → 3km → max 세 단계로 둔 이유: 강남역 기준 실측에서 1km 2,856행(1.2ms),
     * 3km 13,866행(3.9ms), 10km 77,067행(23.8ms)이었다. 단계를 더 잘게 쪼개면 헛도는 쿼리가
     * 늘고, 성기게 두면 조기 종료 효과가 준다. 대부분의 검색은 1km에서 끝난다.
     */
    private static int[] radiusLadder(int max) {
        if (max <= 1_000) {
            return new int[] { max };
        }
        if (max <= 3_000) {
            return new int[] { 1_000, max };
        }
        return new int[] { 1_000, 3_000, max };
    }

    /** 좌표를 모르면 null, 알면 거리(m). 0으로 채우지 않는다 — "0m"와 "모름"은 다른 사실이다. */
    private static Long distanceOrNull(Double lat, Double lng, Place p) {
        if (lat == null || lng == null) {
            return null;
        }
        return Math.round(haversine(lat, lng, p.getLatitude(), p.getLongitude()));
    }

    /**
     * 요청 위치에서 반경 {@code radius}m 이내의 영업 중인 장소를 거리순으로 반환하고
     * ACTIVE 혼밥러 수와 SEEKING(모집중) 수를 오버레이한다.
     *
     * <p>바운딩박스 1차 필터 후 Haversine으로 원형 반경 보정·거리 정렬하고, 혼밥러 수는
     * {@link CheckInRepository#countActiveByPlaceIds}로, 모집중 수는
     * {@link CheckInRepository#countSeekingByPlaceIds}로 각각 일괄 조회해 오버레이한다(없으면 0).
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
        Map<Long, Long> seekingCounts;
        if (within.isEmpty()) {
            counts = Map.of();
            seekingCounts = Map.of();
        } else {
            List<Long> placeIds = within.stream().map(pd -> pd.place().getId()).toList();
            counts = checkInRepository.countActiveByPlaceIds(placeIds).stream()
                    .collect(Collectors.toMap(PlaceActiveCount::placeId, PlaceActiveCount::activeCount));
            seekingCounts = checkInRepository.countSeekingByPlaceIds(placeIds).stream()
                    .collect(Collectors.toMap(PlaceActiveCount::placeId, PlaceActiveCount::activeCount));
        }

        long total = within.size();
        int from = Math.min(page * clampedSize, within.size());
        int to = Math.min(from + clampedSize, within.size());
        List<PlaceDistance> pageSlice = within.subList(from, to);

        // 현재 페이지 식당들의 대표 사진(리뷰 사진)·리뷰 집계(수·별점 평균)만 배치 조회해 오버레이한다.
        List<Long> pagePlaceIds = pageSlice.stream().map(pd -> pd.place().getId()).toList();
        Map<Long, List<String>> photosByPlace = loadPhotos(pagePlaceIds);
        Map<Long, PlaceReviewStatRow> statsByPlace = loadReviewStats(pagePlaceIds);

        List<PlaceNearbyResponse> content = pageSlice.stream()
                .map(pd -> {
                    Long id = pd.place().getId();
                    PlaceReviewStatRow stat = statsByPlace.get(id);
                    return new PlaceNearbyResponse(
                            id, pd.place().getName(), pd.place().getCategory(), pd.place().getRoadAddress(),
                            pd.place().getLatitude(), pd.place().getLongitude(),
                            Math.round(pd.meters()),
                            counts.getOrDefault(id, 0L),
                            seekingCounts.getOrDefault(id, 0L),
                            photosByPlace.getOrDefault(id, List.of()),
                            stat != null ? stat.getReviewCount() : 0L,
                            stat != null ? round1(stat.getAvgTaste()) : null,
                            stat != null ? round1(stat.getAvgSolo()) : null);
                })
                .toList();

        return PageResponse.of(content, page, clampedSize, total);
    }

    /**
     * 주어진 식당들의 리뷰 사진을 식당별 최대 {@link #MAX_PHOTOS_PER_PLACE}장(최신순)으로 모아 맵으로 반환한다.
     *
     * <p>빈 목록이면 {@code IN ()} 쿼리를 피하려 곧바로 빈 맵을 반환한다.
     *
     * @param placeIds 사진을 붙일 식당 ID 목록(현재 페이지 분량)
     * @return placeId → 사진 URL 목록(최대 N장). 사진 없는 식당은 키가 없다.
     */
    private Map<Long, List<String>> loadPhotos(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> byPlace = new HashMap<>();
        for (PlacePhotoRow row : reviewPhotoRepository.findByPlaceIdsFlattened(placeIds)) {
            List<String> photos = byPlace.computeIfAbsent(row.getPlaceId(), k -> new ArrayList<>());
            if (photos.size() < MAX_PHOTOS_PER_PLACE) {
                photos.add(row.getImageUrl());
            }
        }
        return byPlace;
    }

    /**
     * 주어진 식당들의 리뷰 집계(수·별점 평균 2종)를 식당별로 모아 맵으로 반환한다.
     *
     * <p>빈 목록이면 {@code IN ()} 쿼리를 피하려 곧바로 빈 맵을 반환한다. 리뷰 없는 식당은 키가 없다.
     *
     * @param placeIds 집계할 식당 ID 목록(현재 페이지 분량)
     * @return placeId → 리뷰 집계 행(개수·맛평균·혼밥평균)
     */
    private Map<Long, PlaceReviewStatRow> loadReviewStats(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.summarizeByPlaceIds(placeIds).stream()
                .collect(Collectors.toMap(PlaceReviewStatRow::getPlaceId, row -> row));
    }

    /** 별점 평균을 소수 1자리로 반올림(리뷰 없으면 null 유지). */
    private static Double round1(Double v) {
        return v == null ? null : Math.round(v * 10) / 10.0;
    }

    /** Haversine 공식으로 두 위경도 좌표 간 거리를 미터 단위로 반환한다. */
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
