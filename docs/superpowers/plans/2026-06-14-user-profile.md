# User 프로필 API (Slice 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 가입한 회원이 자기 프로필을 조회·수정하고 닉네임 사용 가능 여부를 확인하는 3개 엔드포인트(`GET/PATCH /api/users/me`, `GET /api/users/nickname-check`)를 TDD로 구현한다.

**Architecture:** 기존 `com.honjeong.user` 도메인에 controller/service/dto 계층을 추가. Auth 슬라이스의 `User` 엔티티·`UserRepository`(findById·existsByNickname)·`@CurrentUserId`·`ApiResponse`·`SecurityConfig`를 그대로 재사용. PATCH는 "null=무시 부분수정"(엔티티가 non-null 인자만 반영), 닉네임은 현재 값과 다를 때만 중복 검사.

**Tech Stack:** Java 21 · Spring Boot 4.0.6 (Web MVC) · Spring Data JPA · Spring Security 7(JWT) · JUnit 5 · Mockito · AssertJ. 브랜치: `feat/slice-3-user-profile`.

설계 근거: `docs/superpowers/specs/2026-06-14-user-profile-design.md`

---

## File Structure

```
backend/src/main/java/com/honjeong/user/
├── controller/UserController.java          # 신규 — GET/PATCH /me, GET /nickname-check
├── service/
│   ├── UserService.java                     # 신규 — 조회/수정/닉네임체크
│   └── UpdateProfileCommand.java            # 신규 — 서비스 입력 record
├── dto/
│   ├── UserProfileResponse.java             # 신규 — from(User)
│   ├── UpdateProfileRequest.java            # 신규 — 전 필드 nullable, toCommand()
│   └── NicknameCheckResponse.java           # 신규 — { nickname, available }
└── domain/User.java                         # 수정 — updateProfile(...) 추가

backend/src/main/java/com/honjeong/global/
├── config/SecurityConfig.java               # 수정 — nickname-check 매처 1줄
└── exception/GlobalExceptionHandler.java    # 수정 — 누락 파라미터 핸들러 1개

backend/src/test/java/com/honjeong/user/
├── domain/UserTest.java                     # 신규
├── service/UserServiceTest.java             # 신규
└── controller/UserControllerTest.java       # 신규
```

> **참고(스펙 대비 정제):** 스펙 §2는 nickname-check 파라미터를 "필수·@NotBlank"로 적었으나, Boot 4에서 `@RequestParam` 빈검증(@Validated)은 미처리 예외 경로를 만든다. 본 계획은 **required `@RequestParam`(누락 시 `MissingServletRequestParameterException`→400)** 로 "필수"를 구현하고 blank 문자열 제약은 생략한다(YAGNI — 빈 닉네임은 PATCH `@Size`/complete `@NotBlank`에서 어차피 걸림).

---

### Task 1: `User.updateProfile(...)` 도메인 변경자

부분수정 규칙("non-null만 반영, null은 보존")을 엔티티에 둔다. 닉네임 중복검사는 서비스 몫이라 여기선 값만 적용.

**Files:**
- Modify: `backend/src/main/java/com/honjeong/user/domain/User.java`
- Test: `backend/src/test/java/com/honjeong/user/domain/UserTest.java` (create)

- [ ] **Step 1: 실패하는 테스트 작성** — `UserTest.java` 생성

