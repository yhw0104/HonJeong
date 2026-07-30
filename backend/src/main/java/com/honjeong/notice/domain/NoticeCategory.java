package com.honjeong.notice.domain;

/**
 * 공지 카테고리 구분 값.
 *
 * <p>공지 카테고리 — 앱 칩 필터(업데이트/이벤트/안내)와 1:1.
 */
public enum NoticeCategory {
    /** 업데이트 소식 */ UPDATE, /** 이벤트 안내 */ EVENT, /** 일반 안내 */ GENERAL
}
