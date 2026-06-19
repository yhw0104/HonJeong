# 식당 데이터 공공데이터 마스터 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 식당(`places`)을 카카오 캐시에서 공공데이터(전국일반음식점표준데이터) 일괄 적재 마스터로 전환하고, 우리 DB 위에서 검색·주변검색을 제공하며, 체크인을 `placeId` 기반으로 바꾼다.

**Architecture:** `places`를 공공데이터로 적재(CSV 스트리밍 → EPSG:5174→WGS84 좌표변환 → 멱등 upsert). 검색/주변은 우리 DB 질의(pg_trgm·바운딩박스 + 현재 혼밥러수 오버레이). 카카오 로컬 데이터 경로(`KakaoPlaceClient` 등) 제거, 카카오는 앱 지도 렌더링 SDK로만 잔존.

**Tech Stack:** Java 21 · Spring Boot 4.0.6 · Spring Data JPA · PostgreSQL(+pg_trgm) · Flyway · proj4j(좌표변환) · OpenCSV(스트리밍 파싱) · JUnit5/AssertJ/Mockito/Testcontainers.

## Global Constraints

- 의존성 주입: 생성자 주입 + `final`. 트랜잭션: `@Transactional`은 서비스 경계, 조회 `readOnly`.
- 테스트: 단위(Service)=Mockito, 리포지토리=`@DataJpaTest`+Testcontainers Postgres, 컨트롤러=`@WebMvcTest`. 외부 IO(파일·네트워크) 모킹/임시파일.
- DB 스키마는 Flyway 소유, JPA `ddl-auto: validate`. 새 마이그레이션은 `V2__*.sql`로 append.
- 네이밍: 엔티티 단수 PascalCase, DTO `XxxResponse`/`XxxRequest`, DB snake_case ↔ 엔티티 camelCase.
- 좌표 변환 proj 정의(verbatim): `+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43` — towgs84 7파라미터 필수.
- `business_status` 정규화 값: `"영업"` | `"폐업"`. 적재 시 영업상태명에 "영업"/"정상" 포함이면 `"영업"`, 그 외 skip(또는 `"폐업"`).

---

### Task 1: 의존성 + `places` V2 스키마 + 엔티티

**Files:**
- Modify: `build.gradle:20-42` (의존성 추가)
- Create: `src/main/resources/db/migration/V2__places_master.sql`
- Modify: `src/main/java/com/honjeong/place/domain/Place.java` (전면 갱신)
- Test: `src/test/java/com/honjeong/place/repository/PlaceRepositoryTest.java` (신규 매핑 검증)

**Interfaces:**
- Produces: `Place` 엔티티 — 필드 `Long id`, `String source`, `String sourceId`, `String name`, `String category`, `String address`, `String roadAddress`, `double latitude`, `double longitude`, `String phone`, `String businessStatus`. 팩토리 `Place.ofPublicData(String sourceId, String name, String category, String address, String roadAddress, double lat, double lng, String phone, String businessStatus)`. 게터 전부.

- [ ] **Step 1: build.gradle 의존성 추가**

`dependencies { ... }` 안에 추가:
```groovy
	implementation 'org.locationtech.proj4j:proj4j:1.3.0'
	implementation 'com.opencsv:opencsv:5.9'
```

- [ ] **Step 2: V2 마이그레이션 작성**

Create `src/main/resources/db/migration/V2__places_master.sql` — **컴파일 안전을 위해 ADD-only**(external_id는 nullable로만 완화, DROP은 Task 8의 V3에서):
```sql
-- places: 공공데이터 마스터 컬럼 추가 (식당 데이터는 비어 있어 데이터 이관 없음).
-- external_id/homepage_url 제거는 호출부 정리 후 Task 8(V3)에서 수행한다.
ALTER TABLE places ALTER COLUMN external_id DROP NOT NULL;     -- 적재 행은 external_id 없음
ALTER TABLE places ADD COLUMN source          VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC_DATA';
ALTER TABLE places ADD COLUMN source_id       VARCHAR(64);
ALTER TABLE places ADD COLUMN road_address    VARCHAR(300);
ALTER TABLE places ADD COLUMN business_status VARCHAR(20);
ALTER TABLE places ALTER COLUMN address TYPE VARCHAR(300);
ALTER TABLE places ALTER COLUMN phone   TYPE VARCHAR(40);
ALTER TABLE places ADD CONSTRAINT uq_places_source UNIQUE (source, source_id);

-- 이름 부분일치 검색용 trigram 인덱스
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_places_name_trgm ON places USING gin (name gin_trgm_ops);
-- idx_places_lat_lng (위경도 바운딩박스)는 V1에 이미 존재
```

- [ ] **Step 3: Place 엔티티 전면 갱신**

Replace `Place.java` body (컬럼 매핑 + 팩토리). 핵심:
```java
@Entity
@Table(name = "places")
public class Place extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String source;        // PUBLIC_DATA
    private String sourceId;                                // 관리번호
    @Column(nullable = false) private String name;
    private String category;
    private String address;
    private String roadAddress;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
    private String phone;
    private String businessStatus;

    protected Place() {}

    private Place(String source, String sourceId, String name, String category, String address,
            String roadAddress, double latitude, double longitude, String phone, String businessStatus) {
        this.source = source; this.sourceId = sourceId; this.name = name; this.category = category;
        this.address = address; this.roadAddress = roadAddress; this.latitude = latitude;
        this.longitude = longitude; this.phone = phone; this.businessStatus = businessStatus;
    }

    public static Place ofPublicData(String sourceId, String name, String category, String address,
            String roadAddress, double latitude, double longitude, String phone, String businessStatus) {
        return new Place("PUBLIC_DATA", sourceId, name, category, address, roadAddress,
                latitude, longitude, phone, businessStatus);
    }
    // getId, getSource, getSourceId, getName, getCategory, getAddress, getRoadAddress,
    // getLatitude, getLongitude, getPhone, getBusinessStatus 모두 추가
}
```
> ⚠️ **컴파일 안전(공존 단계)**: 이 태스크에서는 기존 `externalId` 필드(매핑 유지)와 `Place.of(externalId, name, address, lat, lng, category)` 팩토리를 **그대로 남겨둔다**. `PlaceService.findOrCreateByExternalId`·`CheckInService`가 아직 쓰기 때문. 신규 필드(`source`/`sourceId`/`roadAddress`/`businessStatus`) + `ofPublicData`만 **추가**한다. `external_id` 컬럼/`Place.of` 제거는 호출부 정리 후 **Task 8(V3)** 에서 한다. (entity에 두 팩토리·`externalId` 필드가 잠시 공존)

