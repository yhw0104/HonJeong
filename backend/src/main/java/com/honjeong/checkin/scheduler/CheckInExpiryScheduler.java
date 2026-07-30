package com.honjeong.checkin.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.honjeong.checkin.service.CheckInService;

/**
 * 방치된 체크인(TTL 초과 ACTIVE·TOGETHER·SEEKING)을 주기적으로 자동 정리하는 스케줄러.
 *
 * <p>사용처: 직접 호출하는 클래스는 없다 — Spring {@code @Scheduled}가 주기 호출한다.
 * 로직은 {@link CheckInService}에 있고 여기서는 호출만 한다(얇게).
 *
 * <p>주기는 {@code honjeong.checkin.expiry-interval-ms}(기본 5분)다.
 */
@Component
public class CheckInExpiryScheduler {

    private final CheckInService checkInService;

    public CheckInExpiryScheduler(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /**
     * 고정 주기(기본 5분)마다 {@link CheckInService#expireStaleCheckIns()}를 호출해 방치 체크인을 만료시킨다.
     */
    @Scheduled(fixedDelayString = "${honjeong.checkin.expiry-interval-ms:300000}")
    public void expire() {
        checkInService.expireStaleCheckIns();
    }
}
