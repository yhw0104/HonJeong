package com.honjeong.push.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬·테스트용 발송기 — 실제로 보내지 않고 로그만 남긴다.
 *
 * <p>mock으로 떠 있는 상태를 조용히 두지 않는다(08-03 결정: 설정 주석은 서버를 띄우는
 * 사람이 읽지 않는다는 전제). 기동 시 한 번, 발송 시도마다 DEBUG로 남긴다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.push.mode", havingValue = "mock", matchIfMissing = true)
public class NoopPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(NoopPushSender.class);

    public NoopPushSender() {
        log.info("[push] mock 모드 — 푸시를 실제로 보내지 않습니다. real 전환은 honjeong.push.mode=real");
    }

    /**
     * 발송하지 않고 시도 사실만 남긴다.
     *
     * @param tokens  대상 FCM 토큰 목록(개수만 로그에 남긴다 — 토큰 원문은 남기지 않는다)
     * @param message 보낼 내용
     * @return 항상 빈 목록(mock에서는 무효 토큰을 판정할 수 없다)
     */
    @Override
    public List<String> send(List<String> tokens, PushMessage message) {
        log.debug("[push] mock 발송 생략 — 대상 {}건, type={}", tokens.size(), message.type());
        return List.of();
    }
}
