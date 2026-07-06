package com.honjeong.place.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.place.domain.Place;

/**
 * 1. 기능: 장소(식당) 마스터 데이터 접근 — 이름 검색·바운딩박스 조회 (대상 테이블: places)
 *
 * <p>[기존 주석] 장소 저장소. V3 이후 공공데이터 마스터만 관리한다.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 기능: 영업 중인 장소를 이름 부분일치(대소문자 무시)로 페이지 조회
     * 쿼리: SELECT * FROM places WHERE business_status = '영업' AND LOWER(name) LIKE LOWER('%' || :q || '%') ORDER BY name LIMIT :size OFFSET :page*:size — pg_trgm GIN 인덱스가 LIKE를 가속
     * Request: q — 검색어(trim 된 값), pageable — 페이지네이션(이름순 정렬) / Response: {@code Page<Place>} — 일치하는 영업 장소 페이지
     *
     * <p>[기존 주석] 영업 중인 장소를 이름 부분일치(대소문자 무시)로 페이지 조회한다. pg_trgm GIN 인덱스가 LIKE 검색을 가속한다.
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
     * 기능: 위경도 바운딩박스 안의 영업 중인 장소 전체 조회(주변 조회 1차 필터)
     * 쿼리: SELECT * FROM places WHERE business_status = '영업' AND latitude BETWEEN :latMin AND :latMax AND longitude BETWEEN :lngMin AND :lngMax
     * Request: latMin/latMax — 위도 하한·상한, lngMin/lngMax — 경도 하한·상한 / Response: {@code List<Place>} — 박스 내 영업 장소 목록
     *
     * <p>[기존 주석] 바운딩박스 안의 영업 중인 장소를 모두 조회한다. 원형 반경 보정은 서비스가 Haversine으로 수행한다.
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

}
