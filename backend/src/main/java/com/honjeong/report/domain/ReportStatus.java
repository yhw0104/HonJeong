package com.honjeong.report.domain;

/** 신고 처리 상태. 관리자 툴이 없는 현재는 전부 RECEIVED로만 저장(예약). */
public enum ReportStatus { RECEIVED, REVIEWING, RESOLVED }
