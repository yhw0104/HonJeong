package com.honjeong.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgeGroupsTest {

    private final LocalDate today = LocalDate.of(2026, 7, 24);

    @Test
    @DisplayName("연 나이 경계: 19→10대, 20→20대")
    void boundary_teens_twenties() {
        assertThat(AgeGroups.rangeOf(LocalDate.of(2007, 1, 1), today)).isEqualTo("10대"); // 19
        assertThat(AgeGroups.rangeOf(LocalDate.of(2006, 12, 31), today)).isEqualTo("20대"); // 20
    }

    @Test
    @DisplayName("연 나이 60↑은 '60대 이상'")
    void sixties_and_above() {
        assertThat(AgeGroups.rangeOf(LocalDate.of(1966, 6, 15), today)).isEqualTo("60대 이상"); // 60
        assertThat(AgeGroups.rangeOf(LocalDate.of(1930, 1, 1), today)).isEqualTo("60대 이상");
    }

    @Test
    @DisplayName("중간 버킷 30/40/50대")
    void middle_buckets() {
        assertThat(AgeGroups.rangeOf(LocalDate.of(1996, 5, 5), today)).isEqualTo("30대"); // 30
        assertThat(AgeGroups.rangeOf(LocalDate.of(1986, 5, 5), today)).isEqualTo("40대"); // 40
        assertThat(AgeGroups.rangeOf(LocalDate.of(1976, 5, 5), today)).isEqualTo("50대"); // 50
    }

    @Test
    @DisplayName("birthDate가 null이면 null")
    void nullBirth() {
        assertThat(AgeGroups.rangeOf(null, today)).isNull();
    }
}
