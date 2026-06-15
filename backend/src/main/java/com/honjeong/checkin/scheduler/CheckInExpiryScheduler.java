package com.honjeong.checkin.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.honjeong.checkin.service.CheckInService;

/**
 * 방치된 ACTIVE 체크인을 주기적으로 만료시키는 스케줄러. 로직은 {@link CheckInService}에 있고 여기서는 호출만 한다(얇게).
 * 주기는 {@code honjeong.checkin.expiry-interval-ms}(기본 5분)다.
 */
@Component
public class CheckInExpiryScheduler {

    private final CheckInService checkInService;

    public CheckInExpiryScheduler(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /** 고정 주기로 방치된 ACTIVE 체크인을 만료시킨다. */
    @Scheduled(fixedDelayString = "${honjeong.checkin.expiry-interval-ms:300000}")
    public void expire() {
        checkInService.expireStaleCheckIns();
    }
}
