package com.honjeong.badge.dto;

import java.time.LocalDateTime;

/** 뱃지 현황 한 건 — key + 획득 여부 + 획득 시각(미획득이면 null). 이모지·이름은 앱이 key로 결합. */
public record BadgeStatusResponse(String key, boolean earned, LocalDateTime earnedAt) {
}
