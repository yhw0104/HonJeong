# Slice 5 설계 — checkin 혼밥 체크인 API (통계·지도·혼밥러 목록·TTL)

> 작성: 2026-06-15 · 대상: `backend/` (혼정 Spring Boot, `com.honjeong.checkin`)
> source of truth: `docs/05-API명세서.md` §5, 요구사항 FR-101~104·107, ERD `04-ERD-데이터모델.md`

## 1. 개요 · 범위

혼정의 **심장**. 사용자가 식당을 선택해 "혼밥 중"을 등록하고, 그 데이터로 사회적 증거 통계·지도 마커·혼밥러 목록을 집계한다. Slice 4의 `PlaceService.findOrCreateByExternalId`(캐싱 upsert)의 첫 호출처다. Auth/User/Place 슬라이스의 수직 슬라이스 패턴(`controller/service/repository/domain/dto`)을 그대로 미러링한다.

**포함 (6개 엔드포인트 + TTL 스케줄러 — 한 슬라이스로 전부)**
- `POST  /api/check-ins` — 혼밥 체크인 시작 (FR-101)
- `PATCH /api/check-ins/{id}/end` — 체크인 종료 (FR-102)
- `GET   /api/check-ins/me` — 내 현재 ACTIVE 체크인
- `GET   /api/check-ins/stats` — "오늘 N명 / 현재 N명" 사회적 증거 (FR-103)
- `GET   /api/check-ins/map` — 반경 내 식당별 현재 혼밥러 수 마커 (FR-104)
- `GET   /api/places/{placeId}/check-ins` — 같은 식당 현재 혼밥러 목록 (FR-107)
- **TTL 자동 만료** (NFR-07) — `@Scheduled`로 방치된 ACTIVE를 자동 ENDED

**범위 밖**: 같이먹기(Slice 6, FR-108) · 반응(보류, FR-106) · 카카오/SMS/OAuth real 스왑(P1 잔여) · 차단/신고(Slice 6 흡수 예정) · 푸시 알림(P2).

**기술 방향 결정 (brainstorming 확정)**
- 지도 반경 검색 = **바운딩박스 + Java Haversine 보정** (PostGIS 아님). 이유: 실시간 집계는 공간인덱스 문제가 아니라 관계형 `GROUP BY`(이미 `idx_check_ins_place_status` 인덱스 있음)이고, PostGIS의 이득(정확 반경·대용량)은 MVP에서 안 옴. 지오 쿼리는 **레포지토리 한 메서드 뒤에 추상화**해 후일 PostGIS 스왑 시 서비스/컨트롤러 무변경.
- TTL = `@Scheduled` 고정주기 + 만료기준 시간은 설정값(`honjeong.checkin.active-ttl-hours`, 기본 3h).
- 단일 활성 정책 = **같은 장소 재요청 멱등 / 다른 장소 409**(아래 §5).

## 2. 엔드포인트 명세

### POST /api/check-ins — 🔒 USER · FR-101
요청 (`CheckInRequest` — 선택한 가게 정보, `external_id`로 places upsert):
```json
{ "externalId": "12345", "name": "혼밥식당", "address": "서울 ...", "latitude": 37.5, "longitude": 127.0, "category": "한식" }
```
응답 **201** (`CheckInResponse`):
```json
{ "success": true, "data": { "checkInId": 10, "placeId": 3, "status": "ACTIVE", "startedAt": "2026-06-15T12:00:00", "endedAt": null } }
```
> `CheckInResponse`는 POST/end/me 공용이라 `endedAt`을 항상 포함한다(ACTIVE면 `null`). 명세 예시의 최소 형태보다 한 필드 많은 **의도적 상위집합** — 프론트는 status로 분기.
- 검증: `externalId`·`name` `@NotBlank`, `latitude`·`longitude` `@NotNull`(`@Valid` → 400 `INVALID_INPUT`). `address`·`category` 선택.
- **이미 ACTIVE 존재 시**: 같은 place면 기존 체크인 **그대로 201 반환(멱등)**, 다른 place면 **409 `CHECKIN_ALREADY_ACTIVE`**.
- 멱등 같은-장소 재요청도 201로 통일(상태코드 분기 안 함 — 더블탭/재시도 안전).