```java
package com.honjeong.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link User#updateProfile} 부분수정 규칙 단위 테스트(null=무시, 빈문자열 비우기, 토글). */
class UserTest {

    private User activeUser() {
        User user = User.pending("01012345678", null);
        user.completeProfile("기존닉", Gender.MALE, "20s", "기존소개", "서울", 37.5, 127.0, DiningStyle.QUIET, "img.png");
        return user;
    }

    @Test
    @DisplayName("updateProfile: non-null 필드만 반영하고 null은 기존값을 보존한다")
    void updateProfile_appliesNonNullOnly() {
        User user = activeUser();

        user.updateProfile("새닉", null, null, null, null, null, DiningStyle.TALK, null);

        assertThat(user.getNickname()).isEqualTo("새닉");
        assertThat(user.getDiningStyle()).isEqualTo(DiningStyle.TALK);
        assertThat(user.getIntroduction()).isEqualTo("기존소개"); // null → 보존
        assertThat(user.getRegion()).isEqualTo("서울");          // null → 보존
        assertThat(user.isAllowMealRequest()).isTrue();          // null → 보존(기본 true)
    }

    @Test
    @DisplayName("updateProfile: 빈 문자열은 해당 필드를 비운다")
    void updateProfile_emptyStringClears() {
        User user = activeUser();

        user.updateProfile(null, null, "", null, null, null, null, null);

        assertThat(user.getIntroduction()).isEqualTo("");
    }

    @Test
    @DisplayName("updateProfile: allowMealRequest=false 토글이 반영된다")
    void updateProfile_togglesAllowMealRequest() {
        User user = activeUser();

        user.updateProfile(null, null, null, null, null, null, null, Boolean.FALSE);

        assertThat(user.isAllowMealRequest()).isFalse();
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests "com.honjeong.user.domain.UserTest"`
Expected: 컴파일 실패 — `cannot find symbol: method updateProfile(...)`

- [ ] **Step 3: 최소 구현** — `User.java`의 `completeProfile(...)` 메서드 바로 아래에 추가

```java
    /**
     * 프로필을 부분 수정한다(PATCH). 전달된 값 중 <b>null이 아닌 필드만</b> 반영하고, null인 필드는 기존 값을
     * 보존한다. 빈 문자열("")은 "해당 필드를 비움"으로 취급해 그대로 반영한다. 닉네임 중복 검사는 호출 측
     * (서비스)에서 미리 수행한다. {@code gender}·{@code ageGroup}은 수정 대상이 아니다(온보딩 시 고정).
     *
     * @param nickname        새 닉네임(선택)
     * @param profileImageUrl 새 프로필 이미지 URL(선택)
     * @param introduction    새 한 줄 소개(선택, ""로 비우기 가능)
     * @param region          새 활동 지역명(선택)
     * @param regionLat       새 지역 위도(선택)
     * @param regionLng       새 지역 경도(선택)
     * @param diningStyle     새 식사 성향(선택)
     * @param allowMealRequest 같이먹기 수신 허용 토글(선택). 엔티티 필드는 primitive지만 부분수정을 위해 Boolean으로 받는다.
     */
    public void updateProfile(String nickname, String profileImageUrl, String introduction,
            String region, Double regionLat, Double regionLng, DiningStyle diningStyle, Boolean allowMealRequest) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (introduction != null) this.introduction = introduction;
        if (region != null) this.region = region;
        if (regionLat != null) this.regionLat = regionLat;
        if (regionLng != null) this.regionLng = regionLng;
        if (diningStyle != null) this.diningStyle = diningStyle;
        if (allowMealRequest != null) this.allowMealRequest = allowMealRequest;
    }
```

> `DiningStyle`은 `User.java`에 이미 import되어 있다(`diningStyle` 필드). 추가 import 불필요.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew test --tests "com.honjeong.user.domain.UserTest"`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/honjeong/user/domain/User.java \
        backend/src/test/java/com/honjeong/user/domain/UserTest.java
git commit -m "feat(backend): User.updateProfile 부분수정 변경자 추가

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: `UserService` 조회·닉네임체크 + 응답 DTO

`getMyProfile`/`checkNickname`과 응답 record 2개를 만든다(수정 로직은 Task 3).

**Files:**
- Create: `backend/src/main/java/com/honjeong/user/dto/UserProfileResponse.java`
- Create: `backend/src/main/java/com/honjeong/user/dto/NicknameCheckResponse.java`
- Create: `backend/src/main/java/com/honjeong/user/service/UserService.java`
- Test: `backend/src/test/java/com/honjeong/user/service/UserServiceTest.java` (create)

- [ ] **Step 1: 실패하는 테스트 작성** — `UserServiceTest.java` 생성

```java
package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;
import com.honjeong.user.domain.User;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.repository.UserRepository;

