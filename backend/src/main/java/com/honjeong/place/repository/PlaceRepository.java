package com.honjeong.place.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.place.domain.Place;

/**
 * 장소(식당) 마스터 데이터 접근 — 이름 검색·바운딩박스 조회. (대상 테이블: places)
 *
 * <p>V3 이후 공공데이터 마스터만 관리한다.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * 영업 중인 장소를 이름 부분일치(대소문자 무시)로 페이지 조회한다.
     *
     * <p>pg_trgm GIN 인덱스가 LIKE 검색을 가속한다.
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
     * 위경도 바운딩박스 안의 영업 중인 장소를 모두 조회한다(주변 조회의 1차 필터).
     *
     * <p>원형 반경 보정은 서비스가 Haversine으로 수행한다.
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
     * 이름이 일치하면서 바운딩박스 안에 있는 영업 중인 장소를 모두 조회한다(내 위치 기준 검색의 1차 필터).
     *
     * <p>원형 반경 보정과 거리 정렬은 서비스가 Haversine으로 수행한다({@code findOpenWithinBounds}와 같은 방식).
     *
     * <p>페이지네이션 없이 목록을 통째로 받는 이유: 거리 정렬을 DB가 못 하기 때문이다(위경도만 있고
     * PostGIS가 없다). 대신 두 조건이 모두 인덱스를 탄다 — 이름은 {@code idx_places_lower_name_trgm}
     * (GIN trigram), 좌표는 {@code idx_places_lat_lng}. 반경까지 걸려 결과 수는
     * {@code findOpenWithinBounds}(이름 조건이 없다)보다 언제나 작다.
     *
     * @param q      검색어(trim 된 값)
     * @param latMin 위도 하한
     * @param latMax 위도 상한
     * @param lngMin 경도 하한
     * @param lngMax 경도 상한
     * @return 박스 내 이름이 일치하는 영업 중인 장소 목록
     */
    @Query("""
            SELECT p FROM Place p
            WHERE p.businessStatus = '영업'
              AND LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
              AND p.latitude BETWEEN :latMin AND :latMax
              AND p.longitude BETWEEN :lngMin AND :lngMax
            """)
    List<Place> searchOpenByNameWithinBounds(@Param("q") String q,
            @Param("latMin") double latMin, @Param("latMax") double latMax,
            @Param("lngMin") double lngMin, @Param("lngMax") double lngMax);

}
