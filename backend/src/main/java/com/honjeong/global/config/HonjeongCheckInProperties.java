package com.honjeong.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 체크인 설정(honjeong.checkin.*). ttlHours 이상 방치된 ACTIVE는 스케줄러가 ENDED로 만료시킨다.
 *
 * @param ttlHours         ACTIVE 유효시간(시간, yml 키 {@code ttl-hours}). 기본 3.
 * @param expiryIntervalMs 만료 스케줄러 주기(ms, yml 키 {@code expiry-interval-ms}). 기본 300000(5분).
 */
@ConfigurationProperties(prefix = "honjeong.checkin")
public record HonjeongCheckInProperties(Integer ttlHours, Long expiryIntervalMs) {

    public HonjeongCheckInProperties {
        if (ttlHours == null) {
            ttlHours = 3;
        }
        if (expiryIntervalMs == null) {
            expiryIntervalMs = 300_000L;
        }
    }
}
