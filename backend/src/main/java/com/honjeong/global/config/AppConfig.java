package com.honjeong.global.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 애플리케이션 공통 빈. 시간은 Clock 빈으로 주입해 테스트에서 고정 가능하게 한다. */
@Configuration
public class AppConfig {

    /**
     * 애플리케이션 전역에서 "현재 시각"의 단일 출처로 쓰는 {@link Clock} 빈.
     * 시간에 의존하는 로직(만료 계산 등)이 이 빈을 주입받으면, 테스트에서 고정된 Clock으로 교체해
     * 시각을 못박을 수 있어 결과가 결정적이 된다.
     *
     * @return 시스템 기본 시간대를 사용하는 Clock
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
