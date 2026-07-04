package com.honjeong.notification.domain;

/** 알림 종류. 값 추가 = 여기 + 발행 한 줄(화면·API 변경 없음). 거절/만료는 의도적으로 없음(스펙). */
public enum NotificationType {
    MEAL_REQUEST_RECEIVED,   // 같이먹기 신청 받음 → 수신자에게
    MEAL_REQUEST_ACCEPTED,   // 같이먹기 신청 수락됨 → 발신자에게
    MATE_REQUEST_RECEIVED,   // 메이트 신청 받음 → 수신자에게
    MATE_REQUEST_ACCEPTED    // 메이트 신청 수락됨 → 발신자에게
}
