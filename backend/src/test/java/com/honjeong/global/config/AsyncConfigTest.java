package com.honjeong.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 푸시 executor의 <b>포화 정책</b>만 보는 단위 테스트.
 *
 * <p>큐 500이 실제로 차게 만들어 확인하면 느리고 잘 깨지는 테스트가 된다. 정책 자체는 순수
 * 함수라 직접 불러 보는 편이 정확하다 — 두 가지를 단언한다.
 *
 * <ol>
 *   <li><b>실행하지 않는다</b> — {@code CallerRunsPolicy}로 되돌아가면 여기서 잡힌다.
 *       그 정책은 요청 스레드({@code afterCommit} 훅)에서 FCM 호출을 대신 돌린다.</li>
 *   <li><b>던지지 않는다</b> — {@code AbortPolicy}면 그 예외가 커밋 후 훅 밖으로 나가
 *       이미 커밋된 요청을 실패로 만든다.</li>
 * </ol>
 */
@DisplayName("AsyncConfig — 푸시 큐 포화 정책")
class AsyncConfigTest {

    @Test
    @DisplayName("큐가 가득 차면 발송을 버린다 — 호출 스레드에서 대신 실행하지 않는다")
    void 포화되면_버린다() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().pushExecutor();
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        AtomicBoolean ran = new AtomicBoolean(false);

        try {
            assertThatCode(() -> pool.getRejectedExecutionHandler()
                    .rejectedExecution(() -> ran.set(true), pool))
                    .doesNotThrowAnyException();

            assertThat(ran).isFalse();
        } finally {
            executor.shutdown();
        }
    }
}
