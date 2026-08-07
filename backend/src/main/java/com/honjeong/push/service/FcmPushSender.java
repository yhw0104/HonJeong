package com.honjeong.push.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

/**
 * FCM 실발송기. <b>이 프로젝트에서 FCM을 아는 유일한 자리다.</b>
 *
 * <p>사용처: PushSendTask.
 *
 * <p>자격증명(서비스 계정 JSON을 base64로 인코딩한 값)이 없으면 <b>생성자에서 기동을 실패시킨다.</b>
 * 08-03에 {@code ${JWT_SECRET}}이 해석되지 않은 채 리터럴 문자열로 서명 키가 된 사고가 있었다 —
 * 스프링의 프로퍼티 바인딩은 미해결 플레이스홀더에 예외를 던지지 않고 문자열을 그대로 넘긴다.
 * 그래서 fail-fast를 직접 만들어 줘야 한다.
 *
 * <p>생성자가 둘이므로 {@code @Autowired}를 명시한다 — 없으면 스프링이 다른 생성자를 찾다가
 * 기동에 실패한다(07-27 KakaoOAuthVerifier에서 겪은 사고).
 */
@Component
@ConditionalOnProperty(name = "honjeong.push.mode", havingValue = "real")
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);
    private static final String APP_NAME = "honjeong-push";

    private final FirebaseMessaging messaging;

    /**
     * 운영 생성자 — 자격증명을 검증한 뒤 FirebaseApp을 초기화한다.
     *
     * @param credentialsBase64 서비스 계정 JSON을 base64로 인코딩한 값
     */
    @Autowired
    public FcmPushSender(@Value("${honjeong.push.credentials-base64}") String credentialsBase64) {
        Assert.hasText(credentialsBase64,
                "honjeong.push.credentials-base64가 비어 있습니다. push.mode=real에는 Firebase 서비스 계정이 필요합니다.");
        Assert.isTrue(!credentialsBase64.startsWith("${"),
                "honjeong.push.credentials-base64가 해석되지 않았습니다(FIREBASE_CREDENTIALS_BASE64 미주입).");
        this.messaging = FirebaseMessaging.getInstance(initApp(credentialsBase64));
        log.info("[push] real 모드 — FCM으로 발송합니다.");
    }

    private FcmPushSender(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    /**
     * 테스트 전용 팩토리 — 외부 호출 없이 발송 규칙만 검증할 때 쓴다.
     *
     * @param messaging 목 FirebaseMessaging
     * @return 그 목을 쓰는 발송기
     */
    public static FcmPushSender withMessaging(FirebaseMessaging messaging) {
        return new FcmPushSender(messaging);
    }

    private static FirebaseApp initApp(String credentialsBase64) {
        return FirebaseApp.getApps().stream()
                .filter(a -> APP_NAME.equals(a.getName()))
                .findFirst()
                .orElseGet(() -> {
                    try {
                        byte[] json = Base64.getDecoder().decode(credentialsBase64);
                        GoogleCredentials creds = GoogleCredentials.fromStream(new ByteArrayInputStream(json));
                        return FirebaseApp.initializeApp(
                                FirebaseOptions.builder().setCredentials(creds).build(), APP_NAME);
                    } catch (IllegalArgumentException | IOException e) {
                        throw new IllegalStateException(
                                "Firebase 자격증명을 읽을 수 없습니다(base64 또는 JSON 형식 확인).", e);
                    }
                });
    }

    /**
     * 기기별로 하나씩 보낸다. 한 토큰이 실패해도 나머지 발송을 계속한다.
     *
     * @param tokens  대상 FCM 토큰 목록
     * @param message 보낼 내용
     * @return 영구 무효로 판정돼 삭제해야 하는 토큰 목록
     */
    @Override
    public List<String> send(List<String> tokens, PushMessage message) {
        List<String> dead = new ArrayList<>();
        for (String token : tokens) {
            try {
                messaging.send(build(token, message));
            } catch (FirebaseMessagingException e) {
                if (isPermanentlyInvalid(e)) {
                    dead.add(token);
                } else {
                    // 일시적 실패는 재시도하지 않는다 — 푸시는 배달 보장이 없는 채널이고
                    // 폴링이 안전망으로 남아 있다(docs/08-실시간-전략.md §8).
                    log.warn("[push] 발송 실패(일시적) type={} code={}", message.type(), e.getMessagingErrorCode());
                }
            } catch (RuntimeException e) {
                log.warn("[push] 발송 실패(예상 밖) type={}", message.type(), e);
            }
        }
        return dead;
    }

    private static boolean isPermanentlyInvalid(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private static Message build(String token, PushMessage m) {
        Message.Builder b = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder().setTitle(m.title()).setBody(m.body()).build())
                .putData("type", m.type().name());
        if (m.conversationId() != null) {
            b.putData("conversationId", String.valueOf(m.conversationId()));
        }
        return b.build();
    }
}
