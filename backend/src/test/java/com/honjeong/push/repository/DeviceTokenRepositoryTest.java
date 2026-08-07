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

import jakarta.persistence.EntityManager;

/**
 * DeviceTokenRepository 슬라이스 테스트.
 *
 * <p>검증 목적: (1) token UNIQUE 제약이 실제로 걸려 서비스가 UPSERT를 해야 한다는 전제가 성립하는지,
 * (2) 네이티브 UPSERT({@code ON CONFLICT})가 주인·플랫폼·사용 시각을 DB까지 갱신하고 같은 토큰을
 * 연달아 등록해도 터지지 않는지, (3) 사용자별 벌크 삭제가 남의 토큰을 건드리지 않는지를 본다.
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
    @Autowired
    private EntityManager entityManager;

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
    @DisplayName("upsert는 처음 보는 토큰을 새로 넣는다")
    void 신규_토큰을_넣는다() {
        User a = userRepository.save(newUser("01011110003"));

        int affected = deviceTokenRepository.upsert(a.getId(), "tok-new", Platform.IOS.name(), NOW);

        assertThat(affected).isEqualTo(1);
        DeviceToken found = deviceTokenRepository.findByToken("tok-new").orElseThrow();
        assertThat(found.getUser().getId()).isEqualTo(a.getId());
        assertThat(found.getPlatform()).isEqualTo(Platform.IOS);
        assertThat(found.getLastUsedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("upsert는 이미 있는 토큰의 주인·플랫폼·사용 시각을 갱신한다 — 행이 늘지 않는다")
    void 기존_토큰을_갱신한다() {
        User a = userRepository.save(newUser("01011110004"));
        User b = userRepository.save(newUser("01011110007"));
        deviceTokenRepository.saveAndFlush(DeviceToken.of(a, "tok-move", Platform.IOS, NOW));
        entityManager.clear(); // 네이티브 UPDATE는 1차 캐시를 갱신하지 않는다 — 비워야 DB 값을 읽는다

        deviceTokenRepository.upsert(b.getId(), "tok-move", Platform.ANDROID.name(), NOW.plusDays(1));
        entityManager.clear();

        DeviceToken found = deviceTokenRepository.findByToken("tok-move").orElseThrow();
        assertThat(found.getUser().getId()).isEqualTo(b.getId());
        assertThat(found.getPlatform()).isEqualTo(Platform.ANDROID);
        assertThat(found.getLastUsedAt()).isEqualTo(NOW.plusDays(1));
        assertThat(deviceTokenRepository.findAllByUser_Id(a.getId())).isEmpty();
    }

    @Test
    @DisplayName("같은 토큰을 연달아 등록해도 예외가 나지 않는다 — 앱 시작 시 등록·토큰갱신 경합")
    void 연달아_등록해도_터지지_않는다() {
        User a = userRepository.save(newUser("01011110008"));

        deviceTokenRepository.upsert(a.getId(), "tok-race", Platform.IOS.name(), NOW);
        deviceTokenRepository.upsert(a.getId(), "tok-race", Platform.IOS.name(), NOW.plusMinutes(1));

        assertThat(deviceTokenRepository.findAllByUser_Id(a.getId())).hasSize(1);
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

    @Test
    @DisplayName("deleteByTokenAndUserId는 주인이 맞을 때만 지운다 — 발송 도중 기기 주인이 바뀌는 경합")
    void 죽은_토큰은_주인이_맞을_때만_지운다() {
        // 발송은 조회 → HTTP → 기록 세 구간이라, 그 사이 upsert가 같은 토큰을 새 주인에게 넘길 수 있다.
        // 토큰 문자열만 보고 지우면 방금 등록한 새 주인의 행이 낡은 발송 결과 때문에 사라진다.
        User oldOwner = userRepository.save(newUser("01011110009"));
        User newOwner = userRepository.save(newUser("01011110010"));
        deviceTokenRepository.saveAndFlush(DeviceToken.of(oldOwner, "tok-handover", Platform.IOS, NOW));
        entityManager.clear();
        deviceTokenRepository.upsert(newOwner.getId(), "tok-handover", Platform.IOS.name(), NOW.plusMinutes(1));
        entityManager.clear();

        int deleted = deviceTokenRepository.deleteByTokenAndUserId("tok-handover", oldOwner.getId());

        assertThat(deleted).isZero();
        assertThat(deviceTokenRepository.findAllByUser_Id(newOwner.getId())).hasSize(1);
    }

    @Test
    @DisplayName("deleteByTokenAndUserId는 주인이 그대로면 지운다 — 정상 경로가 죽지 않았는지")
    void 주인이_그대로면_지운다() {
        User owner = userRepository.save(newUser("01011110011"));
        deviceTokenRepository.saveAndFlush(DeviceToken.of(owner, "tok-dead", Platform.IOS, NOW));

        int deleted = deviceTokenRepository.deleteByTokenAndUserId("tok-dead", owner.getId());

        assertThat(deleted).isEqualTo(1);
        assertThat(deviceTokenRepository.findByToken("tok-dead")).isEmpty();
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
