package com.honjeong.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.Assert;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * 자체 발급 JWT(access·onboarding)의 생성·검증을 담당한다.
 *
 * <p>사용처: SecurityConfig(빈 등록·JwtDecoder 노출), AuthService·TokenService(토큰 발급).
 *
 * <p>HS256(HMAC) 대칭키 서명. 토큰 종류는 {@code typ} 클레임으로 구분하며
 * ({@link #TYPE_ACCESS}/{@link #TYPE_ONBOARDING}), 이 값이 Security 권한(ROLE_*)으로 매핑된다.
 * refresh 토큰은 JWT가 아니라 DB 저장 불투명 토큰이므로 여기서 다루지 않는다.
 */
public class JwtProvider {

    // 토큰 종류를 담는 클레임 이름과 그 값들. SecurityConfig가 이 값으로 ROLE을 매핑한다.
    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_ONBOARDING = "onboarding";

    private final JwtEncoder encoder;   // 토큰 서명·발급
    private final JwtDecoder decoder;   // 토큰 서명·만료 검증
    private final long accessTtlSeconds;        // access 토큰 유효기간(초)
    private final long onboardingTtlSeconds;    // onboarding 토큰 유효기간(초)
    private final Clock clock;          // 발급/만료 시각 계산용(테스트에서 고정 가능)

    /**
     * 대칭키·TTL·Clock으로 JWT 발급기(인코더)와 검증기(디코더)를 구성한다.
     *
     * <p>시크릿 문자열의 UTF-8 바이트로 HmacSHA256 키를 만들어 인코더·디코더(HS256) 모두 같은 키를 쓰게 한다.
     *
     * @param secret HS256 서명용 대칭키 문자열
     * @param accessTtlSeconds 액세스 토큰 유효기간(초)
     * @param onboardingTtlSeconds 온보딩 토큰 유효기간(초)
     * @param clock 발급/만료 시각의 기준 Clock(테스트에서 교체 가능)
     */
    public JwtProvider(String secret, long accessTtlSeconds, long onboardingTtlSeconds, Clock clock) {
        // 서명 키가 될 수 없는 값이면 여기서 부팅을 세운다. 조용히 뜨면 발급되는 모든 토큰이 무의미해진다.
        // ★특히 "${JWT_SECRET}" 검사가 핵심이다: 스프링 부트의 @ConfigurationProperties 바인딩은
        //   해석하지 못한 플레이스홀더에 예외를 던지지 않고 리터럴 문자열을 그대로 넘긴다. 그래서
        //   환경변수를 빠뜨린 채 prod로 띄우면 서명 키가 문자 그대로 "${JWT_SECRET}"이 되어 —
        //   누구나 아는 값이라 임의 사용자를 사칭하는 토큰을 위조할 수 있다. 실제로 그렇게 기동되는
        //   것을 확인하고 이 가드를 넣었다.
        Assert.hasText(secret, "honjeong.jwt.secret이 비어 있습니다.");
        Assert.isTrue(!secret.startsWith("${"),
                "honjeong.jwt.secret이 해석되지 않았습니다 — JWT_SECRET 환경변수를 주입하세요.");
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        Assert.isTrue(keyBytes.length >= 32,
                "honjeong.jwt.secret은 HS256용으로 최소 32바이트여야 합니다(현재 " + keyBytes.length + "바이트).");
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.accessTtlSeconds = accessTtlSeconds;
        this.onboardingTtlSeconds = onboardingTtlSeconds;
        this.clock = clock;
    }

    /**
     * 정식 로그인용 액세스 토큰을 발급한다(typ=access → 인가 시 ROLE_USER).
     *
     * @param userId sub 클레임에 담을 사용자 id
     * @return 서명된 JWT 문자열
     */
    public String createAccessToken(long userId) {
        return create(userId, TYPE_ACCESS, accessTtlSeconds);
    }

    /**
     * 가입 진행 중에만 쓰는 온보딩 임시 토큰을 발급한다(typ=onboarding → 인가 시 ROLE_ONBOARDING).
     *
     * <p>약관 동의·프로필 완료 단계 엔드포인트에서만 통용되고 일반 API는 통과하지 못한다.
     *
     * @param userId sub 클레임에 담을 사용자 id
     * @return 서명된 JWT 문자열(온보딩 단계 엔드포인트에서만 통용)
     */
    public String createOnboardingToken(long userId) {
        return create(userId, TYPE_ONBOARDING, onboardingTtlSeconds);
    }

    /**
     * 토큰의 서명과 만료를 검증하고 파싱된 클레임을 반환한다.
     *
     * @param token 검증할 JWT 문자열
     * @return 검증에 성공한 {@link Jwt}(sub·typ 등 클레임 포함)
     * @throws org.springframework.security.oauth2.jwt.JwtException 위조(서명 불일치)·만료 등 검증 실패 시
     */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    /**
     * 내부 디코더를 노출한다 — 리소스 서버({@code oauth2ResourceServer().jwt()})에 등록해
     * 들어온 토큰을 발급과 같은 키로 검증하게 하기 위함이다.
     *
     * @return HS256 대칭키 디코더
     */
    public JwtDecoder getDecoder() {
        return decoder;
    }

    /**
     * 공통 토큰 생성 로직 — sub·발급시각·만료·typ 클레임을 구성해 HS256으로 서명한다.
     *
     * <p>{@link #createAccessToken}/{@link #createOnboardingToken}이 typ와 TTL만 바꿔 호출한다.
     *
     * @param userId sub 클레임에 넣을 사용자 id
     * @param type typ 클레임 값(access/onboarding)
     * @param ttlSeconds 발급 시각으로부터의 만료까지 초
     * @return 서명된 JWT 문자열
     */
    private String create(long userId, String type, long ttlSeconds) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId))    // sub = 사용자 id(문자열). 검증 측에서 다시 Long으로 파싱.
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))     // 만료 = 현재 + TTL
                .claim(CLAIM_TYPE, type)                    // typ 클레임으로 토큰 종류 구분 → ROLE 매핑 근거
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