/** {@link UserService} 단위 테스트(Repository는 Mockito 모킹, DB 불필요). */
class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    /** ACTIVE 회원 + id 강제 주입. */
    private User userWithId(long id) {
        User user = User.pending("01012345678", null);
        user.completeProfile("기존닉", Gender.MALE, "20s", "기존소개", "서울", 37.5, 127.0, DiningStyle.QUIET, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("getMyProfile: 회원이 있으면 프로필을 반환한다")
    void getMyProfile_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId(1L)));

        UserProfileResponse res = userService.getMyProfile(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.nickname()).isEqualTo("기존닉");
        assertThat(res.phone()).isEqualTo("01012345678"); // 원문 반환(마스킹 없음)
    }

    @Test
    @DisplayName("getMyProfile: 회원이 없으면 USER_NOT_FOUND")
    void getMyProfile_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(99L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("checkNickname: 사용 중이면 available=false")
    void checkNickname_taken() {
        when(userRepository.existsByNickname("쓰임")).thenReturn(true);

        NicknameCheckResponse res = userService.checkNickname("쓰임");

        assertThat(res.nickname()).isEqualTo("쓰임");
        assertThat(res.available()).isFalse();
    }

    @Test
    @DisplayName("checkNickname: 미사용이면 available=true")
    void checkNickname_free() {
        when(userRepository.existsByNickname("빈닉")).thenReturn(false);

        assertThat(userService.checkNickname("빈닉").available()).isTrue();
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests "com.honjeong.user.service.UserServiceTest"`
Expected: 컴파일 실패 — `UserService`, `UserProfileResponse`, `NicknameCheckResponse` 없음

- [ ] **Step 3: 구현** — DTO 2개 + 서비스 생성

`UserProfileResponse.java`:
```java
package com.honjeong.user.dto;

import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/** 내 프로필 응답. {@code GET/PATCH /api/users/me}에서 반환. phone은 본인 프로필이므로 원문 그대로 노출(마스킹 없음). */
public record UserProfileResponse(
        Long id, String phone, String email, String nickname, String profileImageUrl,
        String introduction, String region, Double regionLat, Double regionLng,
        DiningStyle diningStyle, Gender gender, String ageGroup,
        boolean allowMealRequest, UserStatus status) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(), user.getPhone(), user.getEmail(), user.getNickname(), user.getProfileImageUrl(),
                user.getIntroduction(), user.getRegion(), user.getRegionLat(), user.getRegionLng(),
                user.getDiningStyle(), user.getGender(), user.getAgeGroup(),
                user.isAllowMealRequest(), user.getStatus());
    }
}
```

`NicknameCheckResponse.java`:
```java
package com.honjeong.user.dto;

/** 닉네임 사용 가능 여부 응답. {@code GET /api/users/nickname-check}. */
public record NicknameCheckResponse(String nickname, boolean available) {
}
```

`UserService.java`:
```java
package com.honjeong.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.user.domain.User;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.repository.UserRepository;

/**
 * 회원 프로필 조회·수정·닉네임 중복확인을 담당하는 서비스. 조회는 readOnly 트랜잭션, 수정은 쓰기 트랜잭션
 * 경계를 가진다. UserRepository(findById·existsByNickname)만 의존한다.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 내 프로필 조회. 회원이 없으면 {@code USER_NOT_FOUND}. */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    /** 닉네임 사용 가능 여부. 존재하지 않으면 available=true. */
    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname) {
        return new NicknameCheckResponse(nickname, !userRepository.existsByNickname(nickname));
    }

    /** userId로 회원을 찾고 없으면 {@code USER_NOT_FOUND}를 던진다. */
    private User findUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew test --tests "com.honjeong.user.service.UserServiceTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/honjeong/user/dto/UserProfileResponse.java \
        backend/src/main/java/com/honjeong/user/dto/NicknameCheckResponse.java \
        backend/src/main/java/com/honjeong/user/service/UserService.java \
        backend/src/test/java/com/honjeong/user/service/UserServiceTest.java
git commit -m "feat(backend): UserService 프로필 조회·닉네임 중복확인

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: `UserService.updateProfile` + `UpdateProfileCommand`

부분수정 + 닉네임 엣지(본인 동일값 통과, 타인 중복 거부).

**Files:**
- Create: `backend/src/main/java/com/honjeong/user/service/UpdateProfileCommand.java`
- Modify: `backend/src/main/java/com/honjeong/user/service/UserService.java`
- Test: `backend/src/test/java/com/honjeong/user/service/UserServiceTest.java` (메서드 추가)

