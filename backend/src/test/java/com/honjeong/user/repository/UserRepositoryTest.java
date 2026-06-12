package com.honjeong.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/**
 * UserRepository 슬라이스 테스트.
 *
 * <p>검증 목적: User 엔티티의 JPA 매핑과 파생 쿼리가 실제 DB에서 의도대로 동작하는지 확인한다.
 * 구체적으로 (1) 저장 시 PK·감사시각(createdAt)이 자동 주입되고 기본 status가 PENDING으로 들어가는지,
 * (2) findByPhone / existsByNickname 파생 쿼리가 올바른 행을 찾는지를 본다.
 *
 * <p>구성: @DataJpaTest로 JPA 관련 빈만 로드하는 슬라이스 테스트. @AutoConfigureTestDatabase(replace=NONE)으로
 * 내장 H2 대체를 끄고 실제 Postgres(AbstractPostgresTest의 Testcontainers)에 붙으며, 감사(Auditing) 동작을
 * 위해 JpaConfig를 @Import 한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class UserRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("PENDING 회원을 저장하면 id·감사시각이 채워지고 phone으로 조회된다")
    void saveAndFindByPhone() {
        // given: 휴대폰만 있는 PENDING 회원을 / when: 저장하면
        User saved = userRepository.save(User.pending("01012345678", null));

        // then: PK·createdAt이 자동 주입되고 기본 status가 PENDING이며 phone으로 다시 찾힌다
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStatus().name()).isEqualTo("PENDING");
        assertThat(userRepository.findByPhone("01012345678")).isPresent();
    }

    @Test
    @DisplayName("프로필 완료 회원의 닉네임 존재 여부를 확인한다")
    void existsByNickname() {
        // given: 닉네임 "혼밥러"로 프로필을 완료(ACTIVE 전환)한 회원을 저장
        User user = User.pending("01099998888", null);
        user.completeProfile("혼밥러", null, null, null, null, null, null, null, null);
        userRepository.save(user);

        // when & then: existsByNickname이 존재하는 닉네임엔 true, 없는 닉네임엔 false를 반환한다
        assertThat(userRepository.existsByNickname("혼밥러")).isTrue();
        assertThat(userRepository.existsByNickname("없는닉")).isFalse();
    }
}
