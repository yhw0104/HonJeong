# Slice 3 설계 — user 프로필 API

> 작성: 2026-06-14 · 대상: `backend/` (혼정 Spring Boot, `com.honjeong.user`)
> source of truth: `docs/05-API명세서.md` §3, 요구사항 엑셀 AUTH-010·018·019·020

## 1. 개요 · 범위

가입을 마친 회원이 **자기 프로필을 조회·수정**하고, 온보딩/수정 중 **닉네임 사용 가능 여부**를 실시간 확인하는 API. Auth 슬라이스에서 만든 `User` 엔티티·`UserRepository`·`@CurrentUserId` 인프라 위에 controller/service/dto 계층을 얹는다.

**포함(3개 엔드포인트)**
- `GET  /api/users/me` — 내 프로필 조회 (AUTH-018)
- `PATCH /api/users/me` — 내 프로필 수정 (AUTH-019)
- `GET  /api/users/nickname-check` — 닉네임 중복확인 (AUTH-010)

**범위 밖**: 회원 탈퇴(AUTH-021, v2.0), 프로필 사진 업로드 스토리지(AUTH-011), 동네 역지오코딩(AUTH-012, geo/reverse), 선호음식(AUTH-016, v2.0). 타인 공개 프로필 조회는 후속(meal/checkin 슬라이스에서 다룸).

## 2. 엔드포인트 명세

### GET /api/users/me — 🔒 USER
요청: 없음 (`@CurrentUserId`로 토큰 sub 주입)
응답 200 (`UserProfileResponse`):
```json
{ "success": true, "data": {
  "id": 1, "phone": "01012345678", "email": null,
  "nickname": "혼밥러", "profileImageUrl": null,
  "introduction": "조용히 먹어요", "region": "서울 강남구",
  "regionLat": 37.5, "regionLng": 127.0,
  "diningStyle": "QUIET", "gender": "MALE", "ageGroup": "20s",
  "allowMealRequest": true, "status": "ACTIVE"
}}
```
- `phone`은 **원문 그대로** 반환(본인 프로필이므로 마스킹하지 않음 — 명세의 마스킹 표기와 의도적으로 다름).
- 회원 없음 → `USER_NOT_FOUND`(404). (정상 토큰이면 사실상 발생하지 않음.)

### PATCH /api/users/me — 🔒 USER
요청 (`UpdateProfileRequest`, **모든 필드 선택**):
```json
{ "nickname": "새닉네임", "introduction": "", "diningStyle": "TALK", "allowMealRequest": false }
```
응답 200: 수정 반영된 `UserProfileResponse` (GET과 동일 형태).
- 부분 수정: **보낸(non-null) 필드만** 반영, 안 보낸 필드는 보존.
- 닉네임 중복 → `NICKNAME_DUPLICATE`(409). 회원 없음 → `USER_NOT_FOUND`(404).

### GET /api/users/nickname-check?nickname=... — 🔒 ONBOARDING|USER
요청: 쿼리 `nickname` (필수, `@NotBlank`)
응답 200 (`NicknameCheckResponse`):
```json
{ "success": true, "data": { "nickname": "새닉네임", "available": true } }
```
- `available = !existsByNickname(nickname)`. 에러로 분기하지 않고 boolean으로 응답.
- **온보딩 토큰으로도 호출 가능**해야 함(ProfileSetup 단계에서 사용) → USER 전용 아님.

## 3. 계층 · 파일 구조 (CLAUDE.md 컨벤션)

```
com.honjeong.user
├── controller/UserController.java       # 신규. 얇게: @CurrentUserId·@Valid·DTO변환만
├── service/
│   ├── UserService.java                  # 신규. @Transactional(조회 readOnly)
│   └── UpdateProfileCommand.java         # 신규. 서비스 입력(웹 DTO 격리용 record)
├── dto/
│   ├── UserProfileResponse.java          # 신규. from(User) 팩토리
│   ├── UpdateProfileRequest.java         # 신규. 전 필드 nullable, toCommand()
│   └── NicknameCheckResponse.java        # 신규. { nickname, available }
├── domain/User.java                      # 수정. updateProfile(...) 변경자 추가
└── repository/UserRepository.java        # 변경 없음(findById·existsByNickname 재사용)
```
- 의존성: 생성자 주입 + `final`. 컨트롤러는 `UserService`만, 서비스는 `UserRepository`만 의존.

## 4. 도메인 변경 — `User.updateProfile(...)`

부분 수정 규칙("null=무시")을 엔티티가 책임진다. 닉네임 중복 검사는 repository가 필요하므로 **서비스가 먼저 수행**하고, 엔티티는 전달된 non-null 값만 적용한다.

```java
// User.java (의사코드)
public void updateProfile(String nickname, String profileImageUrl, String introduction,
        String region, Double regionLat, Double regionLng,
        DiningStyle diningStyle, Boolean allowMealRequest) {
    if (nickname != null)         this.nickname = nickname;
    if (profileImageUrl != null)  this.profileImageUrl = profileImageUrl;
    if (introduction != null)     this.introduction = introduction;   // "" 허용(비우기)
    if (region != null)           this.region = region;
    if (regionLat != null)        this.regionLat = regionLat;
    if (regionLng != null)        this.regionLng = regionLng;
    if (diningStyle != null)      this.diningStyle = diningStyle;
    if (allowMealRequest != null) this.allowMealRequest = allowMealRequest;
}
```
- `allowMealRequest`는 엔티티에선 `boolean`(primitive)이지만, 토글 부분수정을 위해 **DTO/커맨드/이 변경자에서는 `Boolean`**(nullable)으로 받는다.
- `gender`·`ageGroup`은 인자에 없음(수정 불가 — 온보딩 시 `completeProfile`로 설정 후 고정).

