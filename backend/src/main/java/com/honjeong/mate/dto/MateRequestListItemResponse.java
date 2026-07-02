package com.honjeong.mate.dto;

import java.time.LocalDateTime;
import com.honjeong.mate.domain.MateRequest;

public record MateRequestListItemResponse(
        Long mateRequestId,
        MateUser fromUser,
        MateUser toUser,
        String status,
        LocalDateTime createdAt) {

    public record MateUser(Long userId, String nickname, String profileImageUrl) {
    }

    public static MateRequestListItemResponse from(MateRequest mr) {
        return new MateRequestListItemResponse(
                mr.getId(),
                new MateUser(mr.getFromUser().getId(), mr.getFromUser().getNickname(),
                        mr.getFromUser().getProfileImageUrl()),
                new MateUser(mr.getToUser().getId(), mr.getToUser().getNickname(),
                        mr.getToUser().getProfileImageUrl()),
                mr.getStatus().name(),
                mr.getCreatedAt());
    }
}
