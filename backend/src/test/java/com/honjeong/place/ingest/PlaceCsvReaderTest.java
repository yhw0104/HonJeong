package com.honjeong.place.ingest;

import org.junit.jupiter.api.*;

import java.io.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class PlaceCsvReaderTest {

    @Test
    @DisplayName("헤더 매핑해 행을 순서대로 sink로 흘린다")
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
