package com.honjeong.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.domain.ChatMessage;
import com.honjeong.chat.domain.Conversation;
import com.honjeong.chat.domain.MessageType;
import com.honjeong.chat.dto.ChatMessageResponse;
import com.honjeong.chat.dto.ConversationSummaryResponse;
import com.honjeong.chat.dto.SendMessageRequest;
import com.honjeong.chat.repository.ChatMessageRepository;
import com.honjeong.chat.repository.ConversationRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.repository.PlaceRepository;
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
    // 2026-07-25T03:00:00Z(UTC) == KST(UTC+9) 2026-07-25T12:00:00 — now()가 KST로 변환하는지도 함께 확인.
    Clock clock = Clock.fixed(Instant.parse("2026-07-25T03:00:00Z"), ZoneId.of("UTC"));
    LocalDateTime expectedNow = LocalDateTime.of(2026, 7, 25, 12, 0, 0);
    ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(conversationRepository, placeRepository, userRepository,
                chatMessageRepository, blockRepository, clock);
    }

    private User userRef(long id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Conversation activeConversation(long fromId, long toId) {
        return Conversation.open(1L, mock(Place.class), userRef(fromId), userRef(toId));
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
    void listMine은_상대정보와_안읽음수_미리보기를_담은_요약을_반환한다() {
        User me = userRef(10L);
        User partner = userRef(20L);
        lenient().when(partner.getNickname()).thenReturn("상대닉네임");
        lenient().when(partner.getProfileImageUrl()).thenReturn("https://img/partner.png");
        Place place = mock(Place.class);
        lenient().when(place.getName()).thenReturn("혼밥식당");
        Conversation conv = Conversation.open(1L, place, me, partner);
        conv.touch(expectedNow);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(-1L)); // 차단 없음(센티널)
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(conv));
        ChatMessage last = ChatMessage.text(conv, 20L, "마지막 메시지", expectedNow);
        when(chatMessageRepository.findByConversationIdOrderByIdAsc(conv.getId()))
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
    void listMine의_미리보기는_IMAGE_메시지면_사진으로_표시한다() {
        User me = userRef(10L);
        User partner = userRef(20L);
        Place place = mock(Place.class);
        lenient().when(place.getName()).thenReturn("혼밥식당");
        Conversation conv = Conversation.open(1L, place, me, partner);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(-1L)); // 차단 없음(센티널)
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(conv));
        ChatMessage last = ChatMessage.image(conv, 20L, "https://img/x.png", expectedNow);
        when(chatMessageRepository.findByConversationIdOrderByIdAsc(conv.getId()))
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
        Conversation blockedConv = Conversation.open(1L, place, me, blockedPartner);
        Conversation okConv = Conversation.open(2L, place, me, okPartner);
        when(blockRepository.findExclusionIds(10L)).thenReturn(List.of(20L)); // 20L 차단 관계
        when(conversationRepository.findAllForUser(10L)).thenReturn(List.of(blockedConv, okConv));
        lenient().when(chatMessageRepository.findByConversationIdOrderByIdAsc(any())).thenReturn(List.of());
        lenient().when(chatMessageRepository.countUnread(any(), any(), any())).thenReturn(0L);

        // when
        List<ConversationSummaryResponse> res = service.listMine(10L);

        // then: 차단 상대(20L)와의 대화는 빠지고, 나머지(30L)만 반환된다 — 닉네임·프로필사진 유출 방지
        assertThat(res).hasSize(1);
        assertThat(res.get(0).partnerUserId()).isEqualTo(30L);
    }
}
