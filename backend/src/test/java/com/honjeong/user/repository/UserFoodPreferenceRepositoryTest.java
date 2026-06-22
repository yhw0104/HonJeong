package com.honjeong.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserFoodPreference;

/**
 * UserFoodPreferenceRepository 슬라이스 테스트. 실제 Postgres(Testcontainers)에서
 * (1) 엔티티↔user_food_preferences 매핑·감사시각 자동 주입, (2) user_id 단건 조회,
 * (3) UNIQUE(user_id) 위반을 검증한다(= V5 마이그레이션과 엔티티 매핑 정합 확인).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class UserFoodPreferenceRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private UserFoodPreferenceRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("저장하면 id·감사시각이 채워지고 user_id로 음식이 조회된다")
    void saveAndFindByUserId() {
        User user = userRepository.save(User.pending("01011112222", null));

        UserFoodPreference saved = repository.save(
                UserFoodPreference.of(user.getId(), List.of("한식", "일식")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(repository.findByUserId(user.getId())).isPresent()
                .get().extracting(UserFoodPreference::toFoods)
                .isEqualTo(List.of("한식", "일식"));
    }

    @Test
    @DisplayName("같은 user_id로 두 행을 저장하면 UNIQUE 제약에 걸린다")
    void uniqueUserId() {
        User user = userRepository.save(User.pending("01033334444", null));
        repository.saveAndFlush(UserFoodPreference.of(user.getId(), List.of("한식")));

        assertThatThrownBy(() ->
                repository.saveAndFlush(UserFoodPreference.of(user.getId(), List.of("일식"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
