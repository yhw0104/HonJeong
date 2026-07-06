package com.honjeong.checkin.domain;

/** 체크인 상태. ACTIVE(혼밥 중) → TOGETHER(같이 먹는 중) → ENDED(종료). ACTIVE → CANCELLED(오집계 취소). */
public enum CheckInStatus {
    /** 혼밥 중(솔로 체크인 진행 상태) */
    ACTIVE,
    /** 같이먹기 매칭돼 같이 먹는 중 */
    TOGETHER,
    /** 종료됨(직접 종료 또는 TTL 자동 만료) */
    ENDED,
    /** 취소됨(짧은 혼밥 오집계 제외용) */
    CANCELLED
}
