## 프로젝트 컨텍스트

**혼정(혼밥을 정상화하다)** — 혼자 식사하는 심리적 부담을 줄이기 위해, 사용자가 식당에서 "혼밥 중"을 체크인하고 지도에서 실시간/누적 혼밥러 수(사회적 증거)를 확인하며, 가벼운 반응과 같이먹기로 느슨하게 연결되는 위치 기반 소셜 서비스입니다. 핵심 가치는 "혼자지만 혼자가 아닌 경험"입니다.

### 핵심 기능 (MVP / 1단계)

- **혼밥 체크인** — 사용자가 식당(place)을 *명시적으로 선택*해 "혼밥 중" 상태를 등록합니다(GPS는 후보를 좁히는 보조 수단일 뿐, 자동 판별 아님). 모든 통계의 원천 데이터입니다.
- **사회적 증거 통계** — "오늘 혼밥 N명", "현재 혼밥 중 N명"을 실시간 집계해 심리적 부담을 완화합니다.
- **지도 기반 혼밥러 표시** — 반경 내 식당별 현재 혼밥러 수를 마커로 보여줍니다.
- **식당 검색·캐싱** — 카카오 로컬 API로 검색하고, 체크인/리뷰 시 `external_id`를 키로 우리 DB(`places`)에 upsert합니다.
<!-- - **초간단 반응** — "나도 여기 있음"(`HERE`) / "같이 먹는 중"(`EATING_TOGETHER`)으로 대화 없는 가벼운 연결을 제공합니다. -->
- **혼밥러 목록** — 같은 식당의 현재 체크인 사용자 목록을 보여줍니다(프라이버시: 닉네임/레벨/경과시간만 노출).
- **같이먹기 신청** — 같은 식당 혼밥러에게 신청하고 수락/거절합니다(수신 opt-in 필수, 차단/신고 안전장치 제공).

> 2~4단계(혼밥 인증/리뷰, 레벨·뱃지, 미션·추천, 매칭)는 로드맵상 후속 기능입니다.

### 팀 / 배경

- 1인 개발 프로젝트입니다(기획·백엔드·앱을 혼자 진행).

## 기술 스택

### Frontend
- React Native + Expo (형제 `app/`)

### Backend
- Java 21 / Spring Boot 4.0.6 (Web MVC)
- Spring Data JPA / Gradle / H2(개발)

### DevOps
- Docker + docker-compose (postgres:17)

### 아직 설치 안 됨 (계획됨)
- Spring Security + JWT, Flyway, QueryDSL, 카카오 API 클라이언트
- PostgreSQL(prod), GitHub Actions, 클라우드 배포

## 코드 컨벤션

Spring Boot:

- **의존성 주입**: 생성자 주입 + `final` 사용 / 필드 주입 지양
- **트랜잭션**: `@Transactional`은 서비스 경계·조회 `readOnly` 지향 / 컨트롤러 부착·트랜잭션 내 외부 호출 지양
- **JPA**: 지연 로딩 기본·필요 시 fetch join 지향 / N+1·양방향 무한 `toString` 금지

### 네이밍

- **클래스 (PascalCase)**: 역할 접미사 `XxxController`/`Service`/`Repository`, DTO는 `XxxRequest`/`XxxResponse`, 엔티티는 단수 명사(`CheckIn`)
- **메서드·필드 (camelCase)**: 메서드는 동사로 시작(`createCheckIn`), 불리언은 `is`/`has`/`allow`(`isActive`)
- **상수·enum 값 (UPPER_SNAKE_CASE)**: `MAX_ACTIVE_CHECK_IN`, `ReactionType.EATING_TOGETHER`
- **DB 테이블·컬럼 (snake_case)**: `check_ins`, `external_id` (엔티티 camelCase ↔ 매핑)

## 아키텍처

### 프로젝트 구조

모노레포(`/Users/yoonhyunwoo/project`):

```
project/
├── app/      # React Native + Expo 클라이언트
├── backend/  # 이 저장소 (Spring Boot API)
└── docs/     # 설계 문서 (source of truth)
```

