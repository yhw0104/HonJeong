package com.honjeong.review.domain;

import java.util.Set;

/** 혼밥 친화 태그 고정 프리셋(ERD F-3, 불변 5종). 자유 입력 금지 — 서비스가 이 집합으로 검증한다. */
public final class SoloFriendlyTags {
    public static final Set<String> ALLOWED = Set.of("1인석 많음", "바테이블", "칸막이", "눈치 없음", "오래 OK");
    private SoloFriendlyTags() {}
}