- [ ] **Step 1: 실패하는 테스트 추가** — `UserServiceTest.java`에 메서드 3개 추가(클래스 닫는 `}` 앞)

```java
    @Test
    @DisplayName("updateProfile: 닉네임을 본인 현재값과 동일하게 두면 중복검사 없이 통과한다")
    void updateProfile_sameNickname_skipsDupCheck() {
        User user = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UpdateProfileCommand cmd = new UpdateProfileCommand("기존닉", null, "새소개", null, null, null, null, null);

        UserProfileResponse res = userService.updateProfile(1L, cmd);

        assertThat(res.introduction()).isEqualTo("새소개");
        assertThat(res.nickname()).isEqualTo("기존닉");
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never())
                .existsByNickname(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("updateProfile: 닉네임을 타인과 중복되게 바꾸면 NICKNAME_DUPLICATE")
    void updateProfile_duplicateNickname_throws() {
        User user = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("중복닉")).thenReturn(true);
        UpdateProfileCommand cmd = new UpdateProfileCommand("중복닉", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> userService.updateProfile(1L, cmd)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("updateProfile: allowMealRequest=false 토글이 반영된다")
    void updateProfile_toggle() {
        User user = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UpdateProfileCommand cmd = new UpdateProfileCommand(null, null, null, null, null, null, null, Boolean.FALSE);

        UserProfileResponse res = userService.updateProfile(1L, cmd);

        assertThat(res.allowMealRequest()).isFalse();
    }
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests "com.honjeong.user.service.UserServiceTest"`
Expected: 컴파일 실패 — `UpdateProfileCommand`, `userService.updateProfile` 없음

- [ ] **Step 3: 구현** — Command 생성 + 서비스에 메서드 추가

`UpdateProfileCommand.java`:
```java
package com.honjeong.user.service;

import com.honjeong.user.domain.DiningStyle;

/** 프로필 부분수정 서비스 입력. 모든 필드 선택(null=미변경). 웹 DTO를 서비스 경계 안으로 들이지 않으려는 분리용. */
public record UpdateProfileCommand(
        String nickname, String profileImageUrl, String introduction,
        String region, Double regionLat, Double regionLng,
        DiningStyle diningStyle, Boolean allowMealRequest) {
}
```

`UserService.java`에 `checkNickname` 위(또는 `getMyProfile` 아래)에 메서드 추가:
```java
    /**
     * 프로필 부분수정. 닉네임이 들어오고 현재 닉네임과 <b>다를 때만</b> 중복 검사를 한다(본인 닉네임 유지는 통과).
     * 이후 엔티티가 non-null 필드만 반영한다. 영속 컨텍스트의 dirty checking으로 UPDATE가 나간다.
     */
    @Transactional
    public UserProfileResponse updateProfile(long userId, UpdateProfileCommand command) {
        User user = findUser(userId);
        if (command.nickname() != null && !command.nickname().equals(user.getNickname())
                && userRepository.existsByNickname(command.nickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
        }
        user.updateProfile(command.nickname(), command.profileImageUrl(), command.introduction(),
                command.region(), command.regionLat(), command.regionLng(),
                command.diningStyle(), command.allowMealRequest());
        return UserProfileResponse.from(user);
    }
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew test --tests "com.honjeong.user.service.UserServiceTest"`
Expected: PASS (7 tests)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/honjeong/user/service/UpdateProfileCommand.java \
        backend/src/main/java/com/honjeong/user/service/UserService.java \
        backend/src/test/java/com/honjeong/user/service/UserServiceTest.java
git commit -m "feat(backend): UserService.updateProfile 부분수정+닉네임 엣지

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: `UserController` + `UpdateProfileRequest` + 누락 파라미터 핸들러

웹 계층 3개 엔드포인트 + nickname-check 누락 시 400.

**Files:**
- Create: `backend/src/main/java/com/honjeong/user/dto/UpdateProfileRequest.java`
- Create: `backend/src/main/java/com/honjeong/user/controller/UserController.java`
- Modify: `backend/src/main/java/com/honjeong/global/exception/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/honjeong/user/controller/UserControllerTest.java` (create)

- [ ] **Step 1: 실패하는 테스트 작성** — `UserControllerTest.java` 생성

