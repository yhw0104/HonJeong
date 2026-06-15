package com.honjeong.place.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.global.common.PageResponse;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.client.KakaoPlaceClient;
import com.honjeong.place.client.PlaceSearchPage;
import com.honjeong.place.client.PlaceSearchQuery;
import com.honjeong.place.domain.Place;
import com.honjeong.place.dto.PlaceSearchResponse;
import com.honjeong.place.repository.PlaceRepository;

/**
 * 장소 도메인 서비스. 두 가지 책임을 가진다.
 *
 * <ul>
 *   <li><b>검색</b> — 외부 {@link KakaoPlaceClient}(Mock/실 카카오)에 위임해 결과를 페이지 엔벨로프로 매핑한다.
 *       이 단계는 DB에 쓰지 않는다(캐싱은 체크인 시점에 일어난다).</li>
 *   <li><b>캐시 upsert</b> — {@code external_id}로 조회해 없으면 생성, 있으면 재사용한다. 체크인(Slice 5)이 호출한다.</li>
 * </ul>
 */
@Service
public class PlaceService {

    // 한 번에 가져올 수 있는 검색 결과 상한. 이보다 큰 size 요청은 이 값으로 줄인다(방어적 클램프).
    static final int MAX_SIZE = 50;

    private final KakaoPlaceClient kakaoPlaceClient;
    private final PlaceRepository placeRepository;

    public PlaceService(KakaoPlaceClient kakaoPlaceClient, PlaceRepository placeRepository) {
        this.kakaoPlaceClient = kakaoPlaceClient;
        this.placeRepository = placeRepository;
    }

    /**
     * 검색어(+선택 좌표)로 장소를 검색해 페이지 엔벨로프로 반환한다. 페이지 슬라이싱·거리정렬은 클라이언트가 하고,
     * 여기서는 입력 검증, size 클램프, 후보→DTO 매핑만 한다.
     *
     * @param query 검색어(공백 불가)
     * @param lat   중심 위도(nullable, 거리순 정렬용)
     * @param lng   중심 경도(nullable, 거리순 정렬용)
     * @param page  0-base 페이지 번호(0 이상)
     * @param size  페이지 크기(1 이상, {@link #MAX_SIZE} 초과 시 클램프)
     * @return content/page/size/totalElements를 담은 페이지 엔벨로프
     * @throws BusinessException 검색어가 공백이거나 page가 음수, size가 1 미만이면 {@link ErrorCode#INVALID_INPUT}
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceSearchResponse> search(String query, Double lat, Double lng, int page, int size) {
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

        PlaceSearchPage result = kakaoPlaceClient.search(new PlaceSearchQuery(query, lat, lng, page, clampedSize));
        List<PlaceSearchResponse> content = result.candidates().stream()
                .map(PlaceSearchResponse::from)
                .toList();
        return PageResponse.of(content, page, clampedSize, result.totalCount());
    }

    /**
     * external_id로 캐시된 장소를 찾고, 없으면 command 값으로 새로 생성·저장한다(upsert). 캐싱 규칙상
     * <b>이미 있으면 그대로 재사용</b>하며 기존 값을 덮어쓰지 않는다 — 멱등하게 동작한다.
     *
     * @param command 선택한 가게 정보(external_id가 캐싱 키)
     * @return 캐시된(또는 새로 만든) 장소 엔티티
     */
    @Transactional
    public Place findOrCreateByExternalId(PlaceUpsertCommand command) {
        return placeRepository.findByExternalId(command.externalId())
                .orElseGet(() -> placeRepository.save(Place.of(
                        command.externalId(), command.name(), command.address(),
                        command.latitude(), command.longitude(), command.category())));
    }
}
