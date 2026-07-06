package com.honjeong.global.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 1. 기능: 애플리케이션 공통 빈 등록 설정 — 전역 Clock 빈 제공, 스케줄링(@EnableScheduling) 활성화, 체크인 프로퍼티(HonjeongCheckInProperties) 바인딩 활성화
 * 2. 사용처: 스프링 컨테이너가 자동 적용(직접 참조 없음) — Clock 빈은 TokenService·AuthService·CheckInService·MealRequestService 등 시간 의존 서비스가 주입받음
 *
 * <p>[기존 주석] 애플리케이션 공통 빈. 시간은 Clock 빈으로 주입해 테스트에서 고정 가능하게 한다.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(HonjeongCheckInProperties.class)
public class AppConfig {

    /**
     * 기능: 애플리케이션 전역 "현재 시각"의 단일 출처가 되는 Clock 빈을 등록한다
     * Request: 없음
     * Response: Clock — 시스템 기본 시간대의 Clock(테스트에서 고정 Clock으로 교체 가능)
     *
     * <p>[기존 주석] 애플리케이션 전역에서 "현재 시각"의 단일 출처로 쓰는 {@link Clock} 빈.
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
