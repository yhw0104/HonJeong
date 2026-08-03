package com.honjeong.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * {@link JwtProvider} 순수 단위 테스트.
 * 발급한 토큰의 sub·typ 클레임이 올바른지, 그리고 다른 시크릿으로 위조된 토큰은 거부되는지(서명 검증) 확인한다.
 */
class JwtProviderTest {

    private static final String SECRET = "test-secret-please-be-long-enough-for-hs256-0123456789";

    /** 테스트용 JwtProvider 생성 헬퍼(access TTL 3600초, onboarding TTL 900초). */
    private JwtProvider provider() {
        return new JwtProvider(SECRET, 3600, 900, Clock.systemUTC());
    }

    @Test
    @DisplayName("해석되지 않은 플레이스홀더가 시크릿으로 들어오면 생성에 실패한다")
    void 미해석_플레이스홀더는_거부된다() {
        // given: JWT_SECRET 환경변수가 없어 스프링이 플레이스홀더를 리터럴로 넘긴 상황
        // (@ConfigurationProperties 바인딩은 해석 못 한 플레이스홀더에 예외를 던지지 않고 문자열 그대로 준다)

        // when·then: "${JWT_SECRET}"이 서명 키가 되는 것을 생성자가 막는다
        assertThatThrownBy(() -> new JwtProvider("${JWT_SECRET}", 3600, 900, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("HS256 최소 길이(32바이트) 미만 시크릿은 거부된다")
    void 짧은_시크릿은_거부된다() {
        // given: 31바이트짜리 시크릿(HS256은 256bit=32바이트 이상이어야 한다)
        String tooShort = "0123456789012345678901234567890";

        // when·then: 생성자가 막는다
        assertThatThrownBy(() -> new JwtProvider(tooShort, 3600, 900, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }

    @Test
    @DisplayName("빈 시크릿은 거부된다")
    void 빈_시크릿은_거부된다() {
        assertThatThrownBy(() -> new JwtProvider("   ", 3600, 900, Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("액세스 토큰은 sub=userId, typ=access 클레임을 담는다")
    void createAccessToken_containsUserIdAndType() {
        // given: 정상 provider
        JwtProvider provider = provider();

        // when: userId=42로 액세스 토큰을 발급하고 디코딩하면
        String token = provider.createAccessToken(42L);
        Jwt jwt = provider.decode(token);

        // then: sub=42, typ=access 클레임이 담겨 있다
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("typ")).isEqualTo("access");
    }

    @Test
    @DisplayName("온보딩 토큰은 typ=onboarding 클레임을 담는다")
    void createOnboardingToken_hasOnboardingType() {
        // given: 정상 provider
        JwtProvider provider = provider();

        // when: userId=7로 온보딩 토큰을 발급하고 디코딩하면
        String token = provider.createOnboardingToken(7L);
        Jwt jwt = provider.decode(token);

        // then: sub=7, typ=onboarding 클레임이 담겨 있다
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("typ")).isEqualTo("onboarding");
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된(위조된) 토큰은 디코딩에 실패한다")
    void decode_rejectsTamperedToken() {
        // given: 정상 발급자와, 다른 시크릿을 쓰는 공격자 provider
        JwtProvider issuer = provider();
        JwtProvider attacker =
                new JwtProvider("a-completely-different-secret-key-0123456789-abcdef", 3600, 900, Clock.systemUTC());

        // when: 공격자가 서명한 토큰을
        String forged = attacker.createAccessToken(1L);

        // then: 발급자가 디코딩하면 서명 불일치로 JwtException이 발생한다
        assertThatThrownBy(() -> issuer.decode(forged)).isInstanceOf(JwtException.class);
    }
}
