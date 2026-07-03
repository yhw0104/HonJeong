package com.honjeong.checkin.domain;

/** 체크인 상태. ACTIVE(혼밥 중) → TOGETHER(같이 먹는 중) → ENDED(종료). ACTIVE → CANCELLED(오집계 취소). */
public enum CheckInStatus {
    ACTIVE, TOGETHER, ENDED, CANCELLED
}
