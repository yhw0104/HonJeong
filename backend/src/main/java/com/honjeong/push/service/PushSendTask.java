package com.honjeong.push.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.honjeong.push.domain.PushType;

/**
 * 실제 푸시 발송 — 도메인 트랜잭션이 커밋된 <b>뒤에</b> 별도 스레드에서 돈다.
 *
 * <p>사용처: PushDispatcher(커밋 후 훅).
 *
 * <p>PushDispatcher와 클래스를 분리한 이유: 같은 클래스 안에서 {@code @Async} 메서드를
 * 호출하면 스프링 프록시를 타지 않아 그냥 동기 실행된다. 별도 빈이어야 실제로 비동기가 된다
 * (06-13에 PhoneAttemptRecorder를 별도 빈으로 뺀 것과 같은 이유).
 *
 * <p><b>★ 이 메서드에 {@code @Transactional}을 붙이지 않는다.</b> 붙이면 FCM HTTP 호출이
 * 트랜잭션 안에서 일어나 <b>응답을 기다리는 동안 DB 커넥션을 붙잡고 아무 일도 안 한다</b> —
 * 스펙 §2가 이 설계(커밋 후 비동기)를 만든 이유가 정확히 그것이므로, 여기에 트랜잭션을 다시
 * 씌우면 도메인 트랜잭션에서 빼낸 문제를 새 트랜잭션 안에 그대로 옮겨 놓는 셈이 된다.
 * 커넥션 풀 10 / 푸시 스레드 최대 4이므로 최악의 경우 커넥션 4개가 firebase-admin 타임아웃
 * 동안 묶인다.
 *
 * <p>그래서 세 구간으로 나눈다. 트랜잭션은 1·3에만 있고, 2는 트랜잭션 밖이다.
 * <ol>
 *   <li><b>조회</b> — {@link PushAudienceReader} (읽기 전용 트랜잭션)</li>
 *   <li><b>발송</b> — {@link PushSender} (트랜잭션 <b>밖</b>, 외부 HTTP)</li>
 *   <li><b>기록</b> — {@link PushDeliveryRecorder} (쓰기 트랜잭션)</li>
 * </ol>
 *
 * <p>1·3이 <b>별도 빈</b>인 이유는 자기호출이 프록시를 타지 않아 {@code @Transactional}이
 * 무효가 되기 때문이다(위 {@code @Async}와 같은 함정). 각 클래스 Javadoc 참조.
 */
@Component
public class PushSendTask {

    private static final Logger log = LoggerFactory.getLogger(PushSendTask.class);

    private final PushAudienceReader audienceReader;
    private final PushSender pushSender;
    private final PushDeliveryRecorder deliveryRecorder;

    public PushSendTask(PushAudienceReader audienceReader, PushSender pushSender,
            PushDeliveryRecorder deliveryRecorder) {
        this.audienceReader = audienceReader;
        this.pushSender = pushSender;
        this.deliveryRecorder = deliveryRecorder;
    }

    /**
     * 수신자의 모든 기기로 푸시를 보낸다.
     *
     * <p>어떤 예외도 밖으로 던지지 않는다 — 커밋 후 경로라 받을 사람이 없다.
     *
     * @param recipientId    받는 사람
     * @param type           푸시 종류
     * @param actorId        상대 id(BADGE_EARNED는 null)
     * @param conversationId 채팅일 때 대화방 id(그 외 null)
     * @param preview        채팅 미리보기(그 외 null)
     */
    @Async("pushExecutor")
    public void send(Long recipientId, PushType type, Long actorId, Long conversationId, String preview) {
        try {
            PushAudience audience = audienceReader.read(recipientId, actorId);
            if (audience.isEmpty()) {
                return; // 푸시 권한을 안 준 사용자 — 보낼 곳이 없다
            }

            PushMessage base = PushMessages.of(type, audience.actorNickname(), preview);
            PushMessage message = new PushMessage(base.title(), base.body(), base.type(), conversationId);

            List<String> dead = pushSender.send(audience.tokenValues(), message);

            deliveryRecorder.recordResult(audience.liveIdsExcluding(dead), dead);
        } catch (RuntimeException e) {
            // 토큰 원문·메시지 본문은 남기지 않는다(대화 내용이 서버 로그에 쌓이면 그 자체가 사고다).
            log.warn("[push] 발송 처리 실패 recipient={} type={}", recipientId, type, e);
        }
    }
}