### PATCH /api/check-ins/{id}/end — 🔒 USER · FR-102
응답 200 (`CheckInResponse`, `status=ENDED`·`endedAt` 채워짐):
```json
{ "success": true, "data": { "checkInId": 10, "placeId": 3, "status": "ENDED", "startedAt": "...", "endedAt": "2026-06-15T13:00:00" } }
```
- `404 CHECKIN_NOT_FOUND`(없음) · `403 FORBIDDEN`(본인 체크인 아님).
- 이미 ENDED면 **멱등 반환**(에러 아님, 기존 endedAt 유지).

### GET /api/check-ins/me — 🔒 USER
응답 200: 현재 ACTIVE 체크인(`CheckInResponse`), **없으면 `data: null`**.

### GET /api/check-ins/stats — 🔒 USER · FR-103
응답 200 (`CheckInStatsResponse`):
```json
{ "success": true, "data": { "todayCount": 124, "activeCount": 17 } }
```
- `todayCount` = **오늘(Asia/Seoul 자정 기준) 체크인한 distinct 사용자 수** ("N명").
- `activeCount` = 현재 `status='ACTIVE'` 수(단일활성이라 곧 distinct 사용자 수와 동일).

### GET /api/check-ins/map — 🔒 USER · FR-104
쿼리: `lat`·`lng`(**필수**), `radius`(m, 기본 1000, 상한 10000 클램프).
응답 200 (`List<MapMarkerResponse>` — 박스 내 ACTIVE 있는 식당만, 거리순):
```json
{ "success": true, "data": [
  { "placeId": 3, "name": "혼밥식당", "latitude": 37.5, "longitude": 127.0, "activeCount": 3 }
] }
```
- `lat`/`lng` 누락 → 400 `INVALID_INPUT`.

### GET /api/places/{placeId}/check-ins — 🔒 USER · FR-107
응답 200 (`List<CheckInUserResponse>` — 현재 ACTIVE 혼밥러, startedAt 오름차순):
```json
{ "success": true, "data": [
  { "checkInId": 10, "nickname": "혼밥러", "startedAt": "2026-06-15T12:00:00", "elapsedMinutes": 15 }
] }
```
- **프라이버시(NFR-03)**: 닉네임·startedAt·경과분만. 정확 좌표·실명·userId 비노출.
- 해당 place에 ACTIVE 없으면 빈 배열.

## 3. 계층 · 파일 구조 (CLAUDE.md 컨벤션)

```
com.honjeong.checkin
├── controller/
│   ├── CheckInController.java        # 신규. @RequestMapping("/api/check-ins") — POST/end/me/stats/map
│   └── PlaceCheckInController.java   # 신규. @RequestMapping("/api/places") — GET /{placeId}/check-ins
├── service/CheckInService.java       # 신규. 비즈니스 규칙·@Transactional 경계
├── scheduler/CheckInExpiryScheduler.java  # 신규. @Scheduled → service.expireStaleCheckIns()
├── repository/CheckInRepository.java # 신규
├── domain/
│   ├── CheckIn.java                  # 신규. @ManyToOne User·Place, start()/end() 변경자
│   └── CheckInStatus.java            # 신규. ACTIVE|ENDED
└── dto/
    ├── CheckInRequest.java           # 신규. POST 바디(@Valid) + toUpsertCommand()
    ├── CheckInResponse.java          # 신규. from(CheckIn) — POST/end/me 공용
    ├── CheckInStatsResponse.java     # 신규. { todayCount, activeCount }
    ├── MapMarkerResponse.java        # 신규. 지도 마커(레포 프로젝션 타깃)
    └── CheckInUserResponse.java      # 신규. 혼밥러 목록(레포 프로젝션 + elapsedMinutes)

com.honjeong.global
├── config/  @EnableScheduling, Clock 빈, HonjeongCheckInProperties(@ConfigurationProperties)
└── exception/ErrorCode.java          # 수정. CHECKIN_ALREADY_ACTIVE(409)·CHECKIN_NOT_FOUND(404) 추가

com.honjeong.place.service.PlaceUpsertCommand  # 재사용(checkin → place 의존)
```
- 의존성: 생성자 주입 + `final`. `CheckInService`는 `CheckInRepository`·`PlaceService`·`UserRepository`(getReference용)·`Clock` 의존.
- 크로스 도메인: `checkin → place`(PlaceService/Place), `checkin → user`(User 엔티티 `@ManyToOne` + nickname). ERD `User 1—N CheckIn`, `Place 1—N CheckIn`상 정상.

