# Slice 6 — meal 같이먹기 신청 API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 같은 식당 혼밥러에게 같이먹기를 신청·수락·거절하는 REST 4개 엔드포인트를 TDD로 구현한다.

**Architecture:** `com.honjeong.meal` 5계층(domain/dto/repository/service/controller). 수신자는 `to_check_in_id → check_ins.user_id` 조인 경유로 식별(역정규화 컬럼 없음). `meal_requests` 테이블·유니크 인덱스는 V1에 이미 존재 → 매핑만, 마이그레이션·SecurityConfig 변경 없음. Slice 5(checkin)의 엔티티·서비스·테스트 패턴을 그대로 미러링한다.

**Tech Stack:** Java 21 / Spring Boot 4 / Spring Data JPA / JUnit5 + Mockito + AssertJ / Testcontainers(Postgres).

**Spec:** `docs/superpowers/specs/2026-06-18-meal-request-slice6-design.md`

---

## File Structure

| 파일 | 책임 |
|---|---|
| `meal/domain/MealRequestStatus.java` | enum PENDING/ACCEPTED/DECLINED |
| `meal/domain/MealRequest.java` | 엔티티 + 상태전이 도메인 메서드 |
| `meal/dto/MealRequestCreateRequest.java` | POST 요청(toCheckInId·message) |
| `meal/dto/MealRequestResponse.java` | POST 201 응답 |
| `meal/dto/MealRequestStatusResponse.java` | accept/decline 200 응답 |
| `meal/dto/MealRequestListItemResponse.java` | 목록 항목 응답 |
| `meal/repository/MealRequestRepository.java` | 조회 쿼리(received/sent/receiver) |
| `meal/service/MealRequestService.java` | 비즈니스 규칙·트랜잭션 |
| `meal/controller/MealRequestController.java` | HTTP 매핑 |
| `global/exception/ErrorCode.java` (수정) | meal 에러 코드 6개 추가 |

테스트: `MealRequestTest`(도메인) · `MealRequestRepositoryTest`(@DataJpaTest) · `MealRequestServiceTest`(단위) · `MealRequestControllerTest`(@WebMvcTest).

---

## Task 1: ErrorCode — meal 에러 코드 6개 추가

**Files:**
- Modify: `src/main/java/com/honjeong/global/exception/ErrorCode.java`

- [ ] **Step 1: 체크인 블록 아래에 meal 블록 추가**

`ErrorCode.java`의 `CHECKIN_NOT_FOUND(...)` 줄(약 30번째 줄) 다음, `// 예기치 못한 서버 내부 오류` 주석 앞에 삽입:

```java
    // 같이먹기 — 대상 체크인 상태·opt-in·자기신청/중복·응답완료 충돌
    MEALREQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "같이먹기 신청을 찾을 수 없습니다."),
    TARGET_CHECKIN_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "대상 체크인이 없거나 이미 종료되었습니다."),
    MEALREQUEST_SELF(HttpStatus.CONFLICT, "자기 자신에게는 신청할 수 없습니다."),
    MEALREQUEST_OPT_OUT(HttpStatus.FORBIDDEN, "상대가 같이먹기 신청을 받지 않습니다."),
    MEALREQUEST_DUPLICATE(HttpStatus.CONFLICT, "이미 신청한 대상입니다."),
    MEALREQUEST_ALREADY_RESPONDED(HttpStatus.CONFLICT, "이미 응답한 신청입니다."),
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/honjeong/global/exception/ErrorCode.java
git commit -m "feat(backend): Slice 6 — meal 에러 코드 6개 추가"
```

---

## Task 2: MealRequestStatus enum + MealRequest 엔티티

**Files:**
- Create: `src/main/java/com/honjeong/meal/domain/MealRequestStatus.java`
- Create: `src/main/java/com/honjeong/meal/domain/MealRequest.java`
- Test: `src/test/java/com/honjeong/meal/domain/MealRequestTest.java`

- [ ] **Step 1: 실패 테스트 작성** — `MealRequestTest.java`

```java
package com.honjeong.meal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

/** MealRequest 도메인 단위 테스트 — 상태 전이(create/accept/decline)·isPending. */
class MealRequestTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 18, 12, 0);

    private MealRequest pending() {
        return MealRequest.create(mock(User.class), mock(CheckIn.class), mock(Place.class), "msg", now);
    }

    @Test
    @DisplayName("create: PENDING으로 생성되고 respondedAt은 비어 있다")
    void create() {
        MealRequest mr = pending();
        assertThat(mr.getStatus()).isEqualTo(MealRequestStatus.PENDING);
        assertThat(mr.isPending()).isTrue();
        assertThat(mr.getCreatedAt()).isEqualTo(now);
        assertThat(mr.getRespondedAt()).isNull();
    }

    @Test
    @DisplayName("accept: ACCEPTED로 전이하고 respondedAt 기록")
    void accept() {
        MealRequest mr = pending();
        mr.accept(now.plusMinutes(10));
        assertThat(mr.getStatus()).isEqualTo(MealRequestStatus.ACCEPTED);
        assertThat(mr.isPending()).isFalse();
        assertThat(mr.getRespondedAt()).isEqualTo(now.plusMinutes(10));
    }

    @Test
    @DisplayName("decline: DECLINED로 전이하고 respondedAt 기록")
    void decline() {
        MealRequest mr = pending();
        mr.decline(now.plusMinutes(10));
        assertThat(mr.getStatus()).isEqualTo(MealRequestStatus.DECLINED);
        assertThat(mr.getRespondedAt()).isEqualTo(now.plusMinutes(10));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "*MealRequestTest"`
