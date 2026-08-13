package com.honjeong.chat.ws;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 열려 있는 WebSocket 세션을 사용자별로 들고 있다가, 지목된 사용자에게 프레임을 민다.
 *
 * <p>한 사용자가 여러 기기로 로그인할 수 있으므로 값은 세션 <b>집합</b>이다.
 *
 * <p>★★ <b>서버 1대 전제다.</b> 이 맵은 이 프로세스의 메모리에만 있다. 서버를 2대로 늘리면
 * A는 서버1에, B는 서버2에 붙어 서버1이 B의 세션을 모르게 되고, 증상은 <b>"가끔 메시지가
 * 안 온다"</b>로 나타나 진단이 매우 어렵다. 증설할 때는 반드시 Redis Pub/Sub 같은 중계
 * 계층을 먼저 넣을 것 — 자세한 배경은 {@code docs/08-실시간-전략.md} §4.
 *
 * <p>전송 실패는 삼킨다. 화면 갱신용 신호라 못 보내도 폴링(30초)이 메우고, 여기서 던진 예외는
 * 브로드캐스트를 부른 도메인 트랜잭션의 커밋 경로로 새어 나간다.
 */
@Component
public class WsSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WsSessionRegistry.class);

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    /**
     * 세션을 등록한다(연결 수립 시).
     *
     * @param userId  세션의 주인
     * @param session 열린 세션
     */
    public void register(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /**
     * 세션을 해제한다(연결 종료 시). 마지막 세션이 빠지면 키도 지운다.
     *
     * @param userId  세션의 주인
     * @param session 닫힌 세션
     */
    public void unregister(Long userId, WebSocketSession session) {
        sessions.computeIfPresent(userId, (k, set) -> {
            set.remove(session);
            return set.isEmpty() ? null : set;
        });
    }

    /**
     * 한 사용자의 모든 기기에 프레임을 민다. 접속 중이 아니면 아무 일도 하지 않는다.
     *
     * <p>닫힌 세션은 보내지 않고 그 자리에서 정리한다 — 연결이 끊겼는데 close 콜백이
     * 오지 않는 경우가 있어서, 여기가 실질적인 두 번째 청소 지점이다.
     *
     * @param userId  받을 사람
     * @param payload 이미 JSON으로 직렬화된 본문
     */
    public void sendTo(Long userId, String payload) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null) {
            return;
        }
        for (WebSocketSession session : set) {
            if (!session.isOpen()) {
                unregister(userId, session);
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException | RuntimeException e) {
                // 한 기기의 실패가 다른 기기의 전달을 막으면 안 된다.
                log.debug("[ws] 전송 실패 user={} session={}", userId, session.getId(), e);
            }
        }
    }

    /**
     * 한 사용자의 열린 세션 수.
     *
     * @param userId 사용자 id
     * @return 세션 수(없으면 0)
     */
    public int sessionCount(Long userId) {
        Set<WebSocketSession> set = sessions.get(userId);
        return set == null ? 0 : set.size();
    }
}
