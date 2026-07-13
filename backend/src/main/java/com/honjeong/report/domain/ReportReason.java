package com.honjeong.report.domain;

/** 신고 사유(유저/리뷰 공용). NO_SHOW는 같이먹기 노쇼 신고용(유저 대상). */
public enum ReportReason { INAPPROPRIATE_MESSAGE /** 부적절한 메시지 */, ABUSE /** 욕설·비방 */, SPAM /** 스팸·광고 */, FALSE_PROFILE /** 허위 프로필·사칭 */, NO_SHOW /** 노쇼(약속 장소에 안 나타남) */, OTHER /** 기타 */ }
