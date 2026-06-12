package com.honjeong.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발용 Mock 구현. 랜덤 대신 항상 고정 인증번호 "000000"을 발급한다. 인증번호가 매번 같으므로,
 * 개발자가 SMS를 받지 않고도 curl이나 수동 테스트에서 "000000"을 입력해 가입 흐름을 끝까지 진행할 수 있다.
 *
 * <p>{@code @ConditionalOnProperty(..., matchIfMissing = true)}: {@code honjeong.sms.mode}가 "mock"이거나
 * 지정되지 않았을 때(기본) 등록된다 — SMS 발송과 같은 설정 키를 쓰므로 Mock SMS와 한 묶음으로 켜진다.
 * 고정값 "000000"은 오직 개발 편의를 위한 것이며, 실 운영에서는 랜덤 6자리 구현으로 교체한다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.sms.mode", havingValue = "mock", matchIfMissing = true)
public class FixedVerificationCodeGenerator implements VerificationCodeGenerator {

    /**
     * 항상 같은 고정 인증번호를 반환한다.
     *
     * @return 고정 코드 "000000"
     */
    @Override
    public String generate() {
        return "000000"; // 개발용 고정 코드(랜덤 아님)
    }
}
