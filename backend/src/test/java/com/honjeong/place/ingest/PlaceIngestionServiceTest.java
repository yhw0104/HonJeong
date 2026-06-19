package com.honjeong.place.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.support.AbstractPostgresTest;

@SpringBootTest
@ActiveProfiles("test")
class PlaceIngestionServiceTest extends AbstractPostgresTest {

    @Autowired
    PlaceIngestionService service;

    @Autowired
    PlaceRepository repo;

    @Autowired
    JdbcTemplate jdbc;

    /** 테스트 전: 다른 테스트 클래스(E2E 등)가 남긴 FK 종속 행을 포함해 관련 테이블을 초기화한다. */
    @BeforeEach
    void setUp() {
        // meal_requests → check_ins → places 참조 순서이므로 TRUNCATE로 한 번에 처리한다.
        jdbc.execute("TRUNCATE meal_requests, check_ins, places RESTART IDENTITY");
    }

    /** 테스트 후: 이 테스트가 커밋한 places 행(MGMT-1 등)이 다른 테스트 클래스를 오염시키지 않도록 정리한다. */
    @AfterEach
    void tearDown() {
        jdbc.execute("TRUNCATE meal_requests, check_ins, places RESTART IDENTITY");
    }

    private Reader fixture() {
        return new InputStreamReader(
                getClass().getResourceAsStream("/fixtures/sample-places.csv"),
                java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("영업 식당만 적재하고 폐업·좌표결측은 skip하며 두 번 돌려도 멱등하다")
    void ingestIdempotent() throws Exception {
        IngestionResult first = service.ingest(fixture());
        assertThat(first.read()).isEqualTo(3);
        assertThat(first.upserted()).isEqualTo(1);       // MGMT-1만
        assertThat(first.skippedClosed()).isEqualTo(1);  // MGMT-2
        assertThat(first.skippedNoCoord()).isEqualTo(1); // MGMT-3
        assertThat(repo.count()).isEqualTo(1);

        service.ingest(fixture());             // 재실행
        assertThat(repo.count()).isEqualTo(1); // 중복 없음
    }
}
