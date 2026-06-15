package com.honjeong.checkin.dto;

import java.time.LocalDateTime;

import com.honjeong.checkin.domain.CheckIn;

/**
 * 체크인 응답(POST/end/me 공용). {@code status}는 enum 이름 문자열(ACTIVE|ENDED), {@code endedAt}은 ACTIVE면 null이다.
 * 명세의 최소 형태(POST는 endedAt 없음)보다 한 필드 많은 의도적 상위집합 — 프론트는 status로 분기한다.
 *
 * @param checkInId 체크인 id
 * @param placeId   식당 id
 * @param status    상태 문자열(ACTIVE|ENDED)
 * @param startedAt 시작 시각
 * @param endedAt   종료 시각(ACTIVE면 null)
 */
public record CheckInResponse(
        Long checkInId,
        Long placeId,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {

    /** 체크인 엔티티를 응답으로 변환한다. */
    public static CheckInResponse from(CheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getPlace().getId(),
                checkIn.getStatus().name(),
                checkIn.getStartedAt(),
                checkIn.getEndedAt());
    }
}
