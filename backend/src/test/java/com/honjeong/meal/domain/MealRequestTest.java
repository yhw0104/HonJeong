package com.honjeong.meal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

/** MealRequest 도메인 단위 테스트 — 상태 전이(create/accept/decline)·isPending. */
class MealRequestTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 6, 18, 12, 0);

    private MealRequest pending() {
        return MealRequest.create(mock(User.class), mock(CheckIn.class), mock(Place.class), "msg", now);
    }

    @Test
    @DisplayName("create: PENDING으로 생성되고 respondedAt은 비어 있다")
    void create() {
        MealRequest mr = pending();
        assertThat(mr.getStatus()).isEqualTo(MealRequestStatus.PENDING);
        assertThat(mr.isPending()).isTrue();
        assertThat(mr.getCreatedAt()).isEqualTo(now);
        assertThat(mr.getRespondedAt()).isNull();
    }

    @Test
    @DisplayName("accept: ACCEPTED로 전이하고 respondedAt 기록")
    void accept() {
        MealRequest mr = pending();
        mr.accept(now.plusMinutes(10));
        assertThat(mr.getStatus()).isEqualTo(MealRequestStatus.ACCEPTED);
        assertThat(mr.isPending()).isFalse();
        assertThat(mr.getRespondedAt()).isEqualTo(now.plusMinutes(10));
    }

    @Test
    @DisplayName("decline: DECLINED로 전이하고 respondedAt 기록")
    void decline() {
        MealRequest mr = pending();
        mr.decline(now.plusMinutes(10));
        assertThat(mr.getStatus()).isEqualTo(MealRequestStatus.DECLINED);
        assertThat(mr.isPending()).isFalse();
        assertThat(mr.getRespondedAt()).isEqualTo(now.plusMinutes(10));
    }

    @Test
    @DisplayName("isReceivedBy: 대상 체크인 주인이면 true, 아니면 false")
    void isReceivedBy() {
        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(2L);
        CheckIn target = mock(CheckIn.class);
        when(target.getUser()).thenReturn(receiver);
        MealRequest mr = MealRequest.create(mock(User.class), target, mock(Place.class), "msg", now);

        assertThat(mr.isReceivedBy(2L)).isTrue();
        assertThat(mr.isReceivedBy(99L)).isFalse();
    }
}
