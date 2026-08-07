package com.honjeong.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p><b>큐가 가득 차면 버리고 로그를 남긴다.</b> 원래는 {@code CallerRunsPolicy}였다 —
 * 호출 스레드에서 대신 실행하는 정책인데, 여기서 호출 스레드는 {@code afterCommit} 훅이 도는
 * <b>요청 스레드</b>다. 즉 큐가 찬 순간부터 사용자 요청이 FCM 응답을 기다리게 되고, DB 커넥션과
 * 톰캣 스레드를 그동안 붙잡는다. 이미 밀려 있을 때 요청까지 느려지는 건 상황을 악화시킬 뿐이다.
 *
 * <p>푸시는 배달이 보장되지 않는 채널이고 폴링이 안전망으로 남아 있다(docs/08-실시간-전략.md §8).
 * <b>요청을 느리게 만드는 것보다 버리고 로그를 남기는 편이 정직하다</b> — 버리면 WARN 한 줄로
 * 드러나지만, 대신 실행하면 "가끔 API가 느리다"라는 훨씬 진단하기 어려운 증상으로만 보인다.
 *
 * <p>{@code AbortPolicy}(기본값)를 쓰지 않는 이유: 거부를 예외로 알리면 그 예외가
 * {@code afterCommit} 훅 밖으로 나가 <b>이미 커밋된 요청</b>을 실패로 만든다. 버리되 던지지 않는다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

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
        executor.setRejectedExecutionHandler(dropAndLog());
        executor.initialize();
        return executor;
    }

    /**
     * 큐가 가득 찼을 때의 정책 — 버리고 WARN을 남긴다. 예외를 던지지 않는다.
     *
     * <p>패키지 밖에 공개하지 않는다. 테스트가 이 정책을 직접 불러 "실행하지 않고 던지지도 않는다"를
     * 확인한다.
     *
     * @return 버리고 로그만 남기는 거부 처리기
     */
    static RejectedExecutionHandler dropAndLog() {
        return (task, executor) -> log.warn(
                "[push] 발송 큐가 가득 차 이번 건을 버립니다 — queue={} active={} pool={}."
                        + " 푸시는 배달 보장이 없는 채널이고 폴링이 안전망으로 남아 있습니다.",
                executor.getQueue().size(), executor.getActiveCount(), executor.getPoolSize());
    }
}
