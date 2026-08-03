package com.honjeong.auth.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.dto.AuthResultResponse;
import com.honjeong.auth.dto.CompleteProfileRequest;
import com.honjeong.auth.dto.OAuthLoginRequest;
import com.honjeong.auth.dto.PhoneSendRequest;
import com.honjeong.auth.dto.PhoneVerifyRequest;
import com.honjeong.auth.dto.RefreshRequest;
import com.honjeong.auth.dto.TermsRequest;
import com.honjeong.auth.dto.TokenResponse;
import com.honjeong.auth.service.AuthService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.CurrentUserId;

import jakarta.validation.Valid;

/**
 * 인증 REST 컨트롤러 — 회원가입/로그인 진입부터 온보딩 완료, 토큰 재발급·로그아웃까지의 HTTP
 * 엔드포인트를 담당한다.
 *
 * <p>기본 경로: /api/auth
 *
 * <p>컨트롤러는 얇게 유지하는 게 원칙이라, 여기서는 ① 요청 본문 검증({@code @Valid}) ② DTO ↔ 서비스 입력 변환
 * ③ {@link AuthService} 호출 ④ 결과를 응답 DTO로 감싸 공통 엔벨로프({@link ApiResponse})로 반환하는 일만 하고,
 * 실제 비즈니스 로직(인증번호 검증, 회원 상태 판별, 토큰 발급 등)은 전부 {@code AuthService}에 위임한다.
 *
 * <p>인증 흐름 요약:
 * <ul>
 *   <li>진입(소셜 {@code /oauth/{provider}} 또는 휴대폰 {@code /phone/verify}): 기존 ACTIVE 회원이면 곧장 로그인 토큰,
 *       신규/미완(PENDING) 회원이면 <b>온보딩 토큰</b>을 받아 온보딩 단계로 넘어간다.</li>
 *   <li>온보딩: 온보딩 토큰을 들고 {@code /terms}(약관 동의) → {@code /complete}(프로필 완료)를 거치면 가입이 확정되며
 *       정식 access/refresh 토큰을 발급받는다.</li>
 *   <li>운영: {@code /refresh}(토큰 회전), {@code /logout}(refresh 무효화).</li>
 * </ul>
 *
 * <p>인증 필요 여부는 {@code SecurityConfig}에서 경로별로 정해진다. {@code /oauth/**}, {@code /phone/**},
 * {@code /refresh}는 토큰 없이 접근 가능(permitAll)하고, {@code /terms}·{@code /complete}는 온보딩(또는 정식) 토큰이
 * 있어야 통과한다. 토큰이 필요한 메서드는 {@code @CurrentUserId}로 JWT의 sub(사용자 id)를 직접 주입받는다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 소셜 로그인(카카오/애플) 진입.
     *
     * <p>사용 화면: 앱에서는 아직 쓰지 않는다 — 시작 화면(Welcome)의 카카오/애플 버튼은 API 미연동 목업이다.
     *
     * <p><b>인증:</b> 불필요(SecurityConfig에서 {@code /api/auth/oauth/**}는 permitAll). 진입 단계이므로 토큰이 없다.
     *
     * @param provider 소셜 공급자(kakao/apple) — 대소문자를 가리지 않는다
     * @param request 소셜에서 받은 {@code idToken}
     * @return 신규/미완이면 onboarding=true + onboardingToken, 기존 ACTIVE 회원이면 access/refresh/expiresIn
     *         (해당 없는 필드는 null이라 직렬화에서 빠진다)
     */
    @PostMapping("/oauth/{provider}")
    public ApiResponse<AuthResultResponse> oauth(@PathVariable String provider,
            @RequestBody @Valid OAuthLoginRequest request) {
        AuthResultResponse body = AuthResultResponse.from(authService.oauthLogin(parseProvider(provider), request.idToken()));
        return ApiResponse.success(body);
    }

    /**
     * 휴대폰 인증번호 발송 — 인증코드를 생성·저장하고 SMS로 보낸다.
     *
     * <p>사용 화면: 휴대폰 번호 입력(PhoneAuth)의 인증번호 요청, 인증번호 입력(VerifyCode)의 재전송 버튼.
     *
     * <p>개발(mock) 모드에서는 항상 {@code 000000}이 발급된다.
     *
     * <p><b>인증:</b> 토큰은 불필요하지만, {@code honjeong.sms.mode}가 {@code real}이 아닌 동안
     * SecurityConfig가 {@code /api/auth/phone/**}를 차단하므로 이 경로는 401로 끊긴다. 현재 real SMS
     * 구현체가 없어 사실상 항상 차단 상태다 — mock 인증번호가 고정값이라 공개 서버에서 무인증 계정
     * 생성에 쓰일 수 있기 때문이다. real 게이트웨이가 붙으면 permitAll로 자동 복귀한다.
     *
     * @param request 인증번호를 받을 휴대폰 번호
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @PostMapping("/phone/send-code")
    public ApiResponse<Void> sendCode(@RequestBody @Valid PhoneSendRequest request) {
        authService.sendPhoneCode(request.phone());
        return ApiResponse.<Void>success(null); // null 인자 → 제네릭 추론 불가라 <Void> 명시
    }

    /**
     * 휴대폰 인증번호 확인(휴대폰 방식의 로그인/회원가입 진입).
     *
     * <p>사용 화면: 인증번호 입력(VerifyCode) — 입력한 인증번호 확인 후 로그인/온보딩으로 분기.
     *
     * <p>만료·시도횟수·일치 여부를 따져 검증하고, 기존 ACTIVE 회원이면 로그인 토큰을, 신규/미완 회원이면
     * PENDING 사용자를 만들고 온보딩 토큰을 돌려준다.
     *
     * <p><b>인증:</b> 토큰은 불필요하지만, {@code honjeong.sms.mode}가 {@code real}이 아닌 동안
     * SecurityConfig가 {@code /api/auth/phone/**}를 차단하므로 이 경로는 401로 끊긴다. 현재 real SMS
     * 구현체가 없어 사실상 항상 차단 상태다 — mock 인증번호가 고정값이라 공개 서버에서 무인증 계정
     * 생성에 쓰일 수 있기 때문이다. real 게이트웨이가 붙으면 permitAll로 자동 복귀한다.
     *
     * @param request 휴대폰 번호와 입력한 인증번호
     * @return 신규/미완이면 onboarding=true + onboardingToken, 기존 ACTIVE 회원이면 access/refresh/expiresIn
     */
    @PostMapping("/phone/verify")
    public ApiResponse<AuthResultResponse> verify(@RequestBody @Valid PhoneVerifyRequest request) {
        AuthResultResponse body = AuthResultResponse.from(authService.verifyPhone(request.phone(), request.code()));
        return ApiResponse.success(body);
    }

    /**
     * 약관 동의(온보딩 1단계). 이미 동의했으면 멱등 처리한다.
     *
     * <p>사용 화면: 프로필 설정(ProfileSetup) — 가입 완료 제출 시 약관 동의를 함께 전송.
     *
     * <p>필수 4종(age/service/privacy/location)이 모두 true인지는 서비스 계층에서 검증한다.
     *
     * <p><b>인증:</b> <b>온보딩 토큰 필요</b>. SecurityConfig에서 {@code /api/auth/terms}는 {@code ONBOARDING|USER}
     * 권한이 있어야 통과하고, 토큰의 sub가 {@code @CurrentUserId Long userId}로 주입된다(별도 DB 조회 없음).
     *
     * @param userId 인증 사용자 ID(온보딩 토큰의 sub)
     * @param request 약관별 동의 여부(age/service/privacy/location/marketing)
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @PostMapping("/terms")
    public ApiResponse<Void> terms(@CurrentUserId Long userId, @RequestBody @Valid TermsRequest request) {
        authService.agreeTerms(userId, request.age(), request.service(), request.privacy(), request.location(), request.marketing());
        return ApiResponse.<Void>success(null); // 반환 데이터 없음 → <Void> 명시
    }

    /**
     * 프로필 완료(온보딩 2단계) → 가입 확정 및 정식 토큰 발급.
     *
     * <p>사용 화면: 프로필 설정(ProfileSetup) — 닉네임 등 프로필 제출로 가입 확정.
     *
     * <p>닉네임 중복 검사 후 프로필을 채워 가입을 확정(ACTIVE)하고 정식 토큰을 발급한다.
     *
     * <p><b>인증:</b> <b>온보딩 토큰 필요</b>({@code terms}와 동일하게 {@code ONBOARDING|USER}).
     *
     * @param userId 인증 사용자 ID(온보딩 토큰의 sub)
     * @param request nickname(필수)·gender·birthDate·introduction·region·regionLat/Lng·diningStyle·
     *                profileImageUrl·favoriteFoods
     * @return accessToken, refreshToken, expiresIn(초)
     */
    @PostMapping("/complete")
    public ApiResponse<TokenResponse> complete(@CurrentUserId Long userId,
            @RequestBody @Valid CompleteProfileRequest request) {
        return ApiResponse.success(TokenResponse.from(authService.complete(userId, request.toCommand())));
    }

    /**
     * 액세스 토큰 재발급 — 제시된 refresh 토큰을 검증하고 새 토큰 쌍으로 회전(rotate)한다.
     *
     * <p>사용 화면: 앱 전역 인증 컨텍스트(AuthContext) — 앱 시작/토큰 만료 시 세션 자동 복구(특정 화면 아님).
     *
     * <p><b>인증:</b> 불필요(permitAll, {@code /api/auth/refresh}). 만료된 access 토큰으로도 갱신할 수 있어야 하므로
     * refresh 토큰 자체를 본문으로 받아 검증한다.
     *
     * @param request 보유 중인 리프레시 토큰 원문
     * @return 새 accessToken, refreshToken, expiresIn(초)
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.success(TokenResponse.from(authService.refresh(request.refreshToken())));
    }

    /**
     * 로그아웃 — 제시된 리프레시 토큰을 폐기(revoke)해 더는 회전에 쓰지 못하게 한다.
     *
     * <p>사용 화면: 더보기(More)의 로그아웃 버튼(실 호출은 앱 전역 인증 컨텍스트 AuthContext.signOut).
     *
     * <p>무상태 JWT라 access 토큰 자체는 만료될 때까지 유효하다.
     *
     * <p><b>인증:</b> SecurityConfig에 별도 permitAll 규칙이 없어 기본 정책({@code anyRequest().hasRole("USER")})을
     * 따른다 — 즉 정식 access 토큰이 있어야 호출할 수 있다.
     *
     * @param request 무효화할 리프레시 토큰 원문
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.<Void>success(null); // 반환 데이터 없음 → <Void> 명시
    }

    /**
     * 경로변수의 공급자 문자열을 {@link Provider} enum으로 변환한다.
     *
     * <p>대소문자를 가리지 않도록 {@code toUpperCase()} 후 {@code Provider.valueOf(...)}로 매칭하며, 일치하는 enum이 없으면
     * {@code valueOf}가 던지는 {@link IllegalArgumentException}을 잡아 {@code INVALID_INPUT}({@link BusinessException})으로
     * 바꿔 던진다 — 결과적으로 클라이언트에는 400(잘못된 요청)으로 응답된다. 예: {@code kakao}/{@code KAKAO} → 정상,
     * {@code naver} → 400.
     *
     * @param provider 경로변수로 받은 공급자 문자열
     * @return 매칭된 공급자 enum
     */
    private Provider parseProvider(String provider) {
        try {
            return Provider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 공급자입니다.");
        }
    }
}
