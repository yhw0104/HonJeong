package com.honjeong.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/**
 * Conversation/ChatMessage 매핑 슬라이스 테스트. 실제 Postgres(Testcontainers)에서 매핑·FK·엔티티 동작을 검증한다.
 * conversations.meal_request_id는 meal_requests(id)를 참조하는 NOT NULL FK이므로, 실제 CheckIn·MealRequest
 * 행을 만들어 그 id를 사용한다(다른 슬라이스 테스트와 동일한 패턴).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ChatMappingTest extends AbstractPostgresTest {

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 25, 12, 0);

    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    private Place persistPlace(String sourceId) {
        return em.persist(Place.ofPublicData(sourceId, sourceId + "식당", "한식", "서울 어딘가", "서울 도로명",
                37.5, 127.0, null, "영업"));
    }

    @Test
    @DisplayName("대화방과 메시지를 저장하고 읽는다")
    void 대화방과_메시지를_저장하고_읽는다() {
        // given: from이 to에게 같이먹기를 신청해 매칭된 상황 — meal_request_id FK를 만족할 실제 신청 1건
        User from = persistUser("01000000001", "보낸이");
        User to = persistUser("01000000002", "받는이");
        Place place = persistPlace("p1");
        CheckIn toCheckIn = em.persist(CheckIn.startSeeking(to, place, NOW));
        MealRequest mealRequest = em.persist(MealRequest.create(from, toCheckIn, place, "같이 드실래요?", NOW));
        mealRequest.accept(NOW);
        em.flush();

        Conversation conv = Conversation.open(mealRequest.getId(), place, from, to);
        ChatMessage msg = ChatMessage.text(conv, from.getId(), "곧 도착해요", NOW);
        conv.touch(NOW);
        conv.markRead(from.getId(), NOW);

        em.persist(conv);
        em.persist(msg);
        em.flush();
        em.clear();

        // when
        Conversation loaded = em.find(Conversation.class, conv.getId());

        // then: 대화방 상태·참여자·마지막 메시지 시각·읽음 시각이 저장/조회된다
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.isParticipant(from.getId())).isTrue();
        assertThat(loaded.isParticipant(to.getId())).isTrue();
        assertThat(loaded.isParticipant(999L)).isFalse();
        assertThat(loaded.getMealRequestId()).isEqualTo(mealRequest.getId());
        assertThat(loaded.getPlace().getId()).isEqualTo(place.getId());
        assertThat(loaded.getFromUser().getId()).isEqualTo(from.getId());
        assertThat(loaded.getToUser().getId()).isEqualTo(to.getId());
        assertThat(loaded.getLastMessageAt()).isEqualTo(NOW);
        assertThat(loaded.lastReadAtFor(from.getId())).isEqualTo(NOW);
        assertThat(loaded.lastReadAtFor(to.getId())).isNull();
        assertThat(loaded.partnerOf(from.getId())).isEqualTo(to.getId());
        assertThat(loaded.partnerOf(to.getId())).isEqualTo(from.getId());
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();

        // and: close() 이후엔 읽기전용(ACTIVE 아님)으로 전환된다
        loaded.close();
        assertThat(loaded.isActive()).isFalse();

        // and: 저장된 메시지도 함께 조회된다
        ChatMessage loadedMsg = em.find(ChatMessage.class, msg.getId());
        assertThat(loadedMsg.getType()).isEqualTo(MessageType.TEXT);
        assertThat(loadedMsg.getText()).isEqualTo("곧 도착해요");
        assertThat(loadedMsg.getImageUrl()).isNull();
        assertThat(loadedMsg.getSenderUserId()).isEqualTo(from.getId());
        assertThat(loadedMsg.getConversation().getId()).isEqualTo(conv.getId());
        assertThat(loadedMsg.getCreatedAt()).isEqualTo(NOW);
    }
}