## 4. 도메인 — `CheckIn` 엔티티

`check_ins`는 `created_at`만 있고 `updated_at`이 없어 **`BaseTimeEntity` 미상속**, 시각을 직접 매핑한다.

```java
@Entity @Table(name = "check_ins")
public class CheckIn {
    @Id @GeneratedValue(strategy = IDENTITY) private Long id;

    @ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id")  private User user;
    @ManyToOne(fetch = LAZY) @JoinColumn(name = "place_id") private Place place;

    @Enumerated(STRING) @Column(nullable = false) private CheckInStatus status;
    @Column(nullable = false) private LocalDateTime startedAt;
    private LocalDateTime endedAt;                       // ENDED 시 채워짐(nullable)
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;

    protected CheckIn() {}
    private CheckIn(...) {...}

    public static CheckIn start(User user, Place place, LocalDateTime now) {
        // status=ACTIVE, startedAt=now, createdAt=now, endedAt=null
    }
    public void end(LocalDateTime now) {
        if (status == ACTIVE) { this.status = ENDED; this.endedAt = now; }  // 이미 ENDED면 멱등(no-op)
    }
    public boolean isOwnedBy(Long userId) { return user.getId().equals(userId); }
    // getters
}
```
- 시각은 모두 서비스가 주입한 `Clock`에서 받아 넘긴다(테스트에서 고정 가능). 감사 리스너(@CreatedDate) 대신 명시적 set — 단일 시간소스로 통일.
- **시간대 일관성(중요)**: 기존 전역 `Clock` 빈(AppConfig, `systemDefaultZone()`)을 **그대로 주입**받되, CheckInService가 **모든 시각을 `clock.instant()`에서 `ZoneId.of("Asia/Seoul")`로 환산**한다 — `now()`(저장 시각)와 `todayStart`(통계 경계)가 같은 KST 기준이라 §5.3의 어긋남이 없다. 전역 빈을 안 건드려 auth 회귀 위험이 없고, instant는 시간대 무관이라 빈의 zone과도 독립적이다.
- `@ManyToOne(LAZY)`로 둬서 혼밥러 목록(`c.user.nickname`)·지도(`c.place.*`)를 **생성자 프로젝션 JPQL**로 N+1 없이 뽑는다.

## 5. 핵심 비즈니스 규칙 (`CheckInService`)

### 5.1 단일 활성 체크인 (3중 방어)
1. **DB** — 부분 유니크 인덱스 `uq_check_ins_active_user ON check_ins(user_id) WHERE status='ACTIVE'`(이미 있음)가 물리적 불변식. 동시요청에도 두 번째 INSERT는 깨짐.
2. **서비스** — POST 시 기존 ACTIVE 선조회 → 같은 place 멱등 / 다른 place 409. 경쟁으로 인덱스 위반 시 `DataIntegrityViolationException` → 409로 변환.
3. **프론트(참고)** — 진입 시 `GET /me`로 토글 선동기화. 진짜 불변식은 ①이 보장하고 프론트는 UX 안내만.

```java
@Transactional
public CheckInResponse createCheckIn(Long userId, CheckInRequest req) {
    Place place = placeService.findOrCreateByExternalId(req.toUpsertCommand());
    Optional<CheckIn> active = checkInRepository.findByUser_IdAndStatus(userId, ACTIVE);
    if (active.isPresent()) {
        CheckIn existing = active.get();
        if (existing.getPlace().getId().equals(place.getId())) {
            return CheckInResponse.from(existing);                 // 같은 장소 → 멱등
        }
        throw new BusinessException(ErrorCode.CHECKIN_ALREADY_ACTIVE); // 다른 장소 → 409
    }
    try {
        User userRef = userRepository.getReferenceById(userId);    // 프록시 — DB 히트 없이 FK만
        CheckIn saved = checkInRepository.save(CheckIn.start(userRef, place, now()));
        return CheckInResponse.from(saved);
    } catch (DataIntegrityViolationException e) {                  // 경쟁 상황(인덱스 위반)
        throw new BusinessException(ErrorCode.CHECKIN_ALREADY_ACTIVE);
    }
}
```
> 409 복구는 명세대로 클라가 `/me`로 동기화 → end 후 재시도. 그래서 409 바디는 평범하게 유지(기존정보 미동봉).

