package com.honjeong.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.push.domain.PushType;
import com.honjeong.push.service.PushAudience.TokenRef;

/**
 * PushSendTask 단위 테스트 — <b>무엇을 보내려 했는가</b>를 값까지 본다.
 *
 * <p>스펙 §7 표의 첫 항목이 이것인데 지금까지 어디에도 없었다. ConversationMessagingTest는
 * {@code pushDispatcher.dispatch(...)}의 인자만 보고(발송기까지 가지 않는다),
 * PushCommitBoundaryTest는 {@code send(anyList(), any())}로 <b>값을 보지 않는다</b>.
 * 그래서 배너 본문·data가 뭐가 되든 전부 초록불이었다.
 *
 * <p>여기서 덮는 경로: 토큰 0건 조기 반환 / 닉네임이 본문에 들어가는지 / conversationId 주입 /
 * 죽은 토큰과 산 토큰이 기록 구간에 제대로 갈리는지 / actorId가 null인 뱃지 경로 /
 * 발송기 예외가 밖으로 새지 않는지.
 *
 * <p>{@code DisplayNames} 통과 여부는 조회 구간의 책임이라 {@link PushAudienceReaderTest}가 본다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PushSendTask")
class PushSendTaskTest {

    private static final TokenRef PHONE = new TokenRef(1L, "tok-phone");
    private static final TokenRef PAD = new TokenRef(2L, "tok-pad");

    @Mock
    private PushAudienceReader audienceReader;
    @Mock
    private PushSender pushSender;
    @Mock
    private PushDeliveryRecorder deliveryRecorder;
    @InjectMocks
    private PushSendTask task;

    @Test
    @DisplayName("토큰이 0건이면 발송기도 기록도 부르지 않는다 — 푸시 권한을 안 준 사용자")
    void 토큰이_없으면_아무것도_하지_않는다() {
        given(audienceReader.read(7L, 9L)).willReturn(PushAudience.EMPTY);

        task.send(7L, PushType.MATE_REQUEST_RECEIVED, 9L, null, null);

        verify(pushSender, never()).send(anyList(), any());
        verify(deliveryRecorder, never()).recordResult(anyList(), anyList());
    }

    @Test
    @DisplayName("조회 구간이 준 닉네임으로 배너 문구를 만든다 — 제목·본문·종류를 값까지")
    void 배너_문구를_값까지_만든다() {
        given(audienceReader.read(7L, 9L)).willReturn(new PushAudience(List.of(PHONE), "김하늘"));
        given(pushSender.send(anyList(), any())).willReturn(List.of());

        task.send(7L, PushType.MEAL_REQUEST_RECEIVED, 9L, null, null);

        PushMessage sent = captureMessage();
        assertThat(sent.title()).isEqualTo("혼정");
        assertThat(sent.body()).isEqualTo("김하늘님이 같이 먹기를 신청했어요");
        assertThat(sent.type()).isEqualTo(PushType.MEAL_REQUEST_RECEIVED);
        assertThat(sent.conversationId()).isNull();
    }

    @Test
    @DisplayName("채팅은 conversationId를 메시지에 주입한다 — 앱이 이 값으로 대화방을 연다")
    void 채팅은_대화방_id를_싣는다() {
        given(audienceReader.read(7L, 9L)).willReturn(new PushAudience(List.of(PHONE), "김하늘"));
        given(pushSender.send(anyList(), any())).willReturn(List.of());

        task.send(7L, PushType.CHAT_MESSAGE, 9L, 42L, "어디세요?");

        PushMessage sent = captureMessage();
        assertThat(sent.body()).isEqualTo("김하늘: 어디세요?");
        assertThat(sent.conversationId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("뱃지(actorId=null)는 상대가 없어 닉네임 없이 보낸다")
    void 뱃지는_상대가_없다() {
        given(audienceReader.read(7L, null)).willReturn(new PushAudience(List.of(PHONE), null));
        given(pushSender.send(anyList(), any())).willReturn(List.of());

        task.send(7L, PushType.BADGE_EARNED, null, null, null);

        assertThat(captureMessage().body()).isEqualTo("새 뱃지를 획득했어요 🎉");
        verify(audienceReader).read(7L, null);
    }

    @Test
    @DisplayName("수신자의 모든 기기 토큰을 발송기에 넘긴다")
    void 모든_기기로_보낸다() {
        given(audienceReader.read(7L, 9L)).willReturn(new PushAudience(List.of(PHONE, PAD), "김하늘"));
        given(pushSender.send(anyList(), any())).willReturn(List.of());

        task.send(7L, PushType.MATE_REQUEST_ACCEPTED, 9L, null, null);

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.captor();
        verify(pushSender).send(tokens.capture(), any());
        assertThat(tokens.getValue()).containsExactly("tok-phone", "tok-pad");
    }

    @Test
    @DisplayName("죽은 토큰은 삭제 대상으로, 나머지는 사용시각 갱신 대상으로 기록 구간에 넘긴다")
    void 산_토큰과_죽은_토큰을_갈라_기록한다() {
        given(audienceReader.read(7L, 9L)).willReturn(new PushAudience(List.of(PHONE, PAD), "김하늘"));
        given(pushSender.send(anyList(), any())).willReturn(List.of("tok-pad"));

        task.send(7L, PushType.MATE_REQUEST_ACCEPTED, 9L, null, null);

        verify(deliveryRecorder).recordResult(List.of(1L), List.of("tok-pad"));
    }

    @Test
    @DisplayName("전부 성공하면 삭제 대상은 비고 모든 토큰이 사용시각 갱신 대상이다")
    void 전부_성공하면_삭제할_것이_없다() {
        given(audienceReader.read(7L, 9L)).willReturn(new PushAudience(List.of(PHONE, PAD), "김하늘"));
        given(pushSender.send(anyList(), any())).willReturn(List.of());

        task.send(7L, PushType.MATE_REQUEST_ACCEPTED, 9L, null, null);

        verify(deliveryRecorder).recordResult(List.of(1L, 2L), List.of());
    }

    @Test
    @DisplayName("발송기가 터져도 예외가 밖으로 새지 않는다 — 커밋 후 경로라 받을 사람이 없다")
    void 발송_실패는_삼킨다() {
        given(audienceReader.read(7L, 9L)).willReturn(new PushAudience(List.of(PHONE), "김하늘"));
        willThrow(new RuntimeException("boom")).given(pushSender).send(anyList(), any());

        task.send(7L, PushType.MATE_REQUEST_ACCEPTED, 9L, null, null); // 예외가 나면 테스트 실패

        verify(deliveryRecorder, never()).recordResult(anyList(), anyList());
    }

    @Test
    @DisplayName("조회가 터져도 예외가 밖으로 새지 않는다")
    void 조회_실패도_삼킨다() {
        willThrow(new RuntimeException("boom")).given(audienceReader).read(7L, null);

        task.send(7L, PushType.BADGE_EARNED, null, null, null); // 예외가 나면 테스트 실패

        verify(pushSender, never()).send(anyList(), isNull());
    }

    private PushMessage captureMessage() {
        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSender).send(anyList(), captor.capture());
        return captor.getValue();
    }
}
