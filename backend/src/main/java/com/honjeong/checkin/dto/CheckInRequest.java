package com.honjeong.checkin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 체크인 시작 요청. 검색 결과에서 선택한 장소의 내부 ID를 받는다.
 *
 * @param placeId 우리 DB의 장소 PK(필수)
 */
public record CheckInRequest(@NotNull Long placeId) {}
