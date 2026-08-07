package com.honjeong.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 실행 설정. 현재 소비처는 푸시 발송(PushSendTask) 하나다.
 *
 * <p>푸시는 트랜잭션 커밋 후에 별도 스레드로 나간다 — 외부 HTTP가 DB 커넥션을 붙잡거나
 * 발송 실패가 도메인을 롤백시키면 안 되기 때문이다(06-13 rate-limit 롤백 사고와 같은 구조).
 *
 * <p>큐가 가득 차면 호출 스레드에서 실행한다({@code CallerRunsPolicy}). 버리는 것보다는
 * 느려지는 편이 낫고, 이미 커밋된 뒤라 도메인에는 영향이 없다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 푸시 발송 전용 스레드 풀. 빈 이름을 {@code @Async("pushExecutor")}로 지정해 쓴다.
     *
     * @return 푸시 발송용 executor
     */
    @Bean("pushExecutor")
    public Executor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("push-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
