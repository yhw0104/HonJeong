package com.honjeong.checkin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckinTimeBucketsTest {

    private LocalDateTime at(int hour) {
        return LocalDateTime.of(2026, 7, 19, hour, 0);
    }

    private List<LocalDateTime> nTimes(int hour, int n) {
        return IntStream.range(0, n).mapToObj(i -> at(hour)).toList();
    }

    @Test
    @DisplayName("버킷 경계: 6/11/16/22시가 각각 아침/점심/저녁/밤")
    void 버킷_경계() {
        assertThat(CheckinTimeBuckets.bucketOf(6)).isEqualTo("MORNING");
        assertThat(CheckinTimeBuckets.bucketOf(10)).isEqualTo("MORNING");
        assertThat(CheckinTimeBuckets.bucketOf(11)).isEqualTo("LUNCH");
        assertThat(CheckinTimeBuckets.bucketOf(15)).isEqualTo("LUNCH");
        assertThat(CheckinTimeBuckets.bucketOf(16)).isEqualTo("EVENING");
        assertThat(CheckinTimeBuckets.bucketOf(21)).isEqualTo("EVENING");
        assertThat(CheckinTimeBuckets.bucketOf(22)).isEqualTo("NIGHT");
        assertThat(CheckinTimeBuckets.bucketOf(0)).isEqualTo("NIGHT");
        assertThat(CheckinTimeBuckets.bucketOf(5)).isEqualTo("NIGHT");
    }

    @Test
    @DisplayName("빈 입력이면 전 버킷 0 + peak null")
    void 빈_입력() {
        var s = CheckinTimeBuckets.summarize(List.of());
        assertThat(s.periods()).extracting("count").containsExactly(0L, 0L, 0L, 0L);
        assertThat(s.peakKey()).isNull();
    }

    @Test
    @DisplayName("전체 세션이 임계(5) 미만이면 peak null")
    void 임계_미만() {
        var s = CheckinTimeBuckets.summarize(nTimes(12, 4)); // 점심 4건 < 5
        assertThat(s.peakKey()).isNull();
    }

    @Test
    @DisplayName("임계 이상이면 최다 버킷이 peak")
    void 피크_최다() {
        var times = new java.util.ArrayList<LocalDateTime>();
        times.addAll(nTimes(12, 6)); // 점심 6
        times.addAll(nTimes(18, 2)); // 저녁 2
        var s = CheckinTimeBuckets.summarize(times);
        assertThat(s.peakKey()).isEqualTo("LUNCH");
    }

    @Test
    @DisplayName("동점이면 점심>저녁>아침>밤 우선")
    void 동점_우선순위() {
        var times = new java.util.ArrayList<LocalDateTime>();
        times.addAll(nTimes(18, 3)); // 저녁 3
        times.addAll(nTimes(12, 3)); // 점심 3 (동점)
        var s = CheckinTimeBuckets.summarize(times); // 총 6 >= 5
        assertThat(s.peakKey()).isEqualTo("LUNCH");
    }

    @Test
    @DisplayName("periods는 아침·점심·저녁·밤 고정 순서로 카운트")
    void 순서_카운트() {
        var times = new java.util.ArrayList<LocalDateTime>();
        times.addAll(nTimes(8, 1));  // 아침 1
        times.addAll(nTimes(12, 2)); // 점심 2
        times.addAll(nTimes(18, 3)); // 저녁 3
        times.addAll(nTimes(23, 4)); // 밤 4
        var s = CheckinTimeBuckets.summarize(times);
        assertThat(s.periods()).extracting("key").containsExactly("MORNING", "LUNCH", "EVENING", "NIGHT");
        assertThat(s.periods()).extracting("count").containsExactly(1L, 2L, 3L, 4L);
    }
}
