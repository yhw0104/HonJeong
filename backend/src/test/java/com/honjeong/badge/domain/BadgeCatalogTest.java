package com.honjeong.badge.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BadgeCatalogTest {

    @Test
    @DisplayName("10종·키·임계값·metric 정의")
    void defs() {
        assertThat(BadgeCatalog.values()).hasSize(10);
        assertThat(BadgeCatalog.SOLO_10.key()).isEqualTo("SOLO_10");
        assertThat(BadgeCatalog.SOLO_10.threshold()).isEqualTo(10);
        assertThat(BadgeCatalog.SOLO_10.metric()).isEqualTo(BadgeMetric.SOLO_CHECKIN);
        assertThat(BadgeCatalog.FAV_10.metric()).isEqualTo(BadgeMetric.FAVORITE);
        assertThat(BadgeCatalog.TOGETHER_1.threshold()).isEqualTo(1);
    }
}
