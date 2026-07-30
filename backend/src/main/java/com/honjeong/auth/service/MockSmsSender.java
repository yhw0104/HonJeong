package com.honjeong.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발용 Mock SMS 발송기 — 실제 발송 대신 인증번호를 로그로만 출력한다.
 *
 * <p>사용처: AuthService(sendPhoneCode) — SmsSender 구현체로 주입된다.
 *
 * <p>실제로 SMS를 보내지 않고 인증번호를 애플리케이션 로그로만 출력한다. 덕분에 개발 중에는
 * 문자 비용·외부 연동 없이 콘솔 로그에서 인증번호를 확인해 가입 흐름을 테스트할 수 있다.
 *
 * <p>{@code @ConditionalOnProperty(..., matchIfMissing = true)}: 설정 {@code honjeong.sms.mode}가 "mock"이거나
 * <b>지정되지 않았을 때</b>(기본) 이 빈이 등록된다. 실 연동은 {@code honjeong.sms.mode=real}로 교체한다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.sms.mode", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    /**
     * 실제 발송 대신 번호와 인증번호를 로그로 남긴다.
     *
     * @param phone 수신 번호(로그 출력용)
     * @param code  인증번호(로그 출력용)
     */
    @Override
    public void send(String phone, String code) {
        log.info("[MockSMS] {} → 인증번호 {}", phone, code); // 실제 SMS 대신 로그만 출력
    }
}
