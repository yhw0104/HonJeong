package com.honjeong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

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
 * PlaceService 단위 테스트(순수 Mockito).
 *
 * <p>검증 목적:
 * <ul>
 *   <li>getById: 존재하면 반환, 없으면 PLACE_NOT_FOUND(404).</li>
 *   <li>빈 검색어는 INVALID_INPUT으로 거부된다.</li>
 *   <li>searchOpenByName 결과가 PlaceSearchResponse 페이지 엔벨로프로 올바르게 매핑된다.</li>
 *   <li>nearby: 반경 내 영업 장소를 거리순으로 반환하고 혼밥러 수를 오버레이한다.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    PlaceRepository placeRepository;

    @Mock
    CheckInRepository checkInRepository;

    @Mock
    ReviewPhotoRepository reviewPhotoRepository;

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    PlaceService service;

    /** 프로젝션(PlacePhotoRow) 한 행을 만드는 테스트 헬퍼. */
    private static PlacePhotoRow photoRow(long placeId, String url) {
        return new PlacePhotoRow() {
            @Override public Long getPlaceId() { return placeId; }
            @Override public String getImageUrl() { return url; }
        };
    }

    /** 프로젝션(PlaceReviewStatRow) 한 행을 만드는 테스트 헬퍼. */
    private static PlaceReviewStatRow statRow(long placeId, long count, Double avgTaste, Double avgSolo) {
        return new PlaceReviewStatRow() {
            @Override public Long getPlaceId() { return placeId; }
            @Override public long getReviewCount() { return count; }
            @Override public Double getAvgTaste() { return avgTaste; }
            @Override public Double getAvgSolo() { return avgSolo; }
        };
    }

    @Test
    @DisplayName("getById: 존재하는 placeId면 장소 엔티티를 반환한다")
    void getById_found() {
        Place p = Place.ofPublicData("M1", "혼밥식당", "한식", "서울", "서울 도로명", 37.5, 127.0, null, "영업");
        ReflectionTestUtils.setField(p, "id", 1L);
        when(placeRepository.findById(1L)).thenReturn(Optional.of(p));

        Place result = service.getById(1L);

        assertThat(result).isSameAs(p);
    }

    @Test
    @DisplayName("getById: 존재하지 않는 placeId면 PLACE_NOT_FOUND(404)")
    void getById_notFound() {
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("getDetail: 존재하는 placeId면 상세 응답으로 매핑한다")
    void getDetail_found() {
        Place p = Place.ofPublicData("M1", "혼밥식당", "한식", "서울 지번", "서울 도로명", 37.5, 127.0, "02-123", "영업");
        ReflectionTestUtils.setField(p, "id", 7L);
        when(placeRepository.findById(7L)).thenReturn(Optional.of(p));

        PlaceDetailResponse res = service.getDetail(7L);

        assertThat(res.placeId()).isEqualTo(7L);
        assertThat(res.name()).isEqualTo("혼밥식당");
        assertThat(res.category()).isEqualTo("한식");
        assertThat(res.address()).isEqualTo("서울 지번");
        assertThat(res.roadAddress()).isEqualTo("서울 도로명");
        assertThat(res.latitude()).isEqualTo(37.5);
        assertThat(res.longitude()).isEqualTo(127.0);
        assertThat(res.phone()).isEqualTo("02-123");
        assertThat(res.businessStatus()).isEqualTo("영업");
    }

    @Test
    @DisplayName("getDetail: 존재하지 않는 placeId면 PLACE_NOT_FOUND(404)")
    void getDetail_notFound() {
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    @DisplayName("빈 검색어는 INVALID_INPUT")
    void blankQuery() {
        assertThatThrownBy(() -> service.search("  ", null, null, 1000, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("null 검색어는 INVALID_INPUT")
    void nullQuery() {
        assertThatThrownBy(() -> service.search(null, null, null, 1000, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("page가 음수면 INVALID_INPUT")
    void negativePage() {
        assertThatThrownBy(() -> service.search("김밥", null, null, 1000, -1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("size가 1 미만이면 INVALID_INPUT")
    void zeroSize() {
        assertThatThrownBy(() -> service.search("김밥", null, null, 1000, 0, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("검색어로 우리 DB를 조회해 페이지 엔벨로프로 매핑한다")
    void search() {
        Place p = Place.ofPublicData("M1", "혼밥김밥", "분식", "주소", "도로명", 37.5, 127.0, "02-111", "영업");
        when(placeRepository.searchOpenByName(eq("김밥"), any()))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        var res = service.search("김밥", null, null, 1000, 0, 20);

        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).name()).isEqualTo("혼밥김밥");
        assertThat(res.content().get(0).category()).isEqualTo("분식");
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.size()).isEqualTo(20);
        assertThat(res.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("size가 MAX_SIZE(50)를 넘으면 50으로 클램프하고 응답 size도 50이다")
    void searchClampsSizeToMax() {
        when(placeRepository.searchOpenByName(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        var res = service.search("김밥", null, null, 1000, 0, 999);

        assertThat(res.size()).isEqualTo(50);
    }

    @Test
    @DisplayName("검색어 앞뒤 공백은 trim해서 조회한다")
    void searchTrimsQuery() {
        when(placeRepository.searchOpenByName(eq("김밥"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.search("  김밥  ", null, null, 1000, 0, 20);

        verify(placeRepository).searchOpenByName(eq("김밥"), any());
    }

    // --- 좌표를 준 검색(내 위치 기준) ---
    //
    // 예전에는 검색이 좌표를 아예 받지 않아 이름 가나다순으로만 나왔다. "국밥"을 치면 부산 국밥집이
    // 서울 국밥집보다 위에 뜰 수 있었다. 아래 테스트들이 그 동작으로 되돌아가는 것을 막는다.

    /** 서울 어딘가. 아래 테스트들이 공유하는 기준점. */
    private static final double MY_LAT = 37.5000;
    private static final double MY_LNG = 127.0000;

    private static Place placeAt(String sourceId, String name, long id, double lat, double lng) {
        Place p = Place.ofPublicData(sourceId, name, "분식", "주소", "도로", lat, lng, "02", "영업");
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    @DisplayName("★좌표를 주면 반경 안에서 거리순으로 정렬하고 거리(m)를 채운다")
    void search_withCoords_sortsByDistance() {
        Place near = placeAt("A", "가까운김밥", 1L, MY_LAT, MY_LNG);              // 0m
        Place far = placeAt("B", "먼김밥", 2L, MY_LAT + 0.0050, MY_LNG + 0.0050); // 약 700m
        // 리포지토리는 일부러 먼 것을 먼저 준다 — 정렬이 서비스에서 일어난다는 것을 증명하기 위함이다.
        when(placeRepository.searchOpenByNameWithinBounds(eq("김밥"), anyDouble(), anyDouble(),
                anyDouble(), anyDouble())).thenReturn(List.of(far, near));

        var res = service.search("김밥", MY_LAT, MY_LNG, 1000, 0, 20);

        assertThat(res.content()).extracting(PlaceSearchResponse::placeId).containsExactly(1L, 2L);
        assertThat(res.content().get(0).distanceMeters()).isZero();
        assertThat(res.content().get(1).distanceMeters()).isGreaterThan(500L);
        // 좌표를 줬으면 전국 이름 검색은 타지 않아야 한다(탔다면 정렬 근거가 거리가 아니게 된다).
        verify(placeRepository, never()).searchOpenByName(any(), any());
    }

    @Test
    @DisplayName("좌표를 주면 반경 밖은 제외한다 — 바운딩박스는 사각형이라 모서리가 남는다")
    void search_withCoords_excludesOutsideRadius() {
        Place inside = placeAt("A", "안김밥", 1L, MY_LAT, MY_LNG);
        // 박스(±1000m) 안이지만 대각선 거리는 반경 1000m를 넘는 지점.
        Place corner = placeAt("B", "모서리김밥", 2L, MY_LAT + 0.0089, MY_LNG + 0.0112);
        when(placeRepository.searchOpenByNameWithinBounds(eq("김밥"), anyDouble(), anyDouble(),
                anyDouble(), anyDouble())).thenReturn(List.of(inside, corner));

        var res = service.search("김밥", MY_LAT, MY_LNG, 1000, 0, 20);

        assertThat(res.content()).extracting(PlaceSearchResponse::placeId).containsExactly(1L);
        assertThat(res.totalElements()).isEqualTo(1L);
    }

    /**
     * ★반경을 두면 "멀리 있는 가게를 이름으로 찾기"가 막힌다 — 좌표를 받기 전에는 되던 일이다.
     * 기능을 더하면서 되던 것을 못 하게 만들지 않으려고, 반경 안이 비면 전국 검색으로 떨어진다.
     */
    @Test
    @DisplayName("★반경 안에 결과가 없으면 전국 이름 검색으로 떨어진다 — 멀리 있는 가게를 이름으로 찾던 길을 막지 않는다")
    void search_withCoords_fallsBackToNationwide() {
        Place farAway = placeAt("Z", "부산김밥", 9L, 35.1796, 129.0756); // 부산
        when(placeRepository.searchOpenByNameWithinBounds(eq("김밥"), anyDouble(), anyDouble(),
                anyDouble(), anyDouble())).thenReturn(List.of()); // 반경 안엔 없다
        when(placeRepository.searchOpenByName(eq("김밥"), any()))
                .thenReturn(new PageImpl<>(List.of(farAway), PageRequest.of(0, 20), 1));

        var res = service.search("김밥", MY_LAT, MY_LNG, 1000, 0, 20);

        assertThat(res.content()).extracting(PlaceSearchResponse::placeId).containsExactly(9L);
        // 좌표를 알고 있으므로 떨어진 경로에서도 거리는 채워 준다(정렬만 이름순일 뿐이다).
        assertThat(res.content().get(0).distanceMeters()).isGreaterThan(300_000L); // 서울-부산 300km+
    }

    @Test
    @DisplayName("좌표가 없으면 기존대로 전국 이름순이고 거리는 null이다")
    void search_withoutCoords_keepsNameOrderAndNullDistance() {
        Place p = placeAt("A", "혼밥김밥", 1L, 37.5, 127.0);
        when(placeRepository.searchOpenByName(eq("김밥"), any()))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        var res = service.search("김밥", null, null, 1000, 0, 20);

        assertThat(res.content().get(0).distanceMeters()).isNull();
        verify(placeRepository, never()).searchOpenByNameWithinBounds(any(), anyDouble(), anyDouble(),
                anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("주변 식당을 거리순으로 반환하고 혼밥러수를 오버레이한다")
    void nearby() {
        Place a = Place.ofPublicData("A", "가까운집", "한식", "주소", "도로", 37.5000, 127.0000, "02", "영업");
        Place b = Place.ofPublicData("B", "먼집", "분식", "주소", "도로", 37.5050, 127.0050, "02", "영업");
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 2L);
        when(placeRepository.findOpenWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(b, a)); // 리포지토리는 b를 먼저 반환하지만
        when(checkInRepository.countActiveByPlaceIds(anyList()))
                .thenReturn(List.of(new PlaceActiveCount(1L, 3L)));

        PageResponse<PlaceNearbyResponse> res = service.nearby(37.5000, 127.0000, 1000, 0, 20);

        assertThat(res.content().get(0).placeId()).isEqualTo(1L);   // 가까운 a가 먼저
        assertThat(res.content().get(0).activeCount()).isEqualTo(3); // 오버레이 확인
        assertThat(res.content().get(1).activeCount()).isEqualTo(0); // 오버레이 없는 b는 0
    }

    @Test
    @DisplayName("주변 식당을 거리순으로 반환하고 모집중(SEEKING) 수를 오버레이한다")
    void nearby_overlaysSeekingCount() {
        Place a = Place.ofPublicData("A", "가까운집", "한식", "주소", "도로", 37.5000, 127.0000, "02", "영업");
        Place b = Place.ofPublicData("B", "먼집", "분식", "주소", "도로", 37.5050, 127.0050, "02", "영업");
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 2L);
        when(placeRepository.findOpenWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(b, a)); // 리포지토리는 b를 먼저 반환하지만
        when(checkInRepository.countActiveByPlaceIds(anyList()))
                .thenReturn(List.of());
        when(checkInRepository.countSeekingByPlaceIds(anyList()))
                .thenReturn(List.of(new PlaceActiveCount(1L, 2L)));

        PageResponse<PlaceNearbyResponse> res = service.nearby(37.5000, 127.0000, 1000, 0, 20);

        assertThat(res.content().get(0).placeId()).isEqualTo(1L);     // 가까운 a가 먼저
        assertThat(res.content().get(0).seekingCount()).isEqualTo(2L); // 오버레이 확인
        assertThat(res.content().get(1).seekingCount()).isEqualTo(0L); // 오버레이 없는 b는 0
    }

    @Test
    @DisplayName("주변 식당에 리뷰 사진을 식당당 최대 N장(최신순)으로 오버레이하고, 없으면 빈 배열")
    void nearby_overlaysPhotos() {
        Place a = Place.ofPublicData("A", "가까운집", "한식", "주소", "도로", 37.5000, 127.0000, "02", "영업");
        Place b = Place.ofPublicData("B", "먼집", "분식", "주소", "도로", 37.5050, 127.0050, "02", "영업");
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 2L);
        when(placeRepository.findOpenWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(a, b));
        // a(1L)에 6장 → 상한 5장으로 절단, b(2L)는 사진 없음
        when(reviewPhotoRepository.findByPlaceIdsFlattened(anyList()))
                .thenReturn(List.of(
                        photoRow(1L, "u1"), photoRow(1L, "u2"), photoRow(1L, "u3"),
                        photoRow(1L, "u4"), photoRow(1L, "u5"), photoRow(1L, "u6")));

        PageResponse<PlaceNearbyResponse> res = service.nearby(37.5000, 127.0000, 1000, 0, 20);

        assertThat(res.content().get(0).placeId()).isEqualTo(1L);
        assertThat(res.content().get(0).photoUrls()).containsExactly("u1", "u2", "u3", "u4", "u5"); // 최대 5장
        assertThat(res.content().get(1).photoUrls()).isEmpty(); // 사진 없는 식당은 빈 배열
    }

    @Test
    @DisplayName("주변 식당에 리뷰 수·별점 평균(소수1자리 반올림)을 오버레이하고, 리뷰 없으면 0/null")
    void nearby_overlaysReviewStats() {
        Place a = Place.ofPublicData("A", "가까운집", "한식", "주소", "도로", 37.5000, 127.0000, "02", "영업");
        Place b = Place.ofPublicData("B", "먼집", "분식", "주소", "도로", 37.5050, 127.0050, "02", "영업");
        ReflectionTestUtils.setField(a, "id", 1L);
        ReflectionTestUtils.setField(b, "id", 2L);
        when(placeRepository.findOpenWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(a, b));
        // a(1L)만 리뷰 3건(맛 4.34→4.3, 혼밥 3.66→3.7), b(2L)는 리뷰 없음
        when(reviewRepository.summarizeByPlaceIds(anyList()))
                .thenReturn(List.of(statRow(1L, 3L, 4.34, 3.66)));

        PageResponse<PlaceNearbyResponse> res = service.nearby(37.5000, 127.0000, 1000, 0, 20);

        assertThat(res.content().get(0).reviewCount()).isEqualTo(3L);
        assertThat(res.content().get(0).avgTasteRating()).isEqualTo(4.3);        // 소수1자리 반올림
        assertThat(res.content().get(0).avgSoloFriendlyRating()).isEqualTo(3.7);
        assertThat(res.content().get(1).reviewCount()).isEqualTo(0L);            // 리뷰 없는 식당
        assertThat(res.content().get(1).avgTasteRating()).isNull();
    }

    @Test
    @DisplayName("lat/lng 누락이면 INVALID_INPUT")
    void nearbyMissingCoord() {
        assertThatThrownBy(() -> service.nearby(null, 127.0, 1000, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("바운딩박스 결과가 없으면 countActiveByPlaceIds를 호출하지 않는다")
    void nearby_emptyBox_noCountQuery() {
        when(placeRepository.findOpenWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(java.util.List.of());
        var res = service.nearby(37.5, 127.0, 1000, 0, 20);
        assertThat(res.content()).isEmpty();
        verify(checkInRepository, never()).countActiveByPlaceIds(anyList());
        verify(checkInRepository, never()).countSeekingByPlaceIds(anyList());
    }
}