### 5.2 종료 / 내 체크인
```java
@Transactional
public CheckInResponse endCheckIn(Long userId, Long checkInId) {
    CheckIn ci = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));
    if (!ci.isOwnedBy(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
    ci.end(now());                       // 이미 ENDED면 멱등
    return CheckInResponse.from(ci);     // dirty checking UPDATE
}

@Transactional(readOnly = true)
public CheckInResponse getMyActiveCheckIn(Long userId) {
    return checkInRepository.findByUser_IdAndStatus(userId, ACTIVE)
            .map(CheckInResponse::from).orElse(null);   // 없으면 null → data:null
}
```

### 5.3 통계 (사회적 증거)
```java
@Transactional(readOnly = true)
public CheckInStatsResponse getStats() {
    LocalDateTime todayStart = LocalDate.ofInstant(clock.instant(), KST).atStartOfDay();  // KST 자정
    long today  = checkInRepository.countDistinctUsersStartedSince(todayStart);
    long active = checkInRepository.countByStatus(ACTIVE);
    return new CheckInStatsResponse(today, active);
}
```
- `now()`·`todayStart` 모두 `clock.instant()`를 KST로 환산해 만든다(§4). `todayStart`(KST 자정)와 저장 `startedAt`(KST)이 같은 기준이라 경계 어긋남 없음. naive timestamp 컬럼이라도 저장·조회가 한 시간대라 안전. 테스트는 고정 Clock으로 경계(자정 직전/직후) 검증.

### 5.4 지도 (바운딩박스 + Haversine 보정)
```java
@Transactional(readOnly = true)
public List<MapMarkerResponse> getMap(Double lat, Double lng, int radius) {
    if (lat == null || lng == null) throw new BusinessException(ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다.");
    int r = Math.min(radius, MAX_RADIUS);                 // 기본 1000, 상한 10000
    double dLat = r / 111_320.0;
    double dLng = r / (111_320.0 * Math.cos(Math.toRadians(lat)));
    List<MapMarkerResponse> box = checkInRepository.countActiveByPlaceWithinBounds(
            lat - dLat, lat + dLat, lng - dLng, lng + dLng);   // GROUP BY 집계 프로젝션
    return box.stream()
            .filter(m -> haversine(lat, lng, m.latitude(), m.longitude()) <= r)  // 원형 보정
            .sorted(comparingDouble(m -> haversine(lat, lng, m.latitude(), m.longitude())))
            .toList();
}
```

### 5.5 혼밥러 목록 (프라이버시)
```java
@Transactional(readOnly = true)
public List<CheckInUserResponse> getActiveDiners(Long placeId) {
    LocalDateTime now = now();
    return checkInRepository.findActiveDinersByPlace(placeId).stream()   // (checkInId, nickname, startedAt) 프로젝션
            .map(d -> new CheckInUserResponse(d.checkInId(), d.nickname(), d.startedAt(),
                    Duration.between(d.startedAt(), now).toMinutes()))
            .toList();
}
```

### 5.6 TTL 자동 만료
```java
@Transactional
public int expireStaleCheckIns() {
    LocalDateTime threshold = now().minusHours(props.activeTtlHours());   // 기본 3h
    return checkInRepository.endActiveBefore(ENDED, threshold, now());    // @Modifying bulk UPDATE, 반환=만료 수
}
```
- 스케줄러는 이 메서드만 호출(스케줄러 자체는 얇게). 테스트는 이 서비스 메서드를 단위 검증.

## 6. Repository — `CheckInRepository`

| 메서드 | 종류 | 용도 |
|---|---|---|
| `findByUser_IdAndStatus(Long, CheckInStatus)` | 파생 | me·단일활성 검사 |
| `findById` | 기본 | end·소유권 |
| `countByStatus(CheckInStatus)` | 파생 | stats activeCount |
| `countDistinctUsersStartedSince(LocalDateTime)` | `@Query COUNT(DISTINCT c.user.id) WHERE c.startedAt >= ?` | stats todayCount |
| `countActiveByPlaceWithinBounds(latMin,latMax,lngMin,lngMax)` | `@Query` 생성자 프로젝션 `MapMarkerResponse`, `JOIN c.place p WHERE c.status=ACTIVE AND p.lat BETWEEN .. AND p.lng BETWEEN .. GROUP BY p.id,p.name,p.lat,p.lng` | 지도 |
| `findActiveDinersByPlace(Long)` | `@Query` 프로젝션 `WHERE c.place.id=? AND c.status=ACTIVE ORDER BY c.startedAt` | 혼밥러 목록 |
| `endActiveBefore(ENDED, threshold, now)` | `@Modifying @Query UPDATE CheckIn SET status=ENDED,endedAt=:now WHERE status=ACTIVE AND startedAt < :threshold` | TTL |

