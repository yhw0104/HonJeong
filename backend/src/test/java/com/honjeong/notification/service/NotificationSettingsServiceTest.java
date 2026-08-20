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
        assertThat(res.badge()).isTrue();
    }

    @Test
    @DisplayName("updateSettings: 행이 없으면 새로 저장하고 요청값을 반환한다")
    void updateSettings_insertsWhenAbsent() {
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(settingsRepository.save(any(NotificationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationSettingsResponse res =
                service.updateSettings(1L, new NotificationSettingsRequest(false, true, false, true, true));

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

        service.updateSettings(1L, new NotificationSettingsRequest(false, false, true, true, true));

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
        s.update(false, true, true, false, true); // meal off, mate on
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.of(s));

        assertThat(service.isEnabled(1L, NotificationType.MEAL_REQUEST_RECEIVED)).isFalse();
        assertThat(service.isEnabled(1L, NotificationType.MEAL_REQUEST_ACCEPTED)).isFalse();
        assertThat(service.isEnabled(1L, NotificationType.MATE_REQUEST_RECEIVED)).isTrue();
        assertThat(service.isEnabled(1L, NotificationType.MATE_REQUEST_ACCEPTED)).isTrue();
    }

    @Test
    @DisplayName("뱃지 알림은 행이 없으면 기본 ON")
    void badgeDefaultsOnWhenAbsent() {
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThat(service.isEnabled(1L, NotificationType.BADGE_EARNED)).isTrue();
    }

    /**
     * 예전에는 {@code BADGE_EARNED -> true}로 못 박혀 있어 사용자가 끌 방법이 없었다.
     * 이 단언이 그 하드코딩이 되살아나는 것을 막는다 — 되돌리면 여기서 빨개진다.
     */
    @Test
    @DisplayName("★뱃지 알림을 끄면 실제로 막힌다 — 설정과 무관하게 항상 ON이던 동작을 고친 것이다")
    void badgeCanBeTurnedOff() {
        NotificationSettings s = NotificationSettings.of(1L);
        s.update(true, true, true, false, false); // badge만 off
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.of(s));

        assertThat(service.isEnabled(1L, NotificationType.BADGE_EARNED)).isFalse();
        // 다른 종류는 그대로 통과해야 한다 — 껐다는 사실이 뱃지에만 걸리는지 함께 본다.
        assertThat(service.isEnabled(1L, NotificationType.MEAL_REQUEST_RECEIVED)).isTrue();
    }

    /**
     * ★badge 필드는 나중에 생겼다. 이미 배포된 앱(1.0.0 빌드 26)은 4필드만 보내므로,
     * badge를 원시 boolean으로 두면 Jackson이 false로 채워 <b>토글을 하나만 건드려도 뱃지 알림이
     * 조용히 꺼진다</b>. 그래서 요청의 badge는 Boolean이고 null이면 기존 값을 유지한다.
     */
    @Test
    @DisplayName("★badge가 없는 요청(구버전 앱)은 기존 badge 값을 건드리지 않는다")
    void updateSettings_nullBadgeKeepsExistingValue() {
        NotificationSettings existing = NotificationSettings.of(1L); // badge 기본 on
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(NotificationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        // 구버전 앱이 보내는 모양: badge 없음(null)
        NotificationSettingsResponse res =
                service.updateSettings(1L, new NotificationSettingsRequest(false, true, true, false, null));

        assertThat(existing.isBadgeEnabled()).isTrue(); // 꺼지지 않았다
        assertThat(res.badge()).isTrue();
        assertThat(existing.isMealEnabled()).isFalse(); // 보낸 값은 정상 반영
    }

    @Test
    @DisplayName("badge를 담아 보내면 저장된다")
    void updateSettings_persistsBadge() {
        NotificationSettings existing = NotificationSettings.of(1L);
        when(settingsRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(NotificationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationSettingsResponse res =
                service.updateSettings(1L, new NotificationSettingsRequest(true, true, true, false, false));

        assertThat(existing.isBadgeEnabled()).isFalse();
        assertThat(res.badge()).isFalse();
    }
}
