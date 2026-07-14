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
 * 인증(휴대폰 SMS 인증·소셜 로그인·온보딩·토큰 재발급/로그아웃) 컨트롤러.
 *
 * <p>기본 경로: /api/auth
 *
 * <p>[기존 주석] 인증 REST 컨트롤러 — 회원가입/로그인 진입부터 온보딩 완료, 토큰 재발급·로그아웃까지의 HTTP 엔드포인트를 담당한다.
 *
 * <p>모든 경로는 클래스의 {@code @RequestMapping("/api/auth")}가 접두사라서 {@code /api/auth/...} 형태가 된다.
 * 컨트롤러는 얇게 유지하는 게 원칙이라, 여기서는 ① 요청 본문 검증({@code @Valid}) ② DTO ↔ 서비스 입력 변환
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
     * 1. API 주소: POST /api/auth/oauth/{provider}
     * 2. 사용 화면: (앱 미사용 — 백엔드 내부용; 시작 화면(Welcome)의 카카오/애플 버튼은 아직 API 미연동 목업)
     * 3. Request: provider(경로) — 소셜 공급자(kakao/apple) / OAuthLoginRequest(바디) — idToken(공급자 발급 ID 토큰)
     * 4. Response: AuthResultResponse — 신규/미완이면 onboarding=true+onboardingToken, 기존 ACTIVE 회원이면 access/refresh/expiresIn
     *
     * <p>[기존 주석] 소셜 로그인(카카오/애플) 진입.
     *
     * <p><b>요청:</b> {@code POST /api/auth/oauth/{provider}} — 경로변수 {@code provider}는 {@code kakao}/{@code apple}
     * 같은 공급자 문자열이고, 본문은 {@link OAuthLoginRequest}(소셜에서 받은 {@code idToken}). {@code @Valid}로 idToken
     * 공백 여부를 먼저 검증한다.
     *
     * <p><b>동작:</b> {@link #parseProvider(String)}로 경로 문자열을 {@link Provider} enum으로 바꾼 뒤
     * {@code authService.oauthLogin(...)}에 위임한다. 신규 회원이면 온보딩 토큰만, 기존 ACTIVE 회원이면 로그인 토큰(3종)이
     * 담긴 {@link com.honjeong.auth.service.AuthResult}가 돌아오고, 이를 {@link AuthResultResponse#from}으로 응답 DTO로 변환한다.
     *
     * <p><b>응답:</b> {@code ApiResponse<AuthResultResponse>}. 신규면 {@code onboarding=true}와 {@code onboardingToken}만,
     * 기존이면 access/refresh/expiresIn만 직렬화된다(나머지 null 필드는 생략).
     *
     * <p><b>인증:</b> 불필요(SecurityConfig에서 {@code /api/auth/oauth/**}는 permitAll). 진입 단계이므로 토큰이 없다.
     */
    @PostMapping("/oauth/{provider}")
    public ApiResponse<AuthResultResponse> oauth(@PathVariable String provider,
            @RequestBody @Valid OAuthLoginRequest request) {
        AuthResultResponse body = AuthResultResponse.from(authService.oauthLogin(parseProvider(provider), request.idToken()));
        return ApiResponse.success(body);
    }

    /**
     * 1. API 주소: POST /api/auth/phone/send-code
     * 2. 사용 화면: 휴대폰 번호 입력(PhoneAuth) — 인증번호 요청 / 인증번호 입력(VerifyCode) — 재전송 버튼
     * 3. Request: PhoneSendRequest(바디) — phone(인증번호 받을 휴대폰 번호)
     * 4. Response: 없음(Void) — 성공 여부만 응답 봉투로 전달
     *
     * <p>[기존 주석] 휴대폰 인증번호 발송.
     *
     * <p><b>요청:</b> {@code POST /api/auth/phone/send-code}, 본문은 {@link PhoneSendRequest}(받는 휴대폰번호 {@code phone}).
     * {@code @Valid}로 phone 공백 여부를 검증한다.
     *
     * <p><b>동작:</b> {@code authService.sendPhoneCode(phone)}에 위임 — 인증코드를 생성·저장하고 SMS로 보낸다.
     * 개발 모드에서는 항상 {@code 000000}이 발급된다.
     *
     * <p><b>응답:</b> 반환할 데이터가 없으므로 {@code ApiResponse<Void>}. {@code ApiResponse.<Void>success(null)}처럼
     * 타입 인자 {@code <Void>}를 명시하는 이유는, 인자가 {@code null}이라 컴파일러가 제네릭 {@code T}를 추론하지 못하기
     * 때문이다(메서드 반환형 {@code ApiResponse<Void>}와 맞추기 위함).
     *
     * <p><b>인증:</b> 불필요(permitAll, {@code /api/auth/phone/**}).
     */
    @PostMapping("/phone/send-code")
    public ApiResponse<Void> sendCode(@RequestBody @Valid PhoneSendRequest request) {
        authService.sendPhoneCode(request.phone());
        return ApiResponse.<Void>success(null); // null 인자 → 제네릭 추론 불가라 <Void> 명시
    }

    /**
     * 1. API 주소: POST /api/auth/phone/verify
     * 2. 사용 화면: 인증번호 입력(VerifyCode) — 입력한 인증번호 확인 후 로그인/온보딩 분기
     * 3. Request: PhoneVerifyRequest(바디) — phone(휴대폰 번호), code(입력한 인증번호)
     * 4. Response: AuthResultResponse — 신규/미완이면 onboarding=true+onboardingToken, 기존 ACTIVE 회원이면 access/refresh/expiresIn
     *
     * <p>[기존 주석] 휴대폰 인증번호 확인(휴대폰 방식의 로그인/회원가입 진입).
     *
     * <p><b>요청:</b> {@code POST /api/auth/phone/verify}, 본문은 {@link PhoneVerifyRequest}({@code phone} + 입력한
     * 인증번호 {@code code}). {@code @Valid}로 두 값의 공백 여부를 검증한다.
     *
     * <p><b>동작:</b> {@code authService.verifyPhone(phone, code)}에 위임 — 만료/시도횟수/일치 여부를 따져 검증하고,
     * 기존 ACTIVE 회원이면 로그인 토큰을, 신규/미완 회원이면 PENDING 사용자를 만들고 온보딩 토큰을 돌려준다.
     * 결과는 {@link AuthResultResponse#from}으로 응답 DTO로 변환한다.
     *
     * <p><b>응답:</b> {@code ApiResponse<AuthResultResponse>} — {@code oauth}와 동일하게 신규/기존에 따라 채워지는 필드가 다르다.
     *
     * <p><b>인증:</b> 불필요(permitAll, {@code /api/auth/phone/**}).
     */
    @PostMapping("/phone/verify")
    public ApiResponse<AuthResultResponse> verify(@RequestBody @Valid PhoneVerifyRequest request) {
        AuthResultResponse body = AuthResultResponse.from(authService.verifyPhone(request.phone(), request.code()));
        return ApiResponse.success(body);
    }

    /**
     * 1. API 주소: POST /api/auth/terms
     * 2. 사용 화면: 프로필 설정(ProfileSetup) — 가입 완료 제출 시 약관 동의 전송(온보딩 1단계)
     * 3. Request: TermsRequest(바디) — age/service/privacy/location/marketing(약관별 동의 여부) / 인증 사용자(@CurrentUserId, 온보딩 토큰)
     * 4. Response: 없음(Void) — 성공 여부만 응답 봉투로 전달
     *
     * <p>[기존 주석] 약관 동의(온보딩 1단계).
     *
     * <p><b>요청:</b> {@code POST /api/auth/terms}, 본문은 {@link TermsRequest}(약관별 동의 여부 5종:
     * age/service/privacy/location/marketing). 필수 4종이 모두 true인지는 서비스 계층에서 검증한다.
     *
     * <p><b>동작:</b> {@code @CurrentUserId}로 주입된 {@code userId}와 동의 항목들을 {@code authService.agreeTerms(...)}에
     * 위임한다(이미 동의했으면 멱등 처리).
     *
     * <p><b>응답:</b> 본문 데이터 없음 → {@code ApiResponse<Void>}.
     *
     * <p><b>인증:</b> <b>온보딩 토큰 필요</b>. SecurityConfig에서 {@code /api/auth/terms}는 {@code ONBOARDING|USER} 권한이
     * 있어야 통과하고, 토큰의 sub가 {@code @CurrentUserId Long userId}로 주입된다(별도 DB 조회 없음).
     */
    @PostMapping("/terms")
    public ApiResponse<Void> terms(@CurrentUserId Long userId, @RequestBody @Valid TermsRequest request) {
        authService.agreeTerms(userId, request.age(), request.service(), request.privacy(), request.location(), request.marketing());
        return ApiResponse.<Void>success(null); // 반환 데이터 없음 → <Void> 명시
    }

    /**
     * 1. API 주소: POST /api/auth/complete
     * 2. 사용 화면: 프로필 설정(ProfileSetup) — 닉네임 등 프로필 제출로 가입 확정(온보딩 2단계)
     * 3. Request: CompleteProfileRequest(바디) — nickname(필수)·gender·ageGroup·introduction·region·regionLat/Lng·diningStyle·profileImageUrl·favoriteFoods / 인증 사용자(@CurrentUserId, 온보딩 토큰)
     * 4. Response: TokenResponse — accessToken, refreshToken, expiresIn(초)
     *
     * <p>[기존 주석] 프로필 완료(온보딩 2단계) → 가입 확정 및 정식 토큰 발급.
     *
     * <p><b>요청:</b> {@code POST /api/auth/complete}, 본문은 {@link CompleteProfileRequest}(닉네임 등 프로필 필드).
     * {@code @Valid}로 닉네임 공백 여부를 검증한다.
     *
     * <p><b>동작:</b> {@code request.toCommand()}로 요청 DTO를 서비스 입력({@code CompleteProfileCommand})으로 변환한 뒤
     * {@code @CurrentUserId}로 주입된 {@code userId}와 함께 {@code authService.complete(...)}에 위임한다 — 닉네임 중복 검사 후
     * 프로필을 채워 가입을 확정(ACTIVE)하고 정식 토큰({@link com.honjeong.auth.service.TokenPair})을 발급한다.
     * 그 결과를 {@link TokenResponse#from}으로 응답 DTO로 변환한다.
     *
     * <p><b>응답:</b> {@code ApiResponse<TokenResponse>} — access/refresh 토큰과 만료(초).
     *
     * <p><b>인증:</b> <b>온보딩 토큰 필요</b>({@code terms}와 동일하게 {@code ONBOARDING|USER}). sub가 {@code userId}로 주입된다.
     */
    @PostMapping("/complete")
    public ApiResponse<TokenResponse> complete(@CurrentUserId Long userId,
            @RequestBody @Valid CompleteProfileRequest request) {
        return ApiResponse.success(TokenResponse.from(authService.complete(userId, request.toCommand())));
    }

    /**
     * 1. API 주소: POST /api/auth/refresh
     * 2. 사용 화면: 앱 전역 인증 컨텍스트(AuthContext) — 앱 시작/토큰 만료 시 세션 자동 복구(특정 화면 아님)
     * 3. Request: RefreshRequest(바디) — refreshToken(보유 중인 리프레시 토큰 원문)
     * 4. Response: TokenResponse — 새 accessToken, refreshToken, expiresIn(초)
     *
     * <p>[기존 주석] 액세스 토큰 재발급(리프레시 토큰 회전).
     *
     * <p><b>요청:</b> {@code POST /api/auth/refresh}, 본문은 {@link RefreshRequest}(보유 중인 {@code refreshToken}).
     *
     * <p><b>동작:</b> {@code authService.refresh(refreshToken)}에 위임 — 제시된 refresh 토큰을 검증하고 새 토큰 쌍으로
     * 회전(rotate)해 돌려준다. 결과를 {@link TokenResponse#from}으로 변환한다.
     *
     * <p><b>응답:</b> {@code ApiResponse<TokenResponse>} — 새 access/refresh 토큰과 만료(초).
     *
     * <p><b>인증:</b> 불필요(permitAll, {@code /api/auth/refresh}). 만료된 access 토큰으로도 갱신할 수 있어야 하므로
     * refresh 토큰 자체를 본문으로 받아 검증한다.
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ApiResponse.success(TokenResponse.from(authService.refresh(request.refreshToken())));
    }

    /**
     * 1. API 주소: POST /api/auth/logout
     * 2. 사용 화면: 더보기(More) — 로그아웃 버튼(실 호출은 앱 전역 인증 컨텍스트 AuthContext.signOut)
     * 3. Request: RefreshRequest(바디) — refreshToken(무효화할 리프레시 토큰 원문)
     * 4. Response: 없음(Void) — 성공 여부만 응답 봉투로 전달
     *
     * <p>[기존 주석] 로그아웃 — 제시된 리프레시 토큰 무효화.
     *
     * <p><b>요청:</b> {@code POST /api/auth/logout}, 본문은 {@link RefreshRequest}(무효화할 {@code refreshToken}).
     *
     * <p><b>동작:</b> {@code authService.logout(refreshToken)}에 위임 — 해당 refresh 토큰을 폐기(revoke)해 더는 회전에
     * 쓰지 못하게 한다. (무상태 JWT라 access 토큰 자체는 만료될 때까지 유효하다.)
     *
     * <p><b>응답:</b> 본문 데이터 없음 → {@code ApiResponse<Void>}.
     *
     * <p><b>인증:</b> SecurityConfig에 별도 permitAll 규칙이 없어 기본 정책({@code anyRequest().hasRole("USER")})을 따른다 —
     * 즉 정식 access 토큰이 있어야 호출할 수 있다.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.<Void>success(null); // 반환 데이터 없음 → <Void> 명시
    }

    /**
     * 기능: 경로변수의 공급자 문자열(kakao/apple 등)을 {@link Provider} enum으로 변환(미지원 값이면 INVALID_INPUT 400)
     *
     * <p>[기존 주석] 경로변수의 공급자 문자열을 {@link Provider} enum으로 변환한다.
     *
     * <p>대소문자를 가리지 않도록 {@code toUpperCase()} 후 {@code Provider.valueOf(...)}로 매칭하며, 일치하는 enum이 없으면
     * {@code valueOf}가 던지는 {@link IllegalArgumentException}을 잡아 {@code INVALID_INPUT}({@link BusinessException})으로
     * 바꿔 던진다 — 결과적으로 클라이언트에는 400(잘못된 요청)으로 응답된다. 예: {@code kakao}/{@code KAKAO} → 정상,
     * {@code naver} → 400.
     */
    private Provider parseProvider(String provider) {
        try {
            return Provider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "지원하지 않는 공급자입니다.");
        }
    }
}