- 집계·목록은 생성자 프로젝션이라 엔티티 하이드레이션·N+1 없음.

## 7. 에러 (`ErrorCode` 추가 2개)

| 코드 | 상태 | 발생 |
|---|---|---|
| `CHECKIN_ALREADY_ACTIVE` | 409 | POST 시 다른 장소에 이미 ACTIVE / 경쟁 인덱스 위반 |
| `CHECKIN_NOT_FOUND` | 404 | end 대상 체크인 없음 |
| `FORBIDDEN`(재사용) | 403 | end 본인 체크인 아님 |
| `INVALID_INPUT`(재사용) | 400 | POST 바디 검증 / map lat·lng 누락 |
| `UNAUTHORIZED`/`FORBIDDEN`(필터) | 401/403 | 토큰 없음 / 온보딩 토큰 |

## 8. 설정 변경

- `@EnableScheduling` (config 클래스에 추가). **`Clock` 빈은 기존 AppConfig 것 재사용**(신규 없음) — 서비스가 `clock.instant()`를 KST로 환산.
- `HonjeongCheckInProperties`(`@ConfigurationProperties("honjeong.checkin")`): `activeTtlHours`(기본 3), `expiryIntervalMs`(기본 300000).
- `application.yml`(local·prod)에 `honjeong.checkin.*` 추가.
- **SecurityConfig 무변경** — `/api/check-ins/**`·`/api/places/{id}/check-ins` 전부 `anyRequest().hasRole("USER")` 커버.

## 9. 테스트 계획 (TDD: 실패 → 구현 → 통과)

**CheckInServiceTest** (순수 단위, Mockito + 고정 `Clock`) — 최우선
- createCheckIn: 신규 201 / 같은 장소 멱등 / 다른 장소 409 / 경쟁 `DataIntegrityViolationException`→409
- endCheckIn: 성공 ENDED / 없음 404 / 본인 아님 403 / 이미 ENDED 멱등
- getMyActiveCheckIn: active 반환 / 없으면 null
- getStats: 오늘(KST 경계) distinct 수 / activeCount
- getMap: lat·lng 누락 400 / 박스 계산 / Haversine 원형 보정 / 거리순 정렬 / radius 클램프
- getActiveDiners: 경과분 계산 / 프라이버시 필드만
- expireStaleCheckIns: 임계 지난 ACTIVE만 만료, 그 외 보존

**CheckInRepositoryTest** (`@DataJpaTest` + `AbstractPostgresTest`, H2 아님)
- 단일활성 인덱스: 같은 user 2번째 ACTIVE 저장 → 예외
- countActiveByPlaceWithinBounds: 박스 안/밖, place별 GROUP BY 정확
- findActiveDinersByPlace: ACTIVE만·정렬·프로젝션 매핑
- countDistinctUsersStartedSince / countByStatus
- endActiveBefore: 일괄 UPDATE 건수·대상

**CheckInControllerTest + PlaceCheckInControllerTest** (`@WebMvcTest` + `@Import(SecurityConfig, WebConfig)`, Service 모킹)
- POST 201 / `@Valid` 400 / 409 / me 200·null / stats 200 / map 200·400(lat 누락) / end 200·404·403 / 혼밥러목록 200
- 인가: 토큰 없음 401 / 온보딩 토큰 403

피라미드: 단위(Service) 다수 > 슬라이스 > 통합. `@SpringBootTest`는 e2e 스모크로 대체.

## 10. 구현 순서 (예정)

1. feature 브랜치 `feat/slice-5-checkin`
2. (RED→GREEN) `CheckIn`·`CheckInStatus` + `CheckInRepositoryTest`/Repository
3. (RED→GREEN) `CheckInServiceTest` → `CheckInService` + DTO + `ErrorCode` 추가
4. (RED→GREEN) 컨트롤러 2개 + 테스트
5. 스케줄러 + 설정(@EnableScheduling·Clock·properties·yml)
6. 전체 테스트 그린 → e2e 라이브(토큰→POST→me→stats→map→혼밥러목록→end)
7. main 병합 → 요구사항 xlsx FR-101~104·107 체크 → 메모리 갱신
