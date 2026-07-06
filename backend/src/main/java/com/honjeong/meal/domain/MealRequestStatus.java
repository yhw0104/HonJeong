package com.honjeong.meal.domain;

/** 같이먹기 신청 상태. PENDING(대기) → ACCEPTED(수락)/DECLINED(거절). */
public enum MealRequestStatus {
    /** 대기 — 수신자가 아직 수락/거절하지 않은 상태(신청 생성 직후 기본값) */
    PENDING,
    /** 수락 — 같이먹기 성사(수신자 체크인 TOGETHER 전이 + 발신자 TOGETHER 체크인 생성) */
    ACCEPTED,
    /** 거절 — 수신자가 직접 거절했거나, 다른 신청 수락·차단으로 자동 정리된 상태 */
    DECLINED
}
