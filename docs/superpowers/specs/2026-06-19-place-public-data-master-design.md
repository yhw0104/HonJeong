# 설계: 식당 데이터 공공데이터 마스터 전환 + 전국 적재 + 우리 DB 검색

> 작성일 2026-06-19 · 대상: 백엔드(`backend/`) · 후속 영향: 앱(`app/`)·docs
> 상태: 설계 확정 대기 → 승인 후 writing-plans

## 1. 배경 · 결정 (왜 이렇게 가는가)

### 1.1 문제 발견
원래 작업은 "카카오 로컬 API 실연동 + `GET /api/places/nearby`"였다. 진행 중 두 가지가 드러났다.

1. **카카오 로컬 데이터 장기 저장 = 약관 위반.** 카카오 공식(운영정책 제20조): 로컬 API로 받은 POI 데이터는 "사용자 환경 개선 목적의 짧은 캐싱(1~2시간)"만 허용, 1달·1년 등 저장은 "데이터 저장사용"으로 판단 → API 차단 대상. 출처: devtalk.kakao.com/t/api/140197.
2. **메뉴·편의시설은 카카오·공공데이터 둘 다 제공하지 않는다.** 카카오 로컬 API 응답 필드 = place_name/phone/category_name/address_name/좌표/place_url 뿐. 공공 인허가도 인허가 정보뿐. → 메뉴·편의시설은 **자체 데이터(점주 등록 / 크롤 / UGC)** 로만 채울 수 있다.

### 1.2 결정
- **식당 마스터 데이터의 출처를 카카오 → 공공데이터(전국일반음식점표준데이터)로 전환한다.** 공공데이터는 이용허락범위 "제한 없음"(상업적 이용·저장 자유), 전국 약 213만 건.
- **카카오는 "지도 렌더링 SDK"로만 격하**한다(앱 화면용, 데이터 저장 무관). 백엔드의 카카오 로컬 API 데이터 경로(검색)는 제거한다.
- **식당 검색·주변검색·상세를 우리 DB(`places`) 위에서** 수행한다. `places`는 카카오 캐시가 아니라 **공공데이터로 일괄 적재된 마스터**가 된다.
- 메뉴·편의시설은 이 마스터 위에 얹는 **강화층(P2)** — 안전한 경로는 **점주 직접 등록**(네이버 스마트플레이스 패턴), 보조로 크롤(법적 리스크 인지). 이번 범위 아님.

### 1.3 식별자(identity) 원칙
- `places.id`(surrogate PK)가 **시스템의 식당 식별자**다. 체크인·리뷰·(향후)메뉴·편의시설은 전부 이 id로 FK 연결.
- 외부 소스 식별자(공공데이터 관리번호 등)는 **참조 속성**(`source_id`)일 뿐. 카카오 id는 더 이상 저장하지 않는다.

## 2. 데이터 모델 — `places` V2

| 컬럼 | 출처(표준데이터) | 비고 |
| --- | --- | --- |
| `id` BIGINT PK | — | 우리 surrogate 식별자(유지) |
| `source` VARCHAR(20) NOT NULL | — | `'PUBLIC_DATA'`(현재). 향후 `MANUAL` 등 |
| `source_id` VARCHAR(64) | 관리번호 | 멱등 적재 키. `(source, source_id)` UNIQUE |
| `name` VARCHAR(200) NOT NULL | 사업장명 | |
| `category` VARCHAR(50) | 위생업태명(업태구분) | 한식/중식/일식/카페… |
| `address` VARCHAR(300) | 소재지(지번)주소 | nullable |
| `road_address` VARCHAR(300) | 도로명주소 | nullable, 신규 |
| `phone` VARCHAR(40) | 소재지전화 | nullable, 신규 |
| `latitude` DOUBLE NOT NULL | 좌표정보(Y)→변환 | WGS84 |
| `longitude` DOUBLE NOT NULL | 좌표정보(X)→변환 | WGS84 |
| `business_status` VARCHAR(20) | 영업상태명 | 영업/폐업 — 노출필터·동기화용, 신규 |
| `created_at`/`updated_at` | — | BaseTimeEntity(유지) |

**제거**: 기존 `external_id`(카카오 id, NOT NULL UNIQUE) 컬럼.
**인덱스**: `UNIQUE(source, source_id)` · `btree(latitude, longitude)`(바운딩박스) · `GIN(name gin_trgm_ops)`(부분일치 검색, `pg_trgm` 확장 필요).

> 실제 표준데이터 컬럼명(예: `위생업태명` vs `업태구분명`, 좌표 X/Y 라벨)은 다운로드 CSV 헤더로 확정해 매핑한다(파서 단계에서 1:1 매핑 상수로).

