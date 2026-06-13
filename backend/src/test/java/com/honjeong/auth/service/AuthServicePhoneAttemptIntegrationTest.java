package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.honjeong.auth.domain.PhoneVerification;
import com.honjeong.auth.repository.PhoneVerificationRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.support.AbstractPostgresTest;

/**
 * {@link AuthService#verifyPhone} 의 인증번호 시도 카운트가 <b>실제 트랜잭션 경계에서</b> 어떻게 영속되는지
 * 검증하는 통합 테스트.
 *
 * <p>단위 테스트({@code AuthServiceTest})는 repository를 Mockito로 모킹해 트랜잭션·롤백이 없으므로, 검증 실패 시
 * {@code attempts} 증가가 롤백돼 DB에 남지 않는 문제를 잡아낼 수 없다. 이 테스트는 Testcontainers Postgres와
 * 실제 스프링 트랜잭션을 사용해, <b>인증번호가 틀려 예외가 나가도 시도 횟수는 DB에 누적</b>되는지를 확인한다
 * (무차별 대입 방어 rate-limit의 근거 데이터).
 */
@SpringBootTest
class AuthServicePhoneAttemptIntegrationTest extends AbstractPostgresTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PhoneVerificationRepository phoneVerificationRepository;

    @Test
    @DisplayName("verifyPhone: 코드가 틀려 예외가 나도 시도 횟수(attempts)는 DB에 누적된다")
    void verifyPhone_mismatch_persistsAttempt() {
        // given: 유효한(미만료) 인증 발송 이력 1건 — 코드는 000000
        String phone = "01099990001";
        PhoneVerification saved = phoneVerificationRepository.save(
                PhoneVerification.issue(phone, "000000", LocalDateTime.now().plusMinutes(3)));

        // when: 틀린 코드로 검증 → 불일치 예외(트랜잭션 롤백 유발)
        assertThatThrownBy(() -> authService.verifyPhone(phone, "111111"))
                .isInstanceOf(BusinessException.class);

        // then: 실패해도 attempts는 1로 DB에 남아야 한다(바깥 트랜잭션 롤백과 무관하게 커밋되어야 함)
        PhoneVerification reloaded = phoneVerificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("verifyPhone: 인증 성공 시에도 누적된 시도 횟수(attempts)가 덮어써지지 않는다")
    void verifyPhone_success_keepsAttempt() {
        // given: 유효한(미만료) 인증 발송 이력 1건 — 신규 번호라 검증 성공 시 온보딩 분기를 탄다
        String phone = "01099990002";
        PhoneVerification saved = phoneVerificationRepository.save(
                PhoneVerification.issue(phone, "000000", LocalDateTime.now().plusMinutes(3)));

        // when: 정확한 코드로 검증 성공 → verified=true 가 바깥 트랜잭션에서 커밋된다
        authService.verifyPhone(phone, "000000");

        // then: 별도 트랜잭션이 올린 attempts(=1)가 성공 커밋(verified=true)에 덮어써지지 않아야 한다
        PhoneVerification reloaded = phoneVerificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAttempts()).isEqualTo(1);
        assertThat(reloaded.isVerified()).isTrue();
    }
}
