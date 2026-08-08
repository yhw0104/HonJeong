package com.honjeong.push.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.honjeong.push.service.DeviceTokenService;

/**
 * staleness window를 벗어난 기기 토큰을 주기적으로 청소하는 스케줄러.
 *
 * <p>사용처: 직접 호출하는 클래스는 없다 — Spring {@code @Scheduled}가 주기 호출한다.
 * 로직은 {@link DeviceTokenService#sweepStale()}에 있고 여기서는 호출만 한다(얇게 —
 * {@code CheckInExpiryScheduler}와 같은 방식).
 *
 * <p>주기는 {@code honjeong.push.sweep-interval-ms}(기본 24시간)다. 청소 대상이 "60일 넘게
 * 재등록이 없는 토큰"이라 하루 단위면 충분하고, 그 사이 간극은 발송 구간의 window 조건
 * ({@code PushAudienceReader})이 메운다.
 */
@Component
public class DeviceTokenSweepScheduler {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenSweepScheduler(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    /** 고정 주기(기본 24시간)마다 {@link DeviceTokenService#sweepStale()}을 호출한다. */
    @Scheduled(fixedDelayString = "${honjeong.push.sweep-interval-ms:86400000}")
    public void sweep() {
        deviceTokenService.sweepStale();
    }
}
