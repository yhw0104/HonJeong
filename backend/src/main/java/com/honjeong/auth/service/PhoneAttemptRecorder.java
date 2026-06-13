package com.honjeong.auth.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.auth.domain.PhoneVerification;
import com.honjeong.auth.repository.PhoneVerificationRepository;

/**
 * 휴대폰 인증번호 시도 횟수(attempts)를 <b>독립 트랜잭션</b>으로 누적·커밋하는 컴포넌트.
 *
 * <p>왜 별도 빈·별도 트랜잭션인가: 시도 카운트는 무차별 대입 방어(rate-limit)의 근거 데이터라,
 * 인증번호가 틀려 {@code verifyPhone}이 예외를 던지고 그 트랜잭션이 롤백되더라도 <b>반드시 DB에 남아야</b> 한다.
 * {@code incrementAttempts()}를 {@code verifyPhone}의 트랜잭션 안에서 호출하면 불일치 throw 시 함께 롤백돼
 * 카운트가 누적되지 않는다(그러면 5회 잠금이 영원히 동작하지 않음).
 *
 * <p>그래서 이 메서드를 {@link Propagation#REQUIRES_NEW}로 둬, 호출하는 쪽 트랜잭션을 잠시 멈추고 <b>새 트랜잭션</b>에서
 * 증가분을 즉시 커밋한다. 바깥 트랜잭션이 이후 롤백돼도 이 커밋은 유지된다. (자기 자신 메서드 호출은 프록시를 타지 않아
 * 새 트랜잭션이 시작되지 않으므로, 반드시 별도 빈으로 분리해야 한다.)
 */
@Component
public class PhoneAttemptRecorder {

    private final PhoneVerificationRepository phoneVerificationRepository;

    public PhoneAttemptRecorder(PhoneVerificationRepository phoneVerificationRepository) {
        this.phoneVerificationRepository = phoneVerificationRepository;
    }

    /**
     * 주어진 발송 기록의 시도 횟수를 1 증가시켜 독립 트랜잭션으로 커밋한다.
     *
     * <p>대상을 id로 새로 조회해(바깥 트랜잭션의 영속성 컨텍스트와 분리된 새 세션) 증가시키므로, 변경은 이 트랜잭션
     * 커밋 시 flush 되어 바깥 롤백과 무관하게 영속된다. 기록이 없으면(이미 삭제 등) 조용히 무시한다.
     *
     * @param verificationId 시도 횟수를 누적할 {@link PhoneVerification}의 id
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long verificationId) {
        phoneVerificationRepository.findById(verificationId)
                .ifPresent(PhoneVerification::incrementAttempts); // dirty checking → 새 트랜잭션 커밋 시 flush
    }
}
