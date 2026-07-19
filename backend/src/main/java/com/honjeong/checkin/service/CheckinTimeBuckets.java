package com.honjeong.checkin.service;

import java.time.LocalDateTime;
import java.util.List;

import com.honjeong.checkin.dto.PeriodCount;

/**
 * 혼밥 체크인 시작 시각(KST)들을 식사 시간대 4버킷으로 집계하고 피크를 판정하는 순수 로직.
 * 버킷: 아침 06–10, 점심 11–15, 저녁 16–21, 밤 22–05. 전체 세션이 적으면(&lt;PEAK_MIN) 피크를 내지 않는다.
 */
public final class CheckinTimeBuckets {

    /** 시간대 피크를 낼 최소 세션 수(이 미만이면 "붐빈다"고 하지 않는다). */
    public static final int PEAK_MIN = 5;

    private CheckinTimeBuckets() {}

    /**
     * 결과: total(전체 세션 수=periods 합, 중복 포함) + 아침→점심→저녁→밤 순 카운트 + 피크 key(없으면 null).
     * total은 distinct 사람 수가 아니라 세션 수라, "N명" 문구와 시간대 바 합이 항상 일치한다.
     */
    public record Summary(long total, List<PeriodCount> periods, String peakKey) {}

    /** 시각(hour 0–23)을 버킷 key로. */
    public static String bucketOf(int hour) {
        if (hour >= 6 && hour <= 10) return "MORNING";
        if (hour >= 11 && hour <= 15) return "LUNCH";
        if (hour >= 16 && hour <= 21) return "EVENING";
        return "NIGHT"; // 22,23,0..5
    }

    public static Summary summarize(List<LocalDateTime> startedAts) {
        long morning = 0, lunch = 0, evening = 0, night = 0;
        for (LocalDateTime t : startedAts) {
            switch (bucketOf(t.getHour())) {
                case "MORNING" -> morning++;
                case "LUNCH" -> lunch++;
                case "EVENING" -> evening++;
                default -> night++;
            }
        }
        List<PeriodCount> periods = List.of(
                new PeriodCount("MORNING", morning),
                new PeriodCount("LUNCH", lunch),
                new PeriodCount("EVENING", evening),
                new PeriodCount("NIGHT", night));

        long total = morning + lunch + evening + night;
        String peak = null;
        if (total >= PEAK_MIN) {
            long[] counts = { lunch, evening, morning, night };          // 동점 우선순위 순
            String[] keys = { "LUNCH", "EVENING", "MORNING", "NIGHT" };
            long best = -1;
            for (int i = 0; i < keys.length; i++) {
                if (counts[i] > best) {
                    best = counts[i];
                    peak = keys[i];
                }
            }
        }
        return new Summary(total, periods, peak);
    }
}
