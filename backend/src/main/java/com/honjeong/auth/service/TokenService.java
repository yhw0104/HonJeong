package com.honjeong.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.auth.domain.RefreshToken;
import com.honjeong.auth.repository.RefreshTokenRepository;
import com.honjeong.global.config.HonjeongJwtProperties;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;

/**
 * 1. 기능: JWT access 토큰과 불투명 refresh 토큰(해시 저장)의 발급·회전(rotate)·무효화(revoke)
 * 2. 사용 Controller: (직접 주입 없음 — AuthService에서 사용)
 *
 * <p>[기존 주석] 토큰 발급·관리 서비스. 인증에 쓰는 두 종류의 토큰을 다룬다.
 *
 * <p>1) access 토큰: JWT(무상태). 서버가 별도로 저장하지 않고, 클라가 보내온 토큰의 서명만
 *    검증해 신원을 확인한다. 수명이 짧다(accessTtlSeconds).
 * <p>2) refresh 토큰: 불투명(opaque) 랜덤 문자열. JWT가 아니라 의미 없는 난수라서, 그 자체로는
 *    아무 정보도 담지 않는다. 서버 DB에 저장해 두고 access 재발급 때 대조한다. 수명이 길다.
 *
 * <p><b>refresh 원문을 저장하지 않고 SHA-256 해시만 저장하는 이유:</b> DB가 유출돼도 저장된 해시값
 * 으로는 원문 refresh를 역산할 수 없으므로(단방향 해시) 토큰을 탈취당하지 않는다. 원문은 오직
 * 클라이언트만 보관하고, 서버는 들어온 원문을 매번 같은 방식으로 해시해 저장된 해시와 비교한다.
 * (비밀번호를 평문이 아닌 해시로 저장하는 것과 같은 원리.)
 *
 * <p>재발급(rotate) 시에는 기존 refresh를 회수(revoke)하고 새 토큰 쌍을 발급하므로, 한 번 쓴
 * refresh는 재사용할 수 없다(회전 = rotation). 로그아웃/탈취 시에는 revoke로 즉시 무효화한다.
 */
@Service
public class TokenService {

    // 불투명 refresh 토큰의 난수 생성기. 암호학적으로 안전한 SecureRandom을 클래스 전체가 공유한다.
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtProvider jwtProvider;                       // access JWT 생성/검증 담당
    private final RefreshTokenRepository refreshTokenRepository; // refresh 해시를 저장·조회하는 저장소
    private final long accessTtlSeconds;                         // access 토큰 유효기간(초)
    private final long refreshTtlSeconds;                        // refresh 토큰 유효기간(초)
    private final Clock clock;                                   // 현재 시각 공급자(테스트에서 고정 시계 주입 가능)