- [ ] **Step 4: 리포지토리 매핑 테스트 작성(실패 확인)**

Create `PlaceRepositoryTest.java` (`@DataJpaTest`, `@ActiveProfiles("test")`):
```java
@DataJpaTest @ActiveProfiles("test")
class PlaceRepositoryTest {
    @Autowired PlaceRepository repo;
    @Autowired TestEntityManager em;

    @Test @DisplayName("공공데이터 식당을 저장하고 source_id로 조회한다")
    void saveAndFind() {
        Place p = Place.ofPublicData("MGMT-1", "혼밥식당", "한식", "서울 지번", "서울 도로명",
                37.5, 127.0, "02-111", "영업");
        em.persistAndFlush(p);
        em.clear();
        Place found = repo.findById(p.getId()).orElseThrow();
        assertThat(found.getSourceId()).isEqualTo("MGMT-1");
        assertThat(found.getRoadAddress()).isEqualTo("서울 도로명");
        assertThat(found.getBusinessStatus()).isEqualTo("영업");
    }
}
```

- [ ] **Step 5: 마이그레이션·테스트 실행**

Run: `./gradlew test --tests "*PlaceRepositoryTest"`
Expected: PASS (Flyway V2 적용 + 매핑 검증). 로컬 dev DB가 V1 적용 상태면 `docker compose down -v && docker compose up -d db`로 초기화 후 진행.

- [ ] **Step 6: Commit**
```bash
git add build.gradle src/main/resources/db/migration/V2__places_master.sql \
        src/main/java/com/honjeong/place/domain/Place.java \
        src/test/java/com/honjeong/place/repository/PlaceRepositoryTest.java
git commit -m "feat(place): places V2 공공데이터 마스터 스키마 + 엔티티"
```

---

### Task 2: `CoordinateConverter` (EPSG:5174 → WGS84)

**Files:**
- Create: `src/main/java/com/honjeong/place/ingest/CoordinateConverter.java`
- Create: `src/main/java/com/honjeong/place/ingest/LatLng.java`
- Test: `src/test/java/com/honjeong/place/ingest/CoordinateConverterTest.java`

**Interfaces:**
- Produces: `record LatLng(double latitude, double longitude)`. `CoordinateConverter.toWgs84(double x, double y): Optional<LatLng>` — x=동(경도방향 TM), y=북(위도방향 TM); 0 이하/변환불가면 empty.

- [ ] **Step 1: LatLng record 작성**
```java
package com.honjeong.place.ingest;
public record LatLng(double latitude, double longitude) {}
```

- [ ] **Step 2: 실패 테스트 작성 (round-trip 일관성 + 한국 좌표 범위)**
```java
package com.honjeong.place.ingest;
import org.junit.jupiter.api.*;
import org.locationtech.proj4j.*;
import static org.assertj.core.api.Assertions.*;

class CoordinateConverterTest {
    static final String EPSG_5174 = "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";
    final CoordinateConverter converter = new CoordinateConverter();

    @Test @DisplayName("WGS84→5174→(converter)→WGS84 round-trip이 원점에 수렴한다")
    void roundTrip() {
        // 테스트에서 역방향(WGS84→5174) 변환으로 알려진 5174 입력을 만든다
        CRSFactory f = new CRSFactory();
        var wgs = f.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
        var tm = f.createFromParameters("EPSG:5174", EPSG_5174);
        var fwd = new CoordinateTransformFactory().createTransform(wgs, tm);
        ProjCoordinate in = new ProjCoordinate(); // 서울시청 부근
        fwd.transform(new ProjCoordinate(126.9779, 37.5663), in);

        LatLng out = converter.toWgs84(in.x, in.y).orElseThrow();
        assertThat(out.latitude()).isCloseTo(37.5663, within(0.0005));
        assertThat(out.longitude()).isCloseTo(126.9779, within(0.0005));
    }

    @Test @DisplayName("좌표가 0 이하이면 빈 결과")
    void missing() {
        assertThat(converter.toWgs84(0, 0)).isEmpty();
        assertThat(converter.toWgs84(-1, 100)).isEmpty();
    }
}
```

- [ ] **Step 3: 실패 확인**

Run: `./gradlew test --tests "*CoordinateConverterTest"`
Expected: FAIL — `CoordinateConverter` 미존재(컴파일 에러).

