package com.honjeong.mate.domain;

/**
 * 메이트 신청의 처리 상태를 나타내는 enum.
 */
public enum MateRequestStatus {
    /** 응답 대기 중 */ PENDING, /** 수락됨(메이트 성립) */ ACCEPTED, /** 수신자가 거절함 */ DECLINED, /** 발신자가 취소함(차단 시 자동 취소 포함) */ CANCELED
}
