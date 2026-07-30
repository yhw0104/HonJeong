package com.honjeong.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 {@code honjeong.checkin.*} 값을 바인딩하는 타입 안전 설정 프로퍼티 —
 * 체크인 TTL·만료 스케줄러 주기.
 *
 * <p>사용처: CheckInService(ACTIVE/TOGETHER 만료 판정·스케줄러 주기). 바인딩 활성화는
 * AppConfig의 {@code @EnableConfigurationProperties}가 담당한다.
 *
 * <p>ttlHours 이상 방치된 ACTIVE는 스케줄러가 ENDED로 만료시킨다.
 *
 * @param ttlHours         ACTIVE 유효시간(시간, yml 키 {@code ttl-hours}). 기본 3.
 * @param expiryIntervalMs 만료 스케줄러 주기(ms, yml 키 {@code expiry-interval-ms}). 기본 300000(5분).
 * @param togetherTtlHours TOGETHER 유효시간(시간, matched_at 기준, yml 키 {@code together-ttl-hours}). 기본 3.
 * @param seekingTtlHours  SEEKING 유효시간(시간, startedAt 기준, yml 키 {@code seeking-ttl-hours}). 기본 3.
 */
@ConfigurationProperties(prefix = "honjeong.checkin")
public record HonjeongCheckInProperties(Integer ttlHours, Long expiryIntervalMs, Integer togetherTtlHours,
        Integer seekingTtlHours) {

    /**
     * 컴팩트 생성자 — yml에 키가 없어 null로 들어온 값을 기본값(ttl 3시간, 주기 5분, together 3시간,
     * seeking 3시간)으로 보정한다.
     */
    public HonjeongCheckInProperties {
        if (ttlHours == null) {
            ttlHours = 3;
        }
        if (expiryIntervalMs == null) {
            expiryIntervalMs = 300_000L;
        }
        if (togetherTtlHours == null) {
            togetherTtlHours = 3;
        }
        if (seekingTtlHours == null) {
            seekingTtlHours = 3;
        }
    }
}
