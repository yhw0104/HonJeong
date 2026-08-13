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
     * <p>★{@code computeIfAbsent(...).add(...)}로 "단순화"하고 싶겠지만 하면 안 된다 —
     * {@code computeIfAbsent}가 집합을 반환한 뒤 {@code add}가 실행되기까지의 틈에서
     * 다른 스레드의 {@link #unregister}가 그 집합을 마지막 세션까지 비우고 맵에서
     * 키를 지워버릴 수 있다. 그러면 이 스레드의 {@code add}는 이미 맵에서 떨어져 나간
     * 집합에 성공적으로 들어가고, 그 세션은 {@link #sendTo}·{@link #sessionCount} 어디에서도
     * 보이지 않는 유령이 된다("기기 하나가 재접속하는 동안 다른 기기가 접속을 끊는" 흔한
     * 케이스에서 실제로 발생). {@code compute}는 이 read-modify-write 전체를 해당 키의
     * 락 안에서 원자적으로 수행해 그 틈을 없앤다.
     *
     * @param userId  세션의 주인
     * @param session 열린 세션
     */
    public void register(Long userId, WebSocketSession session) {
        sessions.compute(userId, (k, existing) -> {
            Set<WebSocketSession> set = existing == null ? ConcurrentHashMap.newKeySet() : existing;
            set.add(session);
            return set;
        });
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
     * <p>★{@code session}에 대해 {@code synchronized}로 감싸 전송을 직렬화한다.
     * {@link WebSocketSession#sendMessage}는 스프링 문서상 여러 스레드가 동시에 호출하는
     * 것이 안전하지 않다 — 채팅 메시지 브로드캐스트와 읽음 처리 브로드캐스트가 서로 다른
     * 요청 스레드에서 같은 수신자에게 겹쳐 들어오면 실제로 벌어질 수 있는 상황이다.
     * 컨테이너에 따라 예외가 나거나 연결의 쓰기 상태가 깨져, 그 세션은 이후의 모든 푸시가
     * 계속 실패하게 된다. 스프링이 제공하는 {@code ConcurrentWebSocketSessionDecorator}가
     * 같은 문제를 풀지만 채택하지 않았다 — 우리 페이로드는 단일 서버에서 도는 작은 JSON이라
     * 그 데코레이터가 주는 버퍼링·전송 시간 제한이 필요 없고, 무엇보다 핸들러는 연결 종료 시
     * 원본(raw) 세션으로 {@link #unregister}를 부르는데 맵에는 데코레이터(wrapper)가 들어있게
     * 되어 둘이 {@code equals}로 같지 않으니 해제가 조용히 실패한다 — 레지스트리 구조를 함께
     * 바꾸지 않고는 못 붙인다. 나중에 느린 클라이언트를 위한 버퍼링이 필요해지면, 세션을
     * {@code session.getId()}로 키잉하도록 바꾸는 리팩터를 거쳐 그때 도입할 것.
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
                synchronized (session) {
                    session.sendMessage(new TextMessage(payload));
                }
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
