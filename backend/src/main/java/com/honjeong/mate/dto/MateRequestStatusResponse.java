package com.honjeong.mate.dto;

import java.time.LocalDateTime;
import com.honjeong.mate.domain.MateRequest;

public record MateRequestStatusResponse(Long mateRequestId, String status, LocalDateTime respondedAt) {
    public static MateRequestStatusResponse from(MateRequest mr) {
        return new MateRequestStatusResponse(mr.getId(), mr.getStatus().name(), mr.getRespondedAt());
    }
}
