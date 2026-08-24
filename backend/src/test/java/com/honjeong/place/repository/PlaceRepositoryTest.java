package com.honjeong.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;

/**
 * PlaceRepository 슬라이스 테스트.
 *
 * <p>검증 목적: Place 엔티티의 JPA 매핑(places 테이블)과 파생 쿼리가 실제 DB에서 의도대로 동작하는지 확인한다.
 * V3 이후 external_id 컬럼이 제거됐으므로 공공데이터 팩토리({@link Place#ofPublicData})만 사용한다.
 *
 * <p>구성: @DataJpaTest로 JPA 관련 빈만 로드하는 슬라이스 테스트. @AutoConfigureTestDatabase(replace=NONE)으로
 * 내장 DB 대체를 끄고 실제 Postgres(AbstractPostgresTest의 Testcontainers)에 붙으며, 감사(Auditing) 동작을
 * 위해 JpaConfig를 @Import 한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class PlaceRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("공공데이터 식당을 저장하고 id로 조회한다")
    void saveAndFind() {
        // given
        Place p = Place.ofPublicData("MGMT-1", "혼밥식당", "한식", "서울 지번", "서울 도로명",
                37.5, 127.0, "02-111", "영업");
        em.persistAndFlush(p);
        em.clear();

        // when
        Place found = placeRepository.findById(p.getId()).orElseThrow();

        // then
        assertThat(found.getSourceId()).isEqualTo("MGMT-1");
        assertThat(found.getRoadAddress()).isEqualTo("서울 도로명");
        assertThat(found.getBusinessStatus()).isEqualTo("영업");
    }

    @Test
    @DisplayName("장소를 저장하면 id·감사시각이 자동으로 채워진다")
    void savePopulatesIdAndAuditTimes() {
        // given / when: 공공데이터 장소를 저장하면
        Place saved = placeRepository.save(
                Place.ofPublicData("AUDIT-1", "혼밥식당", "한식", "서울 중구", "서울 도로명",
                        37.5665, 126.9780, null, "영업"));

        // then: PK와 createdAt/updatedAt이 자동 주입된다
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("searchOpenByName: 이름 부분일치 + 영업 중인 장소만 반환한다")
    void searchOpenByName() {
        // given: 영업 중인 장소와 폐업 장소를 각각 저장
        em.persistAndFlush(Place.ofPublicData("SRCH-1", "혼밥김밥천국", "분식", "서울 중구", "서울 도로명",
                37.5665, 126.9780, "02-111", "영업"));
        em.persistAndFlush(Place.ofPublicData("SRCH-2", "김밥나라(폐업)", "분식", "서울 중구", "서울 도로명",
                37.5666, 126.9781, "02-222", "폐업"));
        em.clear();

        // when: "김밥"으로 검색하면
        java.util.List<Place> result =
                placeRepository.searchOpenByName("김밥",
                        org.springframework.data.domain.PageRequest.of(0, 10));

        // then: 영업 중인 장소만 반환된다(폐업은 제외)
        assertThat(result)
                .hasSize(1)
                .allSatisfy(p -> {
                    assertThat(p.getName()).contains("김밥");
                    assertThat(p.getBusinessStatus()).isEqualTo("영업");
                });
    }

    @Test
    @DisplayName("searchOpenByName: 대소문자를 무시하고 검색한다")
    void searchOpenByNameCaseInsensitive() {
        // given
        em.persistAndFlush(Place.ofPublicData("SRCH-3", "Tokyo Ramen", "일식", "서울", "서울 도로명",
                37.5, 127.0, null, "영업"));
        em.clear();

        // when & then: 소문자로 검색해도 찾힌다
        java.util.List<Place> result =
                placeRepository.searchOpenByName("tokyo",
                        org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Tokyo Ramen");
    }
}
