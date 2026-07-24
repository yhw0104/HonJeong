package com.honjeong.user.domain;

import java.time.LocalDate;

/**
 * 생년월일 → 표시용 연령대 파생. 연 나이(올해−출생연도) 기준 10년 버킷.
 * (사용자 결정: 정확한 만 나이가 아니라 "출생연도로 20대 판단")
 */
public final class AgeGroups {

    private AgeGroups() {}

    /**
     * @param birthDate 생년월일(null 가능)
     * @param today     기준일(KST 오늘)
     * @return "10대"/"20대"/…/"60대 이상", birthDate가 null이면 null
     */
    public static String rangeOf(LocalDate birthDate, LocalDate today) {
        if (birthDate == null) return null;
        int age = today.getYear() - birthDate.getYear();
        if (age < 20) return "10대";
        if (age < 30) return "20대";
        if (age < 40) return "30대";
        if (age < 50) return "40대";
        if (age < 60) return "50대";
        return "60대 이상";
    }
}
