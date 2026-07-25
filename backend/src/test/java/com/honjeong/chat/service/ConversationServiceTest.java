package com.honjeong.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.chat.domain.Conversation;
import com.honjeong.chat.repository.ChatMessageRepository;
import com.honjeong.chat.repository.ConversationRepository;
import com.honjeong.place.domain.Place;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * ConversationService 단위 테스트(순수 Mockito + 고정 Clock). open/close의 멱등성과
 * findIdByMealRequestId의 존재/부재 분기를 값으로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock ConversationRepository conversationRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UserRepository userRepository;
    Clock clock = Clock.fixed(Instant.parse("2026-07-25T03:00:00Z"), ZoneId.of("UTC"));
    ConversationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationService(conversationRepository, placeRepository, userRepository,
                chatMessageRepository, clock);
    }

    /**
     * id를 가진 User mock — 엔티티 연관관계 자리에 DB 없이 대입한다. getId() 스텁은 이 헬퍼를 공유하는
     * 테스트마다 실제로 쓰이지 않을 수 있어(참조 동일성만 비교하는 케이스 등) lenient로 둔다.
     */
    private User userRef(long id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    @Test
    void open은_이미_있으면_새로_만들지_않는다_멱등() {
        when(conversationRepository.findByMealRequestId(1L)).thenReturn(Optional.of(mock(Conversation.class)));

        service.open(1L, 10L, 20L, 100L);

        verify(conversationRepository, never()).save(any());
    }

    @Test
    void open은_없으면_레퍼런스로_생성해서_저장한다() {
        when(conversationRepository.findByMealRequestId(1L)).thenReturn(Optional.empty());
        Place place = mock(Place.class);
        User fromUser = userRef(10L);
        User toUser = userRef(20L);
        when(placeRepository.getReferenceById(100L)).thenReturn(place);
        when(userRepository.getReferenceById(10L)).thenReturn(fromUser);
        when(userRepository.getReferenceById(20L)).thenReturn(toUser);

        service.open(1L, 10L, 20L, 100L);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository, times(1)).save(captor.capture());
        Conversation saved = captor.getValue();
        assertThat(saved.getMealRequestId()).isEqualTo(1L);
        assertThat(saved.getPlace()).isSameAs(place);
        assertThat(saved.getFromUser()).isSameAs(fromUser);
        assertThat(saved.getToUser()).isSameAs(toUser);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void close는_ACTIVE면_닫고_없으면_무해() {
        Conversation conv = Conversation.open(1L, mock(Place.class), userRef(10L), userRef(20L));
        when(conversationRepository.findByMealRequestId(1L)).thenReturn(Optional.of(conv));

        service.close(1L);

        assertThat(conv.getStatus().name()).isEqualTo("CLOSED");

        when(conversationRepository.findByMealRequestId(2L)).thenReturn(Optional.empty());
        service.close(2L); // 예외 없이 무해
    }

    @Test
    void close는_이미_CLOSED면_다시_닫지_않는다_멱등() {
        Conversation conv = mock(Conversation.class);
        when(conv.isActive()).thenReturn(false);
        when(conversationRepository.findByMealRequestId(1L)).thenReturn(Optional.of(conv));

        service.close(1L);

        verify(conv, never()).close();
    }

    @Test
    void findIdByMealRequestId는_있으면_대화id_없으면_null() {
        Conversation conv = mock(Conversation.class);
        when(conv.getId()).thenReturn(999L);
        when(conversationRepository.findByMealRequestId(1L)).thenReturn(Optional.of(conv));
        when(conversationRepository.findByMealRequestId(2L)).thenReturn(Optional.empty());

        assertThat(service.findIdByMealRequestId(1L)).isEqualTo(999L);
        assertThat(service.findIdByMealRequestId(2L)).isNull();
    }
}
