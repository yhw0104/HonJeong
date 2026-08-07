package com.honjeong.push.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.honjeong.notification.domain.NotificationType;
import com.honjeong.push.domain.PushType;

/**
 * 푸시 발송 예약. 도메인 트랜잭션이 <b>커밋된 뒤</b>에만 실제 발송이 일어나게 한다.
 *
 * <p>사용처: NotificationService.publish(알림 6종), ConversationService.sendMessage(채팅).
 *
 * <p>왜 커밋 후인가: 발송은 외부 HTTP다. 트랜잭션 안에서 부르면 ①응답을 기다리는 동안
 * DB 커넥션을 붙잡고 ②발송 실패가 신청 자체를 롤백시킨다. 롤백되면 예약도 함께 사라지므로
 * "없던 신청에 대한 푸시"도 나가지 않는다.
 *
 * <p>★ 이 메서드는 <b>절대 예외를 던지지 않는다.</b> 호출처 두 곳
 * (MealRequestService.create, MateRequestService.create)이 {@code try/catch(DataIntegrityViolationException)}
 * 안에 있어서, 여기서 새는 예외가 "중복 신청"으로 오역될 수 있다.
 */
@Component
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    private final PushSendTask pushSendTask;

    public PushDispatcher(PushSendTask pushSendTask) {
        this.pushSendTask = pushSendTask;
    }

    /**
     * 알림함 종류로 커밋 후 발송을 예약한다({@link com.honjeong.notification.service.NotificationService} 전용).
     *
     * <p><b>사상을 이 메서드 안에서 하는 이유.</b> {@link PushType#from}은 {@code valueOf}라
     * {@link NotificationType}에만 값을 추가하고 {@link PushType}에 빠뜨리면
     * {@code IllegalArgumentException}을 던진다. 호출 인자 자리에서 평가하면 그 예외가
     * 아래 try 바깥에서 나 "절대 예외를 던지지 않는다"는 이 클래스의 보장을 통과하지 못하고,
     * 결국 {@code MealRequestService.create}의 {@code catch(DataIntegrityViolationException)}에도
     * 안 걸려 <b>같이먹기 신청 자체가 롤백된다.</b> 그래서 사상을 격리 안으로 들여왔다.
     *
     * @param recipientId 받는 사람
     * @param type        알림함 종류(같은 이름의 PushType으로 사상된다)
     * @param actorId     상대 id(BADGE_EARNED는 null)
     */
    public void dispatch(Long recipientId, NotificationType type, Long actorId) {
        try {
            dispatch(recipientId, PushType.from(type), actorId, null, null);
        } catch (RuntimeException e) {
            log.warn("[push] 발송 예약 실패(사상 불가) recipient={} type={}", recipientId, type, e);
        }
    }

    /**
     * 커밋 후 발송을 예약한다.
     *
     * @param recipientId    받는 사람
     * @param type           푸시 종류
     * @param actorId        상대 id(BADGE_EARNED는 null)
     * @param conversationId 채팅일 때 대화방 id(그 외 null)
     * @param preview        채팅 미리보기(그 외 null)
     */
    public void dispatch(Long recipientId, PushType type, Long actorId, Long conversationId, String preview) {
        try {
            if (!TransactionSynchronizationManager.isSynchronizationActive()) {
                // 트랜잭션 밖에서 불린 경우 — 기다릴 커밋이 없으니 바로 보낸다.
                pushSendTask.send(recipientId, type, actorId, conversationId, preview);
                return;
            }
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pushSendTask.send(recipientId, type, actorId, conversationId, preview);
                }
            });
        } catch (RuntimeException e) {
            // 예약 실패가 도메인 트랜잭션을 깨면 안 된다(위 Javadoc 참조).
            log.warn("[push] 발송 예약 실패 recipient={} type={}", recipientId, type, e);
        }
    }
}