- [ ] **Step 4: CoordinateConverter 구현**
```java
package com.honjeong.place.ingest;
import java.util.Optional;
import org.locationtech.proj4j.*;
import org.springframework.stereotype.Component;

@Component
public class CoordinateConverter {
    private static final String EPSG_5174 = "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";
    private final CoordinateTransform transform;

    public CoordinateConverter() {
        CRSFactory f = new CRSFactory();
        CoordinateReferenceSystem src = f.createFromParameters("EPSG:5174", EPSG_5174);
        CoordinateReferenceSystem dst = f.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
        this.transform = new CoordinateTransformFactory().createTransform(src, dst);
    }

    /** TM 좌표(x=동, y=북) → WGS84. 0 이하·변환불가면 empty. proj4j는 스레드 안전하지 않으므로 동기화. */
    public synchronized Optional<LatLng> toWgs84(double x, double y) {
        if (x <= 0 || y <= 0) return Optional.empty();
        ProjCoordinate out = new ProjCoordinate();
        transform.transform(new ProjCoordinate(x, y), out);
        if (Double.isNaN(out.x) || Double.isNaN(out.y)) return Optional.empty();
        return Optional.of(new LatLng(out.y, out.x)); // out.y=위도, out.x=경도
    }
}
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew test --tests "*CoordinateConverterTest"`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add src/main/java/com/honjeong/place/ingest/ src/test/java/com/honjeong/place/ingest/
git commit -m "feat(place): EPSG:5174→WGS84 좌표 변환기(proj4j)"
```

---

### Task 3: CSV 행 파싱·매핑 (`PlaceCsvRow` + `PlaceCsvReader`)

**Files:**
- Create: `src/main/java/com/honjeong/place/ingest/PlaceCsvRow.java`
- Create: `src/main/java/com/honjeong/place/ingest/PlaceCsvReader.java`
- Test: `src/test/java/com/honjeong/place/ingest/PlaceCsvReaderTest.java`
- Test fixture: `src/test/resources/fixtures/sample-places.csv`

**Interfaces:**
- Produces: `record PlaceCsvRow(String managementId, String name, String category, String address, String roadAddress, String phone, String businessStatusName, String coordX, String coordY)`. `PlaceCsvReader.read(Reader source, Consumer<PlaceCsvRow> sink): void` — 스트리밍으로 한 행씩 sink에 넘긴다(헤더→컬럼 인덱스 매핑).

> 표준데이터 실제 헤더명은 다운로드 CSV로 확정해 `HEADER_*` 상수에 반영한다. 초기 매핑 기준: `관리번호`,`사업장명`,`위생업태명`,`소재지전체주소`,`도로명전체주소`,`소재지전화`,`영업상태명`,`좌표정보(X)`,`좌표정보(Y)`.

- [ ] **Step 1: PlaceCsvRow record 작성**
```java
package com.honjeong.place.ingest;
public record PlaceCsvRow(String managementId, String name, String category, String address,
        String roadAddress, String phone, String businessStatusName, String coordX, String coordY) {}
```

- [ ] **Step 2: 픽스처 CSV 작성** `src/test/resources/fixtures/sample-places.csv` (UTF-8, 3행: 정상·폐업·좌표결측)
```csv
관리번호,사업장명,위생업태명,소재지전체주소,도로명전체주소,소재지전화,영업상태명,좌표정보(X),좌표정보(Y)
MGMT-1,혼밥식당,한식,서울 어딘가,서울 도로명1,02-111,영업,198000.0,451900.0
MGMT-2,폐업식당,분식,서울 폐업로,서울 도로명2,02-222,폐업,198100.0,451800.0
MGMT-3,좌표없음식당,일식,서울 무좌표로,서울 도로명3,02-333,영업,,
```

- [ ] **Step 3: 실패 테스트 작성**
```java
package com.honjeong.place.ingest;
import org.junit.jupiter.api.*;
import java.io.*; import java.util.*;
import static org.assertj.core.api.Assertions.*;

class PlaceCsvReaderTest {
    @Test @DisplayName("헤더 매핑해 행을 순서대로 sink로 흘린다")
    void streamsRows() throws Exception {
        var reader = new PlaceCsvReader();
        List<PlaceCsvRow> rows = new ArrayList<>();
        try (Reader r = new InputStreamReader(
                getClass().getResourceAsStream("/fixtures/sample-places.csv"), java.nio.charset.StandardCharsets.UTF_8)) {
            reader.read(r, rows::add);
        }
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).managementId()).isEqualTo("MGMT-1");
        assertThat(rows.get(0).name()).isEqualTo("혼밥식당");
        assertThat(rows.get(0).businessStatusName()).isEqualTo("영업");
        assertThat(rows.get(2).coordX()).isBlank();
    }
}
```

- [ ] **Step 4: 실패 확인**

Run: `./gradlew test --tests "*PlaceCsvReaderTest"`
Expected: FAIL — `PlaceCsvReader` 미존재.

- [ ] **Step 5: PlaceCsvReader 구현 (OpenCSV 스트리밍)**
```java
package com.honjeong.place.ingest;
import java.io.Reader; import java.util.*; import java.util.function.Consumer;
import com.opencsv.CSVReaderHeaderAware;

public class PlaceCsvReader {
    public void read(Reader source, Consumer<PlaceCsvRow> sink) {
        try (CSVReaderHeaderAware csv = new CSVReaderHeaderAware(source)) {
            Map<String, String> m;
            while ((m = csv.readMap()) != null) {
                sink.accept(new PlaceCsvRow(
                        get(m, "관리번호"), get(m, "사업장명"), get(m, "위생업태명"),
                        get(m, "소재지전체주소"), get(m, "도로명전체주소"), get(m, "소재지전화"),
                        get(m, "영업상태명"), get(m, "좌표정보(X)"), get(m, "좌표정보(Y)")));
            }
        } catch (Exception e) {
            throw new IllegalStateException("CSV 파싱 실패: " + e.getMessage(), e);
        }
    }
    private static String get(Map<String, String> m, String key) {
        String v = m.get(key); return v == null ? "" : v.trim();
    }
}
```
> 실제 CSV가 CP949면 호출부(Task 4 Runner)에서 `InputStreamReader(.., Charset.forName("MS949"))`로 연다. 헤더명이 다르면 `get(...)` 키를 실제 헤더로 교체.

- [ ] **Step 6: 통과 확인 + Commit**

Run: `./gradlew test --tests "*PlaceCsvReaderTest"` → PASS
```bash
git add src/main/java/com/honjeong/place/ingest/PlaceCsvRow.java \
        src/main/java/com/honjeong/place/ingest/PlaceCsvReader.java \
        src/test/java/com/honjeong/place/ingest/PlaceCsvReaderTest.java \
        src/test/resources/fixtures/sample-places.csv
