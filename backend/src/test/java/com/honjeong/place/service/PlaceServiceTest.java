package com.honjeong.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.honjeong.global.common.PageResponse;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.client.KakaoPlaceClient;
import com.honjeong.place.client.PlaceCandidate;
import com.honjeong.place.client.PlaceSearchPage;
import com.honjeong.place.client.PlaceSearchQuery;
import com.honjeong.place.domain.Place;
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.repository.PlaceRepository;

/**
 * PlaceService 단위 테스트(순수 Mockito).
 *
 * <p>검증 목적: 비즈니스 규칙을 본다 — (1) 검색은 클라이언트 결과를 1:1 매핑해 페이지 엔벨로프로 감싸고,
 * 좌표·페이지·(클램프된)size를 클라이언트로 전달한다, (2) 블랭크 검색어·음수 page는 INVALID_INPUT으로 막고
 * size는 최대치로 클램프한다, (3) upsert는 external_id가 있으면 기존을 반환(저장 안 함), 없으면 생성한다.
 */
class PlaceServiceTest {

    private final KakaoPlaceClient kakaoPlaceClient = mock(KakaoPlaceClient.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceService placeService = new PlaceService(kakaoPlaceClient, placeRepository);

    private PlaceCandidate candidate(String id) {
        return new PlaceCandidate(id, id + "식당", "서울 어딘가", 37.5, 127.0, "한식");
    }

    @Test
    @DisplayName("search: 클라이언트 결과를 1:1 매핑하고 page/size/totalElements를 채운 엔벨로프를 만든다")
    void searchMapsAndPaginates() {
        // given: 클라이언트가 후보 2건·전체 23건을 반환한다
        when(kakaoPlaceClient.search(any(PlaceSearchQuery.class)))
                .thenReturn(new PlaceSearchPage(List.of(candidate("ext-1"), candidate("ext-2")), 23L));

        // when: 검색하면
        PageResponse<PlaceSearchResponse> res = placeService.search("김밥", null, null, 0, 5);

        // then: content가 1:1 매핑되고 page/size/totalElements가 채워진다
        assertThat(res.content()).hasSize(2);
        assertThat(res.content().get(0).externalId()).isEqualTo("ext-1");
        assertThat(res.content().get(0).name()).isEqualTo("ext-1식당");
        assertThat(res.content().get(0).category()).isEqualTo("한식");
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.size()).isEqualTo(5);
        assertThat(res.totalElements()).isEqualTo(23L);
    }

    @Test
    @DisplayName("search: 좌표와 page를 클라이언트로 그대로 전달한다")
    void searchForwardsLatLngToClient() {
        // given
        when(kakaoPlaceClient.search(any(PlaceSearchQuery.class)))
                .thenReturn(new PlaceSearchPage(List.of(), 0L));

        // when: 좌표·page를 주고 검색하면
        placeService.search("김밥", 37.5665, 126.9780, 2, 10);

        // then: 클라이언트에 같은 좌표·page가 전달된다
        ArgumentCaptor<PlaceSearchQuery> captor = ArgumentCaptor.forClass(PlaceSearchQuery.class);
        verify(kakaoPlaceClient).search(captor.capture());
        assertThat(captor.getValue().lat()).isEqualTo(37.5665);
        assertThat(captor.getValue().lng()).isEqualTo(126.9780);
        assertThat(captor.getValue().page()).isEqualTo(2);
    }

    @Test
    @DisplayName("search: size가 최대치(50)를 넘으면 50으로 클램프해 전달하고 응답에도 50을 반영한다")
    void searchClampsSizeToMax() {
        // given
        when(kakaoPlaceClient.search(any(PlaceSearchQuery.class)))
                .thenReturn(new PlaceSearchPage(List.of(), 0L));

        // when: size=999로 검색하면
        PageResponse<PlaceSearchResponse> res = placeService.search("김밥", null, null, 0, 999);

        // then: 클라이언트로는 50이 전달되고 응답 size도 50이다
        ArgumentCaptor<PlaceSearchQuery> captor = ArgumentCaptor.forClass(PlaceSearchQuery.class);
        verify(kakaoPlaceClient).search(captor.capture());
        assertThat(captor.getValue().size()).isEqualTo(50);
        assertThat(res.size()).isEqualTo(50);
    }

    @Test
    @DisplayName("search: 검색어가 비어 있으면 INVALID_INPUT으로 막고 클라이언트를 호출하지 않는다")
    void searchBlankQueryThrows() {
        // when & then: 공백 검색어는 INVALID_INPUT
        assertThatThrownBy(() -> placeService.search("  ", null, null, 0, 5))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(kakaoPlaceClient);
    }

    @Test
    @DisplayName("search: page가 음수면 INVALID_INPUT으로 막는다")
    void searchNegativePageThrows() {
        assertThatThrownBy(() -> placeService.search("김밥", null, null, -1, 5))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(kakaoPlaceClient);
    }

    @Test
    @DisplayName("findOrCreateByExternalId: external_id가 이미 있으면 기존 장소를 반환하고 저장하지 않는다")
    void upsertReturnsExistingWhenFound() {
        // given: 캐시에 이미 존재
        Place existing = Place.of("kakao-1", "기존식당", "주소", 37.5, 127.0, "한식");
        when(placeRepository.findByExternalId("kakao-1")).thenReturn(Optional.of(existing));

        // when
        Place result = placeService.findOrCreateByExternalId(
                new PlaceUpsertCommand("kakao-1", "다른이름", "다른주소", 37.6, 127.1, "일식"));

        // then: 기존을 그대로 반환하고 save는 호출되지 않는다(있으면 재사용)
        assertThat(result).isSameAs(existing);
        verify(placeRepository, never()).save(any());
    }

    @Test
    @DisplayName("findOrCreateByExternalId: external_id가 없으면 command 값으로 새 장소를 생성·저장한다")
    void upsertCreatesWhenAbsent() {
        // given: 캐시에 없음, save는 인자를 그대로 반환
        when(placeRepository.findByExternalId("kakao-2")).thenReturn(Optional.empty());
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        Place result = placeService.findOrCreateByExternalId(
                new PlaceUpsertCommand("kakao-2", "새식당", "새주소", 37.7, 127.2, "분식"));

        // then: save가 한 번 호출되고 반환 엔티티가 command 값을 담는다
        verify(placeRepository).save(any(Place.class));
        assertThat(result.getExternalId()).isEqualTo("kakao-2");
        assertThat(result.getName()).isEqualTo("새식당");
        assertThat(result.getLatitude()).isEqualTo(37.7);
        assertThat(result.getCategory()).isEqualTo("분식");
    }
}
