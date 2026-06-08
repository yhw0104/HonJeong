# 🔌 REST API 명세서 — 혼정 (혼밥을 정상화하다)

> 2단계(설계) 문서. MVP(FR-001~003, FR-101~108) 기준 REST API 정의.
> Base URL: `/api` · 포맷: JSON · 인증: JWT (Bearer)

---

## 1. 공통 규약

### 1.1 인증 헤더
```
Authorization: Bearer <accessToken>
```
- 인증 필요 API에 누락/만료 시 `401 Unauthorized`.

### 1.2 공통 응답 포맷
성공:
```json
{ "success": true, "data": { ... } }
```
에러:
```json
{ "success": false, "error": { "code": "CHECKIN_NOT_FOUND", "message": "체크인을 찾을 수 없습니다." } }
```

### 1.3 상태 코드
| 코드 | 의미 |
| --- | --- |
| 200 | 조회/수정 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청(검증 실패) |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌(중복 등) |

### 1.4 페이징 규약
`?page=0&size=20` (0-base). 응답에 `content`, `page`, `size`, `totalElements` 포함.

---

## 2. 인증 (Auth) — FR-001/004/005/006

> **소셜 로그인(OAuth: 카카오/애플) + 휴대폰 번호 인증(SMS)** 기반. 이메일/비밀번호 가입·로그인은 폐기. 공급자 access/refresh 토큰은 저장하지 않고 우리 자체 JWT를 발급한다.

### POST `/api/auth/oauth/{provider}` — 소셜 로그인 · 🔓 · FR-004
`provider` = `kakao` | `apple`. 클라가 받은 토큰을 서버가 검증 → `(provider, providerUserId)`로 회원 조회/생성.
요청:
```json
{ "idToken": "ey...", "authCode": "..." }
```
응답 `200`(기존 회원):
```json
{ "success": true, "data": { "accessToken": "ey...", "refreshToken": "ey...", "expiresIn": 3600 } }
```
- 신규 회원이면 **온보딩용 임시 토큰** 발급 → 휴대폰 인증·약관 동의·프로필 셋업 후 가입 완료.

### POST `/api/auth/phone/send-code` — 인증번호 발송 · 🔓 · FR-005
요청: `{ "phone": "01012345678" }` → SMS 발송(발송 rate-limit).

### POST `/api/auth/phone/verify` — 인증번호 확인 · 🔓 · FR-005
요청: `{ "phone": "01012345678", "code": "123456" }`
응답 `200`: 기존 회원=JWT 발급 / 신규=온보딩용 임시 토큰.
- `400` 코드 불일치/만료, `429` 시도 횟수 초과.

### POST `/api/auth/terms` — 약관 동의 저장 · 🔒(임시토큰) · FR-006
요청: `{ "service": true, "privacy": true, "location": true, "marketing": false }` (필수 3종 true 필요).

### POST `/api/auth/refresh` — 토큰 재발급 · 🔓
요청: `{ "refreshToken": "ey..." }` → 응답: 새 `accessToken`.

---

## 3. 사용자 (User) — FR-003

### GET `/api/users/me` — 내 프로필 · 🔒
응답 `200`: `{ "id":1, "phone":"010********", "email":"...(OAuth 선택)", "nickname":"혼밥러", "profileImageUrl":"...", "region":"...", "allowMealRequest":true }`

### PATCH `/api/users/me` — 프로필 수정 · 🔒
요청: `{ "nickname": "새닉네임", "allowMealRequest": false }`(필드 선택) → 응답 `200`: 수정된 프로필.
- `allowMealRequest`로 같이먹기 수신 opt-in/거부 토글 (FR-108, NFR-03).

---

## 4. 장소 (Place) — FR-105

### GET `/api/places/search` — 가게 검색 · 🔒
카카오 로컬 API 프록시. 선택된 가게는 체크인/리뷰 시 `places`에 캐싱.

쿼리 파라미터:
| 파라미터 | 필수 | 설명 |
| --- | --- | --- |
| query | ✅ | 검색어(가게명/지역) |
| lat, lng | ⬜ | 중심 좌표(거리순 정렬용) |

응답 `200`:
```json
{ "success": true, "data": [
  { "externalId": "12345", "name": "혼밥식당", "address": "서울 ...", "latitude": 37.5, "longitude": 127.0, "category": "한식" }
] }
```

---

## 5. 체크인 (CheckIn) — FR-101~104

### POST `/api/check-ins` — 혼밥 체크인 시작 · 🔒 · FR-101
요청 (선택한 가게의 externalId 전달 → 서버가 places upsert):
```json
{ "externalId": "12345", "name": "혼밥식당", "address": "서울 ...", "latitude": 37.5, "longitude": 127.0, "category": "한식" }
```
응답 `201`:
```json
{ "success": true, "data": { "checkInId": 10, "placeId": 3, "status": "ACTIVE", "startedAt": "2026-05-22T12:00:00" } }
```
- `409` 이미 ACTIVE 체크인 존재 시(정책: 사용자당 1개만 — 부분 유니크 인덱스). **복구**: 클라는 진입 시 `GET /api/check-ins/me`로 선동기화, 409면 기존 체크인 종료(`PATCH .../end`) 후 재시도. 같은 장소 재요청은 멱등 반환. 방치된 ACTIVE는 TTL 자동 만료(NFR-07).

### PATCH `/api/check-ins/{id}/end` — 체크인 종료 · 🔒 · FR-102
응답 `200`: `{ "checkInId": 10, "status": "ENDED", "endedAt": "..." }`
- `403` 본인 체크인이 아닐 때, `404` 없음.

