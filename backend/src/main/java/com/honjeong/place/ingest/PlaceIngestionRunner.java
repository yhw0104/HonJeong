package com.honjeong.place.ingest;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;

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
