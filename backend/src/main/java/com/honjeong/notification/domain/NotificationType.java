package com.honjeong.notification.domain;

/**
 * 알림 종류를 나타내는 enum. 앱이 이 값 + actor 닉네임으로 알림 문구를 조립한다(문구는 DB에 저장하지 않음).
 *
 * <p>알림 종류. 값 추가 = 여기 + 발행 한 줄(화면·API 변경 없음). 거절/만료는 의도적으로 없음(스펙).
 */
public enum NotificationType {
    MEAL_REQUEST_RECEIVED,   // 같이먹기 신청 받음 → 수신자에게
    MEAL_REQUEST_ACCEPTED,   // 같이먹기 신청 수락됨 → 발신자에게
    MEAL_MATCH_CANCELLED,    // 같이먹기 매칭이 상대에 의해 깨짐(노쇼/취소) → 남은 상대에게
    MATE_REQUEST_RECEIVED,   // 메이트 신청 받음 → 수신자에게
    MATE_REQUEST_ACCEPTED,   // 메이트 신청 수락됨 → 발신자에게
    BADGE_EARNED             // 뱃지 획득 → 본인에게(actor 없음)
}
