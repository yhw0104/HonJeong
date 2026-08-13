package com.honjeong.chat.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.honjeong.chat.dto.ChatMessageResponse;
import com.honjeong.chat.dto.WsMessageEvent;

import tools.jackson.databind.ObjectMapper;

/**
 * 채팅 이벤트를 열린 소켓으로 민다.
 *
 * <p>★ <b>커밋된 뒤에만 보낸다.</b> {@code PushDispatcher}와 같은 방식이다 — 커밋 전에 밀면
 * 롤백됐을 때 화면에만 존재하는 유령 메시지가 남고, 그건 새로고침 전까지 사라지지 않는다.
 *
 * <p>★ <b>실패는 삼킨다.</b> 이 훅은 도메인 트랜잭션의 커밋 경로에서 돌기 때문에, 여기서 새는
 * 예외는 "저장은 됐는데 응답은 실패"를 만든다. 화면 갱신 신호를 못 보낸 대가는 폴링(30초)이 메운다.
 */
@Component
public class ChatEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ChatEventBroadcaster.class);

    private final WsSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public ChatEventBroadcaster(WsSessionRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    /**
     * 새 메시지를 양쪽에 민다.
     *
     * <p>수신 대상의 판정이 둘로 갈린다.
     * <ul>
     *   <li><b>보낸 사람 자신</b> — 언제나 보낸다. 내 다른 기기를 갱신하는 것이라 차단·뮤트와 무관하다.</li>
     *   <li><b>상대</b> — 차단 관계면 보내지 않는다(그 대화는 목록에서 이미 가려져 있다).</li>
     * </ul>
     *
     * <p>★ <b>뮤트는 보지 않는다.</b> 뮤트는 "알림 끄기"지 "화면 갱신 끄기"가 아니다 —
     * 뮤트한 대화를 열어 놨는데 메시지가 안 뜨면 고장으로 읽힌다. 푸시와 갈리는 유일한 지점이다.
     *
     * @param senderId       보낸 사람
     * @param partnerId      상대
     * @param blocked        두 사람이 차단 관계인가
     * @param conversationId 대화방 id
     * @param message        저장된 메시지
     */
    public void broadcastMessage(Long senderId, Long partnerId, boolean blocked,
            Long conversationId, ChatMessageResponse message) {
        String payload = serialize(WsMessageEvent.of(conversationId, message));
        if (payload == null) {
            return;
        }
        afterCommit(() -> {
            registry.sendTo(senderId, payload);
            if (!blocked) {
                registry.sendTo(partnerId, payload);
            }
        });
    }

    /**
     * 커밋 후에 실행한다. 트랜잭션이 없으면 즉시 실행한다(기다릴 커밋이 없다).
     */
    void afterCommit(Runnable action) {
        try {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                run(action);
                return;
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    run(action);
                }
            });
        } catch (RuntimeException e) {
            log.debug("[ws] 브로드캐스트 예약 실패", e);
        }
    }

    private void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.debug("[ws] 브로드캐스트 실패", e);
        }
    }

    /**
     * 직렬화 실패는 보낼 것이 없다는 뜻이라 null을 돌려 조용히 끝낸다.
     *
     * <p>Boot 4는 Jackson 3({@code tools.jackson})을 기본 {@link ObjectMapper} 빈으로 등록한다 — Jackson 2와
     * 달리 {@code writeValueAsString}이 던지는 {@link tools.jackson.core.JacksonException}은 이미
     * {@link RuntimeException}이라(checked가 아니다) 별도 catch가 필요 없다.
     */
    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (RuntimeException e) {
            log.warn("[ws] 이벤트 직렬화 실패", e);
            return null;
        }
    }
}
