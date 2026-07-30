package com.honjeong.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.mate.dto.PublicProfileResponse;
import com.honjeong.mate.dto.UserSearchResponse;
import com.honjeong.mate.service.MateProfileService;
import com.honjeong.user.dto.ActivitySummaryResponse;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UpdateProfileRequest;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.service.AccountWithdrawalService;
import com.honjeong.user.service.UserActivityService;
import com.honjeong.user.service.UserService;

import jakarta.validation.Valid;

/**
 * 사용자 프로필 REST 컨트롤러 — 프로필 조회·수정, 닉네임 확인·검색, 공개 프로필 조회, 회원 탈퇴.
 *
 * <p>기본 경로: /api/users
 *
 * <p>컨트롤러는 얇게 유지한다 — {@code @CurrentUserId}로 본인 식별, {@code @Valid} 검증, DTO ↔ 서비스 입력 변환만 하고
 * 실제 비즈니스 로직(프로필 patch 병합, 닉네임 중복 검사)은 {@link UserService}에 위임한다.
 *
 * <p>인가 구분:
 * <ul>
 *   <li>{@code GET /me}, {@code PATCH /me} — 정식 회원(USER 권한)만 접근 가능. SecurityConfig의
 *       {@code anyRequest().hasRole("USER")} 규칙을 따른다.</li>
 *   <li>{@code GET /nickname-check} — ONBOARDING|USER 모두 허용. 온보딩 ProfileSetup 화면에서도
 *       닉네임 실시간 확인을 위해 호출하기 때문이다.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserActivityService userActivityService;
    private final MateProfileService mateProfileService;
    private final AccountWithdrawalService accountWithdrawalService;

    public UserController(UserService userService, UserActivityService userActivityService,
            MateProfileService mateProfileService, AccountWithdrawalService accountWithdrawalService) {
        this.userService = userService;
        this.userActivityService = userActivityService;
        this.mateProfileService = mateProfileService;
        this.accountWithdrawalService = accountWithdrawalService;
    }

    /**
     * 내 프로필을 조회한다.
     *
     * <p>사용 화면: 마이페이지(MyProfileScreen)·더보기(MoreScreen)·프로필 편집(ProfileEditScreen) —
     * 내 프로필 표시와 편집 초기값.
     *
     * <p><b>인증:</b> <b>정식 USER 토큰 필요</b>. SecurityConfig의 {@code anyRequest().hasRole("USER")} 규칙 적용.
     * 토큰의 sub가 {@code @CurrentUserId Long userId}로 주입된다(별도 DB 조회 없음).
     *
     * @param userId 인증 사용자 ID(JWT sub)
     * @return 닉네임·전화번호·이미지·지역·식사성향·선호음식 등 프로필 전 필드
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(@CurrentUserId Long userId) {
        return ApiResponse.success(userService.getMyProfile(userId));
    }

    /**
     * 내 프로필을 부분 수정한다(PATCH).
     *
     * <p>사용 화면: 프로필 편집(ProfileEditScreen) — 저장 버튼으로 변경 필드만 전송.
     *
     * <p>모든 필드가 선택이므로 보내지 않은(null) 필드는 수정 대상에서 제외된다. {@code @Valid}로 닉네임
     * 길이(20자) 등 제약을 먼저 검증하고, null 필드를 건너뛰는 patch 병합은 서비스가 처리한다.
     *
     * <p><b>인증:</b> <b>정식 USER 토큰 필요</b>. SecurityConfig의 {@code anyRequest().hasRole("USER")} 규칙 적용.
     *
     * @param userId 인증 사용자 ID(JWT sub)
     * @param request nickname·profileImageUrl·introduction·region·regionLat·regionLng·diningStyle·
     *                allowMealRequest·favoriteFoods(전부 선택)
     * @return 수정이 반영된 최신 프로필 전 필드
     */
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@CurrentUserId Long userId,
            @RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(userId, request.toCommand()));
    }

    /**
     * 내 활동요약을 조회한다(프로필 카드 통계: 혼밥·리뷰·즐겨찾기·메이트 카운트).
     *
     * <p>사용 화면: 마이페이지(MyProfileScreen)·더보기(MoreScreen) — 프로필 카드 통계 행.
     *
     * <p><b>인증:</b> ROLE_USER. {@code @CurrentUserId}로 본인을 식별한다.
     *
     * @param userId 인증 사용자 ID(JWT sub)
     * @return checkInCount·reviewCount·favoriteCount·mateCount
     */
    @GetMapping("/me/activity-summary")
    public ApiResponse<ActivitySummaryResponse> getActivitySummary(@CurrentUserId Long userId) {
        return ApiResponse.success(userActivityService.getActivitySummary(userId));
    }

    /**
     * 닉네임 사용 가능 여부를 확인한다(온보딩 및 프로필 수정 중 실시간 중복 체크).
     *
     * <p>사용 화면: 온보딩 프로필 설정(ProfileSetupScreen)·프로필 편집(ProfileEditScreen) —
     * 닉네임 입력 시 실시간 중복 확인(디바운스).
     *
     * <p>쿼리 파라미터가 누락되면 Spring MVC의 {@code MissingServletRequestParameterException} 핸들러가
     * 400({@code INVALID_INPUT})을 돌려준다(컨트롤러 본문에 도달하지 않는다).
     *
     * <p><b>인증:</b> <b>ONBOARDING 또는 USER 토큰 모두 허용</b>. SecurityConfig에서 이 경로에 한해 온보딩 권한도
     * 통과시킨다 — 온보딩 ProfileSetup 화면에서도 닉네임 실시간 확인을 위해 호출하기 때문이다.
     *
     * @param nickname 확인할 닉네임(필수)
     * @return 확인한 닉네임(echo)과 사용 가능 여부 — {@code available=false}면 이미 사용 중이다
     */
    @GetMapping("/nickname-check")
    public ApiResponse<NicknameCheckResponse> checkNickname(@RequestParam String nickname) {
        return ApiResponse.success(userService.checkNickname(nickname));
    }

    /**
     * 닉네임으로 사용자를 검색한다 — 활성 사용자 중 닉네임에 검색어가 포함된 최대 20명을 반환한다.
     *
     * <p>사용 화면: 메이트(MatesScreen) — 닉네임으로 사용자를 검색해 메이트 신청.
     *
     * <p><b>인증:</b> ROLE_USER 필요(기본 anyRequest() 규칙).
     *
     * @param userId 인증 사용자 ID(JWT sub)
     * @param nickname 검색어(필수)
     * @return 본인 제외 활성 사용자 최대 20명(각 항목에 isMate·requestStatus 포함)
     */
    @GetMapping("/search")
    public ApiResponse<List<UserSearchResponse>> search(@CurrentUserId Long userId,
            @RequestParam("nickname") String nickname) {
        return ApiResponse.success(mateProfileService.searchUsers(userId, nickname));
    }

    /**
     * 타인의 공개 프로필을 조회한다 — 닉네임·소개·선호음식·관계상태를 포함한다.
     *
     * <p>사용 화면: 메이트 프로필(MateProfileScreen) — 타인 공개 프로필 표시.
     *
     * <p>온라인 상태(currentPlaceName)는 메이트일 때만 노출한다.
     *
     * <p><b>인증:</b> ROLE_USER 필요(기본 anyRequest() 규칙).
     *
     * @param userId 인증 사용자 ID(JWT sub)
     * @param id 조회 대상 사용자 PK
     * @return 공개 프로필(닉네임·소개·선호음식·관계상태)
     */
    @GetMapping("/{id}/profile")
    public ApiResponse<PublicProfileResponse> profile(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateProfileService.getPublicProfile(userId, id));
    }

    /**
     * 회원 탈퇴 — 개인정보를 파기하고 계정을 익명화한다(되돌릴 수 없음).
     *
     * <p>성공 후 클라이언트는 즉시 로그아웃해야 한다. 남은 access 토큰은 다음 요청에서
     * {@code ActiveUserFilter}가 401(ACCOUNT_INACTIVE)로 막는다.
     *
     * @param userId 탈퇴할 인증 사용자 ID(JWT sub)
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@CurrentUserId Long userId) {
        accountWithdrawalService.withdraw(userId);
        return ApiResponse.success(null);
    }
}
