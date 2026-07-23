package com.honjeong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.honjeong.notification.domain.NotificationSettings;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.dto.NotificationSettingsRequest;
import com.honjeong.notification.dto.NotificationSettingsResponse;
import com.honjeong.notification.repository.NotificationSettingsRepository;

/** NotificationSettingsService 단위 테스트(Mockito). 기본값·업서트·타입 게이팅 매핑을 검증한다. */
class NotificationSettingsServiceTest {

    private final NotificationSettingsRepository settingsRepository =
            mock(NotificationSettingsRepository.class);
    private final NotificationSettingsService service =
            new NotificationSettingsService(settingsRepository);

    @Test
    @DisplayName("getSettings: 행이 없으면 기본값(meal·mate·notice on, marketing off)")
    void getSettings_defaultsWhenAbsent() {
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.empty());

        NotificationSettingsResponse res = service.getSettings(1L);

        assertThat(res.meal()).isTrue();
        assertThat(res.mate()).isTrue();
        assertThat(res.notice()).isTrue();
        assertThat(res.marketing()).isFalse();
    }

    @Test
    @DisplayName("updateSettings: 행이 없으면 새로 저장하고 요청값을 반환한다")
    void updateSettings_insertsWhenAbsent() {
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(settingsRepository.save(any(NotificationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationSettingsResponse res =
                service.updateSettings(1L, new NotificationSettingsRequest(false, true, false, true));

        ArgumentCaptor<NotificationSettings> captor = ArgumentCaptor.forClass(NotificationSettings.class);
        verify(settingsRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().isMealEnabled()).isFalse();
        assertThat(captor.getValue().isMarketingEnabled()).isTrue();
        assertThat(res.meal()).isFalse();
        assertThat(res.notice()).isFalse();
        assertThat(res.marketing()).isTrue();
    }

    @Test
    @DisplayName("updateSettings: 행이 있으면 그 엔티티를 갱신한다")
    void updateSettings_updatesWhenPresent() {
        NotificationSettings existing = NotificationSettings.of(1L); // 전부 기본값
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(NotificationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateSettings(1L, new NotificationSettingsRequest(false, false, true, true));

        assertThat(existing.isMealEnabled()).isFalse();
        assertThat(existing.isMateEnabled()).isFalse();
        assertThat(existing.isMarketingEnabled()).isTrue();
    }

    @Test
    @DisplayName("isEnabled: 행 없으면 meal/mate 타입 모두 기본 true")
    void isEnabled_defaultTrueWhenAbsent() {
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThat(service.isEnabled(1L, NotificationType.MEAL_REQUEST_RECEIVED)).isTrue();
        assertThat(service.isEnabled(1L, NotificationType.MATE_REQUEST_ACCEPTED)).isTrue();
    }

    @Test
    @DisplayName("isEnabled: meal off면 MEAL_* 막고 MATE_*는 허용(타입→필드 매핑)")
    void isEnabled_mapsTypeToField() {
        NotificationSettings s = NotificationSettings.of(1L);
        s.update(false, true, true, false); // meal off, mate on
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.of(s));

        assertThat(service.isEnabled(1L, NotificationType.MEAL_REQUEST_RECEIVED)).isFalse();
        assertThat(service.isEnabled(1L, NotificationType.MEAL_REQUEST_ACCEPTED)).isFalse();
        assertThat(service.isEnabled(1L, NotificationType.MATE_REQUEST_RECEIVED)).isTrue();
        assertThat(service.isEnabled(1L, NotificationType.MATE_REQUEST_ACCEPTED)).isTrue();
    }

    @Test
    @DisplayName("뱃지 알림은 설정과 무관하게 항상 ON")
    void badgeAlwaysEnabled() {
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThat(service.isEnabled(1L, NotificationType.BADGE_EARNED)).isTrue();
    }
}
