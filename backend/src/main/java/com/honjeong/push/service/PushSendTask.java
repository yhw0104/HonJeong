package com.honjeong.push.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.global.common.DisplayNames;
import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.domain.PushType;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.user.repository.UserRepository;

/**
 * 실제 푸시 발송 — 도메인 트랜잭션이 커밋된 <b>뒤에</b> 별도 스레드에서 돈다.
 *
 * <p>사용처: PushDispatcher(커밋 후 훅).
 *
 * <p>PushDispatcher와 클래스를 분리한 이유: 같은 클래스 안에서 {@code @Async} 메서드를
 * 호출하면 스프링 프록시를 타지 않아 그냥 동기 실행된다. 별도 빈이어야 실제로 비동기가 된다
 * (06-13에 PhoneAttemptRecorder를 별도 빈으로 뺀 것과 같은 이유).
 *
 * <p>토큰·닉네임 조회를 여기서 하는 이유: 도메인 트랜잭션 밖이어야 그만큼 그 트랜잭션이 짧아진다.
 *
 * <p>{@code readOnly = true}가 <b>아닌</b> 이유: 이 메서드는 읽기만 하지 않는다 —
 * {@link DeviceToken#markUsed}의 더티체킹 UPDATE와 죽은 토큰의 DELETE를 여기서 커밋해야 한다.
 * {@code readOnly}로 두면 그 두 쓰기가 조용히 사라지거나 예외가 된다.
 */
@Component
public class PushSendTask {

    private static final Logger log = LoggerFactory.getLogger(PushSendTask.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final PushSender pushSender;
    private final Clock clock;

    public PushSendTask(DeviceTokenRepository deviceTokenRepository, UserRepository userRepository,
            PushSender pushSender, Clock clock) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
        this.pushSender = pushSender;
        this.clock = clock;
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
    @Transactional
    public void send(Long recipientId, PushType type, Long actorId, Long conversationId, String preview) {
        try {
            List<DeviceToken> tokens = deviceTokenRepository.findAllByUser_Id(recipientId);
            if (tokens.isEmpty()) {
                return; // 푸시 권한을 안 준 사용자 — 보낼 곳이 없다
            }
            String nickname = actorId == null ? null
                    : userRepository.findById(actorId)
                            .map(u -> DisplayNames.nicknameOrUnknown(u.getNickname()))
                            .orElse(null);

            PushMessage base = PushMessages.of(type, nickname, preview);
            PushMessage message = new PushMessage(base.title(), base.body(), base.type(), conversationId);

            List<String> dead = pushSender.send(tokens.stream().map(DeviceToken::getToken).toList(), message);

            LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
            tokens.stream().filter(t -> !dead.contains(t.getToken())).forEach(t -> t.markUsed(now));
            // 죽은 토큰을 안 지우면 계정마다 쓰레기가 쌓여 실패 호출만 늘어난다.
            dead.forEach(deviceTokenRepository::deleteByToken);
        } catch (RuntimeException e) {
            // 토큰 원문·메시지 본문은 남기지 않는다(대화 내용이 서버 로그에 쌓이면 그 자체가 사고다).
            log.warn("[push] 발송 처리 실패 recipient={} type={}", recipientId, type, e);
        }
    }
}
