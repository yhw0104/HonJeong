package com.honjeong.push.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import com.honjeong.notification.domain.NotificationType;
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

    @Test
    @DisplayName("커밋 훅에서 발송이 터져도 커밋 경로로 예외가 새지 않는다 — 저장은 됐는데 응답만 실패하면 안 된다")
    void 커밋훅_실패도_삼킨다() {
        willThrow(new RuntimeException("boom"))
                .given(pushSendTask).send(anyLong(), any(), isNull(), isNull(), isNull());

        TransactionSynchronizationManager.initSynchronization();
        try {
            dispatcher.dispatch(7L, PushType.BADGE_EARNED, null, null, null);
            // 커밋 훅을 직접 돌린다. 이 호출이 던지면 트랜잭션 매니저의 commit() 밖으로 나가
            // 이미 커밋된 요청이 실패로 응답된다.
            TransactionSynchronizationUtils.invokeAfterCommit(
                    TransactionSynchronizationManager.getSynchronizations());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(pushSendTask).send(7L, PushType.BADGE_EARNED, null, null, null);
    }

    @Test
    @DisplayName("알림함 종류를 받으면 같은 이름의 푸시 종류로 사상해 예약한다")
    void 알림함_종류를_사상한다() {
        dispatcher.dispatch(7L, NotificationType.MEAL_REQUEST_RECEIVED, 9L);

        verify(pushSendTask).send(7L, PushType.MEAL_REQUEST_RECEIVED, 9L, null, null);
    }

    @Test
    @DisplayName("사상이 실패해도 예외가 밖으로 새지 않는다 — 사상은 호출 인자 자리가 아니라 격리 안에서 해야 한다")
    void 사상_실패도_삼킨다() {
        // PushType.from이 던지는 상황(= NotificationType에 값을 추가하고 PushType에 빠뜨린 경우)을
        // 흉내낸다. 사상을 호출 인자 자리에서 평가하면 이 예외가 격리 밖에서 터져
        // MealRequestService.create의 catch에도 안 걸리고 신청 자체를 롤백시킨다(I-4).
        dispatcher.dispatch(7L, null, 9L); // 예외가 나면 테스트 실패

        verify(pushSendTask, never()).send(anyLong(), any(), any(), any(), any());
    }
}
