package com.honjeong.global.common;

/**
 * 1. 기능: 표시용 닉네임을 만든다 — 탈퇴로 닉네임이 사라진 사용자를 '알 수 없음'으로 보여준다
 * 2. 사용처: PlaceReviewResponse·ConversationService·MealRequestListItemResponse·NotificationResponse
 *
 * <p>탈퇴는 users 행을 남기고 개인정보만 비우므로(익명화), 그 사용자가 남긴 리뷰·대화·신청 이력에는
 * 닉네임이 null인 채로 참조가 남는다. 표시 문구를 여기 한 곳에서만 만들어 화면마다 달라지지 않게 한다.
 */
public final class DisplayNames {

    /** 닉네임이 없는 사용자(탈퇴)의 표시 이름. */
    public static final String UNKNOWN = "알 수 없음";

    private DisplayNames() {
    }

    /**
     * 기능: 닉네임이 비어 있으면 '알 수 없음'으로 바꾼다
     * Request: nickname — 원본 닉네임(null 허용)
     * Response: String — 표시용 닉네임
     */
    public static String nicknameOrUnknown(String nickname) {
        return (nickname == null || nickname.isBlank()) ? UNKNOWN : nickname;
    }
}