Expected: FAIL (컴파일 에러 — MealRequest/MealRequestStatus 없음)

- [ ] **Step 3: enum 구현** — `MealRequestStatus.java`

```java
package com.honjeong.meal.domain;

/** 같이먹기 신청 상태. PENDING(대기) → ACCEPTED(수락)/DECLINED(거절). */
public enum MealRequestStatus {
    PENDING, ACCEPTED, DECLINED
}
```

- [ ] **Step 4: 엔티티 구현** — `MealRequest.java`

```java
package com.honjeong.meal.domain;

import java.time.LocalDateTime;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 같이먹기 신청. 신청자(fromUser)가 대상 혼밥러의 체크인(toCheckIn)에 보낸다. 수신자는 {@code toCheckIn.user}로 식별한다.
 * {@code created_at}·{@code responded_at}만 있고 {@code updated_at}이 없어 {@code BaseTimeEntity}를 상속하지 않는다(CheckIn 패턴).
 * 단일 신청 중복은 DB 유니크(from_user_id, to_check_in_id)가 강제한다.
 */
@Entity
@Table(name = "meal_requests")
public class MealRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청자. LAZY — 목록에서 fetch join으로 닉네임만 가져온다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    // 대상 혼밥러의 체크인. 수신자(소유자)·장소의 원천. LAZY.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_check_in_id", nullable = false)
    private CheckIn toCheckIn;

    // 신청 발생 장소(대상 체크인의 place에서 파생·역정규화). LAZY — 응답엔 id만 필요(프록시 getId는 로딩 없음).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    // 인사 한마디(선택, 최대 200자).
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealRequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수락/거절 시각. PENDING 동안 null.
    private LocalDateTime respondedAt;

    /** JPA용 기본 생성자. 외부 직접 호출은 막으려고 protected. */
    protected MealRequest() {
    }

    private MealRequest(User fromUser, CheckIn toCheckIn, Place place, String message, LocalDateTime now) {
        this.fromUser = fromUser;
        this.toCheckIn = toCheckIn;
        this.place = place;
        this.message = message;
        this.status = MealRequestStatus.PENDING;
        this.createdAt = now;
    }

    /**
     * 새 PENDING 신청을 만든다.
     *
     * @param fromUser  신청자(영속 또는 프록시 참조)
     * @param toCheckIn 대상 혼밥러의 체크인
     * @param place     대상 체크인의 장소(역정규화 저장)
     * @param message   인사 한마디(nullable)
     * @param now       생성 시각
     * @return PENDING 신청
     */
    public static MealRequest create(User fromUser, CheckIn toCheckIn, Place place, String message, LocalDateTime now) {
        return new MealRequest(fromUser, toCheckIn, place, message, now);
    }

    /** 신청을 수락 처리한다(ACCEPTED + respondedAt). PENDING 가드는 서비스가 한다. */
    public void accept(LocalDateTime now) {
        this.status = MealRequestStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /** 신청을 거절 처리한다(DECLINED + respondedAt). */
    public void decline(LocalDateTime now) {
        this.status = MealRequestStatus.DECLINED;
        this.respondedAt = now;
    }

    /** 아직 응답 전(PENDING)인지. */
    public boolean isPending() {
        return status == MealRequestStatus.PENDING;
    }

    /** 이 신청의 수신자(대상 체크인 주인)가 주어진 사용자인지. */
    public boolean isReceivedBy(Long userId) {
        return toCheckIn.getUser().getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public User getFromUser() {
        return fromUser;
    }

    public CheckIn getToCheckIn() {
        return toCheckIn;
    }

    public Place getPlace() {
        return place;
    }

    public String getMessage() {
        return message;
    }

    public MealRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew test --tests "*MealRequestTest"`
