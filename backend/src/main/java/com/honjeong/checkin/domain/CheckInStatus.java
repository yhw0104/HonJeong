package com.honjeong.checkin.domain;

/** 체크인 상태. ACTIVE(혼밥 중) → ENDED(종료). 단일 활성 제약은 ACTIVE 행에만 적용된다. */
public enum CheckInStatus {
    ACTIVE, ENDED
}
