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