git commit -m "feat(place): 공공데이터 CSV 스트리밍 파서"
```

---

### Task 4: 적재 서비스 `PlaceIngestionService` (멱등 upsert)

**Files:**
- Create: `src/main/java/com/honjeong/place/ingest/PlaceIngestionService.java`
- Create: `src/main/java/com/honjeong/place/ingest/IngestionResult.java`
- Modify: `src/main/java/com/honjeong/place/repository/PlaceRepository.java` (upsert 네이티브 쿼리)
- Test: `src/test/java/com/honjeong/place/ingest/PlaceIngestionServiceTest.java` (`@SpringBootTest`+Testcontainers)

**Interfaces:**
- Consumes: `PlaceCsvReader.read`, `CoordinateConverter.toWgs84`, `PlaceCsvRow`.
- Produces: `PlaceIngestionService.ingest(Reader csv): IngestionResult`. `record IngestionResult(int read, int upserted, int skippedClosed, int skippedNoCoord)`. `PlaceRepository.upsertPublicData(...)` (배치 호출용 단건 upsert).

- [ ] **Step 1: IngestionResult record**
```java
package com.honjeong.place.ingest;
public record IngestionResult(int read, int upserted, int skippedClosed, int skippedNoCoord) {}
```

- [ ] **Step 2: PlaceRepository에 멱등 upsert 추가**

`PlaceRepository.java`에 추가(네이티브, `ON CONFLICT`):
```java
@Modifying
@Query(value = """
        INSERT INTO places(source, source_id, name, category, address, road_address,
                           latitude, longitude, phone, business_status, created_at, updated_at)
        VALUES ('PUBLIC_DATA', :sourceId, :name, :category, :address, :roadAddress,
                :lat, :lng, :phone, :status, now(), now())
        ON CONFLICT (source, source_id) DO UPDATE SET
            name = EXCLUDED.name, category = EXCLUDED.category, address = EXCLUDED.address,
            road_address = EXCLUDED.road_address, latitude = EXCLUDED.latitude,
            longitude = EXCLUDED.longitude, phone = EXCLUDED.phone,
            business_status = EXCLUDED.business_status, updated_at = now()
        """, nativeQuery = true)
void upsertPublicData(@Param("sourceId") String sourceId, @Param("name") String name,
        @Param("category") String category, @Param("address") String address,
        @Param("roadAddress") String roadAddress, @Param("lat") double lat,
        @Param("lng") double lng, @Param("phone") String phone, @Param("status") String status);
```

- [ ] **Step 3: 실패 테스트 작성 (멱등·폐업필터·좌표결측 skip)**
```java
@SpringBootTest @ActiveProfiles("test")
class PlaceIngestionServiceTest {
    @Autowired PlaceIngestionService service;
    @Autowired PlaceRepository repo;

