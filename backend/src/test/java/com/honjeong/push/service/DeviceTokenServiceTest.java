package com.honjeong.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.domain.Platform;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * DeviceTokenService 단위 테스트(Mockito + 고정 Clock).
 *
 * <p>검증 목적: (1) 등록이 UPSERT인지 — 처음 보는 토큰은 저장하고 이미 있는 토큰은 주인만 갱신하는지,
 * (2) 해제가 소유자를 확인하는지 — 남의 토큰은 지우지 않고, 없는 토큰을 지워도 예외가 나지 않는지를 본다.
 * 해제가 예외를 던지면 앱의 로그아웃이 막히므로 조용히 넘어가는 것이 요구사항이다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceTokenService")
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private UserRepository userRepository;

    private DeviceTokenService service;

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-08-07T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        service = new DeviceTokenService(deviceTokenRepository, userRepository, FIXED);
    }

    @Test
    @DisplayName("처음 보는 토큰이면 새로 저장한다")
    void 신규_토큰_저장() {
        User user = mock(User.class);
        given(deviceTokenRepository.findByToken("tok-new")).willReturn(Optional.empty());
        given(userRepository.getReferenceById(7L)).willReturn(user);

        service.register(7L, "tok-new", Platform.IOS);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("tok-new");
        assertThat(captor.getValue().getPlatform()).isEqualTo(Platform.IOS);
    }

    @Test
    @DisplayName("이미 있는 토큰이면 저장하지 않고 주인을 지금 사용자로 갱신한다")
    void 기존_토큰은_주인만_갱신() {
        User owner = mock(User.class);
        DeviceToken existing = mock(DeviceToken.class);
        given(deviceTokenRepository.findByToken("tok-old")).willReturn(Optional.of(existing));
        given(userRepository.getReferenceById(9L)).willReturn(owner);

        service.register(9L, "tok-old", Platform.IOS);

        verify(existing).reassignTo(eq(owner), any());
        verify(deviceTokenRepository, never()).save(any());
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