```java
package com.honjeong.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.service.UserService;

/**
 * {@link UserController} 웹 슬라이스 테스트. SecurityConfig/WebConfig를 @Import해 실제 인가 규칙과
 * @CurrentUserId 주입을 함께 검증한다. UserService는 @MockitoBean으로 대체.
 */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, WebConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserService userService;

    private UserProfileResponse sampleProfile() {
        return new UserProfileResponse(1L, "01012345678", null, "혼밥러", null,
                null, null, null, null, null, null, null, true, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("GET /me: access 토큰이면 200 + 프로필")
    void getMe_ok() throws Exception {
        when(userService.getMyProfile(1L)).thenReturn(sampleProfile());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("혼밥러"))
                .andExpect(jsonPath("$.data.phone").value("01012345678"));
    }

    @Test
    @DisplayName("GET /me: 토큰 없으면 401")
    void getMe_noToken_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /me: access 토큰이면 200이고 서비스에 위임한다")
    void patchMe_ok() throws Exception {
        when(userService.updateProfile(eq(1L), any())).thenReturn(sampleProfile());
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉\",\"allowMealRequest\":false}"))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq(1L), any());
    }

    @Test
    @DisplayName("GET /nickname-check: access 토큰이면 200 + available")
    void checkNickname_ok() throws Exception {
        when(userService.checkNickname("새닉")).thenReturn(new NicknameCheckResponse("새닉", true));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/users/nickname-check").param("nickname", "새닉")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @DisplayName("GET /nickname-check: nickname 파라미터 누락 시 400")
    void checkNickname_missingParam_400() throws Exception {
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(get("/api/users/nickname-check").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests "com.honjeong.user.controller.UserControllerTest"`
Expected: 컴파일 실패 — `UserController`, `UpdateProfileRequest` 없음

- [ ] **Step 3: 구현** — Request DTO + Controller + 예외 핸들러

`UpdateProfileRequest.java`:
```java
package com.honjeong.user.dto;

import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.service.UpdateProfileCommand;

import jakarta.validation.constraints.Size;

/** 프로필 부분수정 요청. 모든 필드 선택(보낸 것만 반영). 닉네임은 보낼 경우 20자 이하. */
public record UpdateProfileRequest(
        @Size(max = 20) String nickname,
        String profileImageUrl,
        String introduction,
        String region,
        Double regionLat,
        Double regionLng,
        DiningStyle diningStyle,
        Boolean allowMealRequest) {

    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(nickname, profileImageUrl, introduction,
                region, regionLat, regionLng, diningStyle, allowMealRequest);
    }
}
```

`UserController.java`:
```java
package com.honjeong.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.user.dto.NicknameCheckResponse;
import com.honjeong.user.dto.UpdateProfileRequest;
import com.honjeong.user.dto.UserProfileResponse;
import com.honjeong.user.service.UserService;

import jakarta.validation.Valid;

/**
 * 사용자 프로필 REST 컨트롤러. {@code /api/users} 접두사. 얇게 유지 — @CurrentUserId로 본인 식별,
 * @Valid 검증, DTO 변환만 하고 로직은 {@link UserService}에 위임한다.
 * 인가: /me 2개는 정식 USER(SecurityConfig anyRequest), nickname-check는 ONBOARDING|USER(SecurityConfig 매처).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 내 프로필 조회. 토큰 sub가 @CurrentUserId로 주입된다. */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(@CurrentUserId Long userId) {
        return ApiResponse.success(userService.getMyProfile(userId));
    }

    /** 내 프로필 부분수정. 보낸(non-null) 필드만 반영. */
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@CurrentUserId Long userId,
            @RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(userId, request.toCommand()));
    }

    /** 닉네임 사용 가능 여부(온보딩/수정 중 실시간 확인). nickname 필수(누락 시 400). */
    @GetMapping("/nickname-check")
    public ApiResponse<NicknameCheckResponse> checkNickname(@RequestParam String nickname) {
        return ApiResponse.success(userService.checkNickname(nickname));
    }
}
```