## 5. 핵심 비즈니스 규칙 (UserService)

```java
@Transactional(readOnly = true)
public UserProfileResponse getMyProfile(long userId) {
    return UserProfileResponse.from(findUser(userId));
}

@Transactional
public UserProfileResponse updateProfile(long userId, UpdateProfileCommand cmd) {
    User user = findUser(userId);
    // 닉네임 변경 엣지: 값이 있고, 현재 닉네임과 "다를 때만" 중복 검사
    if (cmd.nickname() != null && !cmd.nickname().equals(user.getNickname())
            && userRepository.existsByNickname(cmd.nickname())) {
        throw new BusinessException(ErrorCode.NICKNAME_DUPLICATE);
    }
    user.updateProfile(cmd.nickname(), cmd.profileImageUrl(), cmd.introduction(),
            cmd.region(), cmd.regionLat(), cmd.regionLng(), cmd.diningStyle(), cmd.allowMealRequest());
    return UserProfileResponse.from(user);   // 영속 컨텍스트 dirty checking으로 UPDATE
}

@Transactional(readOnly = true)
public NicknameCheckResponse checkNickname(String nickname) {
    return new NicknameCheckResponse(nickname, !userRepository.existsByNickname(nickname));
}

private User findUser(long userId) {
    return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
}
```
**핵심 규칙 요약**
1. PATCH는 non-null 필드만 반영(null 보존), 텍스트는 `""`로 비우기 허용.
2. 닉네임은 **현재 값과 다를 때만** 중복 검사 → 본인 닉네임 그대로 두고 다른 필드만 수정해도 통과.
3. nickname-check은 가용성 boolean만 반환(상태 변경 없음).

## 6. 보안 변경 — `SecurityConfig`

`anyRequest().hasRole("USER")` **앞에** 한 줄 추가:
```java
.requestMatchers(HttpMethod.GET, "/api/users/nickname-check").hasAnyRole("ONBOARDING", "USER")
```
- `GET/PATCH /api/users/me`는 기존 `anyRequest().hasRole("USER")`로 자동 보호(추가 규칙 불필요).
- nickname-check만 온보딩 토큰 허용(ProfileSetup 단계 지원). permitAll로 완전 개방하지 않는 이유: 닉네임 enumeration을 최소 인증 뒤로 둠.

## 7. DTO 상세

- **UserProfileResponse**(record): §2 GET 응답 필드 전부 + `static from(User)`.
- **UpdateProfileRequest**(record): `nickname, profileImageUrl, introduction, region, regionLat(Double), regionLng(Double), diningStyle(DiningStyle), allowMealRequest(Boolean)` — 전부 nullable. 검증: `nickname`은 present일 때 `@Size(max = 20)`(공백/길이 가드, 기존 `complete`의 `@NotBlank`와 정합). `toCommand()`로 `UpdateProfileCommand` 변환.
- **UpdateProfileCommand**(record): 위와 동일 필드(서비스 입력).
- **NicknameCheckResponse**(record): `nickname(String), available(boolean)`.

## 8. 에러 (기존 `ErrorCode` 재사용 — 신규 없음)

| 코드 | 상태 | 발생 |
|---|---|---|
| `USER_NOT_FOUND` | 404 | GET/PATCH 대상 회원 없음 |
| `NICKNAME_DUPLICATE` | 409 | PATCH 닉네임이 타인과 중복 |
| `UNAUTHORIZED` / `FORBIDDEN` | 401 / 403 | 토큰 없음 / 권한 부족(필터 계층) |

## 9. 테스트 계획 (TDD: 실패 → 구현 → 통과)

**UserServiceTest** (순수 단위, Mockito로 `UserRepository` 모킹)
- `getMyProfile`: 존재 → 응답 매핑 / 없음 → `USER_NOT_FOUND`
- `updateProfile`:
  - non-null 필드만 반영, null 필드는 기존값 보존
  - 닉네임 변경 + 타인과 중복 → `NICKNAME_DUPLICATE`
  - **닉네임을 본인 현재 값과 동일하게 보내도 통과**(중복검사 미발동) — 회귀 방지 핵심
  - `allowMealRequest=false` 토글 반영
  - `introduction=""`로 비우기 반영
- `checkNickname`: 존재 → `available=false` / 미존재 → `available=true`

**UserControllerTest** (`@WebMvcTest(UserController.class)`, `UserService` 모킹)
- `GET /me` 200 본문 매핑
- `PATCH /me` 200, `@Valid`(nickname 길이 초과 → 400)
- `GET /nickname-check` 200 `available`
- 인가: 토큰 없음 → 401, 온보딩 토큰으로 `/me` → 403 (SecurityConfig 연동은 통합/슬라이스 범위에 맞게)

피라미드: 단위(Service) 우선, 컨트롤러 슬라이스 보조. `@SpringBootTest`는 필요 시 스모크만.

## 10. 구현 순서 (예정)

1. (RED) `UserServiceTest` 작성 → 실패 확인
2. `User.updateProfile(...)` + `UserService` + DTO/Command 구현 → GREEN
3. `UserController` + (RED→GREEN) `UserControllerTest`
4. `SecurityConfig` nickname-check 매처 추가
5. 라이브 e2e(Postman): 로그인 → GET /me → PATCH /me → 재조회 / nickname-check / 닉네임 중복 409
6. 요구사항 엑셀 AUTH-010·018·019·020 완료 체크
</content>
