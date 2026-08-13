package com.honjeong.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.domain.ChatMessage;
import com.honjeong.chat.domain.Conversation;
import com.honjeong.chat.domain.MessageType;
import com.honjeong.chat.dto.ChatMessageResponse;
import com.honjeong.chat.dto.ConversationSummaryResponse;
import com.honjeong.chat.dto.SendMessageRequest;
import com.honjeong.chat.repository.ChatMessageRepository;
import com.honjeong.chat.repository.ConversationRepository;
import com.honjeong.chat.ws.ChatEventBroadcaster;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.push.domain.PushType;
import com.honjeong.push.service.PushDispatcher;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * ConversationService의 메시징 확장(sendMessage/messages/markRead/listMine) 단위 테스트.
 * 순수 Mockito + 고정 Clock(KST 기준 now()가 무엇을 반환하는지 값으로 검증).
 */
@ExtendWith(MockitoExtension.class)
class ConversationMessagingTest {

    @Mock ConversationRepository conversationRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UserRepository userRepository;
    @Mock BlockRepository blockRepository;
    @Mock PushDispatcher pushDispatcher;
    @Mock ChatEventBroadcaster chatEventBroadcaster;
    // 2026-07-25T03:00:00Z(UTC) == KST(UTC+9) 2026-07-25T12:00:00 — now()가 KST로 변환하는지도 함께 확인.
    Clock clock = Clock.fixed(Instant.parse("2026-07-25T03:00:00Z"), ZoneId.of("UTC"));
    LocalDateTime expectedNow = LocalDateTime.of(2026, 7, 25, 12, 0, 0);
    ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(conversationRepository, placeRepository, userRepository,
                chatMessageRepository, blockRepository, pushDispatcher, clock, chatEventBroadcaster);
    }

    private User userRef(long id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Conversation activeConversation(long fromId, long toId) {
        return Conversation.open(1L, mock(Place.class), userRef(fromId), userRef(toId));
    }

    /** DB가 채우는 id를 단위 테스트에서 주입 — 미리보기 배치 조회가 대화방별로 정확히 매칭되는지 보려면 id가 필요하다. */
    private Conversation withId(Conversation conversation, long id) {
        ReflectionTestUtils.setField(conversation, "id", id);
        return conversation;
    }

    @Test
    void sendMessage_TEXT_정상이면_저장하고_touch하고_발신자를_읽음처리한다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        ChatMessageResponse res = service.sendMessage(10L, 5L,
                new SendMessageRequest(MessageType.TEXT, "안녕하세요", null));

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(1)).save(captor.capture());
        ChatMessage saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(MessageType.TEXT);
        assertThat(saved.getText()).isEqualTo("안녕하세요");
        assertThat(saved.getSenderUserId()).isEqualTo(10L);
        assertThat(saved.getCreatedAt()).isEqualTo(expectedNow);

        assertThat(conv.getLastMessageAt()).isEqualTo(expectedNow);
        assertThat(conv.lastReadAtFor(10L)).isEqualTo(expectedNow); // 보낸 사람은 읽은 것으로 처리
        assertThat(conv.lastReadAtFor(20L)).isNull(); // 상대는 아직 안읽음

        assertThat(res.type()).isEqualTo("TEXT");
        assertThat(res.text()).isEqualTo("안녕하세요");
        assertThat(res.senderUserId()).isEqualTo(10L);
    }

    @Test
    void sendMessage_종료된_대화엔_전송할_수_없다() {
        Conversation closed = activeConversation(10L, 20L);
        closed.close();
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.sendMessage(10L, 5L,
                new SendMessageRequest(MessageType.TEXT, "hi", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONVERSATION_CLOSED);

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void sendMessage_비참여자면_대화없음으로_위장한다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.sendMessage(999L, 5L,
                new SendMessageRequest(MessageType.TEXT, "hi", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void sendMessage_대화방자체가_없으면_대화없음() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(10L, 999L,
                new SendMessageRequest(MessageType.TEXT, "hi", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    void sendMessage_TEXT인데_공백이면_거부된다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.sendMessage(10L, 5L,
                new SendMessageRequest(MessageType.TEXT, "   ", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void sendMessage_IMAGE인데_imageUrl이_공백이면_거부된다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.sendMessage(10L, 5L,
                new SendMessageRequest(MessageType.IMAGE, null, "  ")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void sendMessage_IMAGE_정상이면_imageUrl을_저장한다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        ChatMessageResponse res = service.sendMessage(10L, 5L,
                new SendMessageRequest(MessageType.IMAGE, null, "https://img/x.png"));

        assertThat(res.type()).isEqualTo("IMAGE");
        assertThat(res.imageUrl()).isEqualTo("https://img/x.png");
        assertThat(res.text()).isNull();
    }

    @Test
    void sendMessage는_상대에게_CHAT_MESSAGE_푸시를_예약한다() {
        Conversation conv = withId(activeConversation(10L, 20L), 5L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));
        when(blockRepository.existsBlockBetween(10L, 20L)).thenReturn(false);

        service.sendMessage(10L, 5L, new SendMessageRequest(MessageType.TEXT, "어디세요?", null));

        verify(pushDispatcher).dispatch(20L, PushType.CHAT_MESSAGE, 10L, 5L, "어디세요?");
    }

    @Test
    void sendMessage는_수신자가_음소거한_대화엔_푸시를_보내지_않지만_소켓은_그대로_민다() {
        // 음소거는 '받는 쪽' 설정이다 — 보낸 사람(10)이 아니라 상대(20)가 껐을 때 막혀야 한다.
        Conversation conv = withId(activeConversation(10L, 20L), 5L);
        conv.setMuted(20L, true);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));
        when(blockRepository.existsBlockBetween(10L, 20L)).thenReturn(false);

        service.sendMessage(10L, 5L, new SendMessageRequest(MessageType.TEXT, "어디세요?", null));

        verify(pushDispatcher, never()).dispatch(anyLong(), any(), any(), any(), any());
        // 뮤트는 "알림 끄기"지 "화면 갱신 끄기"가 아니다 — 소켓 브로드캐스트는 뮤트와 무관하게 나가야 한다.
        // (푸시 가드 if문 안으로 브로드캐스트 호출이 실수로 들어가면 이 verify가 실패해서 잡아낸다.)
        verify(chatEventBroadcaster).broadcastMessage(eq(10L), eq(20L), eq(false), eq(5L), any(ChatMessageResponse.class));
    }

    @Test
    void sendMessage는_차단한_사이면_푸시를_보내지_않는다() {
        Conversation conv = withId(activeConversation(10L, 20L), 5L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));
        when(blockRepository.existsBlockBetween(10L, 20L)).thenReturn(true);

        service.sendMessage(10L, 5L, new SendMessageRequest(MessageType.TEXT, "어디세요?", null));

        verify(pushDispatcher, never()).dispatch(anyLong(), any(), any(), any(), any());
    }

    @Test
    void sendMessage의_사진_메시지는_미리보기가_대체문구다() {
        Conversation conv = withId(activeConversation(10L, 20L), 5L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));
        when(blockRepository.existsBlockBetween(10L, 20L)).thenReturn(false);

        service.sendMessage(10L, 5L,
                new SendMessageRequest(MessageType.IMAGE, null, "https://example.com/a.jpg"));

        verify(pushDispatcher).dispatch(20L, PushType.CHAT_MESSAGE, 10L, 5L, "사진을 보냈어요");
    }

    @Test
    void messages_비참여자면_대화없음() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.messages(999L, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    void messages_참여자면_작성순_목록을_DTO로_변환해_반환한다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));
        ChatMessage m1 = ChatMessage.text(conv, 10L, "first", expectedNow);
        ChatMessage m2 = ChatMessage.text(conv, 20L, "second", expectedNow.plusMinutes(1));
        when(chatMessageRepository.findByConversationIdOrderByIdAsc(5L)).thenReturn(List.of(m1, m2));

        List<ChatMessageResponse> res = service.messages(10L, 5L);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).text()).isEqualTo("first");
        assertThat(res.get(1).text()).isEqualTo("second");
    }

    @Test
    void markRead는_내_lastReadAt을_now로_갱신한다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        service.markRead(20L, 5L);

        assertThat(conv.lastReadAtFor(20L)).isEqualTo(expectedNow);
        assertThat(conv.lastReadAtFor(10L)).isNull();
    }

    @Test
    void markRead_비참여자면_대화없음() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.markRead(999L, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);
    }

    @Test
    void markRead는_읽을_것이_있었으면_읽음을_브로드캐스트한다() {
        // 상대(20)가 13:00에 마지막 메시지를 보냈고, 내(10) 읽음 시각은 12:00이다.
        Conversation conv = withId(activeConversation(10L, 20L), 5L);
        conv.touch(LocalDateTime.parse("2026-08-13T13:00:00"));
        conv.markRead(10L, LocalDateTime.parse("2026-08-13T12:00:00"));
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        service.markRead(10L, 5L);

        verify(chatEventBroadcaster).broadcastRead(eq(10L), eq(20L), anyBoolean(), eq(5L), any());
    }

    @Test
    void markRead는_읽을_것이_없었으면_브로드캐스트하지_않는다() {
        // 내(10) 읽음 시각이 이미 마지막 메시지 시각과 같다(내가 마지막으로 보낸 경우).
        // "읽음 시각이 바뀌었는가"는 조건이 될 수 없다 — markRead는 언제나 새 now를 쓰므로 항상 바뀐다.
        // 대화방을 열어만 놔도(새 메시지 없이) markRead가 반복 호출되므로, 이 가드가 없으면
        // 이벤트가 계속 나간다.
        Conversation conv = withId(activeConversation(10L, 20L), 5L);
        conv.touch(LocalDateTime.parse("2026-08-13T13:00:00"));
        conv.markRead(10L, LocalDateTime.parse("2026-08-13T13:00:00"));
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        service.markRead(10L, 5L);

        verify(chatEventBroadcaster, never()).broadcastRead(anyLong(), anyLong(), anyBoolean(), anyLong(), any());
    }

    @Test
    void setMuted는_내_음소거만_바꾼다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        service.setMuted(10L, 5L, true);

        assertThat(conv.isMutedBy(10L)).isTrue();
        assertThat(conv.isMutedBy(20L)).isFalse();
    }

    @Test
    void setMuted는_CLOSED_대화도_토글할_수_있다() {
        // 삭제(deleteForMe)와 달리 상태 제한이 없다 — 끝난 대화의 알림도 끌 수 있어야 한다.
        Conversation closed = activeConversation(10L, 20L);
        closed.close();
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(closed));

        service.setMuted(10L, 5L, true);

        assertThat(closed.isMutedBy(10L)).isTrue();
    }

    @Test
    void setMuted_비참여자면_대화없음으로_위장한다() {
        Conversation conv = activeConversation(10L, 20L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conv));

        assertThatThrownBy(() -> service.setMuted(999L, 5L, true))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONVERSATION_NOT_FOUND);

        assertThat(conv.isMutedBy(10L)).isFalse();
        assertThat(conv.isMutedBy(20L)).isFalse();
    }

    @Test
    void listMine의_muted는_상대가_아니라_조회자의_설정을_반영한다() {
        // given: 상대(20)만 음소거한 대화 — 내(10) 목록에는 muted=false로 보여야 한다
        User me = userRef(10L);
        User partner = userRef(20L);
        Place place = mock(Place.class);
        lenient().when(place.getName()).thenReturn("혼밥식당");
        Conversation conv = withId(Conversation.open(1L, place, me, partner), 7L);
        conv.setMuted(20L, true);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(-1L));
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(conv));
        lenient().when(chatMessageRepository.findLastMessagesByConversationIds(List.of(7L))).thenReturn(List.of());
        lenient().when(chatMessageRepository.countUnread(any(), any(), any())).thenReturn(0L);

        // when
        assertThat(service.listMine(10L).get(0).muted()).isFalse();

        // and: 내가 끄면 내 목록에만 반영된다
        conv.setMuted(10L, true);
        assertThat(service.listMine(10L).get(0).muted()).isTrue();
    }

    @Test
    void listMine은_상대정보와_안읽음수_미리보기를_담은_요약을_반환한다() {
        User me = userRef(10L);
        User partner = userRef(20L);
        lenient().when(partner.getNickname()).thenReturn("상대닉네임");
        lenient().when(partner.getProfileImageUrl()).thenReturn("https://img/partner.png");
        Place place = mock(Place.class);
        lenient().when(place.getName()).thenReturn("혼밥식당");
        Conversation conv = withId(Conversation.open(1L, place, me, partner), 7L);
        conv.touch(expectedNow);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(-1L)); // 차단 없음(센티널)
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(conv));
        ChatMessage last = ChatMessage.text(conv, 20L, "마지막 메시지", expectedNow);
        when(chatMessageRepository.findLastMessagesByConversationIds(List.of(7L)))
                .thenReturn(List.of(last));
        when(chatMessageRepository.countUnread(conv.getId(), 10L, conv.lastReadAtFor(10L))).thenReturn(3L);

        List<ConversationSummaryResponse> res = service.listMine(10L);

        assertThat(res).hasSize(1);
        ConversationSummaryResponse s = res.get(0);
        assertThat(s.partnerUserId()).isEqualTo(20L);
        assertThat(s.partnerNickname()).isEqualTo("상대닉네임");
        assertThat(s.partnerProfileImageUrl()).isEqualTo("https://img/partner.png");
        assertThat(s.placeName()).isEqualTo("혼밥식당");
        assertThat(s.lastMessagePreview()).isEqualTo("마지막 메시지");
        assertThat(s.lastMessageAt()).isEqualTo(expectedNow);
        assertThat(s.unreadCount()).isEqualTo(3L);
        assertThat(s.status()).isEqualTo("ACTIVE");
    }

    @Test
    void listMine은_탈퇴한_상대의_닉네임을_알수없음으로_표시한다() {
        User me = userRef(10L);
        User partner = userRef(20L);
        lenient().when(partner.getNickname()).thenReturn(null); // 탈퇴로 닉네임이 사라짐
        Place place = mock(Place.class);
        lenient().when(place.getName()).thenReturn("혼밥식당");
        Conversation conv = withId(Conversation.open(1L, place, me, partner), 7L);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(-1L)); // 차단 없음(센티널)
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(conv));
        lenient().when(chatMessageRepository.findLastMessagesByConversationIds(List.of(7L))).thenReturn(List.of());
        lenient().when(chatMessageRepository.countUnread(any(), any(), any())).thenReturn(0L);

        List<ConversationSummaryResponse> res = service.listMine(10L);

        assertThat(res.get(0).partnerNickname()).isEqualTo("알 수 없음");
    }

    @Test
    void listMine의_미리보기는_IMAGE_메시지면_사진으로_표시한다() {
        User me = userRef(10L);
        User partner = userRef(20L);
        Place place = mock(Place.class);
        lenient().when(place.getName()).thenReturn("혼밥식당");
        Conversation conv = withId(Conversation.open(1L, place, me, partner), 7L);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(-1L)); // 차단 없음(센티널)
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(conv));
        ChatMessage last = ChatMessage.image(conv, 20L, "https://img/x.png", expectedNow);
        when(chatMessageRepository.findLastMessagesByConversationIds(List.of(7L)))
                .thenReturn(List.of(last));
        lenient().when(chatMessageRepository.countUnread(any(), any(), any())).thenReturn(0L);

        List<ConversationSummaryResponse> res = service.listMine(10L);

        assertThat(res.get(0).lastMessagePreview()).isEqualTo("사진");
    }

    @Test
    void listMine은_차단된_상대와의_대화는_목록에서_제외한다() {
        // given: 나(10)와 차단 상대(20)의 대화 하나, 차단 아닌 상대(30)와의 대화 하나
        User me = userRef(10L);
        User blockedPartner = userRef(20L);
        User okPartner = userRef(30L);
        Place place = mock(Place.class);
        lenient().when(place.getName()).thenReturn("혼밥식당");
        Conversation blockedConv = withId(Conversation.open(1L, place, me, blockedPartner), 100L);
        Conversation okConv = withId(Conversation.open(2L, place, me, okPartner), 200L);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(20L)); // 20L 차단 관계
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(blockedConv, okConv));
        lenient().when(chatMessageRepository.findLastMessagesByConversationIds(any())).thenReturn(List.of());
        lenient().when(chatMessageRepository.countUnread(any(), any(), any())).thenReturn(0L);

        // when
        List<ConversationSummaryResponse> res = service.listMine(10L);

        // then: 차단 상대(20L)와의 대화는 빠지고, 나머지(30L)만 반환된다 — 닉네임·프로필사진 유출 방지
        assertThat(res).hasSize(1);
        assertThat(res.get(0).partnerUserId()).isEqualTo(30L);
        // and: 미리보기 배치 조회도 차단 대화(100L)를 제외한 id만 넘긴다(필터 후 조회)
        verify(chatMessageRepository).findLastMessagesByConversationIds(List.of(200L));
    }

    @Test
    void listMine은_대화가_하나도_없으면_미리보기_쿼리를_호출하지_않는다() {
        // given: 참여 중인 대화 0건 — 빈 IN 절로 미리보기 쿼리를 부르면 안 된다
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(-1L));
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of());

        // when
        List<ConversationSummaryResponse> res = service.listMine(10L);

        // then
        assertThat(res).isEmpty();
        verify(chatMessageRepository, never()).findLastMessagesByConversationIds(any());
    }
}
