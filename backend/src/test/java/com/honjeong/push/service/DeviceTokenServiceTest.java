package com.honjeong.push.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.domain.Platform;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.user.domain.User;

/**
 * DeviceTokenService 단위 테스트(Mockito + 고정 Clock).
 *
 * <p>검증 목적: (1) 등록이 조회 없는 원자적 UPSERT인지 — 주인·플랫폼·시각을 그대로 넘기는지,
 * (2) 해제가 소유자를 확인하는지 — 남의 토큰은 지우지 않고, 없는 토큰을 지워도 예외가 나지 않는지를 본다.
 * 해제가 예외를 던지면 앱의 로그아웃이 막히므로 조용히 넘어가는 것이 요구사항이다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceTokenService")
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private DeviceTokenService service;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-08-07T03:00:00Z"), KST);

    @BeforeEach
    void setUp() {
        service = new DeviceTokenService(deviceTokenRepository, FIXED);
    }

    @Test
    @DisplayName("등록은 조회 없이 UPSERT 한 방이다 — 주인·플랫폼·시각을 그대로 넘긴다")
    void 등록은_upsert_한_방() {
        service.register(7L, "tok-new", Platform.IOS);

        verify(deviceTokenRepository).upsert(7L, "tok-new", "IOS", LocalDateTime.now(FIXED.withZone(KST)));
        // 조회 후 분기하면 앱 시작 시 등록·토큰갱신 경합에서 한쪽이 UNIQUE 위반으로 500이 난다.
        verify(deviceTokenRepository, never()).findByToken(any());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("플랫폼도 UPSERT 인자로 넘긴다 — 재등록 때 갱신되지 않으면 안드로이드가 붙을 때 어긋난다")
    void 플랫폼도_넘긴다() {
        service.register(9L, "tok-android", Platform.ANDROID);

        verify(deviceTokenRepository).upsert(eq(9L), eq("tok-android"), eq("ANDROID"), any());
    }

    @Test
    @DisplayName("남의 토큰은 지우지 못한다 — 내 것일 때만 삭제된다")
    void 남의_토큰은_삭제되지_않는다() {
        User other = mock(User.class);
        given(other.getId()).willReturn(99L);
        DeviceToken existing = mock(DeviceToken.class);
        given(existing.getUser()).willReturn(other);
        given(deviceTokenRepository.findByToken("tok-other")).willReturn(Optional.of(existing));

        service.unregister(7L, "tok-other");

        verify(deviceTokenRepository, never()).deleteByToken(any());
    }

    @Test
    @DisplayName("내 토큰이면 삭제한다")
    void 내_토큰은_삭제된다() {
        User me = mock(User.class);
        given(me.getId()).willReturn(7L);
        DeviceToken existing = mock(DeviceToken.class);
        given(existing.getUser()).willReturn(me);
        given(deviceTokenRepository.findByToken("tok-mine")).willReturn(Optional.of(existing));

        service.unregister(7L, "tok-mine");

        verify(deviceTokenRepository).deleteByToken("tok-mine");
    }

    @Test
    @DisplayName("없는 토큰을 지워도 예외가 나지 않는다 — 로그아웃이 실패하면 안 된다")
    void 없는_토큰_삭제는_무해하다() {
        given(deviceTokenRepository.findByToken("tok-gone")).willReturn(Optional.empty());

        service.unregister(7L, "tok-gone");

        verify(deviceTokenRepository, never()).deleteByToken(any());
    }
}