`GlobalExceptionHandler.java`에 import 1줄과 핸들러 1개 추가:
- import 추가(다른 import들과 함께): `import org.springframework.web.bind.MissingServletRequestParameterException;`
- `handleValidation`(MethodArgumentNotValidException) 메서드 바로 아래에 추가:
```java
    /**
     * 필수 {@code @RequestParam} 누락을 400 INVALID_INPUT으로 변환한다(예: nickname-check의 nickname 누락).
     *
     * @param ex 누락된 파라미터 정보를 담은 예외
     * @return 400 상태 + INVALID_INPUT 코드 엔벨로프
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.code(), ex.getParameterName() + " 파라미터가 필요합니다."));
    }
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew test --tests "com.honjeong.user.controller.UserControllerTest"`
Expected: PASS (5 tests). (참고: nickname-check은 아직 SecurityConfig 매처가 없어 USER access 토큰으로만 통과 — 온보딩 허용은 Task 5에서.)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/honjeong/user/dto/UpdateProfileRequest.java \
        backend/src/main/java/com/honjeong/user/controller/UserController.java \
        backend/src/main/java/com/honjeong/global/exception/GlobalExceptionHandler.java \
        backend/src/test/java/com/honjeong/user/controller/UserControllerTest.java
git commit -m "feat(backend): UserController(GET/PATCH /me, nickname-check) + 누락 파라미터 400

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 5: `SecurityConfig` — nickname-check 온보딩 허용

온보딩 토큰(ProfileSetup 단계)도 nickname-check를 호출할 수 있게 한다.

**Files:**
- Modify: `backend/src/main/java/com/honjeong/global/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/honjeong/user/controller/UserControllerTest.java` (메서드 추가)

- [ ] **Step 1: 실패하는 테스트 추가** — `UserControllerTest.java`에 메서드 2개 추가(클래스 닫는 `}` 앞)

```java
    @Test
    @DisplayName("GET /me: 온보딩 토큰이면 403(USER 전용)")
    void getMe_onboardingToken_403() throws Exception {
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /nickname-check: 온보딩 토큰으로도 200(온보딩 ProfileSetup 지원)")
    void checkNickname_onboardingToken_ok() throws Exception {
        when(userService.checkNickname("새닉")).thenReturn(new NicknameCheckResponse("새닉", true));
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(get("/api/users/nickname-check").param("nickname", "새닉")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew test --tests "com.honjeong.user.controller.UserControllerTest"`
Expected: `checkNickname_onboardingToken_ok` FAIL — 현재 nickname-check은 `anyRequest().hasRole("USER")`라 온보딩 토큰엔 403. (`getMe_onboardingToken_403`은 이미 통과.)

- [ ] **Step 3: 구현** — 매처 1줄 추가

`SecurityConfig.java`의 `filterChain` 안 `authorizeHttpRequests` 블록에서, `terms`/`complete` 매처 줄과 `anyRequest()` 줄 **사이에** 추가:
```java
                        // 닉네임 중복확인은 온보딩 단계(ProfileSetup)에서도 호출하므로 ONBOARDING도 허용.
                        .requestMatchers(HttpMethod.GET, "/api/users/nickname-check").hasAnyRole("ONBOARDING", "USER")
```
그리고 파일 상단 import에 추가: `import org.springframework.http.HttpMethod;`

- [ ] **Step 4: 통과 확인**

Run: `./gradlew test --tests "com.honjeong.user.controller.UserControllerTest"`
Expected: PASS (7 tests)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/honjeong/global/config/SecurityConfig.java \
        backend/src/test/java/com/honjeong/user/controller/UserControllerTest.java
git commit -m "feat(backend): nickname-check 온보딩 토큰 허용(SecurityConfig)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: 전체 검증 · 라이브 e2e · 요구사항 엑셀 갱신

**Files:** 없음(검증·문서). 코드 변경 없음.

- [ ] **Step 1: 전체 테스트 그린 확인**

Run: `./gradlew test`
Expected: 기존 31개 + 신규(User 도메인 3 + 서비스 7 + 컨트롤러 7 = 17) 모두 PASS. (DB 필요한 테스트가 있으므로 OrbStack 실행 중이어야 함.)

- [ ] **Step 2: 라이브 e2e (Postman, 수동)** — `docker compose up -d db && ./gradlew bootRun` 후