백엔드 — 도메인 중심 패키지(`com.honjeong.<domain>`), 횡단 관심사는 `global`:

```
com.honjeong
├── global/            # 횡단 관심사: common(BaseTimeEntity·ApiResponse), config, exception, security, web
├── auth/              # 인증 — controller / service / dto
├── user/              # 사용자·프로필
├── place/             # 장소 검색·캐싱 (+ client: KakaoPlaceClient)
├── checkin/           # 혼밥 체크인 (통계·혼밥러 목록 포함)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── domain/        # 엔티티
│   └── dto/
├── reaction/          # 반응
└── meal/              # 같이먹기 신청
```

`user`·`place`·`checkin`·`reaction`·`meal`은 위 `checkin`처럼 `controller / service / repository / domain / dto` 5계층으로 구성한다(`place`는 외부 호출용 `client` 추가, `auth`는 `controller / service / dto`만). 계층 폴더는 생성돼 있으나 비어 있고(`.gitkeep`), 실제 클래스는 `global/web/HealthController`와 메인 클래스뿐이다.

### 아키텍처 패턴

표준 계층형. 흐름은 단방향(상위→하위):

`Controller → Service → Repository → DB`

- **Controller**: HTTP 요청/응답, `@Valid` 검증, DTO 변환. 얇게 유지.
- **Service**: 비즈니스 로직과 `@Transactional` 경계.
- **Repository**: Spring Data JPA로 DB 접근.
- **Domain(Entity)**: DB 매핑·도메인 규칙. 경계 밖으로 직접 노출하지 않음.
- **Client**: 외부 API 호출 격리 — `KakaoPlaceClient`(검색 전용). 결과는 `places`에 upsert.

횡단 관심사(예외·공통 응답·보안)는 `global`에 둔다.

## 테스팅 요구사항

테스트는 JUnit 5 + Spring Boot Test를 사용한다. 단언은 AssertJ(`assertThat`), 모킹은 Mockito, 테스트 DB는 H2 **인메모리**를 사용한다.

### 작성 원칙

- **TDD 권장**: 기능·버그 수정은 실패하는 테스트를 먼저 작성한 뒤 통과시킨다(실패 → 구현 → 통과).
- **핵심 로직 우선**: Service의 비즈니스 규칙(체크인 제약, 통계 집계, 같이먹기 opt-in 등)을 최우선으로 테스트한다. 단순 위임·자명한 코드는 생략 가능.

### 테스트 종류 (계층별 전략)

| 대상 | 어노테이션 | 방식 |
|---|---|---|
| Service | (순수 단위) | Repository를 Mockito로 모킹, 비즈니스 로직 검증 |
| Repository | `@DataJpaTest` | H2 인메모리, 쿼리·매핑 검증(fetch join, N+1 방지 확인) |
| Controller | `@WebMvcTest` | MockMvc로 요청/응답·`@Valid` 검증, Service는 모킹 |
| 통합 | `@SpringBootTest` | 전체 컨텍스트 — 스모크·핵심 시나리오에 한정 |

피라미드: 단위(Service) 다수 > 슬라이스(`@DataJpaTest`/`@WebMvcTest`) > 통합(`@SpringBootTest`) 최소.

### 작성 규칙

- **네이밍**: 테스트 클래스는 `XxxTest`(예: `CheckInServiceTest`), 메서드는 의도를 드러내는 한글 `@DisplayName` 권장.
- **구조**: given-when-then 3단 구성.
- **격리**: 테스트는 인메모리 H2(`jdbc:h2:mem:`)를 쓰고 dev 파일 DB(`./data/honjeong`)와 분리한다. `src/test/resources/application.yml` + `@ActiveProfiles("test")`로 프로파일을 격리한다.
- 외부 API(카카오)는 모킹한다 — 테스트는 네트워크·실DB에 의존하지 않는다.

### 커버리지 목표

