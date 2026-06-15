package com.honjeong.global.common;

import java.util.List;

/**
 * 페이지네이션 응답 엔벨로프(API 명세 §1.4: {@code content}/{@code page}/{@code size}/{@code totalElements}).
 * 특정 도메인에 종속되지 않는 공통 엔벨로프라 {@code ApiResponse}와 같은 global/common에 둔다 — 이후 같이먹기 목록 등
 * 다른 페이징 엔드포인트가 그대로 재사용한다. 보통 {@code ApiResponse<PageResponse<T>>} 형태로 감싸 응답한다.
 *
 * @param content       현재 페이지의 항목들(범위를 벗어난 페이지면 빈 목록)
 * @param page          0-base 페이지 번호
 * @param size          페이지 크기(클램프된 실제 적용값)
 * @param totalElements 전체 항목 수(페이지와 무관)
 * @param <T>           항목 타입
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {

    /**
     * 페이지 엔벨로프를 만든다.
     *
     * @param content       현재 페이지 항목들
     * @param page          0-base 페이지 번호
     * @param size          페이지 크기
     * @param totalElements 전체 항목 수
     * @param <T>           항목 타입
     * @return 페이지 응답 엔벨로프
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponse<>(content, page, size, totalElements);
    }
}