Expected: PASS (3 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/honjeong/meal/domain/ src/test/java/com/honjeong/meal/domain/
git commit -m "feat(backend): Slice 6 — MealRequest 엔티티·상태전이 + 도메인 테스트"
```

---

## Task 3: MealRequestRepository

**Files:**
- Create: `src/main/java/com/honjeong/meal/repository/MealRequestRepository.java`
- Test: `src/test/java/com/honjeong/meal/repository/MealRequestRepositoryTest.java`

- [ ] **Step 1: 실패 테스트 작성** — `MealRequestRepositoryTest.java`

```java
package com.honjeong.meal.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/**
 * MealRequestRepository 슬라이스 테스트. 실제 Postgres(Testcontainers)에서 매핑·유니크 제약·조회 쿼리를 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class MealRequestRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private MealRequestRepository mealRequestRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 18, 12, 0);

    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    private Place persistPlace(String externalId) {
        return em.persist(Place.of(externalId, externalId + "식당", "서울", 37.5, 127.0, "한식"));
    }

    private CheckIn persistCheckIn(User user, Place place) {
        return em.persist(CheckIn.start(user, place, NOW));
    }

    @Test
    @DisplayName("중복 신청: 같은 (from_user, to_check_in)이면 유니크 위반")
    void duplicateConstraint() {
        User from = persistUser("01000000001", "신청자");
        User to = persistUser("01000000002", "수신자");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        em.persist(MealRequest.create(from, target, place, "1차", NOW));
        em.flush();

        MealRequest dup = MealRequest.create(from, target, place, "2차", NOW);
        assertThatThrownBy(() -> mealRequestRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findReceived: 내가 수신자인 신청만, fromUser fetch, placeId 노출")
    void findReceived() {
        User from = persistUser("01000000001", "신청자");
        User me = persistUser("01000000002", "나");
        Place place = persistPlace("ext-1");
        CheckIn myCheckIn = persistCheckIn(me, place);
        em.persist(MealRequest.create(from, myCheckIn, place, "받은신청", NOW));
        em.flush();
        em.clear();

        List<MealRequest> received = mealRequestRepository.findReceived(me.getId(), null);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).getFromUser().getNickname()).isEqualTo("신청자");
        assertThat(received.get(0).getPlace().getId()).isEqualTo(place.getId());
    }

    @Test
    @DisplayName("findReceived: status 필터")
    void findReceivedWithStatus() {
        User from = persistUser("01000000001", "신청자");
        User me = persistUser("01000000002", "나");
        Place place = persistPlace("ext-1");
        CheckIn myCheckIn = persistCheckIn(me, place);
        em.persist(MealRequest.create(from, myCheckIn, place, "PENDING건", NOW));
        em.flush();
        em.clear();

        assertThat(mealRequestRepository.findReceived(me.getId(), MealRequestStatus.PENDING)).hasSize(1);
        assertThat(mealRequestRepository.findReceived(me.getId(), MealRequestStatus.ACCEPTED)).isEmpty();
    }

    @Test
    @DisplayName("findSent: 내가 신청자인 신청만")
    void findSent() {
        User me = persistUser("01000000001", "나");
        User to = persistUser("01000000002", "수신자");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        em.persist(MealRequest.create(me, target, place, "보낸신청", NOW));
        em.flush();
        em.clear();

        List<MealRequest> sent = mealRequestRepository.findSent(me.getId(), null);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getFromUser().getNickname()).isEqualTo("나");
    }

    @Test
    @DisplayName("findWithReceiverById: toCheckIn.user fetch로 수신자 식별 가능")
    void findWithReceiverById() {
        User from = persistUser("01000000001", "신청자");
        User to = persistUser("01000000002", "수신자");
        Place place = persistPlace("ext-1");
        CheckIn target = persistCheckIn(to, place);
        MealRequest saved = em.persist(MealRequest.create(from, target, place, "msg", NOW));
        em.flush();
        em.clear();

        MealRequest found = mealRequestRepository.findWithReceiverById(saved.getId()).orElseThrow();
        assertThat(found.isReceivedBy(to.getId())).isTrue();
        assertThat(found.isReceivedBy(from.getId())).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "*MealRequestRepositoryTest"`
Expected: FAIL (컴파일 에러 — MealRequestRepository 없음)

- [ ] **Step 3: 레포지토리 구현** — `MealRequestRepository.java`

```java
package com.honjeong.meal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;

/**
 * 같이먹기 신청 저장소. 중복 방지는 DB 유니크(uq_meal_request_from_target)가 강제하고, 여기서는 조회 쿼리를 제공한다.
 * 목록은 fromUser를 fetch join해 닉네임 N+1을 막고, place는 프록시 getId만 쓰므로 fetch하지 않는다.
 */
public interface MealRequestRepository extends JpaRepository<MealRequest, Long> {

    /**
     * 응답(수락/거절) 권한 검사용 — 수신자(toCheckIn.user)까지 fetch해 단건 조회한다.
     *
     * @param id 신청 id
     * @return 신청(수신자 로딩됨) 또는 빈 Optional
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN FETCH mr.toCheckIn ci
            JOIN FETCH ci.user
            WHERE mr.id = :id
            """)
    Optional<MealRequest> findWithReceiverById(@Param("id") Long id);

    /**
     * 내가 수신자인(toCheckIn.user = me) 신청 목록. status가 null이면 전체. fromUser fetch, createdAt 내림차순.
     *
     * @param userId 수신자(나) id
     * @param status 상태 필터(null이면 전체)
     * @return 받은 신청 목록
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN mr.toCheckIn ci
            JOIN FETCH mr.fromUser
            WHERE ci.user.id = :userId
              AND (:status IS NULL OR mr.status = :status)
            ORDER BY mr.createdAt DESC
            """)
    List<MealRequest> findReceived(@Param("userId") Long userId, @Param("status") MealRequestStatus status);

    /**
     * 내가 신청자인(fromUser = me) 신청 목록. status가 null이면 전체. fromUser fetch, createdAt 내림차순.
     *
     * @param userId 신청자(나) id
     * @param status 상태 필터(null이면 전체)
     * @return 보낸 신청 목록
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN FETCH mr.fromUser
            WHERE mr.fromUser.id = :userId
              AND (:status IS NULL OR mr.status = :status)
            ORDER BY mr.createdAt DESC
            """)
    List<MealRequest> findSent(@Param("userId") Long userId, @Param("status") MealRequestStatus status);
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "*MealRequestRepositoryTest"`
Expected: PASS (5 tests) — Testcontainers Postgres 기동(OrbStack 실행 필수)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/honjeong/meal/repository/ src/test/java/com/honjeong/meal/repository/
git commit -m "feat(backend): Slice 6 — MealRequestRepository 조회 쿼리 + 슬라이스 테스트"
```

---

## Task 4: DTO 4개

**Files:**
- Create: `src/main/java/com/honjeong/meal/dto/MealRequestCreateRequest.java`
- Create: `src/main/java/com/honjeong/meal/dto/MealRequestResponse.java`
- Create: `src/main/java/com/honjeong/meal/dto/MealRequestStatusResponse.java`
- Create: `src/main/java/com/honjeong/meal/dto/MealRequestListItemResponse.java`

> DTO는 단순 record + `from()` 매핑이라 별도 테스트 없이 작성한다(Task 5·6의 서비스·웹 테스트가 매핑을 검증). 컴파일로만 확인.

- [ ] **Step 1: 요청 DTO** — `MealRequestCreateRequest.java`

```java
package com.honjeong.meal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 같이먹기 신청 요청 본문. {@code POST /api/meal-requests}.
 *
 * @param toCheckInId 대상 혼밥러의 체크인 id(필수). place는 서버가 이 체크인에서 파생한다.
 * @param message     인사 한마디(선택, 최대 200자).
 */
public record MealRequestCreateRequest(
        @NotNull Long toCheckInId,
        @Size(max = 200) String message) {
}
```

- [ ] **Step 2: POST 응답 DTO** — `MealRequestResponse.java`

```java
package com.honjeong.meal.dto;

import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 생성 응답(POST 201).
 *
 * @param mealRequestId 신청 id
 * @param toCheckInId   대상 체크인 id
 * @param message       인사 한마디(nullable)
 * @param status        상태 문자열(PENDING)
 */
public record MealRequestResponse(Long mealRequestId, Long toCheckInId, String message, String status) {

    public static MealRequestResponse from(MealRequest mr) {
        return new MealRequestResponse(mr.getId(), mr.getToCheckIn().getId(), mr.getMessage(), mr.getStatus().name());
    }
}
```

- [ ] **Step 3: 응답(수락/거절) DTO** — `MealRequestStatusResponse.java`

```java
package com.honjeong.meal.dto;

import java.time.LocalDateTime;

import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 수락/거절 응답(200).
 *
 * @param mealRequestId 신청 id
 * @param status        전이된 상태(ACCEPTED|DECLINED)
 * @param respondedAt   응답 시각
 */
public record MealRequestStatusResponse(Long mealRequestId, String status, LocalDateTime respondedAt) {

    public static MealRequestStatusResponse from(MealRequest mr) {
        return new MealRequestStatusResponse(mr.getId(), mr.getStatus().name(), mr.getRespondedAt());
    }
}
```

- [ ] **Step 4: 목록 항목 DTO** — `MealRequestListItemResponse.java`

```java
package com.honjeong.meal.dto;

import java.time.LocalDateTime;

import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 목록 항목(GET). 프라이버시상 신청자는 닉네임만 노출한다.
 *
 * @param mealRequestId 신청 id
 * @param fromUser      신청자(닉네임만)
 * @param placeId       신청 발생 장소 id
 * @param message       인사 한마디(nullable)
 * @param status        상태 문자열
 * @param createdAt     신청 시각
 */
public record MealRequestListItemResponse(
        Long mealRequestId,
        FromUser fromUser,
        Long placeId,
        String message,
        String status,
        LocalDateTime createdAt) {

    /** 신청자 요약(닉네임만). */
    public record FromUser(String nickname) {
    }

    public static MealRequestListItemResponse from(MealRequest mr) {
        return new MealRequestListItemResponse(
                mr.getId(),
                new FromUser(mr.getFromUser().getNickname()),
                mr.getPlace().getId(),
                mr.getMessage(),
                mr.getStatus().name(),
                mr.getCreatedAt());
    }
}
```

- [ ] **Step 5: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/honjeong/meal/dto/
git commit -m "feat(backend): Slice 6 — meal DTO 4개(요청·응답·목록)"
```

---

## Task 5: MealRequestService (핵심 — 비즈니스 규칙)

**Files:**
- Create: `src/main/java/com/honjeong/meal/service/MealRequestService.java`
- Test: `src/test/java/com/honjeong/meal/service/MealRequestServiceTest.java`

- [ ] **Step 1: 실패 테스트 작성** — `MealRequestServiceTest.java`

```java
package com.honjeong.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * MealRequestService 단위 테스트(순수 Mockito + 고정 Clock). 신청 생성 분기(404/409/403/중복)·응답·목록 라우팅을 검증한다.
 * 엔티티 id·상태가 필요한 비교는 mock 엔티티 스텁으로 해결한다(DB 없이).
 */
class MealRequestServiceTest {

    private final MealRequestRepository mealRequestRepository = mock(MealRequestRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    // KST 12:00 = UTC 03:00 으로 고정. now() = 2026-06-18T12:00.
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-18T03:00:00Z"), ZoneOffset.UTC);
    private final MealRequestService service =
            new MealRequestService(mealRequestRepository, checkInRepository, userRepository, clock);

    private final LocalDateTime nowKst = LocalDateTime.of(2026, 6, 18, 12, 0);

    // ACTIVE 대상 체크인 mock: id·주인(id·opt-in)·place를 스텁.
    private CheckIn targetCheckIn(long checkInId, long ownerId, boolean allowMealRequest, long placeId) {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        when(owner.isAllowMealRequest()).thenReturn(allowMealRequest);
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(placeId);
        CheckIn ci = mock(CheckIn.class);
        when(ci.getId()).thenReturn(checkInId);
        when(ci.getStatus()).thenReturn(CheckInStatus.ACTIVE);
        when(ci.getUser()).thenReturn(owner);
        when(ci.getPlace()).thenReturn(place);
        return ci;
    }

    private MealRequestCreateRequest request(long toCheckInId) {
        return new MealRequestCreateRequest(toCheckInId, "같이 드실래요?");
    }

    // 수신자 id가 receiverId인 PENDING 신청 mock 엔티티.
    private MealRequest pendingRequest(long receiverId) {
        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(receiverId);
        CheckIn ci = mock(CheckIn.class);
        when(ci.getUser()).thenReturn(receiver);
        return MealRequest.create(mock(User.class), ci, mock(Place.class), "msg", nowKst);
    }

    @Test
    @DisplayName("create: 정상이면 PENDING 신청을 저장하고 응답 반환")
    void create_success() {
        CheckIn target = targetCheckIn(10L, 2L, true, 3L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(mealRequestRepository.save(any(MealRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        MealRequestResponse res = service.create(1L, request(10L));

        assertThat(res.toCheckInId()).isEqualTo(10L);
        assertThat(res.message()).isEqualTo("같이 드실래요?");
        assertThat(res.status()).isEqualTo("PENDING");
        verify(mealRequestRepository).save(any(MealRequest.class));
    }

    @Test
    @DisplayName("create: 대상 체크인 없으면 TARGET_CHECKIN_NOT_AVAILABLE(404)")
    void create_targetNotFound() {
        when(checkInRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));
        verify(mealRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: 대상 체크인이 ENDED면 TARGET_CHECKIN_NOT_AVAILABLE(404)")
    void create_targetEnded() {
        CheckIn ended = mock(CheckIn.class);
        when(ended.getStatus()).thenReturn(CheckInStatus.ENDED);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(ended));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("create: 대상 주인이 나 자신이면 MEALREQUEST_SELF(409)")
    void create_self() {
        CheckIn target = targetCheckIn(10L, 1L, true, 3L); // ownerId == userId(1)
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_SELF));
        verify(mealRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: 대상이 수신 거부면 MEALREQUEST_OPT_OUT(403)")
    void create_optOut() {
        CheckIn target = targetCheckIn(10L, 2L, false, 3L); // allowMealRequest=false
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_OPT_OUT));
        verify(mealRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: 중복 신청(유니크 위반)이면 MEALREQUEST_DUPLICATE(409)")
    void create_duplicate() {
        CheckIn target = targetCheckIn(10L, 2L, true, 3L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(mealRequestRepository.save(any())).thenThrow(new DataIntegrityViolationException("uq violation"));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_DUPLICATE));
    }

    @Test
    @DisplayName("accept: 수신자가 PENDING을 수락하면 ACCEPTED·respondedAt 기록")
    void accept_success() {
        MealRequest mr = pendingRequest(2L);
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));

        MealRequestStatusResponse res = service.accept(2L, 7L);

        assertThat(res.status()).isEqualTo("ACCEPTED");
        assertThat(res.respondedAt()).isEqualTo(nowKst);
    }

    @Test
    @DisplayName("decline: 수신자가 PENDING을 거절하면 DECLINED")
    void decline_success() {
        MealRequest mr = pendingRequest(2L);
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));

        MealRequestStatusResponse res = service.decline(2L, 7L);

        assertThat(res.status()).isEqualTo("DECLINED");
        assertThat(res.respondedAt()).isEqualTo(nowKst);
    }

    @Test
    @DisplayName("accept: 신청 없으면 MEALREQUEST_NOT_FOUND(404)")
    void accept_notFound() {
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.accept(2L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_NOT_FOUND));
    }

    @Test
    @DisplayName("accept: 수신자가 아니면 FORBIDDEN(403)")
    void accept_notReceiver() {
        MealRequest mr = pendingRequest(2L); // 수신자=2
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));
        assertThatThrownBy(() -> service.accept(99L, 7L)) // 99가 수락 시도
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("accept: 이미 응답한 신청이면 MEALREQUEST_ALREADY_RESPONDED(409)")
    void accept_alreadyResponded() {
        MealRequest mr = pendingRequest(2L);
        mr.accept(nowKst.minusMinutes(5)); // 이미 ACCEPTED
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));
        assertThatThrownBy(() -> service.accept(2L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_ALREADY_RESPONDED));
    }

    @Test
    @DisplayName("getMealRequests: role 기본 received, sent면 findSent 라우팅")
    void list_roleRouting() {
        when(mealRequestRepository.findReceived(1L, null)).thenReturn(List.of());
        when(mealRequestRepository.findSent(1L, null)).thenReturn(List.of());

        service.getMealRequests(1L, "received", null);
        verify(mealRequestRepository).findReceived(1L, null);

        service.getMealRequests(1L, "sent", null);
        verify(mealRequestRepository).findSent(1L, null);
    }

    @Test
    @DisplayName("getMealRequests: status를 enum으로 변환해 전달")
    void list_statusFilter() {
        when(mealRequestRepository.findReceived(1L, MealRequestStatus.PENDING)).thenReturn(List.of());
        service.getMealRequests(1L, "received", "PENDING");
        verify(mealRequestRepository).findReceived(1L, MealRequestStatus.PENDING);
    }

    @Test
    @DisplayName("getMealRequests: 잘못된 role이면 INVALID_INPUT(400)")
    void list_badRole() {
        assertThatThrownBy(() -> service.getMealRequests(1L, "garbage", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("getMealRequests: 잘못된 status면 INVALID_INPUT(400)")
    void list_badStatus() {
        assertThatThrownBy(() -> service.getMealRequests(1L, "received", "NOPE"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "*MealRequestServiceTest"`
Expected: FAIL (컴파일 에러 — MealRequestService 없음)

- [ ] **Step 3: 서비스 구현** — `MealRequestService.java`

```java
package com.honjeong.meal.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestListItemResponse;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 같이먹기 신청 도메인 서비스. 신청 생성(대상 검증·opt-in·자기/중복 차단)·수락·거절·목록을 담당한다.
 * 모든 시각은 주입된 {@link Clock}을 Asia/Seoul로 환산해 KST로 통일한다(CheckInService와 동일).
 */
@Service
public class MealRequestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MealRequestRepository mealRequestRepository;
    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public MealRequestService(MealRequestRepository mealRequestRepository, CheckInRepository checkInRepository,
            UserRepository userRepository, Clock clock) {
        this.mealRequestRepository = mealRequestRepository;
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * 같이먹기 신청을 생성한다. 대상 체크인이 ACTIVE가 아니면 404, 자기 자신이면 409, 수신 거부면 403,
     * 중복(유니크 위반)이면 409로 처리한다. place_id는 대상 체크인의 장소에서 파생한다.
     *
     * @param userId  신청자 id
     * @param request 대상 체크인 id·인사말
     * @return 생성된 신청 응답
     */
    @Transactional
    public MealRequestResponse create(Long userId, MealRequestCreateRequest request) {
        CheckIn target = checkInRepository.findById(request.toCheckInId())
                .filter(c -> c.getStatus() == CheckInStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));

        User receiver = target.getUser();
        if (receiver.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.MEALREQUEST_SELF);
        }
        if (!receiver.isAllowMealRequest()) {
            throw new BusinessException(ErrorCode.MEALREQUEST_OPT_OUT);
        }

        try {
            User fromRef = userRepository.getReferenceById(userId);
            MealRequest saved = mealRequestRepository.save(
                    MealRequest.create(fromRef, target, target.getPlace(), request.message(), now()));
            return MealRequestResponse.from(saved);
        } catch (DataIntegrityViolationException e) {              // 중복 신청(유니크 위반)
            throw new BusinessException(ErrorCode.MEALREQUEST_DUPLICATE);
        }
    }

    /**
     * 신청을 수락한다. 없으면 404, 수신자가 아니면 403, 이미 응답했으면 409.
     *
     * @param userId 요청 회원 id(수신자여야 함)
     * @param id     신청 id
     * @return 수락 결과
     */
    @Transactional
    public MealRequestStatusResponse accept(Long userId, Long id) {
        MealRequest mr = loadPendingForReceiver(userId, id);
        mr.accept(now());
        return MealRequestStatusResponse.from(mr);
    }

    /**
     * 신청을 거절한다. 가드는 {@link #accept}와 동일하다.
     *
     * @param userId 요청 회원 id(수신자여야 함)
     * @param id     신청 id
     * @return 거절 결과
     */
    @Transactional
    public MealRequestStatusResponse decline(Long userId, Long id) {
        MealRequest mr = loadPendingForReceiver(userId, id);
        mr.decline(now());
        return MealRequestStatusResponse.from(mr);
    }

    /** 응답 가능한 신청을 로드한다 — 없음 404 / 비수신자 403 / 이미 응답 409. */
    private MealRequest loadPendingForReceiver(Long userId, Long id) {
        MealRequest mr = mealRequestRepository.findWithReceiverById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEALREQUEST_NOT_FOUND));
        if (!mr.isReceivedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!mr.isPending()) {
            throw new BusinessException(ErrorCode.MEALREQUEST_ALREADY_RESPONDED);
        }
        return mr;
    }

    /**
     * 받은/보낸 신청 목록을 조회한다. role=received(기본)|sent, status는 선택 필터.
     *
     * @param userId 회원 id
     * @param role   "received"(기본)|"sent" — 그 외는 400
     * @param status 상태 필터 문자열(선택) — 잘못되면 400
     * @return 신청 목록(createdAt 내림차순)
     */
    @Transactional(readOnly = true)
    public List<MealRequestListItemResponse> getMealRequests(Long userId, String role, String status) {
        boolean sent = parseRole(role);
        MealRequestStatus statusFilter = parseStatus(status);
        List<MealRequest> result = sent
                ? mealRequestRepository.findSent(userId, statusFilter)
                : mealRequestRepository.findReceived(userId, statusFilter);
        return result.stream().map(MealRequestListItemResponse::from).toList();
    }

    /** role 문자열을 sent 여부로 변환한다. null/빈/"received" → false, "sent" → true, 그 외 → 400. */
    private boolean parseRole(String role) {
        if (role == null || role.isBlank() || "received".equals(role)) {
            return false;
        }
        if ("sent".equals(role)) {
            return true;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 role입니다.");
    }

    /** status 문자열을 enum으로 변환한다. null/빈 → null(전체), 잘못된 값 → 400. */
    private MealRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MealRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 status입니다.");
        }
    }

    /** 현재 시각을 KST LocalDateTime으로 반환한다. */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "*MealRequestServiceTest"`
Expected: PASS (15 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/honjeong/meal/service/ src/test/java/com/honjeong/meal/service/
git commit -m "feat(backend): Slice 6 — MealRequestService 비즈니스 규칙 + 단위 테스트"
```

---

## Task 6: MealRequestController

**Files:**
- Create: `src/main/java/com/honjeong/meal/controller/MealRequestController.java`
- Test: `src/test/java/com/honjeong/meal/controller/MealRequestControllerTest.java`

- [ ] **Step 1: 실패 테스트 작성** — `MealRequestControllerTest.java`

```java
package com.honjeong.meal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

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
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.meal.dto.MealRequestListItemResponse;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.service.MealRequestService;

/**
 * {@link MealRequestController} 웹 슬라이스 테스트. HTTP 매핑·상태코드·인가·{@code @Valid}를 검증하고 로직은 서비스 모킹.
 */
@WebMvcTest(controllers = MealRequestController.class)
@Import({SecurityConfig.class, WebConfig.class})
class MealRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private MealRequestService mealRequestService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("POST /api/meal-requests: 201 + 신청 응답")
    void create_201() throws Exception {
        when(mealRequestService.create(eq(1L), any()))
                .thenReturn(new MealRequestResponse(7L, 10L, "같이 드실래요?", "PENDING"));

        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10,\"message\":\"같이 드실래요?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mealRequestId").value(7))
                .andExpect(jsonPath("$.data.toCheckInId").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST: toCheckInId 누락이면 400")
    void create_invalid() throws Exception {
        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST: 토큰 없으면 401")
    void create_401() throws Exception {
        mockMvc.perform(post("/api/meal-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST: 수신 거부면 403 MEALREQUEST_OPT_OUT")
    void create_403() throws Exception {
        when(mealRequestService.create(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.MEALREQUEST_OPT_OUT));
        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEALREQUEST_OPT_OUT"));
    }

    @Test
    @DisplayName("POST: 중복이면 409 MEALREQUEST_DUPLICATE")
    void create_409() throws Exception {
        when(mealRequestService.create(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.MEALREQUEST_DUPLICATE));
        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEALREQUEST_DUPLICATE"));
    }

    @Test
    @DisplayName("GET /api/meal-requests: received 기본, 200 + 목록")
    void list_200() throws Exception {
        when(mealRequestService.getMealRequests(eq(1L), eq("received"), any()))
                .thenReturn(List.of(new MealRequestListItemResponse(
                        7L, new MealRequestListItemResponse.FromUser("옆자리"), 3L, "같이 드실래요?", "PENDING",
                        LocalDateTime.of(2026, 6, 18, 12, 5))));

        mockMvc.perform(get("/api/meal-requests").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mealRequestId").value(7))
                .andExpect(jsonPath("$.data[0].fromUser.nickname").value("옆자리"))
                .andExpect(jsonPath("$.data[0].placeId").value(3))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /{id}/accept: 200 + ACCEPTED")
    void accept_200() throws Exception {
        when(mealRequestService.accept(1L, 7L))
                .thenReturn(new MealRequestStatusResponse(7L, "ACCEPTED", LocalDateTime.of(2026, 6, 18, 12, 10)));

        mockMvc.perform(patch("/api/meal-requests/7/accept").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("PATCH /{id}/accept: 수신자 아니면 403")
    void accept_403() throws Exception {
        when(mealRequestService.accept(1L, 7L))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));
        mockMvc.perform(patch("/api/meal-requests/7/accept").header("Authorization", userToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/decline: 200 + DECLINED")
    void decline_200() throws Exception {
        when(mealRequestService.decline(1L, 7L))
                .thenReturn(new MealRequestStatusResponse(7L, "DECLINED", LocalDateTime.of(2026, 6, 18, 12, 10)));

        mockMvc.perform(patch("/api/meal-requests/7/decline").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "*MealRequestControllerTest"`
Expected: FAIL (컴파일 에러 — MealRequestController 없음)

- [ ] **Step 3: 컨트롤러 구현** — `MealRequestController.java`

```java
package com.honjeong.meal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestListItemResponse;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.service.MealRequestService;

import jakarta.validation.Valid;

/**
 * 같이먹기 신청 REST 컨트롤러(/api/meal-requests). 얇게 유지 — {@code @CurrentUserId}·{@code @Valid}·DTO 변환만 하고
 * 검증·매핑은 {@link MealRequestService}에 위임한다.
 *
 * <p><b>인가:</b> 모든 경로가 정식 USER 전용이다. SecurityConfig의 {@code anyRequest().hasRole("USER")} 기본 규칙이
 * 커버하므로 별도 매처가 필요 없다(토큰 없으면 401, 온보딩 토큰이면 403).
 */
@RestController
@RequestMapping("/api/meal-requests")
public class MealRequestController {

    private final MealRequestService mealRequestService;

    public MealRequestController(MealRequestService mealRequestService) {
        this.mealRequestService = mealRequestService;
    }

    /** 같이먹기 신청. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MealRequestResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody MealRequestCreateRequest request) {
        return ApiResponse.success(mealRequestService.create(userId, request));
    }

    /** 받은/보낸 신청 목록. role 기본 received, status 선택 필터. */
    @GetMapping
    public ApiResponse<List<MealRequestListItemResponse>> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "received") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(mealRequestService.getMealRequests(userId, role, status));
    }

    /** 신청 수락. */
    @PatchMapping("/{id}/accept")
    public ApiResponse<MealRequestStatusResponse> accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.accept(userId, id));
    }

    /** 신청 거절. */
    @PatchMapping("/{id}/decline")
    public ApiResponse<MealRequestStatusResponse> decline(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.decline(userId, id));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "*MealRequestControllerTest"`
Expected: PASS (9 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/honjeong/meal/controller/ src/test/java/com/honjeong/meal/controller/
git commit -m "feat(backend): Slice 6 — MealRequestController + 웹 슬라이스 테스트"
```

---

## Task 7: 전체 테스트 그린 + 라이브 e2e 검증

**Files:** (없음 — 검증만)

- [ ] **Step 1: 전체 테스트 실행**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — 기존 103개 + meal 32개(도메인3·repo5·서비스15·웹9) 모두 그린

- [ ] **Step 2: 앱 기동 후 라이브 e2e**

```bash
docker compose up -d db
./gradlew bootRun
```
스펙의 테스트 가이드(STEP 1~4) curl 흐름을 실행해 정상/에러 경로를 확인한다:
- 사용자 A(수신자) 가입 + 체크인 → checkInId 확보
- 사용자 B(신청자) 가입 → TOKEN_B 확보
- `POST /api/meal-requests`(B→A) 201 → `GET ?role=received`(A) → `PATCH /{id}/accept`(A) 200
- 에러: 미인증 401 · toCheckInId 누락 400 · opt-out 403 · 자기신청 409 · 중복 409 · 종료대상 404 · 비수신자 403 · 이미응답 409

- [ ] **Step 3: 요구사항 xlsx FR-108 완료 체크 (선택)**

`docs/요구사항.xlsx`의 FR-108(같이먹기) 완료 체크를 갱신한다. ⚠️ 폼컨트롤 체크박스라 openpyxl 금지 — raw XML 직접 수정(메모리 `requirements-xlsx-checkbox-tracking` 참조).

- [ ] **Step 4: finishing 단계**

`superpowers:finishing-a-development-branch` 스킬로 main 병합/PR 여부를 결정한다.

---

## Self-Review (작성자 점검)

**1. 스펙 커버리지:**
- POST(신청)·GET(목록 role/status)·PATCH accept·PATCH decline → Task 5·6 ✅
- 에러 6종(404 없음/종료·409 자기·403 opt-out·409 중복·404 신청없음·409 이미응답)·403 비수신자 → Task 5 테스트로 전부 커버 ✅
- 수신자=조인 경유, place 역정규화, 평면 배열, 신청 전제 없음 → 엔티티·레포·서비스에 반영 ✅
- 마이그레이션·SecurityConfig 변경 없음 → 계획에 신규 SQL/보안 작업 없음 ✅

**2. Placeholder 스캔:** TBD/TODO 없음. 모든 코드 스텝에 실제 코드 포함 ✅

**3. 타입 일관성:**
- `MealRequest.create(User, CheckIn, Place, String, LocalDateTime)` — Task 2 정의 ↔ Task 5 호출 일치 ✅
- `findReceived/findSent(Long, MealRequestStatus)`·`findWithReceiverById(Long)` — Task 3 정의 ↔ Task 5 호출 일치 ✅
- DTO `from()` 시그니처 — Task 4 정의 ↔ Task 5·6 사용 일치 ✅
- ErrorCode 6개 — Task 1 정의 ↔ Task 5·6 참조 일치 ✅
- 컨트롤러 `getMealRequests(userId, role, status)` 파라미터 순서 — Task 5 ↔ Task 6 일치 ✅