마이그레이션 `V2__places_master.sql`: 신규 컬럼 추가 + `external_id` 및 제약 제거 + 인덱스 + `CREATE EXTENSION IF NOT EXISTS pg_trgm`. (dev/prod 모두 `places`가 사실상 비어 있어 데이터 보존 이슈 없음. 기존 체크인 dev 데이터는 재적재 전제.)

## 3. 컴포넌트 설계

### 3.1 좌표 변환 — `CoordinateConverter` (격리·TDD 핵심)
- 책임: EPSG:5174(TM 중부원점, Bessel) → WGS84(EPSG:4326) 단일 변환.
- 구현: `org.locationtech.proj4j` 사용. 5174는 기본 레지스트리에 없을 수 있어 **proj 문자열로 직접 CRS 등록**:
  `+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43`
  (towgs84 7파라미터 누락 시 ~400m 오차 — 반드시 포함)
- 인터페이스: `LatLng convert(double x, double y)`; 변환 실패/범위밖이면 빈 결과(Optional) → 호출부가 skip.
- 테스트: 알려진 기준점(예: 서울시청 좌표쌍)으로 변환 결과가 WGS84 실좌표와 수십 m 이내 일치하는지 검증.

### 3.2 적재 파이프라인 — `PlaceIngestionService`
- 입력: 로컬에 받아둔 표준데이터 **CSV 파일 경로**(전국 전체분). xlsx 금지(100만행 한계). 인코딩 **CP949** 기본(감지/설정 가능).
- 흐름(스트리밍, 213만 건 메모리 안전):
  1. CSV 스트리밍 파싱(OpenCSV 등; 따옴표·콤마 필드 안전)
  2. 행 → 필드 매핑(상수 매핑 테이블)
  3. 필터: `영업상태명`이 영업(정상)인 행만; 좌표 결측/0 행 skip(카운트 로깅)
  4. 좌표 변환(`CoordinateConverter`)
  5. 배치 upsert: `INSERT ... ON CONFLICT(source, source_id) DO UPDATE`(name/category/주소/phone/좌표/business_status 갱신) — **멱등**(두 번 돌려도 중복 없음)
- 적재 방식: JdbcTemplate 배치(`rewriteBatchedStatements`) 우선; 성능 필요 시 Postgres COPY로 최적화(노트). 인덱스는 적재 후 생성/활성.
- 트리거: `PlaceIngestionRunner`(`ApplicationRunner`) — 프로퍼티 `honjeong.place.ingest.file=<csv경로>`가 지정됐을 때만 1회 실행(평상시 미동작). 운영 반복 동기화는 P2.
- 결과 로깅: 총 읽음/적재/갱신/skip(좌표결측·폐업) 건수.

### 3.3 검색 — `PlaceService.search`(재작성)
- 기존 카카오 위임 제거 → `PlaceRepository` 직접 질의.
- 쿼리: `name`에 `pg_trgm` 부분일치(`ILIKE %q%` 또는 `% 연산자`), `business_status=영업`, (선택)중심좌표 있으면 거리순, 페이지네이션(0-base, size 클램프 유지).
- 응답: 기존 `PlaceSearchResponse`에 **`placeId`(우리 id) 추가**(앱이 체크인에 사용). external_id 필드 제거.

### 3.4 주변검색 — `PlaceService.nearby`(신규) + `GET /api/places/nearby`
- 파라미터: `lat`,`lng`(필수), `radius`(m, 기본 1000·클램프), `page`/`size`.
- 흐름:
  1. `PlaceRepository`로 바운딩박스 내 `영업` 식당 조회(기존 map의 바운딩박스 + Haversine 원형보정·거리정렬 패턴 재사용)
  2. 그 식당들의 현재 active 혼밥러 수를 `CheckInRepository.countActiveByPlaceIds(ids)`로 한 번에 조회 → 맵
  3. 오버레이(없으면 0) → 거리순 → 페이지 엔벨로프
- 응답 DTO `PlaceNearbyResponse`: `placeId, name, category, roadAddress, latitude, longitude, distanceMeters, activeCount`.
- 도메인 경계: `PlaceService`가 `CheckInRepository`(레포지토리)에 의존(서비스 순환 없음). count 질의만 교차.

### 3.5 체크인 흐름 변경 (마스터 전환의 필연적 결과)
- 식당이 미리 마스터에 있으므로 업서트 불필요.
- `CheckInRequest`: `{ externalId, name, address, latitude, longitude, category }` → **`{ placeId }`**.
- `CheckInService.createCheckIn`: `placeRepository.findById(placeId)`(없으면 404 `PLACE_NOT_FOUND` 신설) 참조. `PlaceService.findOrCreateByExternalId`·`PlaceUpsertCommand` 제거.
- 영향: 체크인 단위/슬라이스/E2E 테스트 갱신, 앱의 체크인 호출 변경(앱 트랙 노트).

