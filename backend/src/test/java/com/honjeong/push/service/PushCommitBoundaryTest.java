package com.honjeong.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.domain.Platform;
import com.honjeong.push.domain.PushType;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 커밋 후 발송 경계 검증 — 이 기능에서 가장 중요한 계약을 지키는 테스트다.
 *
 * <p>검증 목적: 도메인 트랜잭션이 <b>롤백되면 푸시가 나가지 않는다.</b> 롤백된 신청에 대한
 * 배너가 상대 폰에 뜨면 사용자에게는 되돌릴 방법이 없다.
 *
 * <p><b>왜 Mockito 단위 테스트로 쓰지 않았나.</b> 커밋·롤백은 실제 트랜잭션이 있어야만
 * 존재한다. 리포지토리를 전부 목으로 잡으면 검증 대상 코드가 애초에 실행되지 않는다 —
 * 07-28에 {@code @Modifying(clearAutomatically)} 버그가 570개 그린인 채로 운영에 나갈 뻔한
 * 사고가 정확히 그 구조였다. 그래서 실 Postgres {@code @SpringBootTest}로 돈다.
 *
 * <p><b>클래스에 {@code @Transactional}을 붙이지 않는다.</b> 붙이면 테스트가 끝날 때 전부
 * 롤백돼 {@code afterCommit} 훅이 단 한 번도 돌지 않고, "커밋되면 발송된다" 쪽이 영영 빨간불이 된다.
 * 대신 픽스처 값을 실행마다 다르게 만들어 공유 컨테이너를 오염 없이 커밋으로 검증한다
 * (AccountWithdrawalIntegrationTest의 재가입 테스트군과 같은 관례).
 *
 * <p><b>기기 토큰을 반드시 심는다.</b> {@link PushSendTask}는 토큰이 0건이면 조기 반환하므로,
 * 토큰 없이 쓰면 "발송 안 됨"이 언제나 참인 가짜 테스트가 된다. {@link #setUp}에서 심은 뒤
 * 사전 단언으로 존재를 확인한다.
 */
@SpringBootTest
@DisplayName("푸시는 커밋된 뒤에만 나간다")
class PushCommitBoundaryTest extends AbstractPostgresTest {

    /** 롤백 검증에서 "아직 안 왔을 뿐"과 "영영 안 온다"를 구분하기 위해 기다리는 시간(ms). */
    private static final long SETTLE_MILLIS = 1500L;
    /** 커밋 검증에서 비동기 스레드를 기다리는 최대 시간(ms). */
    private static final long AWAIT_MILLIS = 5000L;

    private static final AtomicLong FIXTURE_SEQ = new AtomicLong();

    @MockitoBean
    private PushSender pushSender;

    @Autowired
    private BoundaryProbe probe;
    @Autowired
    private DeviceTokenRepository deviceTokenRepository;
    @Autowired
    private UserRepository userRepository;

    private Long recipientId;

    @BeforeEach
    void setUp() {
        User recipient = userRepository.save(User.pending(freshPhone(), null));
        recipientId = recipient.getId();
        deviceTokenRepository.saveAndFlush(DeviceToken.of(
                recipient, freshToken(), Platform.IOS, LocalDateTime.now()));

        // 사전 단언: 토큰이 없으면 PushSendTask가 조기 반환해 롤백 테스트가 가짜로 통과한다.
        assertThat(deviceTokenRepository.findAllByUser_Id(recipientId)).hasSize(1);
    }

    @Test
    @DisplayName("트랜잭션이 커밋되면 발송된다")
    void 커밋되면_발송된다() {
        probe.publishThenCommit(recipientId);

        verify(pushSender, timeout(AWAIT_MILLIS)).send(anyList(), any());
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 발송되지 않는다 — 없던 신청에 대한 푸시가 나가면 안 된다")
    void 롤백되면_발송되지_않는다() {
        assertThatThrownBy(() -> probe.publishThenFail(recipientId))
                .isInstanceOf(IllegalStateException.class);

        // never()만 쓰면 비동기 스레드보다 단언이 먼저 도는 경합으로 통과할 수 있다.
        // after(...)로 일정 시간을 실제로 기다린 뒤 "그동안 한 번도 안 왔다"를 확인한다.
        verify(pushSender, after(SETTLE_MILLIS).never()).send(anyList(), any());
    }

    private static String freshPhone() {
        // 실행마다 달라지는 번호. 0103 대역을 쓰는 이유는 다른 커밋형 통합 테스트와 겹치지 않기 위해서다
        // (0102=AccountWithdrawalIntegrationTest · 0107777x=E2E · 0109999x=AuthServicePhoneAttempt).
        long ms = System.currentTimeMillis() % 10_000L;
        long seq = FIXTURE_SEQ.incrementAndGet() % 1000L;
        return String.format("0103%04d%03d", ms, seq);
    }

    private static String freshToken() {
        // device_tokens.token은 UNIQUE다 — 고정 문자열을 쓰면 재실행 때 검증과 무관한 제약 위반이 난다.
        return "tok-boundary-" + System.nanoTime() + "-" + FIXTURE_SEQ.incrementAndGet();
    }

    @TestConfiguration
    static class Config {
        @Bean
        BoundaryProbe boundaryProbe(PushDispatcher dispatcher) {
            return new BoundaryProbe(dispatcher);
        }
    }

    /** 도메인 서비스 흉내 — 트랜잭션 안에서 예약하고, 성공/실패를 각각 만든다. */
    public static class BoundaryProbe {
        private final PushDispatcher dispatcher;

        public BoundaryProbe(PushDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        /** 예약 후 정상 커밋 — 커밋 훅이 돌아야 한다. */
        @Transactional
        public void publishThenCommit(Long recipientId) {
            dispatcher.dispatch(recipientId, PushType.MATE_REQUEST_RECEIVED, null, null, null);
        }

        /** 예약 후 도메인 실패 — 롤백되므로 커밋 훅이 돌면 안 된다. */
        @Transactional
        public void publishThenFail(Long recipientId) {
            dispatcher.dispatch(recipientId, PushType.MATE_REQUEST_RECEIVED, null, null, null);
            throw new IllegalStateException("도메인 실패");
        }
    }
}
