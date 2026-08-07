package com.honjeong.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.honjeong.push.domain.PushType;

/**
 * FcmPushSender 단위 테스트 — 외부 호출 없이 <b>죽은 토큰 판정</b>과 <b>페이로드 모양</b>만 본다.
 *
 * <p>구성: {@link FcmPushSender#withMessaging}에 목 {@link FirebaseMessaging}을 넣는다. 운영 생성자는
 * 서비스 계정 자격증명을 요구하고 FirebaseApp을 초기화하므로 단위 테스트에서 쓸 수 없다 —
 * 그 팩토리가 존재하는 이유가 이것이다.
 *
 * <p><b>이 테스트가 잡는 사고.</b> 판정이 넓으면 발송기 하나가 사용자의 푸시를 통째로 끊는다.
 * {@code INVALID_ARGUMENT}(= 우리 페이로드가 잘못됨)를 무효 토큰으로 오판하면 긴 메시지 한 건이
 * 상대의 토큰을 지우고, 그 사용자는 앱을 다시 켤 때까지 아무 푸시도 못 받는다. 조용히.
 */
@DisplayName("FcmPushSender")
class FcmPushSenderTest {

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = FcmPushSender.withMessaging(messaging);

    private static final PushMessage CHAT =
            new PushMessage("혼정", "김하늘: 어디세요?", PushType.CHAT_MESSAGE, 42L);

    @Test
    @DisplayName("UNREGISTERED(앱 삭제)는 죽은 토큰으로 판정한다")
    void 미등록_토큰은_죽는다() throws Exception {
        givenSendFails(MessagingErrorCode.UNREGISTERED);

        assertThat(sender.send(List.of("tok-dead"), CHAT)).containsExactly("tok-dead");
    }

    @Test
    @DisplayName("SENDER_ID_MISMATCH(다른 프로젝트의 토큰)도 죽은 토큰으로 판정한다")
    void 발신자_불일치도_죽는다() throws Exception {
        givenSendFails(MessagingErrorCode.SENDER_ID_MISMATCH);

        assertThat(sender.send(List.of("tok-other-project"), CHAT)).containsExactly("tok-other-project");
    }

    @Test
    @DisplayName("일시적 오류(UNAVAILABLE)는 토큰을 지우지 않는다")
    void 일시적_오류는_살려둔다() throws Exception {
        givenSendFails(MessagingErrorCode.UNAVAILABLE);

        assertThat(sender.send(List.of("tok-live"), CHAT)).isEmpty();
    }

    @Test
    @DisplayName("INVALID_ARGUMENT는 토큰을 지우지 않는다 — 토큰이 아니라 페이로드가 잘못됐다는 뜻이다")
    void 잘못된_인자는_토큰을_지우지_않는다() throws Exception {
        givenSendFails(MessagingErrorCode.INVALID_ARGUMENT);

        assertThat(sender.send(List.of("tok-live"), CHAT)).isEmpty();
    }

    @Test
    @DisplayName("한 토큰이 죽어도 나머지 기기 발송은 계속한다")
    void 기기별로_독립_처리한다() throws Exception {
        FirebaseMessagingException dead = messagingException(MessagingErrorCode.UNREGISTERED);
        given(messaging.send(any())).willThrow(dead).willReturn("ok");

        assertThat(sender.send(List.of("tok-dead", "tok-live"), CHAT)).containsExactly("tok-dead");
        verify(messaging, times(2)).send(any());
    }

    @Test
    @DisplayName("data에는 type과 conversationId만 담는다 — 앱이 이동·무효화 판단에 쓰는 두 값")
    void 채팅_페이로드_data() throws Exception {
        sender.send(List.of("tok-1"), CHAT);

        assertThat(dataOf(captureSent()))
                .containsExactlyInAnyOrderEntriesOf(Map.of("type", "CHAT_MESSAGE", "conversationId", "42"));
    }

    @Test
    @DisplayName("채팅이 아니면 conversationId 키 자체가 없다")
    void 알림_페이로드_data() throws Exception {
        sender.send(List.of("tok-1"),
                new PushMessage("혼정", "김하늘님이 메이트를 신청했어요", PushType.MATE_REQUEST_RECEIVED, null));

        assertThat(dataOf(captureSent())).containsExactlyInAnyOrderEntriesOf(Map.of("type", "MATE_REQUEST_RECEIVED"));
    }

    private void givenSendFails(MessagingErrorCode code) throws FirebaseMessagingException {
        // 예외 목을 먼저 완성한다 — given(...) 안에서 또 given(...)을 부르면 Mockito가
        // 미완성 스터빙(UnfinishedStubbingException)으로 본다.
        FirebaseMessagingException failure = messagingException(code);
        given(messaging.send(any())).willThrow(failure);
    }

    /**
     * 주어진 오류 코드를 가진 예외 목.
     *
     * <p>{@link FirebaseMessagingException}은 final이고 생성자가 패키지 전용이라 직접 만들 수 없다.
     * Mockito 5의 기본 mock-maker(inline)가 final 클래스를 목으로 만들 수 있어 이 방법을 쓴다.
     *
     * @param code FCM이 돌려주는 오류 코드
     * @return 그 코드를 반환하는 예외
     */
    private static FirebaseMessagingException messagingException(MessagingErrorCode code) {
        FirebaseMessagingException e = mock(FirebaseMessagingException.class);
        given(e.getMessagingErrorCode()).willReturn(code);
        return e;
    }

    private Message captureSent() throws FirebaseMessagingException {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messaging).send(captor.capture());
        return captor.getValue();
    }

    /**
     * 보낸 {@link Message}의 data 맵.
     *
     * <p>{@code Message.getData()}는 패키지 전용이라(직렬화 전용 객체다) 리플렉션으로 읽는다.
     * firebase-admin 9.4.3 기준 필드명이 {@code data}다 — 버전을 올려 필드명이 바뀌면 이 테스트가
     * 먼저 빨개져 알려준다.
     *
     * @param message 발송기가 만든 메시지
     * @return data 맵
     * @throws ReflectiveOperationException 필드가 없거나 접근할 수 없을 때
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> dataOf(Message message) throws ReflectiveOperationException {
        Field field = Message.class.getDeclaredField("data");
        field.setAccessible(true);
        return (Map<String, String>) field.get(message);
    }
}
