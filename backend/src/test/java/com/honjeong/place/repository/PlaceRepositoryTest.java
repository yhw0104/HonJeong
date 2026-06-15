package com.honjeong.place.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;

/**
 * PlaceRepository 슬라이스 테스트.
 *
 * <p>검증 목적: Place 엔티티의 JPA 매핑(places 테이블)과 파생 쿼리가 실제 DB에서 의도대로 동작하는지 확인한다.
 * 구체적으로 (1) 저장 시 PK·감사시각(createdAt/updatedAt)이 자동 주입되는지, (2) external_id 캐싱 키로
 * findByExternalId가 올바른 행을 찾고 없으면 비는지, (3) external_id UNIQUE 제약이 중복 INSERT를 막는지를 본다.
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

    @Test
    @DisplayName("장소를 저장하면 id·감사시각이 자동으로 채워진다")
    void savePopulatesIdAndAuditTimes() {
        // given: 카카오 검색 결과를 본뜬 장소를 / when: 저장하면
        Place saved = placeRepository.save(
                Place.of("kakao-1", "혼밥식당", "서울 중구 어딘가로", 37.5665, 126.9780, "한식"));

        // then: PK와 createdAt/updatedAt이 자동 주입된다
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByExternalId: external_id로 저장된 장소를 찾고, 없으면 빈 Optional을 반환한다")
    void findByExternalIdHitAndMiss() {
        // given: external_id "kakao-42"인 장소를 저장
        placeRepository.save(
                Place.of("kakao-42", "김밥천국", "서울 강남구 테헤란로", 37.4979, 127.0276, "분식"));

        // when & then: 같은 external_id는 찾히고(이름·좌표 일치), 없는 키는 비어 있다
        assertThat(placeRepository.findByExternalId("kakao-42"))
                .isPresent()
                .get()
                .satisfies(p -> {
                    assertThat(p.getName()).isEqualTo("김밥천국");
                    assertThat(p.getLatitude()).isEqualTo(37.4979);
                    assertThat(p.getLongitude()).isEqualTo(127.0276);
                });
        assertThat(placeRepository.findByExternalId("nope")).isEmpty();
    }

    @Test
    @DisplayName("external_id UNIQUE 제약: 같은 external_id로 두 번 저장하면 무결성 위반이 난다")
    void externalIdUniqueConstraint() {
        // given: external_id "dup"인 장소를 먼저 저장
        placeRepository.save(Place.of("dup", "가게A", "주소A", 37.5, 127.0, "한식"));

        // when & then: 같은 external_id로 또 저장(flush까지)하면 UNIQUE 위반으로 예외가 발생한다
        Place duplicate = Place.of("dup", "가게B", "주소B", 37.6, 127.1, "일식");
        assertThatThrownBy(() -> placeRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