### GET `/api/check-ins/me` — 내 현재 체크인 · 🔒
응답 `200`: 현재 ACTIVE 체크인(없으면 `data: null`).

### GET `/api/check-ins/stats` — 혼밥 통계 · 🔒 · FR-103
응답 `200`:
```json
{ "success": true, "data": { "todayCount": 124, "activeCount": 17 } }
```

### GET `/api/check-ins/map` — 지도 마커 · 🔒 · FR-104
반경 내 장소별 현재 혼밥러 수.

쿼리: `lat`, `lng`(필수), `radius`(m, 기본 1000)
응답 `200`:
```json
{ "success": true, "data": [
  { "placeId": 3, "name": "혼밥식당", "latitude": 37.5, "longitude": 127.0, "activeCount": 3 }
] }
```

### GET `/api/places/{placeId}/check-ins` — 같은 식당 현재 혼밥러 목록 · 🔒 · FR-107
선택한 식당에 현재 ACTIVE 체크인 중인 혼밥러 목록. **프라이버시(NFR-03): 닉네임/체크인 경과만 노출, 정확 좌표·실명 비노출.**
응답 `200`:
```json
{ "success": true, "data": [
  { "checkInId": 10, "nickname": "혼밥러", "startedAt": "2026-05-22T12:00:00", "elapsedMinutes": 15 }
] }
```

---

## 6. 반응 (Reaction) — FR-106 · ⏸️ **보류(추후 도입)**

> 초간단 반응("나도 여기 있음"/"같이 먹는 중")은 **이번 범위에서 보류**한다. 핵심 연결은 같이먹기(§7)만으로 성립. 추후 도입 시 `POST /api/reactions { checkInId, type(HERE|EATING_TOGETHER) }` + `UNIQUE(fromUser, checkIn, type)`로 복원.

---

## 7. 같이먹기 (MealRequest) — FR-108

### POST `/api/meal-requests` — 같이먹기 신청 · 🔒
같은 식당의 혼밥러(대상 체크인)에게 같이먹기 신청을 보낸다.
요청:
```json
{ "toCheckInId": 10, "message": "같이 드실래요?" }
```
응답 `201`:
```json
{ "success": true, "data": { "mealRequestId": 7, "toCheckInId": 10, "message": "같이 드실래요?", "status": "PENDING" } }
```
- `403` 대상이 수신 거부(opt-in off, `allowMealRequest=false`).
- `404` 대상 체크인 없음/이미 종료.
- `409` 자기 자신에게 신청 / 동일 대상 중복 신청.

### GET `/api/meal-requests` — 신청 목록 · 🔒
쿼리: `role=received`(받은, 기본) | `sent`(보낸), `status`(선택: PENDING/ACCEPTED/DECLINED)
응답 `200`:
```json
{ "success": true, "data": [
  { "mealRequestId": 7, "fromUser": { "nickname": "옆자리" }, "placeId": 3, "message": "같이 드실래요?", "status": "PENDING", "createdAt": "2026-05-22T12:05:00" }
] }
```

### PATCH `/api/meal-requests/{id}/accept` — 수락 · 🔒
응답 `200`: `{ "mealRequestId": 7, "status": "ACCEPTED", "respondedAt": "..." }`
- `403` 수신자가 아닐 때, `404` 없음, `409` 이미 응답 완료.

### PATCH `/api/meal-requests/{id}/decline` — 거절 · 🔒
응답 `200`: `{ "mealRequestId": 7, "status": "DECLINED", "respondedAt": "..." }`
- `403` 수신자가 아닐 때, `404` 없음, `409` 이미 응답 완료.

> 신청 수신 시 알림(NFR-08)은 푸시/폴링 중 택1(설계 단계 확정). 차단/신고는 운영 정책으로 처리(추후 구체화).

---

## 8. 엔드포인트 ↔ 요구사항 추적표

| 엔드포인트 | FR |
| --- | --- |
| POST /api/auth/oauth/{provider}, /phone/send-code, /phone/verify, /terms, /refresh | FR-001, FR-004, FR-005, FR-006 |
| GET/PATCH /api/users/me | FR-003 |
| GET /api/places/search | FR-105 |
| POST /api/check-ins, PATCH .../end, GET .../me | FR-101, FR-102 |
| GET /api/check-ins/stats | FR-103 |
| GET /api/check-ins/map | FR-104 |
| GET /api/places/{placeId}/check-ins | FR-107 |
| ~~POST /api/reactions~~ (보류) | FR-106 |
| POST /api/meal-requests, PATCH .../accept, .../decline, GET /api/meal-requests | FR-108 |

---

## 9. 다음 단계 (3단계: 개발)
이 명세를 기준으로 `backend/`에 Spring Boot 프로젝트를 생성하고 다음 순서로 구현:
1. 프로젝트 셋업(Gradle, application.yml, Flyway, PostgreSQL via Docker)
2. 엔티티 + 마이그레이션(`V1__init.sql`)
3. 인증(Security + JWT): OAuth(카카오/애플)+휴대폰 인증/약관/refresh
4. 장소 검색(카카오 클라이언트 + 캐싱)
5. 체크인 + 통계 + 지도 API (+ 같은 식당 혼밥러 목록, FR-107)
6. ~~반응 API~~ (보류)
7. 같이먹기 API (신청/수락/거절/목록, FR-108) + opt-in 토글
8. 테스트 → 컨테이너화 → CI/CD
