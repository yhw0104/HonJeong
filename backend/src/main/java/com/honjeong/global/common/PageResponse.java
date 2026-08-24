package com.honjeong.global.common;

import java.util.List;

/**
 * 목록 응답 엔벨로프.
 *
 * <p>{@code totalElements}가 <b>nullable</b>인 이유: 총 개수를 싸게 구할 수 있는 경로와 그렇지 않은
 * 경로가 섞여 있기 때문이다. 주변 조회·좌표 검색은 후보를 이미 메모리에 들고 있어 세는 데 비용이
 * 들지 않지만, 위치 없는 전국 이름 검색은 {@code LIKE '%..%'} 조건의 행을 전부 세야 해서
 * {@code SELECT count(*)}가 655,163행을 훑는다(2026-08-24 실측 298ms). 앱의 어느 화면도 이 값을
 * 읽지 않으므로, 그 경로에서는 총 개수를 만들지 않고 {@code null}로 둔다.
 *
 * <p>대신 모든 경로가 {@code hasNext}를 채운다 — "더 있는가"는 총 개수 없이도 알 수 있고
 * (한 건 더 조회해 보면 된다), 무한 스크롤에 실제로 필요한 정보도 그쪽이다.
 */
public record PageResponse<T>(List<T> content, int page, int size, Long totalElements, boolean hasNext) {

    /** 총 개수를 아는 경로용(메모리에 후보 전체를 들고 있을 때). */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponse<>(content, page, size, totalElements, (long) (page + 1) * size < totalElements);
    }

    /** 총 개수를 세지 않는 경로용. 다음 페이지 유무만 호출자가 판단해 넘긴다. */
    public static <T> PageResponse<T> ofSlice(List<T> content, int page, int size, boolean hasNext) {
        return new PageResponse<>(content, page, size, null, hasNext);
    }
}
