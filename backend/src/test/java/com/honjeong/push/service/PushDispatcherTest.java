package com.honjeong.push.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.push.domain.PushType;

/**
 * PushDispatcher 단위 테스트 — 트랜잭션이 없을 때의 동작과 예외 격리만 본다.
 *
 * <p>커밋·롤백 경계 자체는 여기서 검증할 수 없다(Mockito에는 트랜잭션이 존재하지 않는다).
 * 그것은 실 Postgres를 쓰는 {@link PushCommitBoundaryTest}가 맡는다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PushDispatcher")
class PushDispatcherTest {

    @Mock
    private PushSendTask pushSendTask;
    @InjectMocks
    private PushDispatcher dispatcher;

    @Test
    @DisplayName("트랜잭션이 없으면 즉시 발송한다")
    void 트랜잭션_밖이면_즉시_발송() {
        dispatcher.dispatch(7L, PushType.BADGE_EARNED, null, null, null);

        verify(pushSendTask).send(7L, PushType.BADGE_EARNED, null, null, null);
    }

    @Test
    @DisplayName("발송이 예외를 던져도 밖으로 새지 않는다 — 호출처가 중복 신청으로 오역한다")
    void 예약_실패는_삼킨다() {
        willThrow(new RuntimeException("boom"))
                .given(pushSendTask).send(anyLong(), any(), isNull(), isNull(), isNull());

        dispatcher.dispatch(7L, PushType.BADGE_EARNED, null, null, null); // 예외가 나면 테스트 실패
    }
}
