# Slice 6 설계 — meal 같이먹기 신청 API (신청·목록·수락·거절)

> 작성: 2026-06-18 · 대상: `backend/` (혼정 Spring Boot, `com.honjeong.meal`)
> source of truth: `docs/05-API명세서.md` §7, 요구사항 FR-108·NFR-03, ERD `04-ERD-데이터모델.md` C-2

## 1. 개요 · 범위

P1 핵심 연결 루프의 **마지막 조각**. 같은 식당의 혼밥러(대상 체크인)에게 "같이 드실래요?" 신청을 보내고 수락/거절한다. 수신 opt-in(`users.allow_meal_request`)을 강제해 "원치 않는 사람에겐 닿지 않는" 안전성을 보장한다(NFR-03). Auth/User/Place/CheckIn 슬라이스의 수직 슬라이스 패턴(`controller/service/repository/domain/dto`)을 그대로 미러링한다.

**포함 (4개 엔드포인트)**
- `POST  /api/meal-requests` — 같이먹기 신청 (FR-108)
- `GET   /api/meal-requests` — 받은/보낸 신청 목록 (`role`=received|sent, `status` 선택)
- `PATCH /api/meal-requests/{id}/accept` — 수락
- `PATCH /api/meal-requests/{id}/decline` — 거절

**범위 밖**: 푸시 알림(P2 — P1은 클라가 `GET /api/meal-requests`를 폴링, 백엔드 추가 작업 없음) · 차단/신고(P2, ERD J: `blocks`/`reports`) · 반응(보류, FR-106) · 수락 후 메이트 관계 생성/채팅(P2+, ERD D).

**기술 방향 결정 (brainstorming 확정)**
- **수신자 식별 = 조인 경유**(방식 A). `meal_requests`엔 `from_user_id`만 있고 수신자는 `to_check_in_id → check_ins.user_id`로 간접 식별한다. `to_user_id` 역정규화 컬럼을 신설하지 않는다 — 테이블은 V1에 이미 있고(마이그레이션 0), 신청 건수가 적어 조인 비용이 무의미하며, ERD C-2 설계 의도와 일치한다.
- **목록 = 평면 배열**. API 명세 §7대로 `data:[...]` 배열로 응답한다(페이징 미적용). 한 사용자의 신청 수가 적어 `PageResponse` 불필요.
- **신청 전제 없음**. 신청자가 자기 ACTIVE 체크인을 가질 필요 없다 — 명세 POST 바디는 `toCheckInId`·`message`만 받는다.
- **상태 전이는 도메인 메서드로**. `MealRequest.accept(now)`/`decline(now)`가 전이를 소유하고, 권한·중복 응답 가드는 서비스가 수행한다(`CheckIn.end()`+서비스 `isOwnedBy` 패턴 그대로).

## 2. 엔드포인트 명세

### POST /api/meal-requests — 🔒 USER · FR-108
요청 (`MealRequestCreateRequest`):
```json
{ "toCheckInId": 10, "message": "같이 드실래요?" }
```
응답 **201** (`MealRequestResponse`):
```json
{ "success": true, "data": { "mealRequestId": 7, "toCheckInId": 10, "message": "같이 드실래요?", "status": "PENDING" } }
```
- 검증: `toCheckInId` `@NotNull`, `message` `@Size(max=200)`(선택, null 허용). `@Valid` 위반 → 400 `INVALID_INPUT`.
- `place_id`는 요청에 없다 → **대상 체크인의 `place`에서 파생**해 저장(역정규화, ERD C-2).
- 에러(검사 순서대로):
  - `404 TARGET_CHECKIN_NOT_AVAILABLE` — 대상 체크인 없음 **또는 이미 종료(status≠ACTIVE)**. (명세의 "없음/이미 종료"를 한 코드로 통합.)
  - `409 MEALREQUEST_SELF` — 대상 체크인 주인이 나 자신.
  - `403 MEALREQUEST_OPT_OUT` — 대상 주인 `allowMealRequest=false`(수신 거부).
  - `409 MEALREQUEST_DUPLICATE` — 동일 대상 중복 신청. DB 유니크(`from_user_id, to_check_in_id`) 위반을 `DataIntegrityViolationException`→409로 변환(체크인 단일활성과 동일 패턴).

