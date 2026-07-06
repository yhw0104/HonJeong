package com.honjeong.mate.dto;

import java.time.LocalDateTime;
import com.honjeong.mate.domain.MateRequest;

/**
 * 메이트 신청 상태 변경(수락/거절/취소) 결과 응답 DTO.
 *
 * @param mateRequestId 신청 ID
 * @param status        변경된 상태(ACCEPTED/DECLINED/CANCELED)
 * @param respondedAt   응답(상태 변경) 시각
 */
public record MateRequestStatusResponse(Long mateRequestId, String status, LocalDateTime respondedAt) {
    /** 기능: MateRequest 엔티티를 상태 변경 결과 DTO로 변환 */
    public static MateRequestStatusResponse from(MateRequest mr) {
        return new MateRequestStatusResponse(mr.getId(), mr.getStatus().name(), mr.getRespondedAt());
    }
}