    private Reader fixture() {
        return new InputStreamReader(getClass().getResourceAsStream("/fixtures/sample-places.csv"),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test @DisplayName("영업 식당만 적재하고 폐업·좌표결측은 skip하며 두 번 돌려도 멱등하다")
    void ingestIdempotent() throws Exception {
        IngestionResult first = service.ingest(fixture());
        assertThat(first.read()).isEqualTo(3);
        assertThat(first.upserted()).isEqualTo(1);      // MGMT-1만
        assertThat(first.skippedClosed()).isEqualTo(1); // MGMT-2
        assertThat(first.skippedNoCoord()).isEqualTo(1);// MGMT-3
        assertThat(repo.count()).isEqualTo(1);

        service.ingest(fixture());                      // 재실행
        assertThat(repo.count()).isEqualTo(1);          // 중복 없음
    }
}
```

- [ ] **Step 4: 실패 확인**

Run: `./gradlew test --tests "*PlaceIngestionServiceTest"`
Expected: FAIL — `PlaceIngestionService` 미존재.

- [ ] **Step 5: PlaceIngestionService 구현**
```java
package com.honjeong.place.ingest;
import java.io.Reader; import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.place.repository.PlaceRepository;

@Service
public class PlaceIngestionService {
    private final PlaceCsvReader reader = new PlaceCsvReader();
    private final CoordinateConverter converter;
    private final PlaceRepository placeRepository;

    public PlaceIngestionService(CoordinateConverter converter, PlaceRepository placeRepository) {
        this.converter = converter; this.placeRepository = placeRepository;
    }

    @Transactional
    public IngestionResult ingest(Reader csv) {
        int[] c = new int[4]; // read, upserted, skippedClosed, skippedNoCoord
        reader.read(csv, row -> {
            c[0]++;
            if (!isOpen(row.businessStatusName())) { c[2]++; return; }
            Optional<LatLng> ll = parseCoord(row);
            if (ll.isEmpty()) { c[3]++; return; }
            placeRepository.upsertPublicData(row.managementId(), row.name(), blankToNull(row.category()),
                    blankToNull(row.address()), blankToNull(row.roadAddress()),
                    ll.get().latitude(), ll.get().longitude(), blankToNull(row.phone()), "영업");
            c[1]++;
        });
        return new IngestionResult(c[0], c[1], c[2], c[3]);
    }

    private boolean isOpen(String statusName) {
        return statusName != null && (statusName.contains("영업") || statusName.contains("정상"));
    }
    private Optional<LatLng> parseCoord(PlaceCsvRow row) {
        try {
            if (row.coordX().isBlank() || row.coordY().isBlank()) return Optional.empty();
            return converter.toWgs84(Double.parseDouble(row.coordX()), Double.parseDouble(row.coordY()));
        } catch (NumberFormatException e) { return Optional.empty(); }
    }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s; }
}
```

- [ ] **Step 6: 통과 확인 + Commit**

Run: `./gradlew test --tests "*PlaceIngestionServiceTest"` → PASS
```bash
git add src/main/java/com/honjeong/place/ingest/PlaceIngestionService.java \
        src/main/java/com/honjeong/place/ingest/IngestionResult.java \
        src/main/java/com/honjeong/place/repository/PlaceRepository.java \
        src/test/java/com/honjeong/place/ingest/PlaceIngestionServiceTest.java
git commit -m "feat(place): 공공데이터 멱등 적재 서비스(폐업·좌표결측 skip)"
```

---

### Task 5: 적재 트리거 `PlaceIngestionRunner`

**Files:**
- Create: `src/main/java/com/honjeong/place/ingest/PlaceIngestionRunner.java`
- Modify: `src/main/resources/application.yml` (place 설정 정리는 Task 7; 여기선 ingest 키만 문서화)

**Interfaces:**
- Consumes: `PlaceIngestionService.ingest`.
- Produces: 앱 부팅 시 `honjeong.place.ingest.file` 프로퍼티가 있으면 그 CSV를 1회 적재(없으면 미동작).

- [ ] **Step 1: Runner 구현**
```java
package com.honjeong.place.ingest;
import java.io.*; import java.nio.charset.Charset; import java.nio.file.*;
import org.slf4j.*; import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*; import org.springframework.stereotype.Component;

@Component
public class PlaceIngestionRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PlaceIngestionRunner.class);
    private final PlaceIngestionService service;
    @Value("${honjeong.place.ingest.file:}") private String file;
    @Value("${honjeong.place.ingest.charset:MS949}") private String charset;

    public PlaceIngestionRunner(PlaceIngestionService service) { this.service = service; }

    @Override public void run(ApplicationArguments args) throws Exception {
        if (file == null || file.isBlank()) return;          // 평상시 미동작
        log.info("공공데이터 적재 시작: {} ({})", file, charset);
        try (Reader r = Files.newBufferedReader(Path.of(file), Charset.forName(charset))) {
            IngestionResult res = service.ingest(r);
            log.info("적재 완료 read={} upserted={} skippedClosed={} skippedNoCoord={}",
                    res.read(), res.upserted(), res.skippedClosed(), res.skippedNoCoord());
        }
    }
}
```

- [ ] **Step 2: 컴파일·전체 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (Runner는 file 미지정이라 테스트 컨텍스트에서 미동작).

- [ ] **Step 3: Commit**
```bash
git add src/main/java/com/honjeong/place/ingest/PlaceIngestionRunner.java
git commit -m "feat(place): 공공데이터 CSV 적재 트리거(프로퍼티 가드 ApplicationRunner)"
```

---

### Task 6: 검색 재작성 (우리 DB)

**Files:**
- Modify: `src/main/java/com/honjeong/place/repository/PlaceRepository.java` (searchByName)
- Modify: `src/main/java/com/honjeong/place/service/PlaceService.java` (search 재작성)
- Modify: `src/main/java/com/honjeong/place/dto/PlaceSearchResponse.java` (placeId 추가, from(Place))
- Modify: `src/main/java/com/honjeong/place/controller/PlaceController.java` (시그니처 정리)
- Test: `src/test/java/com/honjeong/place/service/PlaceServiceTest.java` (재작성)

**Interfaces:**
- Produces: `PlaceService.search(String query, int page, int size): PageResponse<PlaceSearchResponse>`. `PlaceSearchResponse(Long placeId, String name, String category, String address, String roadAddress, double latitude, double longitude, String phone)` + `from(Place)`. `PlaceRepository.searchOpenByName(String q, Pageable): Page<Place>`.

- [ ] **Step 1: PlaceSearchResponse 갱신**
```java
public record PlaceSearchResponse(Long placeId, String name, String category, String address,
        String roadAddress, double latitude, double longitude, String phone) {
    public static PlaceSearchResponse from(Place p) {
        return new PlaceSearchResponse(p.getId(), p.getName(), p.getCategory(), p.getAddress(),
                p.getRoadAddress(), p.getLatitude(), p.getLongitude(), p.getPhone());
    }
}
```

- [ ] **Step 2: PlaceRepository.searchOpenByName 추가**
```java
@Query("""
        SELECT p FROM Place p
        WHERE p.businessStatus = '영업'
          AND LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
        """)
