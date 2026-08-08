package com.honjeong.push.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.InOrder;
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
    private static final int STALENESS_DAYS = 60;

    @BeforeEach
    void setUp() {
        service = new DeviceTokenService(deviceTokenRepository, FIXED, STALENESS_DAYS);
    }

    @Test
    @DisplayName("등록은 조회 없이 UPSERT 한 방이다 — 주인·플랫폼·시각을 그대로 넘긴다")
    void 등록은_upsert_한_방() {
        service.register(7L, "tok-new", Platform.IOS, null);

        verify(deviceTokenRepository).upsert(7L, "tok-new", "IOS", LocalDateTime.now(FIXED.withZone(KST)), null);
        // 조회 후 분기하면 앱 시작 시 등록·토큰갱신 경합에서 한쪽이 UNIQUE 위반으로 500이 난다.
        verify(deviceTokenRepository, never()).findByToken(any());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("플랫폼도 UPSERT 인자로 넘긴다 — 재등록 때 갱신되지 않으면 안드로이드가 붙을 때 어긋난다")
    void 플랫폼도_넘긴다() {
        service.register(9L, "tok-android", Platform.ANDROID, null);

        verify(deviceTokenRepository).upsert(eq(9L), eq("tok-android"), eq("ANDROID"), any(), isNull());
    }

    @Test
    @DisplayName("★ 설치 ID가 오면 같은 기기의 옛 토큰을 먼저 지운다 — 폐기 실패로 남은 고아 토큰이 여기서 정리된다")
    void 설치ID가_오면_같은_기기의_옛_토큰을_지운다() {
        service.register(7L, "tok-new", Platform.IOS, "install-1");

        InOrder order = inOrder(deviceTokenRepository);
        // 순서가 중요하다 — 지우고 넣어야 방금 넣은 행을 자기가 지우는 일이 없다.
        order.verify(deviceTokenRepository).deleteByInstallationIdAndTokenNot("install-1", "tok-new");
        order.verify(deviceTokenRepository).upsert(eq(7L), eq("tok-new"), eq("IOS"), any(), eq("install-1"));
    }

    @Test
    @DisplayName("★ 설치 ID가 없으면(구버전 앱) 아무것도 지우지 않는다 — 서버가 앱보다 먼저 배포된다")
    void 설치ID가_없으면_지우지_않는다() {
        // 지금 TestFlight에 나가 있는 빌드는 설치 ID를 보내지 않는다. 그 등록이 남의 행을
        // 지우면 안 되고, 기존 동작 그대로여야 한다.
        service.register(7L, "tok-legacy", Platform.IOS, null);

        verify(deviceTokenRepository, never()).deleteByInstallationIdAndTokenNot(any(), any());
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

    @Test
    @DisplayName("청소 임계값은 '지금 - stalenessDays'다 — 시스템 시계가 아니라 주입된 Clock을 쓴다")
    void 청소_임계값은_주입된_clock_기준() {
        service.sweepStale();

        verify(deviceTokenRepository).deleteAllByLastRegisteredAtBefore(
                LocalDateTime.now(FIXED.withZone(KST)).minusDays(STALENESS_DAYS));
    }
}
