package com.honjeong.mate.dto;

import com.honjeong.mate.domain.MateRequest;

public record MateRequestResponse(Long mateRequestId, Long toUserId, String status) {
    public static MateRequestResponse from(MateRequest mr) {
        return new MateRequestResponse(mr.getId(), mr.getToUser().getId(), mr.getStatus().name());
    }
}
