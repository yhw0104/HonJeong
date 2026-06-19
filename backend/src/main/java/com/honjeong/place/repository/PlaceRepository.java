package com.honjeong.place.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.place.domain.Place;

/**
 * 장소 저장소. V3 이후 공공데이터 마스터만 관리한다.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 영업 중인 장소를 이름 부분일치(대소문자 무시)로 페이지 조회한다. pg_trgm GIN 인덱스가 LIKE 검색을 가속한다.
     *
     * @param q        검색어(trim 된 값)
     * @param pageable 페이지네이션 파라미터
     * @return 일치하는 영업 장소 페이지
     */
    @Query("""
            SELECT p FROM Place p
            WHERE p.businessStatus = '영업'
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Place> searchOpenByName(@Param("q") String q, Pageable pageable);

    /**
     * 바운딩박스 안의 영업 중인 장소를 모두 조회한다. 원형 반경 보정은 서비스가 Haversine으로 수행한다.
     *
     * @param latMin 위도 하한
     * @param latMax 위도 상한
     * @param lngMin 경도 하한
     * @param lngMax 경도 상한
     * @return 박스 내 영업 중인 장소 목록
     */
    @Query("""
            SELECT p FROM Place p
            WHERE p.businessStatus = '영업'
              AND p.latitude BETWEEN :latMin AND :latMax
              AND p.longitude BETWEEN :lngMin AND :lngMax
            """)
    List<Place> findOpenWithinBounds(@Param("latMin") double latMin, @Param("latMax") double latMax,
            @Param("lngMin") double lngMin, @Param("lngMax") double lngMax);

    /**
     * 공공데이터 단건을 멱등 upsert한다. (source, source_id) 충돌 시 필드를 덮어쓴다.
     * {@code @Modifying} + {@code @Transactional}이 서비스 계층에 있으므로 여기에는 별도 선언 불필요.
     */
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
}