확인 시나리오(휴대폰 가입으로 access 토큰 확보 후 Bearer 사용):
1. `GET /api/users/me` → 200, 내 프로필(phone 원문 포함)
2. `PATCH /api/users/me` body `{"nickname":"새닉","introduction":"조용히","allowMealRequest":false}` → 200, 반영된 프로필
3. `GET /api/users/me` 재조회 → 변경 반영 + 안 보낸 필드(region 등) 보존 확인
4. `GET /api/users/nickname-check?nickname=새닉` → `available:false`(방금 내가 씀) / 안 쓰는 값 → `available:true`
5. `PATCH /api/users/me` body `{"nickname":"<다른 ACTIVE 회원 닉네임>"}` → 409 `NICKNAME_DUPLICATE`
6. 온보딩 토큰으로 `GET /api/users/nickname-check?nickname=...` → 200 (온보딩 허용), 온보딩 토큰으로 `GET /me` → 403

- [ ] **Step 3: 요구사항 엑셀 완료 체크** — `docs/혼정-기능요구사항-목록.xlsx`

`/tmp/check_done.py`의 `DONE_ROWS`를 이번 슬라이스 행으로 바꿔 실행(엑셀 닫은 상태에서):
- AUTH-010(닉네임 중복확인)=행11, AUTH-018(내 프로필 조회)=행19, AUTH-019(내 프로필 수정)=행20, AUTH-020(같이먹기 수신 토글)=행21
- 즉 `DONE_ROWS = [11, 19, 20, 21]` 로 수정 후 `python3 /tmp/check_done.py` (백업·검증 포함, '완료' 문자열 재사용).

> 메모리 [[requirements-xlsx-checkbox-tracking]] 참고: openpyxl 저장 금지, 잠금파일 확인.

- [ ] **Step 4: 슬라이스 마무리** — main 병합

```bash
git checkout main
git merge --no-ff feat/slice-3-user-profile -m "feat(backend): Slice 3 — user 프로필 API(조회·수정·닉네임 중복확인)"
```
(원격이 있으면 `git push`. 병합 전 `./gradlew build`로 최종 확인 권장.)

---

## Self-Review

**1. Spec coverage**
- §2 GET /me → Task 2(서비스)+Task 4(컨트롤러) ✅
- §2 PATCH /me → Task 1(도메인)+Task 3(서비스)+Task 4(컨트롤러) ✅
- §2 nickname-check → Task 2(서비스)+Task 4(컨트롤러)+Task 5(보안) ✅
- §4 도메인 updateProfile(null=무시, "" 비우기) → Task 1 ✅
- §5 닉네임 본인동일 통과/타인중복 거부 → Task 3 ✅
- §6 보안(nickname-check ONBOARDING|USER, /me USER) → Task 5 ✅
- §7 DTO 4종 → Task 2(응답2)+Task 3(command)+Task 4(request) ✅
- §8 에러(USER_NOT_FOUND·NICKNAME_DUPLICATE 재사용) → Task 2·3 ✅
- §9 테스트(서비스 단위·컨트롤러 슬라이스) → Task 1~5 ✅
- 추가(스펙 정제): nickname 누락 400 핸들러 → Task 4

**2. Placeholder scan:** 모든 step에 실제 코드/명령/기대값 포함. 빈 항목 없음. ✅

**3. Type consistency:**
- `User.updateProfile(String,String,String,String,Double,Double,DiningStyle,Boolean)` — Task 1 정의 = Task 3 호출 인자순 일치 ✅
- `UpdateProfileCommand(nickname,profileImageUrl,introduction,region,regionLat,regionLng,diningStyle,allowMealRequest)` — Task 3 정의 = Task 4 `toCommand()` 인자순 일치 ✅
- `UserProfileResponse` 14필드(id,phone,email,nickname,profileImageUrl,introduction,region,regionLat,regionLng,diningStyle,gender,ageGroup,allowMealRequest,status) — Task 2 정의 = Task 4 `sampleProfile()` 생성자 인자수 일치 ✅
- `NicknameCheckResponse(nickname,available)` — Task 2 정의 = Task 4·5 사용 일치 ✅
- `jwtProvider.createAccessToken(long)`·`createOnboardingToken(long)` — 기존 JwtProvider 시그니처와 일치 ✅
- `@WebMvcTest` import `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`(Boot4 경로) — 기존 AuthControllerTest와 동일 ✅
</content>
