package com.honjeong.global.common;

import java.util.List;

/**
 * 목록 응답 엔벨로프.
 *
 * <p><b>왜 페이지 정보가 없는가</b>: 예전에는 검색 결과를 20건씩 끊어 스크롤할 때마다 이어 붙일
 * 생각으로 페이징을 썼다. 그래서 {@code page}/{@code size}/{@code totalElements}/{@code hasNext}를
 * 함께 내려보냈는데, 검색이 느려 무한 스크롤을 포기하고 한 번에 20건만 보여주는 쪽으로 화면을
 * 정리하면서 <b>앱이 그 값들을 하나도 읽지 않게</b> 됐다. 읽는 곳이 없는 값을 만들려고 서버가
 * {@code SELECT count(*)}로 655,163행을 세고 있었다(2026-08-24 실측 298ms).
 *
 * <p>그래서 봉투를 {@code content} 하나로 줄였다. <b>필드 이름을 {@code content}로 유지하는 이유</b>는
 * 배포된 앱(TestFlight 1.0.0(30))이 응답에서 {@code data.content}를 읽기 때문이다 — 벗겨서 배열을
 * 그대로 내려보내면 구버전 앱에서 목록이 통째로 비어 보인다.
 *
 * <p>다시 무한 스크롤이 필요해지면 여기에 {@code hasNext}를 되살리면 된다. 총 개수는 필요 없다 —
 * "더 있는가"는 한 건 더 조회해 보면 알 수 있고, 무한 스크롤에 실제로 필요한 정보도 그쪽이다.
 */
public record ListResponse<T>(List<T> content) {

    public static <T> ListResponse<T> of(List<T> content) {
        return new ListResponse<>(content);
    }
}
