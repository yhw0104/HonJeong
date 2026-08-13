package com.honjeong.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * userId -> 열린 세션들. 브로드캐스트의 배달 계층이다.
 */
class WsSessionRegistryTest {

    private static WebSocketSession openSession() {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    @DisplayName("등록한 사용자에게 보내면 그 세션으로 전달된다")
    void sendsToRegisteredSession() throws Exception {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession s = openSession();
        registry.register(1L, s);

        registry.sendTo(1L, "{\"type\":\"message\"}");

        verify(s).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("★한 사용자의 여러 기기 모두에 전달된다")
    void sendsToAllDevices() throws Exception {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession phone = openSession();
        WebSocketSession tablet = openSession();
        registry.register(1L, phone);
        registry.register(1L, tablet);

        registry.sendTo(1L, "{}");

        verify(phone).sendMessage(any(TextMessage.class));
        verify(tablet).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("접속하지 않은 사용자에게 보내도 아무 일도 일어나지 않는다")
    void sendToAbsentUserIsNoop() {
        WsSessionRegistry registry = new WsSessionRegistry();

        registry.sendTo(999L, "{}"); // 예외 없이 지나가면 통과
    }

    @Test
    @DisplayName("해제하면 더 이상 전달되지 않는다")
    void unregisterStopsDelivery() throws Exception {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession s = openSession();
        registry.register(1L, s);

        registry.unregister(1L, s);
        registry.sendTo(1L, "{}");

        verify(s, never()).sendMessage(any(TextMessage.class));
        assertThat(registry.sessionCount(1L)).isZero();
    }

    @Test
    @DisplayName("한 기기만 끊어도 남은 기기에는 계속 전달된다")
    void unregisterOneKeepsOther() throws Exception {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession phone = openSession();
        WebSocketSession tablet = openSession();
        registry.register(1L, phone);
        registry.register(1L, tablet);

        registry.unregister(1L, phone);
        registry.sendTo(1L, "{}");

        verify(tablet).sendMessage(any(TextMessage.class));
        assertThat(registry.sessionCount(1L)).isEqualTo(1);
    }

    @Test
    @DisplayName("★닫힌 세션은 건너뛰고 정리한다")
    void skipsAndCleansClosedSession() throws Exception {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession dead = mock(WebSocketSession.class);
        when(dead.isOpen()).thenReturn(false);
        registry.register(1L, dead);

        registry.sendTo(1L, "{}");

        verify(dead, never()).sendMessage(any(TextMessage.class));
        assertThat(registry.sessionCount(1L)).isZero();
    }

    @Test
    @DisplayName("★한 세션의 전송 실패가 다른 세션의 전달을 막지 않는다")
    void oneFailureDoesNotBlockOthers() throws Exception {
        WsSessionRegistry registry = new WsSessionRegistry();
        WebSocketSession broken = openSession();
        WebSocketSession healthy = openSession();
        doThrow(new IOException("연결 끊김")).when(broken).sendMessage(any(TextMessage.class));
        registry.register(1L, broken);
        registry.register(1L, healthy);

        registry.sendTo(1L, "{}");

        verify(healthy).sendMessage(any(TextMessage.class));
    }

    @Test
    @DisplayName("★같은 사용자에게 등록과 해제가 동시에 일어나도 등록된 세션을 잃지 않는다")
    void concurrentRegisterAndUnregisterDoesNotLoseSessions() throws Exception {
        // 라운드를 독립적으로 반복한다 — 살아남는 세션이 한 라운드에 쌓이면 그 사용자의
        // 라이브 셋이 다시는 비지 않아 경쟁 창이 첫 라운드 이후로 막혀버린다. 매 라운드
        // 새 레지스트리로 "셋이 막 비려는 찰나"를 매번 다시 만들어야 경쟁을 반복해서 노려볼 수 있다.
        int rounds = 300;
        Long userId = 1L;

        for (int i = 0; i < rounds; i++) {
            WsSessionRegistry registry = new WsSessionRegistry();
            WebSocketSession victim = openSession();
            registry.register(userId, victim); // 라이브 셋을 {victim} 하나짜리로 만들어 둔다

            WebSocketSession keeper = openSession();
            CountDownLatch start = new CountDownLatch(1);
            Thread registerer = new Thread(() -> {
                await(start);
                registry.register(userId, keeper);
            });
            Thread unregisterer = new Thread(() -> {
                await(start);
                registry.unregister(userId, victim);
            });

            registerer.start();
            unregisterer.start();
            start.countDown();
            joinOrFail(registerer, i);
            joinOrFail(unregisterer, i);

            // 등록/해제 순서와 무관하게 최종 상태는 항상 {keeper} 하나여야 한다 —
            // computeIfAbsent(...).add(...)라면 경쟁 상태로 keeper가 유령이 되어 0이 나올 수 있다.
            assertThat(registry.sessionCount(userId)).as("round %d", i).isEqualTo(1);
        }
    }

    /**
     * 스레드가 끝나기를 기다리되 <b>영원히 기다리지는 않는다.</b>
     *
     * <p>★ 이 테스트는 register/unregister의 락 순서가 깨지는 회귀를 잡으려고 있다. 그런데 정작
     * 그 회귀가 일어나면 두 스레드가 서로 물려 멈추고, 타임아웃 없는 {@code join()}도 같이 멈춘다 —
     * CI가 <b>실패하지 않고 정지한다.</b> 에러 메시지 없이 러너 제한 시간까지 매달려 있어서 실패보다
     * 진단이 어렵다. 시한을 두면 같은 회귀가 "데드락 의심"이라는 읽을 수 있는 실패로 나온다.
     *
     * @param t     기다릴 스레드
     * @param round 실패 메시지에 남길 라운드 번호
     */
    private static void joinOrFail(Thread t, int round) throws InterruptedException {
        t.join(5_000);
        assertThat(t.isAlive())
                .as("round %d: 스레드가 5초 안에 끝나지 않았다 — register/unregister 데드락 의심", round)
                .isFalse();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
