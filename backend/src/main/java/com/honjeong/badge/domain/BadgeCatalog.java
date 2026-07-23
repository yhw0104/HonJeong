package com.honjeong.badge.domain;

/** 뱃지 카탈로그(서버가 소유하는 판정 정의). 이모지·이름은 앱이 key로 결합한다. */
public enum BadgeCatalog {
    SOLO_1(BadgeMetric.SOLO_CHECKIN, 1),
    SOLO_10(BadgeMetric.SOLO_CHECKIN, 10),
    SOLO_50(BadgeMetric.SOLO_CHECKIN, 50),
    DIARY_1(BadgeMetric.REVIEW, 1),
    DIARY_10(BadgeMetric.REVIEW, 10),
    TOGETHER_1(BadgeMetric.TOGETHER, 1),
    MATE_1(BadgeMetric.MATE, 1),
    MATE_5(BadgeMetric.MATE, 5),
    FAV_1(BadgeMetric.FAVORITE, 1),
    FAV_10(BadgeMetric.FAVORITE, 10);

    private final BadgeMetric metric;
    private final int threshold;

    BadgeCatalog(BadgeMetric metric, int threshold) {
        this.metric = metric;
        this.threshold = threshold;
    }

    public BadgeMetric metric() {
        return metric;
    }

    public int threshold() {
        return threshold;
    }

    public String key() {
        return name();
    }
}
