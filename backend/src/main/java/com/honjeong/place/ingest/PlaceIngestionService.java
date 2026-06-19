package com.honjeong.place.ingest;

import java.io.Reader;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.place.repository.PlaceRepository;

/**
 * 공공데이터 CSV를 읽어 places 테이블에 멱등 upsert한다.
 *
 * <p>필터 기준:
 * <ul>
 *   <li>폐업(영업상태명에 '영업'·'정상'이 없음) → skippedClosed 증가, skip.</li>
 *   <li>좌표 결측·파싱 불가 → skippedNoCoord 증가, skip.</li>
 * </ul>
 */
@Service
public class PlaceIngestionService {

    private final PlaceCsvReader reader = new PlaceCsvReader();
    private final CoordinateConverter converter;
    private final PlaceRepository placeRepository;

    public PlaceIngestionService(CoordinateConverter converter, PlaceRepository placeRepository) {
        this.converter = converter;
        this.placeRepository = placeRepository;
    }

    @Transactional
    public IngestionResult ingest(Reader csv) {
        int[] c = new int[4]; // [0]=read, [1]=upserted, [2]=skippedClosed, [3]=skippedNoCoord
        reader.read(csv, row -> {
            c[0]++;
            if (!isOpen(row.businessStatusName())) {
                c[2]++;
                return;
            }
            Optional<LatLng> ll = parseCoord(row);
            if (ll.isEmpty()) {
                c[3]++;
                return;
            }
            placeRepository.upsertPublicData(
                    row.managementId(), row.name(), blankToNull(row.category()),
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
            if (isBlank(row.coordX()) || isBlank(row.coordY())) return Optional.empty();
            return converter.toWgs84(Double.parseDouble(row.coordX()), Double.parseDouble(row.coordY()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