Page<Place> searchOpenByName(@Param("q") String q, Pageable pageable);
```

- [ ] **Step 3: PlaceService.search 재작성 (실패 테스트 먼저)**

`PlaceServiceTest.java`를 재작성 — 카카오 클라이언트 의존 제거, `PlaceRepository` 모킹:
```java
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {
    @Mock PlaceRepository placeRepository;
    @Mock CheckInRepository checkInRepository;     // Task 7 nearby용
    @InjectMocks PlaceService service;

    @Test @DisplayName("빈 검색어는 INVALID_INPUT")
    void blankQuery() {
        assertThatThrownBy(() -> service.search("  ", 0, 20))
                .isInstanceOf(BusinessException.class);
    }

    @Test @DisplayName("검색어로 우리 DB를 조회해 페이지 엔벨로프로 매핑한다")
    void search() {
        Place p = Place.ofPublicData("M1", "혼밥김밥", "분식", "주소", "도로명", 37.5, 127.0, "02", "영업");
        when(placeRepository.searchOpenByName(eq("김밥"), any()))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        var res = service.search("김밥", 0, 20);
        assertThat(res.content()).hasSize(1);
        assertThat(res.content().get(0).name()).isEqualTo("혼밥김밥");
    }
}
```

- [ ] **Step 4: 실패 확인** → `./gradlew test --tests "*PlaceServiceTest"` → FAIL (시그니처 불일치/컴파일).

- [ ] **Step 5: PlaceService.search 구현**
```java
@Transactional(readOnly = true)
public PageResponse<PlaceSearchResponse> search(String query, int page, int size) {
    if (query == null || query.isBlank())
        throw new BusinessException(ErrorCode.INVALID_INPUT, "검색어를 입력해주세요.");
    if (page < 0) throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
    if (size < 1) throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1 이상이어야 합니다.");
    int clampedSize = Math.min(size, MAX_SIZE);
    Page<Place> result = placeRepository.searchOpenByName(query.trim(),
            PageRequest.of(page, clampedSize, Sort.by("name")));
    List<PlaceSearchResponse> content = result.getContent().stream().map(PlaceSearchResponse::from).toList();
    return PageResponse.of(content, page, clampedSize, result.getTotalElements());
}
```
> ⚠️ **컴파일 안전(공존 단계)**: 이 태스크에서 `PlaceService` 생성자를 `(PlaceRepository placeRepository, CheckInRepository checkInRepository)`로 교체하고 **`KakaoPlaceClient` 의존만 제거**(검색=DB). `findOrCreateByExternalId`는 `CheckInService`가 아직 쓰므로 **잔존시킨다**(내부 구현은 `placeRepository`+`Place.of` 그대로). `KakaoPlaceClient`/`MockKakaoPlaceClient` 빈은 아직 존재하나 미사용 — 삭제는 Task 8. `findOrCreateByExternalId`·`PlaceUpsertCommand` 제거도 Task 8.

- [ ] **Step 6: 컨트롤러 시그니처 정리**

`PlaceController.search`: `@RequestParam String query, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size` → `placeService.search(query, page, size)`. (lat/lng 파라미터 제거)

- [ ] **Step 7: 통과 확인 + Commit**

Run: `./gradlew test --tests "*PlaceServiceTest" --tests "*PlaceRepositoryTest"` → PASS
```bash
git add src/main/java/com/honjeong/place/ src/test/java/com/honjeong/place/service/PlaceServiceTest.java
git commit -m "feat(place): 검색을 우리 DB(pg_trgm)로 재작성"
```

---

### Task 7: 주변검색 `nearby` + 혼밥러수 오버레이

**Files:**
- Create: `src/main/java/com/honjeong/place/dto/PlaceNearbyResponse.java`
- Create: `src/main/java/com/honjeong/checkin/dto/PlaceActiveCount.java`
- Modify: `src/main/java/com/honjeong/checkin/repository/CheckInRepository.java` (countActiveByPlaceIds)
- Modify: `src/main/java/com/honjeong/place/repository/PlaceRepository.java` (findOpenWithinBounds)
- Modify: `src/main/java/com/honjeong/place/service/PlaceService.java` (nearby)
- Modify: `src/main/java/com/honjeong/place/controller/PlaceController.java` (GET /nearby)
- Test: `PlaceServiceTest.java`(nearby), `PlaceControllerTest.java`

**Interfaces:**
- Consumes: `PlaceRepository.findOpenWithinBounds`, `CheckInRepository.countActiveByPlaceIds`.
- Produces: `PlaceNearbyResponse(Long placeId, String name, String category, String roadAddress, double latitude, double longitude, long distanceMeters, long activeCount)`. `PlaceService.nearby(Double lat, Double lng, int radius, int page, int size): PageResponse<PlaceNearbyResponse>`. `CheckInRepository.countActiveByPlaceIds(List<Long>): List<PlaceActiveCount>`. `PlaceActiveCount(Long placeId, long activeCount)`.

- [ ] **Step 1: DTO 2개 작성**
```java
// place/dto/PlaceNearbyResponse.java
public record PlaceNearbyResponse(Long placeId, String name, String category, String roadAddress,
        double latitude, double longitude, long distanceMeters, long activeCount) {}
// checkin/dto/PlaceActiveCount.java
public record PlaceActiveCount(Long placeId, long activeCount) {}
```

- [ ] **Step 2: 리포지토리 쿼리 2개 추가**

`CheckInRepository`:
```java
@Query("""
        SELECT new com.honjeong.checkin.dto.PlaceActiveCount(c.place.id, COUNT(c.id))
        FROM CheckIn c
        WHERE c.place.id IN :placeIds AND c.status = com.honjeong.checkin.domain.CheckInStatus.ACTIVE
        GROUP BY c.place.id
        """)
List<PlaceActiveCount> countActiveByPlaceIds(@Param("placeIds") List<Long> placeIds);
```
`PlaceRepository`:
```java
@Query("""
        SELECT p FROM Place p
        WHERE p.businessStatus = '영업'
          AND p.latitude BETWEEN :latMin AND :latMax
          AND p.longitude BETWEEN :lngMin AND :lngMax
        """)
List<Place> findOpenWithinBounds(@Param("latMin") double latMin, @Param("latMax") double latMax,
        @Param("lngMin") double lngMin, @Param("lngMax") double lngMax);
