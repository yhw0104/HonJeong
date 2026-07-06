package com.honjeong.review.domain;

import java.util.Set;

/**
 * 리뷰에 붙일 수 있는 혼밥 친화 태그의 고정 프리셋 상수 모음.
 *
 * <p>[기존 주석] 혼밥 친화 태그 고정 프리셋(ERD F-3, 불변 5종). 자유 입력 금지 — 서비스가 이 집합으로 검증한다.
 */
public final class SoloFriendlyTags {
    /** 허용 태그 5종 — 이 집합 밖의 태그는 ReviewService가 400(INVALID_INPUT)으로 거절 */
    public static final Set<String> ALLOWED = Set.of("1인석 많음", "바테이블", "칸막이", "눈치 없음", "오래 OK");
    private SoloFriendlyTags() {}
}
