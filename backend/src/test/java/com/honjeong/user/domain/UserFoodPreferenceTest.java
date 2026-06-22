package com.honjeong.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserFoodPreferenceTest {

    @Test
    @DisplayName("of: 음식 3개를 받으면 toFoods가 같은 순서로 3개를 돌려준다")
    void ofThreeFoods() {
        UserFoodPreference pref = UserFoodPreference.of(1L, List.of("한식", "일식", "면 요리"));
        assertThat(pref.getUserId()).isEqualTo(1L);
        assertThat(pref.toFoods()).containsExactly("한식", "일식", "면 요리");
    }

    @Test
    @DisplayName("of: 3개 초과로 주면 앞 3개만 보관한다(방어)")
    void ofTruncatesBeyondThree() {
        UserFoodPreference pref = UserFoodPreference.of(1L, List.of("a", "b", "c", "d"));
        assertThat(pref.toFoods()).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("of: 빈/일부만 주면 toFoods는 비어있지 않은 값만 순서대로 돌려준다")
    void ofPartial() {
        assertThat(UserFoodPreference.of(1L, List.of()).toFoods()).isEmpty();
        assertThat(UserFoodPreference.of(1L, List.of("한식")).toFoods()).containsExactly("한식");
    }

    @Test
    @DisplayName("updateFoods: 기존 값을 통째로 교체한다")
    void updateReplaces() {
        UserFoodPreference pref = UserFoodPreference.of(1L, List.of("한식", "일식"));
        pref.updateFoods(List.of("양식"));
        assertThat(pref.toFoods()).containsExactly("양식");
    }
}
