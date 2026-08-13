package com.honjeong.chat.ws;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * WebSocket 핸드셰이크용 1회용 티켓의 발급과 소모.
 *
 * <p><b>왜 티켓인가.</b> 핸드셰이크는 HTTP 필터 체인을 타지 않아 인증을 직접 해야 하는데,
 * access 토큰을 {@code ?token=}으로 URL에 실으면 <b>1시간짜리 자격증명이 Caddy·docker 로그에
 * 평문으로 남는다</b>(access-token-ttl-seconds: 3600). 티켓은 30초·1회용이라 로그에 남아도 쓸모가 없다.
 *
 * <p>부수 효과가 하나 더 있다 — 티켓 발급은 일반 REST라 {@code ActiveUserFilter}를 그대로 타므로,
 * 탈퇴·정지 사용자 차단을 WebSocket 경로에 따로 구현할 필요가 없다.
 *
 * <p>★ <b>서버 1대 전제다.</b> 저장소가 이 프로세스의 메모리라, 서버를 2대로 늘리면
 * 티켓을 발급한 서버와 연결을 받는 서버가 달라져 핸드셰이크가 실패한다.
 * 그때는 티켓 저장소를 공유 캐시(Redis 등)로 옮겨야 한다.
 */
@Service
public class WsTicketService {

    /** 티켓 수명. 발급 직후 곧바로 연결하는 용도라 짧을수록 좋다. */
    private static final Duration TTL = Duration.ofSeconds(30);
    private static final int TICKET_BYTES = 32;

    private final Map<String, WsTicket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public WsTicketService(Clock clock) {
        this.clock = clock;
    }

    /**
     * 티켓을 발급한다.
     *
     * @param userId 이 티켓으로 연결할 사용자
     * @return 불투명 티켓 문자열(URL-safe)
     */
    public String issue(Long userId) {
        sweepExpired();
        byte[] bytes = new byte[TICKET_BYTES];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(ticket, new WsTicket(userId, clock.instant().plus(TTL)));
        return ticket;
    }

    /**
     * 티켓을 소모한다. 성공하면 그 티켓은 즉시 사라진다(1회용).
     *
     * <p>실패 이유(없음·만료·이미 소모)를 구분해 돌려주지 않는다 — 호출자가 할 수 있는 일이
     * "연결 거부" 하나뿐이고, 구분해 주면 티켓 존재 여부를 캐볼 수 있는 통로가 된다.
     *
     * @param ticket 클라이언트가 제시한 티켓(null·빈 문자열 허용)
     * @return 유효하면 사용자 id, 아니면 null
     */
    public Long consume(String ticket) {
        sweepExpired();
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        WsTicket found = tickets.remove(ticket);
        if (found == null || found.isExpired(clock.instant())) {
            return null;
        }
        return found.userId();
    }

    /** 남아 있는 티켓 수(테스트용 — 만료분이 쌓이지 않는지 확인한다). */
    int size() {
        return tickets.size();
    }

    /**
     * 만료된 티켓을 치운다.
     *
     * <p>{@link #issue}·{@link #consume} 양쪽 진입점에서 다 호출한다 — {@code consume}에서만
     * 훑으면, 발급만 되고 소모는 안 되는 경우(방치된 핸드셰이크 등)에 맵이 무한정 자란다.
     * 양쪽에서 훑으면 아무도 연결하지 않아도 맵에는 TTL 한 창(window) 분량만 남는다는
     * 불변식이 성립한다.
     *
     * <p>별도 스케줄러는 두지 않는다 — 티켓은 30초짜리고 발급량도 접속 시도 수준이라,
     * 진입점마다 훑는 것으로 충분하다. 스케줄러는 관리할 것만 늘린다.
     */
    private void sweepExpired() {
        Instant now = clock.instant();
        tickets.entrySet().removeIf(e -> e.getValue().isExpired(now));
    }
}
