package com.honjeong.meal.domain;

/** 같이먹기 신청 상태. PENDING(대기) → ACCEPTED(수락)/DECLINED(거절)/EXPIRED(만료)/WITHDRAWN(철회). */
public enum MealRequestStatus {
    /** 대기 — 수신자가 아직 수락/거절하지 않은 상태(신청 생성 직후 기본값) */
    PENDING,
    /** 수락 — 같이먹기 성사(수신자 체크인 TOGETHER 전이 + 발신자 TOGETHER 체크인 생성) */
    ACCEPTED,
    /** 거절 — 수신자가 "거절"을 직접 눌러 명시적으로 거절한 상태(사용자 행위) */
    DECLINED,
    /** 만료 — 수신자가 직접 거절하지 않았는데 자동 정리된 상태(다른 신청 수락·그만두기·혼자먹기·시간만료·차단) */
    EXPIRED,
    /** 철회 — 신청자가 스스로 보낸 PENDING 신청을 거둬들인 상태(발신자 행위) */
    WITHDRAWN
}