### GET /api/meal-requests — 🔒 USER
쿼리: `role`=`received`(기본)|`sent`, `status`(선택: `PENDING`|`ACCEPTED`|`DECLINED`).
응답 200 (`List<MealRequestListItemResponse>`, 평면 배열):
```json
{ "success": true, "data": [
  { "mealRequestId": 7, "fromUser": { "nickname": "옆자리" }, "placeId": 3, "message": "같이 드실래요?", "status": "PENDING", "createdAt": "2026-06-18T12:05:00" }
] }
```
- `received`: 내가 수신자인 신청(`toCheckIn.user.id = me`). `sent`: 내가 신청자(`fromUser.id = me`).
- 두 경우 모두 항목에 `fromUser.nickname`(신청자 닉네임)을 담는다 — 명세 형태 그대로. `sent`면 fromUser=나 자신(중복이지만 명세 형태 유지, YAGNI).
- `createdAt DESC` 정렬. `role`/`status` 파싱 실패 → 400 `INVALID_INPUT`.

### PATCH /api/meal-requests/{id}/accept · /decline — 🔒 USER
응답 200 (`MealRequestStatusResponse`):
```json
{ "success": true, "data": { "mealRequestId": 7, "status": "ACCEPTED", "respondedAt": "2026-06-18T12:10:00" } }
```
(`decline`은 `status=DECLINED`.)
- `404 MEALREQUEST_NOT_FOUND` — 신청 없음.
- `403 FORBIDDEN` — 내가 수신자가 아님(`!isReceivedBy(me)`). 기존 공통 코드 재사용.
- `409 MEALREQUEST_ALREADY_RESPONDED` — 이미 응답(status≠PENDING).

## 3. 컴포넌트 (`com.honjeong.meal`, 5계층)

```
meal/
├── domain/
│   ├── MealRequest.java          # 엔티티 (BaseTimeEntity 미상속 — created_at·responded_at만)
│   └── MealRequestStatus.java    # enum: PENDING, ACCEPTED, DECLINED
├── dto/
│   ├── MealRequestCreateRequest.java    # { toCheckInId, message } — @NotNull/@Size
│   ├── MealRequestResponse.java         # POST 201: { mealRequestId, toCheckInId, message, status }
│   ├── MealRequestStatusResponse.java   # accept/decline 200: { mealRequestId, status, respondedAt }
│   └── MealRequestListItemResponse.java # 목록: { mealRequestId, fromUser{nickname}, placeId, message, status, createdAt }
├── repository/MealRequestRepository.java
├── service/MealRequestService.java
└── controller/MealRequestController.java
```

### 엔티티 매핑 (`meal_requests` 테이블은 V1에 이미 존재 → 매핑만, 마이그레이션 0)
- `@ManyToOne(LAZY)`: `fromUser`→users · `toCheckIn`→check_ins · `place`→places (모두 `nullable=false`)
- `message`(nullable), `status`(`@Enumerated(STRING)`, NOT NULL), `createdAt`(`updatable=false`, NOT NULL), `respondedAt`(nullable)
- `protected` 기본 생성자 + `private` 생성자 + 정적 팩토리(`CheckIn` 패턴)
- 도메인 메서드:
  - `static create(User fromUser, CheckIn toCheckIn, Place place, String message, LocalDateTime now)` → status=PENDING, createdAt=now
  - `accept(LocalDateTime now)` / `decline(LocalDateTime now)` → status 전이 + respondedAt 기록 (서비스가 PENDING 가드 후 호출)
  - `isReceivedBy(Long userId)` → `toCheckIn.getUser().getId().equals(userId)`
  - `isPending()` → status==PENDING

### MealRequestStatus
`PENDING`, `ACCEPTED`, `DECLINED` (ERD §5 ENUM 정의와 일치).

## 4. 서비스 로직 (`MealRequestService`)

생성자 주입 + `final`. 의존: `MealRequestRepository`, `CheckInRepository`(대상 체크인 로드), `UserRepository`(`getReferenceById`로 fromUser 참조), `Clock`(KST 시각 — `CheckInService`와 동일 방식).

- `createMealRequest(Long userId, MealRequestCreateRequest req)` `@Transactional`
  1. `checkInRepository.findById(toCheckInId)` → 없거나 `status≠ACTIVE` → 404 `TARGET_CHECKIN_NOT_AVAILABLE`
  2. 대상 주인 id == userId → 409 `MEALREQUEST_SELF`
  3. 대상 주인 `allowMealRequest==false` → 403 `MEALREQUEST_OPT_OUT`
  4. `MealRequest.create(userRef, toCheckIn, toCheckIn.getPlace(), message, now())` 저장
  5. `DataIntegrityViolationException` catch → 409 `MEALREQUEST_DUPLICATE`
  - 단건 조회라 lazy 접근(주인·place)을 트랜잭션 내에서 처리(N+1 루프 아님).
- `accept(Long userId, Long id)` / `decline(Long userId, Long id)` `@Transactional`
  1. `findWithReceiverById(id)`(toCheckIn.user fetch join) → 없음 404 `MEALREQUEST_NOT_FOUND`
  2. `!isReceivedBy(userId)` → 403 `FORBIDDEN`
  3. `!isPending()` → 409 `MEALREQUEST_ALREADY_RESPONDED`
  4. `accept(now())`/`decline(now())` → `MealRequestStatusResponse`
