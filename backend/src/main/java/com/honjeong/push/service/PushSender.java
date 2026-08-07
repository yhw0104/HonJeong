package com.honjeong.push.service;

import java.util.List;

/**
 * 기기 푸시 발송. 구현체만 FCM을 안다 — 전달 경로를 바꾸면 이 인터페이스 뒤만 교체한다.
 *
 * <p>구현: {@link FcmPushSender}(real) · {@link NoopPushSender}(mock).
 */
public interface PushSender {

    /**
     * 여러 기기로 같은 메시지를 보낸다.
     *
     * <p>기기별로 독립 처리한다 — 한 토큰이 죽었다고 나머지 발송을 멈추지 않는다.
     * 예외를 던지지 않는다(호출자가 커밋 후 비동기 경로라 받을 사람이 없다).
     *
     * @param tokens  대상 FCM 토큰 목록
     * @param message 보낼 내용
     * @return 더 이상 유효하지 않아 <b>삭제해야 하는</b> 토큰 목록(없으면 빈 목록)
     */
    List<String> send(List<String> tokens, PushMessage message);
}
