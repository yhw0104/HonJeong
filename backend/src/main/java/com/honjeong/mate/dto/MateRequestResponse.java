package com.honjeong.mate.dto;

import com.honjeong.mate.domain.MateRequest;

/**
 * 메이트 신청 생성 결과 응답 DTO (POST /api/mate-requests).
 *
 * @param mateRequestId 생성된 신청 ID
 * @param toUserId      신청을 받은 상대 사용자 ID
 * @param status        신청 상태(생성 직후 PENDING)
 */
public record MateRequestResponse(Long mateRequestId, Long toUserId, String status) {
    /** 기능: MateRequest 엔티티를 생성 결과 DTO로 변환 */
    public static MateRequestResponse from(MateRequest mr) {
        return new MateRequestResponse(mr.getId(), mr.getToUser().getId(), mr.getStatus().name());
    }
}
