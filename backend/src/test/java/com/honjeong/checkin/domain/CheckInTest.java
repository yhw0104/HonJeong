package com.honjeong.checkin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

/**
 * CheckIn 도메인 단위 테스트(순수 JUnit5 + AssertJ, DB·스프링 컨텍스트 불필요).
 * User·Place는 protected no-arg 생성자만 있어 Mockito mock으로 대체한다(id 불필요).
 */
class CheckInTest {

    private final LocalDateTime t0 = LocalDateTime.of(2026, 7, 3, 12, 0);

    @Test
    @DisplayName("matchTogether: ACTIVE를 TOGETHER로 전이하고 matchedAt·mealRequestId를 채운다")
    void matchTogether_transitions() {
        CheckIn c = CheckIn.start(mock(User.class), mock(Place.class), t0);
        c.matchTogether(77L, t0.plusMinutes(30));

        assertThat(c.getStatus()).isEqualTo(CheckInStatus.TOGETHER);
        assertThat(c.getMatchedAt()).isEqualTo(t0.plusMinutes(30));
        assertThat(c.getMealRequestId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("startTogether: TOGETHER 체크인을 startedAt=matchedAt=now로 생성한다")
    void startTogether_creates() {
        CheckIn c = CheckIn.startTogether(mock(User.class), mock(Place.class), 77L, t0);

        assertThat(c.getStatus()).isEqualTo(CheckInStatus.TOGETHER);
        assertThat(c.getStartedAt()).isEqualTo(t0);
        assertThat(c.getMatchedAt()).isEqualTo(t0);
        assertThat(c.getMealRequestId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("cancel: ACTIVE를 CANCELLED로 전이하고 endedAt을 채운다")
    void cancel_fromActive() {
        CheckIn c = CheckIn.start(mock(User.class), mock(Place.class), t0);
        c.cancel(t0.plusMinutes(10));

        assertThat(c.getStatus()).isEqualTo(CheckInStatus.CANCELLED);
        assertThat(c.getEndedAt()).isEqualTo(t0.plusMinutes(10));
    }

    @Test
    @DisplayName("end: TOGETHER도 ENDED로 종료한다")
    void end_fromTogether() {
        CheckIn c = CheckIn.startTogether(mock(User.class), mock(Place.class), 77L, t0);
        c.end(t0.plusHours(1));

        assertThat(c.getStatus()).isEqualTo(CheckInStatus.ENDED);
        assertThat(c.getEndedAt()).isEqualTo(t0.plusHours(1));
    }

    @Test
    @DisplayName("cancel: 이미 ENDED면 무시(멱등)")
    void cancel_idempotentWhenEnded() {
        CheckIn c = CheckIn.start(mock(User.class), mock(Place.class), t0);
        c.end(t0.plusHours(1));
        c.cancel(t0.plusHours(2));

        assertThat(c.getStatus()).isEqualTo(CheckInStatus.ENDED);
    }
}
