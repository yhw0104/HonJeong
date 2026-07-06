package com.honjeong.checkin.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.honjeong.checkin.service.CheckInService;

/**
 * 1. 기능: 방치된 체크인(TTL 초과 ACTIVE·TOGETHER)을 주기적으로 자동 만료(ENDED) 처리하는 스케줄러
 * 2. 사용처: (직접 사용하는 클래스 없음 — Spring @Scheduled가 주기 호출, 로직은 CheckInService에 위임)
 *
 * <p>[기존 주석] 방치된 ACTIVE 체크인을 주기적으로 만료시키는 스케줄러. 로직은 {@link CheckInService}에 있고 여기서는 호출만 한다(얇게).
 * 주기는 {@code honjeong.checkin.expiry-interval-ms}(기본 5분)다.
 */
@Component
public class CheckInExpiryScheduler {

    private final CheckInService checkInService;

    public CheckInExpiryScheduler(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /**
     * 기능: 고정 주기(기본 5분)마다 CheckInService.expireStaleCheckIns()를 호출해 방치 체크인을 만료시킨다
     * Request: 없음
     * Response: 없음(void)
     *
     * <p>[기존 주석] 고정 주기로 방치된 ACTIVE 체크인을 만료시킨다.
     */
    @Scheduled(fixedDelayString = "${honjeong.checkin.expiry-interval-ms:300000}")
    public void expire() {
        checkInService.expireStaleCheckIns();
    }
}