- `getMealRequests(Long userId, String role, String status)` `@Transactional(readOnly=true)`
  - `role` 파싱(received 기본/sent, 그 외 400) · `status` 파싱(선택, 그 외 400)
  - received/sent 각각 레포지토리 쿼리 호출 → `MealRequestListItemResponse`로 매핑

## 5. 레포지토리 (`MealRequestRepository extends JpaRepository<MealRequest, Long>`)

단일 활성처럼 중복 방지는 DB 유니크(`uq_meal_request_from_target`)가 강제하고, 여기서는 조회 쿼리를 제공한다.

- `findWithReceiverById(Long id)` — `SELECT mr FROM MealRequest mr JOIN FETCH mr.toCheckIn ci JOIN FETCH ci.user WHERE mr.id = :id` (accept/decline 권한 검사용, 수신자 로딩)
- `findReceived(Long userId, MealRequestStatus statusOrNull)` — `mr JOIN mr.toCheckIn ci JOIN FETCH mr.fromUser WHERE ci.user.id = :userId AND (:status IS NULL OR mr.status = :status) ORDER BY mr.createdAt DESC` (fromUser fetch join → 닉네임 N+1 방지)
- `findSent(Long userId, MealRequestStatus statusOrNull)` — `mr JOIN FETCH mr.fromUser WHERE mr.fromUser.id = :userId AND (:status IS NULL OR mr.status = :status) ORDER BY mr.createdAt DESC`

> received는 `idx_meal_requests_target_status(to_check_in_id, status)`를, 단일활성/중복방지는 `uq_meal_request_from_target`을 활용한다(모두 V1에 있음).

## 6. 에러 코드 (`ErrorCode`에 meal 블록 신설)

체크인 블록 아래에 추가:

| 코드 | HTTP | 상황 |
|---|---|---|
| `MEALREQUEST_NOT_FOUND` | 404 | 신청 자체 없음(accept/decline) |
| `TARGET_CHECKIN_NOT_AVAILABLE` | 404 | 대상 체크인 없음 또는 이미 종료 |
| `MEALREQUEST_SELF` | 409 | 자기 자신에게 신청 |
| `MEALREQUEST_OPT_OUT` | 403 | 대상 수신 거부(allowMealRequest=false) |
| `MEALREQUEST_DUPLICATE` | 409 | 동일 대상 중복 신청 |
| `MEALREQUEST_ALREADY_RESPONDED` | 409 | 이미 응답한 신청 |

수신자 권한 미달은 기존 `FORBIDDEN`, 입력 파싱 실패는 기존 `INVALID_INPUT` 재사용.

## 7. 보안

4개 엔드포인트 모두 🔒 정식 USER 전용 → `SecurityConfig`의 `anyRequest().hasRole("USER")` 기본 규칙이 커버한다. **SecurityConfig 변경 없음**(토큰 없으면 401, 온보딩 토큰이면 403). `@CurrentUserId`로 JWT sub→userId 주입.

## 8. 테스트 전략 (CLAUDE.md 피라미드 — Service 단위 최다)

- **`MealRequestServiceTest`** (순수 단위, Mockito) — 핵심:
  - create: 성공 / 404(없음·종료) / 409(자기신청) / 403(opt-out) / 409(중복, `DataIntegrityViolationException`)
  - accept: 성공 / 404 / 403(비수신자) / 409(이미 응답) — decline 대칭
  - list: received / sent / status 필터 / role·status 파싱 실패(400)
  - **opt-in 거부(403)가 1순위 시나리오**(NFR-03)
- **`MealRequestRepositoryTest`** (`@DataJpaTest`, Testcontainers Postgres) — 유니크 제약 위반, received/sent 쿼리 정확성, fetch join N+1 방지, status 필터.
- **`MealRequestControllerTest`** (`@WebMvcTest`, Service 모킹) — 응답 형태(POST 201 / list / accept·decline), `@Valid`(toCheckInId 필수) 400, `role` 기본값(received), 에러 코드→HTTP 매핑.

## 9. 작업 영향 요약

| 항목 | 변경 |
|---|---|
| 신규 파일 | `meal/` 8개(엔티티·enum·DTO 4·repo·service·controller) + 테스트 3 |
| `ErrorCode.java` | meal 블록 6개 코드 추가 |
| 마이그레이션 | **없음** (`meal_requests`·인덱스 V1에 이미 존재) |
| `SecurityConfig` | **없음** |
| 기존 슬라이스 | `CheckInRepository`·`UserRepository`는 기존 메서드(`findById`/`getReferenceById`) 재사용, 변경 없음 |