    /**
     * 의존성을 주입받아 토큰 서비스를 구성한다. access/refresh 유효기간은 설정값
     * ({@link HonjeongJwtProperties})에서 읽어 필드에 보관한다.
     *
     * @param jwtProvider             access JWT 발급·검증기
     * @param refreshTokenRepository  refresh 토큰(해시) 저장소
     * @param properties              JWT 관련 설정(유효기간 등)
     * @param clock                   현재 시각 공급자(테스트 시 고정 시계 주입용)
     */
    public TokenService(JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository,
            HonjeongJwtProperties properties, Clock clock) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTtlSeconds = properties.accessTokenTtlSeconds();
        this.refreshTtlSeconds = properties.refreshTokenTtlSeconds();
        this.clock = clock;
    }

    /**
     * 기능: 사용자에게 새 토큰 쌍(access JWT + refresh 난수) 발급 — refresh는 해시로만 DB 저장
     * Request: userId — 토큰을 발급받을 사용자 ID
     * Response: TokenPair — access 토큰, refresh 원문, access 만료(초)
     *
     * <p>[기존 주석] 주어진 사용자에게 새 토큰 쌍(access + refresh)을 발급한다. 로그인·온보딩 완료 시 호출되는
     * 가장 기본적인 발급 메서드다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>userId로 access JWT를 생성한다(서버 저장 없음).</li>
     *   <li>불투명 refresh 원문을 난수로 생성한다({@link #generateOpaqueToken}).</li>
     *   <li>refresh 만료 시각을 (현재 + refreshTtlSeconds)로 계산한다.</li>
     *   <li>refresh는 <b>원문이 아니라 해시</b>로 변환해 DB에 저장한다({@link #hash}).</li>
     *   <li>클라이언트에는 access와 refresh <b>원문</b>, access 만료(초)를 묶어 돌려준다.</li>
     * </ol>
     * 즉 refresh 원문은 이 반환값으로 한 번만 클라에 전달되고, 서버에는 해시만 남는다.
     *
     * @param userId 토큰을 발급받을 사용자 식별자
     * @return access 토큰·refresh 원문·access 만료(초)를 담은 {@link TokenPair}
     */
    @Transactional // 토큰 저장(쓰기)을 한 트랜잭션으로 묶는다
    public TokenPair issue(long userId) {
        String accessToken = jwtProvider.createAccessToken(userId);
        String rawRefresh = generateOpaqueToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusSeconds(refreshTtlSeconds);
        refreshTokenRepository.save(RefreshToken.issue(userId, hash(rawRefresh), expiresAt)); // 해시만 저장
        return new TokenPair(accessToken, rawRefresh, accessTtlSeconds); // 원문은 반환값으로만 전달
    }

    /**
     * 기능: refresh 토큰 회전 — 유효성 확인 후 기존 토큰 revoke + 새 토큰 쌍 재발급(재사용 차단)
     * Request: rawRefreshToken — 클라이언트가 보관하던 refresh 원문
     * Response: TokenPair — 새로 발급된 access/refresh 토큰 쌍 (무효 토큰이면 INVALID_REFRESH_TOKEN 예외)
     *
     * <p>[기존 주석] refresh 토큰을 회전(rotate)해 새 토큰 쌍을 재발급한다. access가 만료됐을 때 클라가 보관 중인
     * refresh 원문을 보내오면, 그 신원을 확인하고 새 access·refresh를 내준다.
     *
     * <p>동작 단계:
     * <ol>
     *   <li>받은 원문을 해시해, 같은 해시를 가진 refresh를 DB에서 조회한다.</li>
     *   <li>조회된 토큰이 "사용 가능한지"(만료되지 않았고 회수되지 않았는지)를 현재 시각 기준으로 확인한다.</li>
     *   <li>없거나 사용 불가면 {@code INVALID_REFRESH_TOKEN} 예외를 던진다.</li>
     *   <li>유효하면 기존 토큰을 즉시 회수(revoke)한다 — 한 번 쓴 refresh는 재사용 불가(회전).</li>
     *   <li>같은 userId로 {@link #issue}를 호출해 완전히 새로운 토큰 쌍을 발급한다.</li>
     * </ol>
     * 회전 덕분에, 탈취된 옛 refresh가 나중에 다시 제시돼도 이미 회수된 상태라 거부된다.
     *
     * @param rawRefreshToken 클라이언트가 보관하던 refresh <b>원문</b>
     * @return 새로 발급된 access·refresh 토큰 쌍
     * @throws BusinessException 토큰이 없거나 만료·회수되어 사용할 수 없을 때({@code INVALID_REFRESH_TOKEN})
     */
    @Transactional // 조회→회수→재발급을 한 트랜잭션으로 묶는다
    public TokenPair rotate(String rawRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)) // 원문 해시로 조회
                .filter(t -> t.isUsable(LocalDateTime.now(clock)))                          // 만료·회수 여부 확인
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)); // 없거나 못 쓰면 거부
        token.revoke();                  // 기존 토큰 회수(재사용 차단)
        return issue(token.getUserId()); // 같은 사용자로 새 쌍 발급
    }

    /**
     * 기능: 제시된 refresh 토큰 무효화(로그아웃) — 없으면 조용히 무시(멱등)
     * Request: rawRefreshToken — 무효화할 refresh 원문
     * Response: 없음(void)
     *
     * <p>[기존 주석] 로그아웃 처리. 제시된 refresh 원문에 해당하는 토큰을 찾아 무효화(revoke)한다. 무효화된 토큰은
     * 이후 {@link #rotate}에서 재발급에 쓸 수 없다.
     *
     * <p>해당 토큰이 DB에 없으면(이미 만료·삭제되었거나 잘못된 값이면) 아무 일도 하지 않고 조용히
     * 넘어간다 — 로그아웃은 "이미 무효" 상태여도 성공으로 보는 것이 자연스럽기 때문이다(예외 안 던짐).
     *
     * @param rawRefreshToken 무효화할 refresh <b>원문</b>
     */
    @Transactional // 회수(쓰기)를 트랜잭션으로 묶는다
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(RefreshToken::revoke); // 있을 때만 회수
    }

    /**
     * 기능: 불투명 refresh 토큰 원문 생성(SecureRandom 32바이트 → URL-safe Base64)
     *
     * <p>[기존 주석] 불투명(opaque) refresh 토큰 원문을 생성한다. 암호학적으로 안전한 난수 32바이트를 만든 뒤,
     * URL-safe Base64(패딩 없음)로 인코딩해 문자열로 돌려준다. JWT처럼 의미를 담지 않는 순수 난수라서
     * 추측이 불가능하다.
     *
     * @return URL-safe Base64로 인코딩된 32바이트 난수 문자열
     */
    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];                                              // 256비트 난수
        RANDOM.nextBytes(bytes);                                                  // SecureRandom으로 채움
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);     // URL 안전·패딩 제거 인코딩
    }

    /**
     * 기능: 문자열을 SHA-256 해시(64자리 hex)로 변환 — refresh 원문 저장·대조에 공통 사용
     *
     * <p>[기존 주석] 문자열을 SHA-256으로 해시해 16진수 문자열로 반환한다. refresh 원문을 저장·조회할 때 항상 이
     * 메서드를 거치므로, 같은 원문은 항상 같은 해시(64자리 hex)가 되어 DB의 저장 해시와 대조할 수 있다.
     *
     * @param value 해시할 원문(여기서는 refresh 토큰)
     * @return SHA-256 다이제스트의 16진수 표현(64자)
     * @throws IllegalStateException 실행 환경이 SHA-256을 지원하지 않을 때(사실상 발생하지 않음)
     */
    private static String hash(String value) {
        try {
            // UTF-8 바이트로 변환 후 SHA-256 다이제스트(단방향) 계산
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest); // 바이트 → 16진수 문자열(64자)
        } catch (NoSuchAlgorithmException e) {
            // 표준 JDK라면 SHA-256은 항상 존재하므로 이 분기는 사실상 도달하지 않는다.
            throw new IllegalStateException("SHA-256 미지원", e);
        }
    }
}
