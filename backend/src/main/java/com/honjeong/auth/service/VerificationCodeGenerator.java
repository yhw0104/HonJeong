package com.honjeong.auth.service;

/**
 * 1. 기능: 휴대폰 인증번호 생성의 책임을 정의하는 인터페이스
 * 2. 사용처: AuthService(sendPhoneCode) — 구현체: 개발용 FixedVerificationCodeGenerator(운영 real 구현은 추후 교체)
 *
 * <p>[기존 주석] 휴대폰 인증번호 생성의 책임을 정의하는 인터페이스. 발송할 인증번호 문자열을 만들어 준다.
 * 생성 방식은 구현에 따라 다르다 — 개발용 {@link FixedVerificationCodeGenerator}는 항상 고정 코드
 * ("000000")를, 실 운영용 구현은 매번 랜덤 6자리를 만든다.
 */
public interface VerificationCodeGenerator {

    /**
     * 기능: 인증번호 한 개 생성
     * Request: 없음
     * Response: String — 발송에 쓸 인증번호 문자열
     *
     * <p>[기존 주석] 인증번호를 한 개 생성한다.
     *
     * @return 발송에 쓸 인증번호 문자열
     */
    String generate();
}
