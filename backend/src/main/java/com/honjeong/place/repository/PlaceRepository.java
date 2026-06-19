package com.honjeong.place.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.place.domain.Place;

/**
 * 장소 캐시 저장소. Spring Data JPA가 메서드 이름으로 쿼리를 생성한다.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 카카오 place id(캐싱 키)로 캐시된 장소를 조회한다. upsert(있으면 재사용/없으면 생성)의 조회 단계에 쓰인다.
     *
     * @param externalId 카카오 place id
     * @return 캐시된 장소(없으면 빈 Optional)
     */
    Optional<Place> findByExternalId(String externalId);

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
