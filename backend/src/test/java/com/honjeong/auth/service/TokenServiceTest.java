package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.honjeong.auth.domain.RefreshToken;
import com.honjeong.auth.repository.RefreshTokenRepository;
import com.honjeong.global.config.HonjeongJwtProperties;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.security.JwtProvider;

/**
 * {@link TokenService}의 단위 테스트. 토큰 발급·회전·거부 동작을 검증한다. RefreshTokenRepository는
 * Mockito로 모킹해 DB 없이 순수 로직만 본다.
 *
 * <p><b>두 개의 시계에 주의:</b> 이 테스트는 시계를 둘로 나눠 쓴다.
 * <ul>
 *   <li>{@code clock}(고정 시계): TokenService가 <b>refresh 만료</b>를 계산·판단할 때만 쓰는 시계다.
 *       시각을 2026-06-12로 고정해, refresh 만료 비교를 결정론적으로 만든다.</li>
 *   <li>{@code jwtProvider}의 시계(실시간 {@code systemUTC}): access JWT는 Nimbus 디코더가 <b>실제
 *       현재 시간</b>으로 만료를 검사한다. 그래서 JWT는 고정 시계가 아니라 실시간 시계로 발급해야
 *       디코딩 시 "이미 만료" 오류가 나지 않는다.</li>
 * </ul>
 * 즉 두 시계를 섞어 쓰는 게 의도된 설계이지 실수가 아니다.
 */
class TokenServiceTest {

    private static final String SECRET = "test-secret-please-be-long-enough-for-hs256-0123456789";

    // refresh 만료 검증용 고정 시계(TokenService 내부에서만 사용).
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T00:00:00Z"), ZoneOffset.UTC);
    // access JWT는 Nimbus 디코더가 실제 시간으로 만료를 보므로 실시간 시계로 발급한다.
    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3600, 900, Clock.systemUTC());
    private final HonjeongJwtProperties props = new HonjeongJwtProperties(SECRET, 3600, 1_209_600, 900);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final TokenService tokenService = new TokenService(jwtProvider, refreshTokenRepository, props, clock);

    /**
     * issue 검증.
     * given: save가 받은 인자를 그대로 반환하도록 모킹.
     * when: userId 42로 토큰을 발급.
     * then: access는 디코딩 시 subject가 "42", 만료(초)는 3600, 저장된 refresh는 원문과 다르고 64자(SHA-256 hex)다.
     *       즉 refresh 원문이 그대로 저장되지 않고 해시로만 저장됨을 확인한다.
     */
    @Test
    @DisplayName("issue: access는 userId로 디코딩되고, refresh는 원문이 아닌 해시로 저장된다")
    void issue_savesHashedRefreshAndReturnsDecodableAccess() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TokenPair pair = tokenService.issue(42L);

        assertThat(jwtProvider.decode(pair.accessToken()).getSubject()).isEqualTo("42");
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.expiresInSeconds()).isEqualTo(3600);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getTokenHash())
                .isNotEqualTo(pair.refreshToken())   // 원문 저장 금지
                .hasSize(64);                          // SHA-256 hex
    }

    /**
     * rotate 정상 경로 검증.
     * given: 유효(미만료·미회수)한 기존 refresh가 조회되도록 모킹(userId 7).
     * when: rotate 호출.
     * then: 기존 토큰이 회수(isRevoked=true)되고, 새 access는 같은 userId 7로 디코딩된다.
     */
    @Test
    @DisplayName("rotate: 기존 refresh를 회수하고 같은 userId로 새 토큰을 발급한다")
    void rotate_revokesOldAndIssuesNew() {
        RefreshToken existing = RefreshToken.issue(7L, "oldhash", LocalDateTime.now(clock).plusDays(1));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TokenPair pair = tokenService.rotate("any-raw-refresh");

        assertThat(existing.isRevoked()).isTrue();
        assertThat(jwtProvider.decode(pair.accessToken()).getSubject()).isEqualTo("7");
    }

    /**
     * rotate 거부 경로(미존재) 검증.
     * given: 어떤 해시로 조회해도 빈 결과가 나오도록 모킹.
     * when/then: 알 수 없는 refresh로 rotate하면 BusinessException이 발생한다.
     */
    @Test
    @DisplayName("rotate: 존재하지 않는 refresh는 거부한다")
    void rotate_rejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.rotate("bad")).isInstanceOf(BusinessException.class);
    }

    /**
     * rotate 거부 경로(이미 회수됨) 검증 — 회전 후 옛 토큰 재사용을 막는 핵심 보안 동작.
     * given: 회수(revoke)된 기존 refresh가 조회되도록 모킹.
     * when/then: 회수된 refresh로 rotate하면 isUsable이 false라 BusinessException이 발생한다.
     */
    @Test
    @DisplayName("rotate: 이미 회수된 refresh는 거부한다")
    void rotate_rejectsRevokedToken() {
        RefreshToken revoked = RefreshToken.issue(7L, "h", LocalDateTime.now(clock).plusDays(1));
        revoked.revoke();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> tokenService.rotate("x")).isInstanceOf(BusinessException.class);
    }
}
