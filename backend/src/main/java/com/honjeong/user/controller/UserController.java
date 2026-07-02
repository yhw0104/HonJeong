package com.honjeong.user.controller;

import java.util.List;

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
import com.honjeong.user.service.UserActivityService;
import com.honjeong.user.service.UserService;

import jakarta.validation.Valid;

/**
 * 사용자 프로필 REST 컨트롤러 — 내 프로필 조회·수정과 닉네임 중복 확인 엔드포인트를 담당한다.
 *
 * <p>모든 경로는 {@code @RequestMapping("/api/users")} 접두사라서 {@code /api/users/...} 형태가 된다.
 * 컨트롤러는 얇게 유지한다 — {@code @CurrentUserId}로 본인 식별, {@code @Valid} 검증, DTO ↔ 서비스 입력 변환만 하고
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

    public UserController(UserService userService, UserActivityService userActivityService,
            MateProfileService mateProfileService) {
        this.userService = userService;
        this.userActivityService = userActivityService;
        this.mateProfileService = mateProfileService;
    }

    /**
     * 내 프로필 조회.
     *
     * <p><b>요청:</b> {@code GET /api/users/me} — 요청 파라미터나 본문 없음. Authorization 헤더에 정식 access 토큰 필요.
     *
     * <p><b>동작:</b> {@code @CurrentUserId}로 JWT sub에서 주입된 {@code userId}를
     * {@code userService.getMyProfile(userId)}에 위임해 {@link UserProfileResponse}를 가져온다.
     *
     * <p><b>응답:</b> {@code ApiResponse<UserProfileResponse>} — 프로필 전 필드(닉네임, 전화번호, 이미지, 지역 등).
     *
     * <p><b>인증:</b> <b>정식 USER 토큰 필요</b>. SecurityConfig의 {@code anyRequest().hasRole("USER")} 규칙 적용.
     * 토큰의 sub가 {@code @CurrentUserId Long userId}로 주입된다(별도 DB 조회 없음).
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(@CurrentUserId Long userId) {
        return ApiResponse.success(userService.getMyProfile(userId));
    }

    /**
     * 내 프로필 부분수정.
     *
     * <p><b>요청:</b> {@code PATCH /api/users/me} — 본문은 {@link UpdateProfileRequest}. 모든 필드가 선택이므로
     * 보내지 않은(null) 필드는 수정 대상에서 제외된다. {@code @Valid}로 닉네임 길이(20자) 등 제약을 먼저 검증한다.
     *
     * <p><b>동작:</b> {@code request.toCommand()}로 요청 DTO를 서비스 입력({@link com.honjeong.user.service.UpdateProfileCommand})으로
     * 변환한 뒤, {@code @CurrentUserId}로 주입된 {@code userId}와 함께
     * {@code userService.updateProfile(userId, command)}에 위임한다.
     * 서비스가 null 필드를 건너뛰는 patch 병합을 처리하고 갱신된 프로필을 돌려준다.
     *
     * <p><b>응답:</b> {@code ApiResponse<UserProfileResponse>} — 수정 후 최신 프로필 전 필드.
     *
     * <p><b>인증:</b> <b>정식 USER 토큰 필요</b>. SecurityConfig의 {@code anyRequest().hasRole("USER")} 규칙 적용.
     * 토큰의 sub가 {@code @CurrentUserId Long userId}로 주입된다.
     */
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@CurrentUserId Long userId,
            @RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(userId, request.toCommand()));
    }

    /**
     * 내 활동요약 조회(프로필 카드 통계: 혼밥·즐겨찾기·메이트 카운트).
     *
     * <p><b>요청:</b> {@code GET /api/users/me/activity-summary} — 정식 access 토큰 필요.
     * <p><b>응답:</b> {@code ApiResponse<ActivitySummaryResponse>}.
     * <p><b>인증:</b> ROLE_USER. {@code @CurrentUserId}로 본인 식별.
     */
    @GetMapping("/me/activity-summary")
    public ApiResponse<ActivitySummaryResponse> getActivitySummary(@CurrentUserId Long userId) {
        return ApiResponse.success(userActivityService.getActivitySummary(userId));
    }

    /**
     * 닉네임 사용 가능 여부 확인(온보딩 및 프로필 수정 중 실시간 중복 체크).
     *
     * <p><b>요청:</b> {@code GET /api/users/nickname-check?nickname=xxx} — 쿼리 파라미터 {@code nickname}이 필수다.
     * 파라미터가 누락되면 Spring MVC의 {@code MissingServletRequestParameterException} 핸들러가 400({@code INVALID_INPUT})을
     * 돌려준다(컨트롤러 본문에 도달하지 않는다).
     *
     * <p><b>동작:</b> {@code userService.checkNickname(nickname)}에 위임해 DB에서 중복 여부를 확인하고
     * {@link NicknameCheckResponse}({@code nickname} + {@code available})를 가져온다.
     *
     * <p><b>응답:</b> {@code ApiResponse<NicknameCheckResponse>} — {@code available=true}면 사용 가능,
     * {@code false}면 이미 사용 중인 닉네임.
     *
     * <p><b>인증:</b> <b>ONBOARDING 또는 USER 토큰 모두 허용</b>. SecurityConfig에서 이 경로에 한해 온보딩 권한도 통과시킨다.
     * 온보딩 ProfileSetup 화면에서도 닉네임 실시간 확인을 위해 호출하기 때문이다.
     */
    @GetMapping("/nickname-check")
    public ApiResponse<NicknameCheckResponse> checkNickname(@RequestParam String nickname) {
        return ApiResponse.success(userService.checkNickname(nickname));
    }

    /**
     * 닉네임 검색 — 활성 사용자 중 닉네임에 검색어가 포함된 최대 20명을 반환한다.
     *
     * <p><b>요청:</b> {@code GET /api/users/search?nickname=xxx} — 쿼리 파라미터 {@code nickname} 필수.
     * <p><b>응답:</b> {@code ApiResponse<List<UserSearchResponse>>} — 본인 제외, 각 항목에 isMate·requestStatus 포함.
     * <p><b>인증:</b> ROLE_USER 필요(기본 anyRequest() 규칙).
     */
    @GetMapping("/search")
    public ApiResponse<List<UserSearchResponse>> search(@CurrentUserId Long userId,
            @RequestParam("nickname") String nickname) {
        return ApiResponse.success(mateProfileService.searchUsers(userId, nickname));
    }

    /**
     * 타인 공개 프로필 조회 — 닉네임·소개·선호음식·관계상태를 포함한 공개 프로필을 반환한다.
     *
     * <p><b>요청:</b> {@code GET /api/users/{id}/profile} — path variable {@code id}는 조회 대상 사용자 PK.
     * <p><b>응답:</b> {@code ApiResponse<PublicProfileResponse>} — 온라인 상태(currentPlaceName)는 메이트일 때만 노출.
     * <p><b>인증:</b> ROLE_USER 필요(기본 anyRequest() 규칙).
     */
    @GetMapping("/{id}/profile")
    public ApiResponse<PublicProfileResponse> profile(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mateProfileService.getPublicProfile(userId, id));
    }
}
