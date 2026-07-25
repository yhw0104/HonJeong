package com.honjeong.checkin.dto;

import java.time.LocalDateTime;

import com.honjeong.checkin.domain.CheckIn;

/**
 * 체크인 응답(POST/end/cancel/me 공용). {@code status}는 enum 이름 문자열(ACTIVE|TOGETHER|ENDED|CANCELLED),
 * {@code endedAt}은 진행 중이면 null, {@code matchedAt}·{@code partnerNickname}은 솔로면 null이다.
 * 명세의 최소 형태보다 필드가 많은 의도적 상위집합 — 프론트는 status로 분기한다.
 *
 * @param checkInId       체크인 id
 * @param placeId         식당 id
 * @param placeName       식당 이름(상태바 표시용 — 목록에 의존하지 않게 응답에 동봉)
 * @param status          상태 문자열(ACTIVE|TOGETHER|ENDED|CANCELLED)
 * @param startedAt       시작 시각
 * @param endedAt         종료 시각(진행 중이면 null)
 * @param matchedAt       매칭 시각(솔로면 null)
 * @param partnerUserId   같이먹기 파트너 사용자 id(TOGETHER /me 응답 전용, 그 외 null) — 노쇼 신고 대상 지정에 필요
 * @param partnerNickname 같이먹기 파트너 닉네임(TOGETHER /me 응답 전용, 그 외 null)
 * @param conversationId  같이먹기 매칭의 대화방 id(TOGETHER /me 응답 전용, 그 외 null) — 상태바→대화 진입용
 */
public record CheckInResponse(
        Long checkInId, Long placeId, String placeName, String status,
        LocalDateTime startedAt, LocalDateTime endedAt,
        LocalDateTime matchedAt, Long partnerUserId, String partnerNickname, Long conversationId) {

    /** 파트너 없는 변환(ACTIVE/ENDED/CANCELLED/POST/end 공용). */
    public static CheckInResponse from(CheckIn c) {
        return from(c, null, null, null);
    }

    /** 파트너(userId·닉네임)·대화방 id를 포함한 변환(TOGETHER /me 응답). */
    public static CheckInResponse from(CheckIn c, Long partnerUserId, String partnerNickname, Long conversationId) {
        return new CheckInResponse(
                c.getId(), c.getPlace().getId(), c.getPlace().getName(), c.getStatus().name(),
                c.getStartedAt(), c.getEndedAt(), c.getMatchedAt(), partnerUserId, partnerNickname, conversationId);
    }
}
