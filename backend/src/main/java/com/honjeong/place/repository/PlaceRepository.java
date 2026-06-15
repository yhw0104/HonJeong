package com.honjeong.place.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