```

- [ ] **Step 3: nearby 실패 테스트 작성 (PlaceServiceTest에 추가)**
```java
@Test @DisplayName("주변 식당을 거리순으로 반환하고 혼밥러수를 오버레이한다")
void nearby() {
    Place a = Place.ofPublicData("A","가까운집","한식","주소","도로",37.5000,127.0000,"02","영업");
    Place b = Place.ofPublicData("B","먼집","분식","주소","도로",37.5050,127.0050,"02","영업");
    ReflectionTestUtils.setField(a, "id", 1L);
    ReflectionTestUtils.setField(b, "id", 2L);
    when(placeRepository.findOpenWithinBounds(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
            .thenReturn(List.of(b, a));
    when(checkInRepository.countActiveByPlaceIds(anyList()))
            .thenReturn(List.of(new PlaceActiveCount(1L, 3L)));
    var res = service.nearby(37.5000, 127.0000, 1000, 0, 20);
    assertThat(res.content().get(0).placeId()).isEqualTo(1L);   // 가까운 a 먼저
    assertThat(res.content().get(0).activeCount()).isEqualTo(3);
    assertThat(res.content().get(1).activeCount()).isEqualTo(0); // 오버레이 없는 b는 0
}

@Test @DisplayName("lat/lng 누락이면 INVALID_INPUT")
void nearbyMissingCoord() {
    assertThatThrownBy(() -> service.nearby(null, 127.0, 1000, 0, 20))
            .isInstanceOf(BusinessException.class);
}
```

- [ ] **Step 4: 실패 확인** → `./gradlew test --tests "*PlaceServiceTest"` → FAIL.

- [ ] **Step 5: PlaceService.nearby 구현** (CheckInService의 Haversine·바운딩박스 패턴 재사용)
```java
static final int MAX_RADIUS = 10_000;
private static final double METERS_PER_DEGREE_LAT = 111_320.0;
private static final double EARTH_RADIUS_M = 6_371_000.0;

@Transactional(readOnly = true)
public PageResponse<PlaceNearbyResponse> nearby(Double lat, Double lng, int radius, int page, int size) {
    if (lat == null || lng == null)
        throw new BusinessException(ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다.");
    if (page < 0 || size < 1)
        throw new BusinessException(ErrorCode.INVALID_INPUT, "page/size가 올바르지 않습니다.");
    int clampedSize = Math.min(size, MAX_SIZE);
    int r = Math.min(Math.max(radius, 1), MAX_RADIUS);
    double dLat = r / METERS_PER_DEGREE_LAT;
    double dLng = r / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));

    List<Place> inBox = placeRepository.findOpenWithinBounds(lat - dLat, lat + dLat, lng - dLng, lng + dLng);
    List<Place> within = inBox.stream()
            .filter(p -> haversine(lat, lng, p.getLatitude(), p.getLongitude()) <= r)
            .sorted(Comparator.comparingDouble(p -> haversine(lat, lng, p.getLatitude(), p.getLongitude())))
            .toList();

    Map<Long, Long> counts = checkInRepository.countActiveByPlaceIds(
            within.stream().map(Place::getId).toList()).stream()
            .collect(Collectors.toMap(PlaceActiveCount::placeId, PlaceActiveCount::activeCount));

    long total = within.size();
    int from = Math.min(page * clampedSize, within.size());
    int to = Math.min(from + clampedSize, within.size());
    List<PlaceNearbyResponse> content = within.subList(from, to).stream()
            .map(p -> new PlaceNearbyResponse(p.getId(), p.getName(), p.getCategory(), p.getRoadAddress(),
                    p.getLatitude(), p.getLongitude(),
                    Math.round(haversine(lat, lng, p.getLatitude(), p.getLongitude())),
                    counts.getOrDefault(p.getId(), 0L)))
            .toList();
    return PageResponse.of(content, page, clampedSize, total);
}

private static double haversine(double lat1, double lng1, double lat2, double lng2) {
    double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
    double a = Math.sin(dLat/2)*Math.sin(dLat/2)
            + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLng/2)*Math.sin(dLng/2);
    return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
}
```
> 빈 placeIds 리스트는 JPQL `IN ()`에서 문제될 수 있으니 `within`이 비면 counts를 빈 맵으로 단락 처리(가드 추가).

- [ ] **Step 6: 컨트롤러에 nearby 추가**
```java
@GetMapping("/nearby")
public ApiResponse<PageResponse<PlaceNearbyResponse>> nearby(
        @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lng,
        @RequestParam(defaultValue = "1000") int radius,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(placeService.nearby(lat, lng, radius, page, size));
}
```

- [ ] **Step 7: 컨트롤러 슬라이스 테스트** `PlaceControllerTest`(`@WebMvcTest(PlaceController.class)`, `PlaceService` 모킹): search 200·nearby 200·query 누락 400 검증.

- [ ] **Step 8: 통과 확인 + Commit**

Run: `./gradlew test --tests "*PlaceServiceTest" --tests "*PlaceControllerTest"` → PASS
```bash
git add src/main/java/com/honjeong/place/ src/main/java/com/honjeong/checkin/ \
        src/test/java/com/honjeong/place/
