package com.honjeong.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.chat.domain.ChatMessage;
import com.honjeong.chat.domain.Conversation;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/**
 * ConversationRepository/ChatMessageRepository 슬라이스 테스트. 실제 Postgres(Testcontainers)에서
 * 목록 정렬(fetch join)과 안읽음 집계 쿼리를 검증한다. conversations.meal_request_id는 meal_requests(id)를
 * 참조하는 NOT NULL UNIQUE FK이므로, 대화마다 실제 CheckIn·MealRequest 행을 만들어 그 id를 쓴다
 * ({@link com.honjeong.chat.domain.ChatMappingTest}와 동일한 패턴).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class ConversationRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

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

    /** meal_request_id UNIQUE FK를 만족할 실제 매칭(수락된 같이먹기 신청) 1건을 만들어 그 id를 돌려준다. */
    private Long persistAcceptedMealRequestId(User from, User to, Place place) {
        CheckIn toCheckIn = em.persist(CheckIn.startSeeking(to, place, NOW));
        MealRequest mealRequest = em.persist(MealRequest.create(from, toCheckIn, place, "같이 드실래요?", NOW));
        mealRequest.accept(NOW);
        return mealRequest.getId();
    }

    @Test
    @DisplayName("findAllForUser: 내가 from이든 to든 참여한 대화를 최근메시지순으로, fetch join으로 반환한다")
    void 사용자_대화목록은_최근메시지순으로_반환된다() {
        // given: 나(me) + 상대 2명, 대화 2개(conv1=내가 fromUser, conv2=내가 toUser)
        User me = persistUser("01000000001", "나");
        User partner1 = persistUser("01000000002", "상대1");
        User partner2 = persistUser("01000000003", "상대2");
        Place place1 = persistPlace("p1");
        Place place2 = persistPlace("p2");

        Long mealRequestId1 = persistAcceptedMealRequestId(me, partner1, place1);
        Long mealRequestId2 = persistAcceptedMealRequestId(partner2, me, place2);

        Conversation conv1 = Conversation.open(mealRequestId1, place1, me, partner1);
        conv1.touch(NOW);
        Conversation conv2 = Conversation.open(mealRequestId2, place2, partner2, me);
        conv2.touch(NOW.plusMinutes(10)); // conv2가 더 최근

        em.persist(conv1);
        em.persist(conv2);
        em.flush();
        em.clear();

        // when
        List<Conversation> list = conversationRepository.findAllForUser(me.getId());

        // then: 최근메시지순(conv2 먼저), 크기 2
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(conv2.getId());
        assertThat(list.get(1).getId()).isEqualTo(conv1.getId());

        // and: fetch join으로 place/fromUser/toUser가 clear 이후에도 LazyInitializationException 없이 읽힌다
        assertThat(list.get(0).getPlace().getName()).isEqualTo("p2식당");
        assertThat(list.get(0).getFromUser().getNickname()).isEqualTo("상대2");
        assertThat(list.get(0).getToUser().getNickname()).isEqualTo("나");
        assertThat(list.get(1).getPlace().getName()).isEqualTo("p1식당");
        assertThat(list.get(1).getFromUser().getNickname()).isEqualTo("나");
        assertThat(list.get(1).getToUser().getNickname()).isEqualTo("상대1");
    }

    @Test
    @DisplayName("countUnread: 상대가 내 lastReadAt 이후 보낸 것만 세고, 내가 보낸 메시지는 제외한다")
    void 안읽음은_상대가_내_lastRead_이후_보낸것만_센다() {
        // given: 대화 1개, 상대가 메시지 3개(t1,t2,t3), 내가 1개(t2.5)
        User me = persistUser("01000000001", "나");
        User partner = persistUser("01000000002", "상대");
        Place place = persistPlace("p1");
        Long mealRequestId = persistAcceptedMealRequestId(partner, me, place);

        Conversation conv = Conversation.open(mealRequestId, place, partner, me);
        em.persist(conv);

        LocalDateTime t1 = NOW;
        LocalDateTime t2 = NOW.plusMinutes(10);
        LocalDateTime t2_5 = NOW.plusMinutes(15);
        LocalDateTime t3 = NOW.plusMinutes(20);

        em.persist(ChatMessage.text(conv, partner.getId(), "상대 메시지1", t1));
        em.persist(ChatMessage.text(conv, partner.getId(), "상대 메시지2", t2));
        em.persist(ChatMessage.text(conv, me.getId(), "내 메시지", t2_5));
        em.persist(ChatMessage.text(conv, partner.getId(), "상대 메시지3", t3));
        conv.touch(t3);
        em.flush();
        em.clear();

        // when: 내 lastReadAt = t1 → 상대가 t1 "이후"에 보낸 t2,t3만 unread(t1 자신은 제외, 내 메시지는 항상 제외)
        long unreadAfterT1 = chatMessageRepository.countUnread(conv.getId(), me.getId(), t1);
        // and: lastReadAt이 없으면(한 번도 안 읽음) 상대가 보낸 전부(3건)를 unread로 센다
        long unreadNeverRead = chatMessageRepository.countUnread(conv.getId(), me.getId(), null);

        // then
        assertThat(unreadAfterT1).isEqualTo(2);
        assertThat(unreadNeverRead).isEqualTo(3);
    }

    @Test
    @DisplayName("findLastMessagesByConversationIds: 대화방마다 마지막 메시지 1건씩만, 한 번의 쿼리로 반환한다(메시지 없는 방은 결과에 없음)")
    void 대화방별_마지막메시지를_한번에_조회한다() {
        // given: 대화 3개 — conv1은 메시지 2건, conv2는 1건, conv3은 0건
        User me = persistUser("01000000001", "나");
        User partner1 = persistUser("01000000002", "상대1");
        User partner2 = persistUser("01000000003", "상대2");
        User partner3 = persistUser("01000000004", "상대3");
        Place place1 = persistPlace("p1");
        Place place2 = persistPlace("p2");
        Place place3 = persistPlace("p3");

        Conversation conv1 = em.persist(
                Conversation.open(persistAcceptedMealRequestId(me, partner1, place1), place1, me, partner1));
        Conversation conv2 = em.persist(
                Conversation.open(persistAcceptedMealRequestId(me, partner2, place2), place2, me, partner2));
        Conversation conv3 = em.persist(
                Conversation.open(persistAcceptedMealRequestId(me, partner3, place3), place3, me, partner3));

        em.persist(ChatMessage.text(conv1, me.getId(), "conv1 첫 메시지", NOW));
        em.persist(ChatMessage.text(conv1, partner1.getId(), "conv1 마지막 메시지", NOW.plusMinutes(5)));
        em.persist(ChatMessage.text(conv2, partner2.getId(), "conv2 유일 메시지", NOW.plusMinutes(1)));
        em.flush();
        em.clear();

        // when
        List<ChatMessage> last = chatMessageRepository
                .findLastMessagesByConversationIds(List.of(conv1.getId(), conv2.getId(), conv3.getId()));

        // then: 메시지 있는 대화 2개만, 각 대화의 마지막 1건
        assertThat(last).hasSize(2);
        assertThat(last)
                .extracting(m -> m.getConversation().getId(), ChatMessage::getText)
                .containsExactlyInAnyOrder(
                        tuple(conv1.getId(), "conv1 마지막 메시지"),
                        tuple(conv2.getId(), "conv2 유일 메시지"));
    }

    @Test
    @DisplayName("findAllForUser: 메시지가 없는 새 대화는 created_at 기준으로 정렬돼 맨 위에 온다")
    void 메시지없는_새대화는_매칭시각으로_정렬된다() {
        // given: 나 + 상대 2명
        User me = persistUser("01000000001", "나");
        User partner1 = persistUser("01000000002", "상대1");
        User partner2 = persistUser("01000000003", "상대2");
        Place place1 = persistPlace("p1");
        Place place2 = persistPlace("p2");

        // convNew = 방금 매칭(메시지 0개). 먼저 persist해 id를 더 낮게 만든다
        // → id DESC 타이브레이커로는 뒤로 밀리므로, COALESCE 정렬이 맞아야만 맨 위에 온다.
        Conversation convNew = Conversation.open(
                persistAcceptedMealRequestId(me, partner2, place2), place2, me, partner2);
        em.persist(convNew);

        // convOld = 예전 매칭, 메시지가 있어 last_message_at = NOW
        Conversation convOld = Conversation.open(
                persistAcceptedMealRequestId(me, partner1, place1), place1, me, partner1);
        convOld.touch(NOW);
        em.persist(convOld);
        em.flush();

        // created_at은 @CreatedDate가 자동으로 채우므로 네이티브 UPDATE로 값을 고정한다.
        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = :ts WHERE id = :id")
                .setParameter("ts", NOW.plusMinutes(10))
                .setParameter("id", convNew.getId())
                .executeUpdate();
        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = :ts WHERE id = :id")
                .setParameter("ts", NOW.minusDays(1))
                .setParameter("id", convOld.getId())
                .executeUpdate();
        em.flush();
        em.clear();

        // when
        List<Conversation> list = conversationRepository.findAllForUser(me.getId());

        // then: 메시지 없는 새 대화(NOW+10분)가 메시지 있는 옛 대화(NOW)보다 앞선다
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(convNew.getId());
        assertThat(list.get(1).getId()).isEqualTo(convOld.getId());
    }

    @Test
    @DisplayName("findAllForUser: 내가 지운 대화는 내 목록에서만 빠지고 상대 목록에는 남는다")
    void 삭제한_대화는_내_목록에서만_빠진다() {
        // given: 나와 상대의 대화 1개
        User me = persistUser("01000000001", "나");
        User partner = persistUser("01000000002", "상대");
        Place place = persistPlace("p1");

        Conversation conv = Conversation.open(
                persistAcceptedMealRequestId(me, partner, place), place, me, partner);
        conv.touch(NOW);
        em.persist(conv);
        em.flush();

        // when: 내가 내 목록에서 지운다
        conv.deleteBy(me.getId(), NOW.plusMinutes(1));
        em.flush();
        em.clear();

        // then: 내 목록에서는 빠지고
        assertThat(conversationRepository.findAllForUser(me.getId())).isEmpty();
        // 상대 목록에는 그대로 남는다
        assertThat(conversationRepository.findAllForUser(partner.getId()))
                .extracting(Conversation::getId)
                .containsExactly(conv.getId());
    }
}
