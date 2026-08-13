package com.honjeong.chat.ws;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.honjeong.chat.dto.ChatMessageResponse;

import tools.jackson.databind.ObjectMapper;

/**
 * 브로드캐스트의 대상 판정과 트랜잭션 결합.
 *
 * <p>여기서 잠그는 두 가지가 이 기능의 핵심 계약이다.
 * 1) 커밋되지 않은 메시지는 절대 나가지 않는다(유령 메시지 방지)
 * 2) 뮤트는 화면 갱신을 막지 않는다(푸시와 갈리는 지점)
 *
 * <p>★ {@link ObjectMapper}는 {@code tools.jackson}(Jackson 3)이다 — Boot 4는 이 타입만 빈으로 등록하고,
 * Jackson 2({@code com.fasterxml.jackson}) ObjectMapper 빈은 없다. Jackson 3은 {@code java.time} 지원이
 * 코어에 내장돼 있어({@code jackson-datatype-jsr310} 불필요) {@code new ObjectMapper()}로 바로
 * {@link ChatMessageResponse#createdAt}을 직렬화할 수 있다.
 */
class ChatEventBroadcasterTest {

    private WsSessionRegistry registry;
    private ChatEventBroadcaster broadcaster;

    private static final ChatMessageResponse MSG =
            new ChatMessageResponse(1L, 10L, "TEXT", "어디세요?", null, LocalDateTime.parse("2026-08-13T13:00:00"));

    @BeforeEach
    void setUp() {
        registry = mock(WsSessionRegistry.class);
        broadcaster = new ChatEventBroadcaster(registry, new ObjectMapper());
    }

    @Test
    @DisplayName("트랜잭션 밖에서 부르면 즉시 보낸다 — 기다릴 커밋이 없다")
    void sendsImmediatelyOutsideTransaction() {
        broadcaster.broadcastMessage(10L, 20L, false, 1L, MSG);

        verify(registry).sendTo(eq(10L), contains("\"type\":\"message\""));
        verify(registry).sendTo(eq(20L), anyString());
    }

    @Test
    @DisplayName("★차단 관계면 상대에게 보내지 않는다 — 보낸 사람 자신에게는 간다")
    void skipsBlockedPartner() {
        broadcaster.broadcastMessage(10L, 20L, true, 1L, MSG);

        verify(registry).sendTo(eq(10L), anyString());
        verify(registry, never()).sendTo(eq(20L), anyString());
    }

    @Test
    @DisplayName("★커밋 전에는 나가지 않는다 — 롤백되면 유령 메시지가 된다")
    void waitsForCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            broadcaster.broadcastMessage(10L, 20L, false, 1L, MSG);

            verify(registry, never()).sendTo(anyLong(), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("★커밋되면 그때 나간다")
    void sendsAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            broadcaster.broadcastMessage(10L, 20L, false, 1L, MSG);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

            verify(registry).sendTo(eq(10L), anyString());
            verify(registry).sendTo(eq(20L), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("직렬화·전송 실패가 도메인 트랜잭션을 깨지 않는다")
    void swallowsFailures() {
        org.mockito.Mockito.doThrow(new RuntimeException("소켓 터짐"))
                .when(registry).sendTo(anyLong(), anyString());

        broadcaster.broadcastMessage(10L, 20L, false, 1L, MSG); // 예외가 새어 나오지 않으면 통과
    }

    @Test
    @DisplayName("읽음도 양쪽에 간다 — 상대는 '읽음' 표시, 나는 다른 기기의 안읽음 배지")
    void broadcastsRead() {
        broadcaster.broadcastRead(10L, 20L, false, 1L, LocalDateTime.parse("2026-08-13T13:05:00"));

        verify(registry).sendTo(eq(10L), contains("\"type\":\"read\""));
        verify(registry).sendTo(eq(20L), contains("\"readerUserId\":10"));
    }

    @Test
    @DisplayName("읽음도 차단 관계면 상대에게 가지 않는다")
    void skipsBlockedPartnerOnRead() {
        broadcaster.broadcastRead(10L, 20L, true, 1L, LocalDateTime.parse("2026-08-13T13:05:00"));

        verify(registry).sendTo(eq(10L), anyString());
        verify(registry, never()).sendTo(eq(20L), anyString());
    }
}
