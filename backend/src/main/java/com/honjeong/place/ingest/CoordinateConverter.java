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
