package com.honjeong.checkin.domain;

/**
 * 체크인 상태. SEEKING(모집중·정문) → 매칭 성공 시 TOGETHER, 매칭 실패 시 ACTIVE(혼밥중) → ENDED.
 * SEEKING/ACTIVE → CANCELLED(오집계 취소, 이력 제외).
 */
public enum CheckInStatus {
    /** 모집중 — 같이 갈 사람 구하는 중(식사 전). 매칭 대상. 체크인의 정문. */
    SEEKING,
    /** 혼밥 중 — 매칭 실패 후 혼자 먹는 중(종착·합류 불가). "현재 N명 혼밥중"의 원천. */
    ACTIVE,
    /** 같이먹기 매칭돼 같이 먹는 중 */
    TOGETHER,
    /** 종료됨(직접 종료 또는 TTL 자동 만료) */
    ENDED,
    /** 취소됨(짧은 혼밥/모집 포기 오집계 제외용) */
    CANCELLED
}
