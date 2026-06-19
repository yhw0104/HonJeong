package com.honjeong.place.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.support.AbstractPostgresTest;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlaceIngestionServiceTest extends AbstractPostgresTest {

    @Autowired
    PlaceIngestionService service;

    @Autowired
    PlaceRepository repo;

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
