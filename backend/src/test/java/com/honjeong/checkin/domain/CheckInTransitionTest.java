package com.honjeong.checkin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckInTransitionTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 11, 12, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 7, 11, 12, 30);

    @Test
    @DisplayName("startSeeking은 SEEKING 상태로 생성된다")
    void 모집_시작() {
        CheckIn c = CheckIn.startSeeking(null, null, T0);
        assertThat(c.getStatus()).isEqualTo(CheckInStatus.SEEKING);
        assertThat(c.getStartedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("dineAlone은 SEEKING을 ACTIVE로 전이하고 startedAt을 재설정한다")
    void 혼자_먹기_시작() {
        CheckIn c = CheckIn.startSeeking(null, null, T0);
        c.dineAlone(T1);
        assertThat(c.getStatus()).isEqualTo(CheckInStatus.ACTIVE);
        assertThat(c.getStartedAt()).isEqualTo(T1); // 실제 식사 시작 시각
    }

    @Test
    @DisplayName("matchTogether는 SEEKING에서만 TOGETHER로 전이한다")
    void 매칭() {
        CheckIn c = CheckIn.startSeeking(null, null, T0);
        c.matchTogether(7L, T1);
        assertThat(c.getStatus()).isEqualTo(CheckInStatus.TOGETHER);
        assertThat(c.getMealRequestId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("cancel은 SEEKING과 ACTIVE 모두에서 CANCELLED로 전이한다")
    void 취소() {
        CheckIn seeking = CheckIn.startSeeking(null, null, T0);
        seeking.cancel(T1);
        assertThat(seeking.getStatus()).isEqualTo(CheckInStatus.CANCELLED);

        CheckIn active = CheckIn.startSeeking(null, null, T0);
        active.dineAlone(T0);
        active.cancel(T1);
        assertThat(active.getStatus()).isEqualTo(CheckInStatus.CANCELLED);
    }

    @Test
    @DisplayName("end는 SEEKING을 종료시키지 않는다(안 먹은 모집이 이력에 잡히면 안 됨)")
    void end는_SEEKING_무시() {
        CheckIn c = CheckIn.startSeeking(null, null, T0);
        c.end(T1);
        assertThat(c.getStatus()).isEqualTo(CheckInStatus.SEEKING); // 변화 없음
    }
}