### 3.6 카카오 데이터 경로 제거(정리)
- 삭제: `KakaoPlaceClient`, `MockKakaoPlaceClient`, `PlaceSearchQuery`, `PlaceSearchPage`, `PlaceCandidate`.
- 설정 정리: `honjeong.place.mode`(mock/real) 제거(검색이 더는 외부호출이 아님). `oauth`/`sms` 모드는 유지.
- 카카오 지도 **렌더링 SDK는 앱에 그대로** — 백엔드와 무관.

## 4. 에러 처리
- 적재: 행 단위 오류(파싱·좌표변환 실패)는 해당 행 skip + 집계 로깅(전체 적재 중단 금지). 파일 없음/헤더 불일치는 즉시 실패(명확한 메시지).
- 멱등성: `ON CONFLICT DO UPDATE`로 재실행 안전.
- 검색/주변: `lat`/`lng` 누락 등 입력 오류 → `INVALID_INPUT`(기존 패턴). 빈 결과는 정상(빈 배열).
- 체크인: 존재하지 않는 `placeId` → 404 `PLACE_NOT_FOUND`.

## 5. 테스트 전략 (TDD)
- `CoordinateConverter`: 기준점 변환 정확도(수십 m 이내) — 최우선.
- CSV 파서/매핑: 샘플 행(정상·폐업·좌표결측·CP949·따옴표필드) 처리 검증.
- `PlaceIngestionService`(@SpringBootTest/Testcontainers): 멱등 적재(2회 → 중복 0), 폐업 필터, 좌표결측 skip, 갱신 동작.
- `PlaceService.search`/`nearby`(단위, 레포 모킹) + `PlaceRepository`(@DataJpaTest: 바운딩박스·trigram) + 컨트롤러(@WebMvcTest).
- 혼밥러수 오버레이: count 정확성.
- 체크인 리팩터: 기존 체크인/E2E 테스트를 `placeId` 기반으로 갱신, 404 케이스 추가.

## 6. docs 갱신 계획 (이 설계 반영)
- **04-ERD-데이터모델.md / 04-ERD-간단정리.md**: `places` 정의를 V2로 갱신(`source`/`source_id`/`road_address`/`phone`/`business_status`, `external_id` 제거). "출처=공공데이터 마스터, 카카오는 지도 SDK 렌더링 전용" 명시.
- **05-API명세서.md**: §4 Place — `search`를 "우리 DB 검색"으로 갱신(응답에 `placeId`), `GET /api/places/nearby` 추가. §5 체크인 요청을 `{ placeId }`로 갱신. "카카오 로컬 프록시" 문구 제거. (식당 상세 `GET /api/places/{placeId}`는 후속 슬라이스 노트로만)
- **06-화면별-기능요구사항.md**: §1.2 장소 데이터 출처=공공데이터(카카오=렌더링), `nearby` 상태 갱신, MapHome/RestaurantDetail 출처 갱신. G3·§4.7(메뉴/편의시설 출처)을 "점주 등록(안전, P2) + 공공 마스터 전제 + 크롤(리스크)"로 구체화. §6 의사결정에 "장소 데이터 출처: 공공데이터 마스터로 확정, 카카오 로컬 데이터 약관상 저장불가" 추가.
- **02-요구사항명세서.md**: FR-105/NFR에 "장소 데이터 출처=공공데이터(저장·상업이용 허용), 카카오 로컬은 저장 불가" 한 줄 보강.
- **신규 `07-식당데이터-전략.md`**: 본 결정의 단일 출처(공공데이터 마스터·카카오 ToS·좌표변환·점주등록 로드맵·동기화 P2)를 요약 기록.

## 7. 범위 밖 (명시적 연기)
- 메뉴·편의시설 데이터 적재(점주 등록 / 크롤 / UGC) — P2.
- 매일 변경분 동기화(LOCALDATA 오픈API + 스케줄러) — P2(초기엔 CSV 스냅샷 1회 적재).
- 좌표 결측 행 지오코딩 보강(도로명주소 API/VWorld) — P2.
- 식당 상세 `GET /api/places/{placeId}` — 후속 슬라이스(RestaurantDetail).
- 점주 인증/클레임 플로우 — P2.

## 8. 구현 순서(요약)
1. `places` V2 스키마(V2 마이그레이션 + 엔티티)
2. `CoordinateConverter`(proj4j, TDD)
3. 적재 파이프라인(CSV 스트리밍 → 변환 → 멱등 upsert)
4. 검색 재작성 + 주변검색(neaby) + 혼밥러수 오버레이
5. 체크인 리팩터(`placeId`) + 카카오 데이터 경로 제거
6. docs 갱신
7. 전국 CSV 1회 적재 실행·검증
