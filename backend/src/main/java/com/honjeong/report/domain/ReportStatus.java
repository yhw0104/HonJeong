package com.honjeong.report.domain;

/** 신고 처리 상태. 관리자 툴이 없는 현재는 전부 RECEIVED로만 저장(예약). */
public enum ReportStatus { RECEIVED /** 접수됨(기본값) */, REVIEWING /** 검토 중(예약) */, RESOLVED /** 처리 완료(예약) */ }
