package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.user.domain.UserFoodPreference;
import com.honjeong.user.repository.UserFoodPreferenceRepository;

@ExtendWith(MockitoExtension.class)
class UserFoodPreferenceServiceTest {

    @Mock
    private UserFoodPreferenceRepository repository;

    @InjectMocks
    private UserFoodPreferenceService service;

    @Test
    @DisplayName("replaceFoods: 기존 행이 없고 목록이 있으면 새로 저장하고 그 목록을 돌려준다")
    void replaceCreatesWhenAbsent() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(UserFoodPreference.class))).thenAnswer(inv -> inv.getArgument(0));

        List<String> result = service.replaceFoods(1L, List.of("한식", "일식"));

        assertThat(result).containsExactly("한식", "일식");
        verify(repository).save(any(UserFoodPreference.class));
    }

    @Test
    @DisplayName("replaceFoods: 기존 행이 있으면 교체하고(save 호출 없이 dirty) 새 목록을 돌려준다")
    void replaceUpdatesWhenPresent() {
        UserFoodPreference existing = UserFoodPreference.of(1L, List.of("한식"));
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));

        List<String> result = service.replaceFoods(1L, List.of("양식", "중식"));

        assertThat(result).containsExactly("양식", "중식");
        assertThat(existing.toFoods()).containsExactly("양식", "중식");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("replaceFoods: 목록이 null이면 미변경 — 저장 안 하고 현재값을 돌려준다")
    void replaceNullNoChange() {
        UserFoodPreference existing = UserFoodPreference.of(1L, List.of("한식"));
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));

        List<String> result = service.replaceFoods(1L, null);

        assertThat(result).containsExactly("한식");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("replaceFoods: 빈 목록 + 기존 행 없음이면 저장 없이 빈 목록")
    void replaceEmptyNoRow() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());

        List<String> result = service.replaceFoods(1L, List.of());

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("getFoods: 행이 없으면 빈 목록")
    void getFoodsEmpty() {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThat(service.getFoods(1L)).isEmpty();
    }
}
