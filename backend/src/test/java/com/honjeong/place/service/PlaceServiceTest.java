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

    @InjectMocks
    PlaceService service;

    /** 프로젝션(PlacePhotoRow) 한 행을 만드는 테스트 헬퍼. */
    private static PlacePhotoRow photoRow(long placeId, String url) {
        return new PlacePhotoRow() {
            @Override public Long getPlaceId() { return placeId; }
            @Override public String getImageUrl() { return url; }
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
        assertThatThrownBy(() -> service.search("  ", 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("null 검색어는 INVALID_INPUT")
    void nullQuery() {
        assertThatThrownBy(() -> service.search(null, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("page가 음수면 INVALID_INPUT")
    void negativePage() {
        assertThatThrownBy(() -> service.search("김밥", -1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("size가 1 미만이면 INVALID_INPUT")
    void zeroSize() {
        assertThatThrownBy(() -> service.search("김밥", 0, 0))
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

        var res = service.search("김밥", 0, 20);

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

        var res = service.search("김밥", 0, 999);

        assertThat(res.size()).isEqualTo(50);
    }

    @Test
    @DisplayName("검색어 앞뒤 공백은 trim해서 조회한다")
    void searchTrimsQuery() {
        when(placeRepository.searchOpenByName(eq("김밥"), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.search("  김밥  ", 0, 20);

        verify(placeRepository).searchOpenByName(eq("김밥"), any());
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
