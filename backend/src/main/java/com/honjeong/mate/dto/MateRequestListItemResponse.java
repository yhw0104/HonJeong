package com.honjeong.mate.dto;

import java.time.LocalDateTime;
import com.honjeong.mate.domain.MateRequest;

/**
 * 메이트 신청 목록(GET /api/mate-requests)의 한 항목을 나타내는 응답 DTO.
 */
public record MateRequestListItemResponse(
        Long mateRequestId, // 메이트 신청 ID
        MateUser fromUser, // 신청을 보낸 사용자 요약
        MateUser toUser, // 신청을 받은 사용자 요약
        String status, // 신청 상태(PENDING/ACCEPTED/DECLINED/CANCELED)
        LocalDateTime createdAt) { // 신청 생성 시각

    /**
     * 신청에 연결된 사용자 요약 정보.
     *
     * @param userId          사용자 ID
     * @param nickname        닉네임
     * @param profileImageUrl 프로필 이미지 URL(없으면 null)
     */
    public record MateUser(Long userId, String nickname, String profileImageUrl) {
    }

    /** 기능: MateRequest 엔티티를 목록 항목 DTO로 변환 (발신·수신 사용자 요약 포함) */
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
