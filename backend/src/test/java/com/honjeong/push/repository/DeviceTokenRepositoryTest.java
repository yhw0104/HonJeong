package com.honjeong.push.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.global.config.JpaConfig;
import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.domain.Platform;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * DeviceTokenRepository 슬라이스 테스트.
 *
 * <p>검증 목적: (1) token UNIQUE 제약이 실제로 걸려 서비스가 UPSERT를 해야 한다는 전제가 성립하는지,
 * (2) 주인 갱신({@code reassignTo})이 DB까지 반영되는지, (3) 사용자별 벌크 삭제가 남의 토큰을
 * 건드리지 않는지를 본다.
 *
 * <p>구성: @DataJpaTest 슬라이스 + @AutoConfigureTestDatabase(replace=NONE)으로 내장 DB 대체를 끄고
 * 실제 Postgres(AbstractPostgresTest의 Testcontainers)에 붙는다. created_at/updated_at 감사 주입을
 * 위해 JpaConfig를 @Import 한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@DisplayName("DeviceTokenRepository")
class DeviceTokenRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private UserRepository userRepository;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 7, 12, 0);

    @Test
    @DisplayName("같은 토큰을 두 사용자가 등록하면 UNIQUE 제약에 걸린다 — 그래서 서비스가 UPSERT를 해야 한다")
    void 토큰은_유니크하다() {
        User a = userRepository.save(newUser("01011110001"));
        User b = userRepository.save(newUser("01011110002"));
        deviceTokenRepository.saveAndFlush(DeviceToken.of(a, "tok-dup", Platform.IOS, NOW));

        assertThatThrownBy(() ->
                deviceTokenRepository.saveAndFlush(DeviceToken.of(b, "tok-dup", Platform.IOS, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("reassignTo는 토큰의 주인을 바꾸고 사용 시각을 갱신한다")
    void 주인을_바꾼다() {
        User a = userRepository.save(newUser("01011110003"));
        User b = userRepository.save(newUser("01011110004"));
        DeviceToken t = deviceTokenRepository.saveAndFlush(DeviceToken.of(a, "tok-move", Platform.IOS, NOW));

        t.reassignTo(b, NOW.plusDays(1));
        deviceTokenRepository.flush();

        DeviceToken found = deviceTokenRepository.findByToken("tok-move").orElseThrow();
        assertThat(found.getUser().getId()).isEqualTo(b.getId());
        assertThat(found.getLastUsedAt()).isEqualTo(NOW.plusDays(1));
    }

    @Test
    @DisplayName("deleteAllByUser_Id는 그 사용자의 토큰만 지운다")
    void 사용자_토큰만_지운다() {
        User a = userRepository.save(newUser("01011110005"));
        User b = userRepository.save(newUser("01011110006"));
        deviceTokenRepository.save(DeviceToken.of(a, "tok-a1", Platform.IOS, NOW));
        deviceTokenRepository.save(DeviceToken.of(a, "tok-a2", Platform.IOS, NOW));
        deviceTokenRepository.saveAndFlush(DeviceToken.of(b, "tok-b1", Platform.IOS, NOW));

        int deleted = deviceTokenRepository.deleteAllByUser_Id(a.getId());

        assertThat(deleted).isEqualTo(2);
        assertThat(deviceTokenRepository.findAllByUser_Id(a.getId())).isEmpty();
        assertThat(deviceTokenRepository.findAllByUser_Id(b.getId())).hasSize(1);
    }

    /**
     * 테스트용 PENDING 회원. 다른 리포지토리 슬라이스 테스트와 같은 방식이다(User.pending).
     *
     * <p>전화번호는 테스트마다 다른 값을 넘긴다 — users.phone이 UNIQUE라 같은 번호를 재사용하면
     * 검증하려는 것과 무관한 제약 위반이 난다.
     *
     * @param phone 이 테스트 전용 휴대폰 번호
     * @return 아직 저장되지 않은 PENDING 회원
     */
    private User newUser(String phone) {
        return User.pending(phone, null);
    }
}
