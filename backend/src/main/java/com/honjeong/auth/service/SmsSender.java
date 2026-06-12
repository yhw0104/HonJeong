package com.honjeong.auth.service;

/**
 * 인증번호 SMS 발송의 책임을 정의하는 인터페이스. 휴대폰 번호로 인증번호를 보내는 일만 담당하며,
 * 실제 발송 수단(외부 SMS 게이트웨이 등)은 구현에 맡긴다. 개발용 {@link MockSmsSender}는 발송 대신
 * 로그만 남기고, 실 운영용 구현은 실제 SMS를 보낸다.
 */
public interface SmsSender {

    /**
     * 주어진 번호로 인증번호를 발송한다.
     *
     * @param phone 수신 휴대폰 번호
     * @param code  보낼 인증번호
     */
    void send(String phone, String code);
}
