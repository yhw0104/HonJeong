package com.honjeong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.checkin.dto.PlaceActiveCount;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.common.PageResponse;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.dto.PlaceNearbyResponse;
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.repository.PlaceRepository;

/**
 * PlaceService 단위 테스트(순수 Mockito).
 *
 * <p>Task 6: 검색이 Kakao 클라이언트에서 우리 DB(PlaceRepository)로 재작성됐다.
 * 검증 목적:
 * <ul>
 *   <li>빈 검색어는 INVALID_INPUT으로 거부된다.</li>
 *   <li>searchOpenByName 결과가 PlaceSearchResponse 페이지 엔벨로프로 올바르게 매핑된다.</li>
 *   <li>upsert(findOrCreateByExternalId)는 external_id 존재 시 재사용, 없으면 생성한다(Task 8까지 잔존).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    PlaceRepository placeRepository;

    @Mock
    CheckInRepository checkInRepository;   // Task 7 nearby용 — 이 태스크에서는 미사용

    @InjectMocks
    PlaceService service;

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
    @DisplayName("findOrCreateByExternalId: external_id가 이미 있으면 기존 장소를 반환하고 저장하지 않는다")
    void upsertReturnsExistingWhenFound() {
        Place existing = Place.of("kakao-1", "기존식당", "주소", 37.5, 127.0, "한식");
        when(placeRepository.findByExternalId("kakao-1")).thenReturn(Optional.of(existing));

        Place result = service.findOrCreateByExternalId(
                new PlaceUpsertCommand("kakao-1", "다른이름", "다른주소", 37.6, 127.1, "일식"));

        assertThat(result).isSameAs(existing);
        verify(placeRepository, never()).save(any());
    }

    @Test
    @DisplayName("findOrCreateByExternalId: external_id가 없으면 command 값으로 새 장소를 생성·저장한다")
    void upsertCreatesWhenAbsent() {
        when(placeRepository.findByExternalId("kakao-2")).thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        Place result = service.findOrCreateByExternalId(
                new PlaceUpsertCommand("kakao-2", "새식당", "새주소", 37.7, 127.2, "분식"));

        verify(placeRepository).save(any(Place.class));
        assertThat(result.getExternalId()).isEqualTo("kakao-2");
        assertThat(result.getName()).isEqualTo("새식당");
        assertThat(result.getLatitude()).isEqualTo(37.7);
        assertThat(result.getCategory()).isEqualTo("분식");
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
    @DisplayName("lat/lng 누락이면 INVALID_INPUT")
    void nearbyMissingCoord() {
        assertThatThrownBy(() -> service.nearby(null, 127.0, 1000, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
