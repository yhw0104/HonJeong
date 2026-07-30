package com.honjeong.global.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 애플리케이션 공통 빈 등록 설정 — 전역 Clock 빈 제공, 스케줄링 활성화, 체크인 프로퍼티 바인딩 활성화.
 *
 * <p>사용처: 스프링 컨테이너가 자동 적용한다(직접 참조 없음). Clock 빈은 TokenService·AuthService·
 * CheckInService·MealRequestService 등 시간에 의존하는 서비스가 주입받아, 테스트에서 시각을 고정할 수 있게 한다.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(HonjeongCheckInProperties.class)
public class AppConfig {

    /**
     * 애플리케이션 전역에서 "현재 시각"의 단일 출처로 쓰는 {@link Clock} 빈을 등록한다.
     *
     * <p>시간에 의존하는 로직(만료 계산 등)이 이 빈을 주입받으면, 테스트에서 고정된 Clock으로 교체해
     * 시각을 못박을 수 있어 결과가 결정적이 된다.
     *
     * @return 시스템 기본 시간대를 사용하는 Clock
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