- **Service 계층 핵심 로직 70%+** 를 목표로 한다(라인보다 분기·시나리오 기준 지향).
- Controller/Repository는 핵심 경로 위주, DTO·엔티티 게터 등 자명한 코드는 제외.
- 강제 게이트가 아니라 핵심 비즈니스 로직 보호가 우선이다. (JaCoCo 등 측정 도구는 아직 미설정.)

> 테스트 실행 명령은 [주요 명령어](#주요-명령어)를 참고한다.

## 주요 명령어

> **전제**: 로컬 개발·테스트 모두 **PostgreSQL**을 쓴다. 컨테이너 런타임으로 **OrbStack**이 실행 중이어야 한다(메뉴바 아이콘 / `docker ps`로 확인). H2는 더 이상 쓰지 않는다(단일 활성 체크인의 부분 유니크 인덱스가 Postgres 전용이기 때문).
> 모든 명령은 `backend/`에서 실행한다: `cd ~/project/backend`.

### 로컬 DB (개발용·영구)

`docker-compose.yml`의 `db` 서비스 = `honjeong-db`(postgres:17, localhost:5432, db/user/pass 모두 `honjeong`).

```bash
docker compose up -d db      # 로컬 Postgres 기동
docker ps                    # 상태 확인
docker compose stop db       # 끄기(데이터 유지)
docker compose down -v       # 완전 초기화(테이블·데이터 삭제)
```

> 빈 DB를 처음 켠 뒤 앱을 한 번 실행하면 **Flyway가 `V1__core.sql`을 적용**해 테이블이 생성된다.

### 앱 실행

```bash
docker compose up -d db                 # DB 먼저
./gradlew bootRun                       # 앱 실행(local 프로파일·Postgres, :8080, Ctrl+C로 종료)
curl http://localhost:8080/api/health   # {"status":"UP"}
```

### 빌드 / 테스트

> 테스트는 **Testcontainers**로 Postgres를 띄우므로 **OrbStack 실행 필수**. 소켓은 `~/.testcontainers.properties`(`docker.host=unix://~/.orbstack/run/docker.sock`)로 잡혀 있어 추가 설정 없이 동작한다.

```bash
./gradlew test                                   # 전체 테스트
./gradlew test --tests "*CheckInServiceTest"     # 특정 테스트
./gradlew test --info                            # 실패 원인 상세
open build/reports/tests/test/index.html         # HTML 리포트
./gradlew build                                  # 컴파일 + 테스트 + 패키징
./gradlew clean bootJar                          # 실행 JAR 생성 (build/libs/*.jar)
```

### DB 들여다보기

로컬 `honjeong-db`를 본다(테스트용 Testcontainers DB는 휘발성이라 직접 못 봄).

```bash
# (A) 터미널 psql — 설치 불필요(컨테이너 안 psql)
docker exec -it honjeong-db psql -U honjeong -d honjeong
#   \dt                                  테이블 목록
#   \d users                             컬럼 구조
#   \d check_ins                         부분 유니크 인덱스 확인
#   SELECT * FROM flyway_schema_history; 적용된 마이그레이션
#   \q                                   나가기
```

(B) GUI 툴(TablePlus / DBeaver) 연결정보: `Host localhost · Port 5432 · DB honjeong · User honjeong · Password honjeong`.

### Docker 배포(통합)

```bash
docker compose up -d        # app(prod 프로파일) + db 동시 기동
docker compose logs -f app  # 앱 로그 추적
docker compose down         # 중지 (-v 추가 시 DB 볼륨까지 삭제)
```

### 프로파일

- `local` (기본) — **PostgreSQL**(docker compose `db`), `show-sql` 활성, 스키마는 Flyway 적용 후 `ddl-auto: validate`.
- `test` — Testcontainers Postgres(`@ActiveProfiles("test")`, 단위 테스트는 Mockito로 DB 불필요).
- `prod` — PostgreSQL, 환경변수(`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) 주입, `ddl-auto: validate`, 외부연동 `honjeong.*.mode=real`.
- 전환: `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun` (Docker는 자동 지정).
