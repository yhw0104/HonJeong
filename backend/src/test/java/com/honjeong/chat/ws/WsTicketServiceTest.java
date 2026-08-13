package com.honjeong.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * WebSocket 핸드셰이크용 1회용 티켓.
 *
 * <p>왜 이런 게 있나: 핸드셰이크는 HTTP 필터 체인을 타지 않아 access 토큰을 URL에 실어야 하는데,
 * 그러면 1시간짜리 자격증명이 Caddy·docker 로그에 평문으로 남는다. 티켓은 30초·1회용이라
 * 로그에 남아도 쓸모가 없다.
 */
class WsTicketServiceTest {

    /** 테스트가 시간을 직접 움직이기 위한 고정 시계. */
    private static class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-08-13T04:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Seoul");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    @DisplayName("발급한 티켓을 소모하면 그 사용자 id가 나온다")
    void issueThenConsume() {
        WsTicketService service = new WsTicketService(new MovableClock());

        String ticket = service.issue(7L);

        assertThat(service.consume(ticket)).isEqualTo(7L);
    }

    @Test
    @DisplayName("★1회용 — 같은 티켓을 두 번 소모할 수 없다")
    void singleUse() {
        WsTicketService service = new WsTicketService(new MovableClock());
        String ticket = service.issue(7L);

        service.consume(ticket);

        assertThat(service.consume(ticket)).isNull();
    }

    @Test
    @DisplayName("★30초가 지나면 만료된다")
    void expires() {
        MovableClock clock = new MovableClock();
        WsTicketService service = new WsTicketService(clock);
        String ticket = service.issue(7L);

        clock.advance(Duration.ofSeconds(31));

        assertThat(service.consume(ticket)).isNull();
    }

    @Test
    @DisplayName("30초 직전까지는 유효하다")
    void validJustBeforeExpiry() {
        MovableClock clock = new MovableClock();
        WsTicketService service = new WsTicketService(clock);
        String ticket = service.issue(7L);

        clock.advance(Duration.ofSeconds(29));

        assertThat(service.consume(ticket)).isEqualTo(7L);
    }

    @Test
    @DisplayName("발급한 적 없는 값은 거부한다")
    void unknownTicket() {
        WsTicketService service = new WsTicketService(new MovableClock());

        assertThat(service.consume("아무거나")).isNull();
    }

    @Test
    @DisplayName("null·빈 문자열도 조용히 거부한다")
    void blankTicket() {
        WsTicketService service = new WsTicketService(new MovableClock());

        assertThat(service.consume(null)).isNull();
        assertThat(service.consume("")).isNull();
    }

    @Test
    @DisplayName("티켓은 매번 다른 값이다 — 추측할 수 없어야 한다")
    void ticketsAreUnique() {
        WsTicketService service = new WsTicketService(new MovableClock());

        assertThat(service.issue(1L)).isNotEqualTo(service.issue(1L));
    }

    @Test
    @DisplayName("만료된 티켓이 쌓이지 않는다 — 소모 시도 때 함께 청소된다")
    void expiredTicketsAreSwept() {
        MovableClock clock = new MovableClock();
        WsTicketService service = new WsTicketService(clock);
        service.issue(1L);
        service.issue(2L);
        String fresh;

        clock.advance(Duration.ofSeconds(31));
        fresh = service.issue(3L);
        service.consume(fresh); // 이 호출이 청소를 유발한다

        assertThat(service.size()).isZero();
    }

    @Test
    @DisplayName("★소모 없이 발급만 반복해도 맵이 무한정 자라지 않는다")
    void issueAloneDoesNotGrowUnbounded() {
        MovableClock clock = new MovableClock();
        WsTicketService service = new WsTicketService(clock);
        service.issue(1L);
        service.issue(2L);
        service.issue(3L);

        clock.advance(Duration.ofSeconds(31));
        service.issue(4L); // consume은 한 번도 호출하지 않는다

        assertThat(service.size()).isOne();
    }
}
