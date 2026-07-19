package com.honjeong.checkin.dto;

/** 시간대 버킷별 혼밥 세션 수. key = MORNING|LUNCH|EVENING|NIGHT(라벨은 프론트 소유). */
public record PeriodCount(String key, long count) {}