git commit -m "feat(place): GET /api/places/nearby 주변검색 + 혼밥러수 오버레이"
```

---

### Task 8: 체크인 `placeId` 전환 + 카카오 데이터 경로 제거

**Files:**
- Modify: `src/main/java/com/honjeong/checkin/dto/CheckInRequest.java` (`{placeId}`)
- Modify: `src/main/java/com/honjeong/checkin/service/CheckInService.java` (placeService.getById)
- Modify: `src/main/java/com/honjeong/place/service/PlaceService.java` (getById 추가, findOrCreateByExternalId 제거)
- Modify: `src/main/java/com/honjeong/place/domain/Place.java` (`externalId` 필드·`Place.of` 제거 — 공존 종료)
- Create: `src/main/resources/db/migration/V3__drop_place_external_id.sql`
- Modify: `src/main/java/com/honjeong/global/exception/ErrorCode.java` (PLACE_NOT_FOUND)
- Delete: `KakaoPlaceClient.java`, `MockKakaoPlaceClient.java`, `PlaceSearchQuery.java`, `PlaceSearchPage.java`, `PlaceCandidate.java`, `PlaceUpsertCommand.java`
- Modify: `src/main/resources/application.yml`·`application-prod.yml` (`place.mode` 제거)
- Modify: 기존 테스트 — `CheckInServiceTest`, `CheckInControllerTest`, `CheckInMealHappyPathE2eTest`

**Interfaces:**
- Produces: `CheckInRequest(@NotNull Long placeId)`. `PlaceService.getById(Long placeId): Place`(없으면 `PLACE_NOT_FOUND`). `ErrorCode.PLACE_NOT_FOUND(404)`.

- [ ] **Step 1: ErrorCode 추가**
```java
PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "식당을 찾을 수 없습니다."),
```

- [ ] **Step 2: CheckInRequest 교체**
```java
public record CheckInRequest(@NotNull Long placeId) {}
```

- [ ] **Step 3: PlaceService.getById 추가, findOrCreateByExternalId 제거**

(생성자는 Task 6에서 이미 `(PlaceRepository, CheckInRepository)`로 교체됨 — 카카오 의존 없음.) 추가:
```java
@Transactional(readOnly = true)
public Place getById(Long placeId) {
    return placeRepository.findById(placeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
}
```
`findOrCreateByExternalId` 메서드 삭제(이제 호출부 없음).

- [ ] **Step 4: CheckInService.createCheckIn 수정**
```java
Place place = placeService.getById(request.placeId());
```
(`request.toUpsertCommand()` 호출 제거. 이후 단일활성 로직 동일.)

- [ ] **Step 5: 엔티티 공존 종료 + V3 마이그레이션 + 카카오/upsert 클래스 삭제 + 설정 정리**

  - `Place.java`에서 `externalId` 필드·게터·`Place.of(...)` 팩토리 삭제(이제 호출부 없음).
  - Create `src/main/resources/db/migration/V3__drop_place_external_id.sql`:
    ```sql
    ALTER TABLE places DROP COLUMN external_id;
    ALTER TABLE places DROP COLUMN homepage_url;
    ```
  - Delete 6개 파일(위 목록).
  - `application.yml`·`application-prod.yml`에서 `place:`(`mode`) 블록 제거. (`oauth`/`sms` mode 유지.)
  - `MockKakaoPlaceClient` 삭제로 검색이 더는 외부호출 아님.

- [ ] **Step 6: 깨진 기존 테스트 갱신**

- `CheckInServiceTest`: `placeService.findOrCreateByExternalId` 스텁 → `placeService.getById(placeId)` 스텁으로 교체. 요청 객체 `new CheckInRequest(placeId)`.
- `CheckInControllerTest`: 요청 JSON `{"placeId": 3}`. 404 케이스(`PLACE_NOT_FOUND`) 추가.
- `CheckInMealHappyPathE2eTest`: 체크인 전에 `placeRepository.save(Place.ofPublicData(...))`로 식당 시드 → 그 `placeId`로 체크인 호출(기존 externalId 페이로드 제거).

- [ ] **Step 7: 전체 테스트 통과 확인**

Run: `./gradlew test`
Expected: PASS (전 슬라이스 그린). 실패 시 해당 테스트의 placeId 전환 누락 점검.

- [ ] **Step 8: Commit**
```bash
git add -A
git commit -m "refactor(checkin): 체크인 placeId 전환 + 카카오 로컬 데이터 경로 제거"
```

---

### Task 9: 전국 적재 실행 + 라이브 검증 (수동)

**Files:** (코드 변경 없음 — 운영 절차)

- [ ] **Step 1: 공공데이터 CSV 확보**

localdata.go.kr 또는 data.go.kr/15096283에서 **전국일반음식점 전체분 CSV**(약 213만 행) 다운로드 → `~/honjeong-data/general-restaurants.csv` 저장. (xlsx 아님 — 100만행 한계)

- [ ] **Step 2: 로컬 DB 기동 + 적재 실행**
```bash
docker compose up -d db
SPRING_PROFILES_ACTIVE=local \
  honjeong_place_ingest_file=$HOME/honjeong-data/general-restaurants.csv \
  honjeong_place_ingest_charset=MS949 \
  ./gradlew bootRun
```
로그에서 `적재 완료 read=... upserted=...` 확인. (헤더명/인코딩이 다르면 Task 3 `get(...)` 키·charset 조정 후 재실행 — 멱등이라 안전.)

- [ ] **Step 3: 라이브 검증**
```bash
# 적재 건수
docker exec -it honjeong-db psql -U honjeong -d honjeong -c "SELECT count(*) FROM places;"
# 좌표 정확도 — 알려진 식당 1건을 위경도로 보고 지도에서 위치 확인(좌표변환 검증)
docker exec -it honjeong-db psql -U honjeong -d honjeong \
  -c "SELECT name, latitude, longitude FROM places WHERE name LIKE '%스타벅스%' LIMIT 3;"
# 검색·주변 API (USER 토큰 필요)
curl "http://localhost:8080/api/places/search?query=김밥&page=0&size=5" -H "Authorization: Bearer <token>"
curl "http://localhost:8080/api/places/nearby?lat=37.5663&lng=126.9779&radius=1000" -H "Authorization: Bearer <token>"
```
좌표가 한국 범위(위도 33~39, 경도 124~132) 안이고 지도상 실제 위치와 일치하면 변환 정상.

- [ ] **Step 4: 적재 가드 비활성 확인**

평상시 `honjeong.place.ingest.file` 미지정 → 재기동해도 재적재 안 됨을 확인(로그에 적재 메시지 없음).

---

## Self-Review (작성자 점검)

- **스펙 커버리지**: §2 스키마=Task1 · §3.1 변환기=Task2 · §3.2 적재=Task3~5 · §3.3 검색=Task6 · §3.4 nearby=Task7 · §3.5 체크인=Task8 · §3.6 카카오제거=Task8 · §7 적재실행=Task9. docs(§6)는 본 세션에서 이미 수정·커밋됨. ✅
- **placeholder 스캔**: 모든 코드 스텝에 실제 코드 포함. 표준데이터 헤더명만 "실파일로 확정"으로 명시(가드 포함). ✅
- **타입 일관성**: `PlaceSearchResponse(placeId, name, category, address, roadAddress, lat, lng, phone)`·`PlaceNearbyResponse(... distanceMeters, activeCount)`·`PlaceActiveCount(placeId, activeCount)`·`Place.ofPublicData(...)`·`PlaceService.search(query,page,size)`/`nearby(lat,lng,radius,page,size)`/`getById(placeId)`·`CheckInRequest(placeId)` — Task 간 시그니처 일치 확인. ✅
- **주의**: nearby의 빈 placeIds 가드(Task7 Step5), CP949 인코딩(Task9), Flyway 로컬 DB 초기화(Task1 Step5).
