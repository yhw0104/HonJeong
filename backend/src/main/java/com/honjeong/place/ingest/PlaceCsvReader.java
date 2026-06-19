package com.honjeong.place.ingest;

import com.opencsv.CSVReaderHeaderAware;

import java.io.Reader;
import java.util.Map;
import java.util.function.Consumer;

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
        String v = m.get(key);
        return v == null ? "" : v.trim();
    }
}
