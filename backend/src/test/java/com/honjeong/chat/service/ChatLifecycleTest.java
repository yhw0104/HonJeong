package com.honjeong.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.block.service.BlockService;
import com.honjeong.chat.domain.Conversation;
import com.honjeong.chat.domain.ConversationStatus;
import com.honjeong.chat.repository.ConversationRepository;
import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.service.MealRequestService;
import com.honjeong.place.domain.Place;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * Task 4 — 매칭 생성·종료 훅이 실제로 대화(Conversation)를 만들고 닫는지 실 Postgres로 검증하는 통합 테스트.
 *
 * <p>커버 범위: ① accept 시 대화 생성(ACTIVE), ② {@link CheckInService#endCheckIn} TOGETHER 수동 종료 시
 * 대화 CLOSED, ③ {@link BlockService#block} 차단 정리 시 대화 CLOSED. TTL 경로
 * ({@link CheckInService#expireStaleCheckIns()})는 벌크 UPDATE라 이 통합 테스트로 매칭을 자연 경과시키기
 * 비현실적이라 다루지 않는다 — 대신 {@code CheckInServiceTest}에 Mockito 단위 테스트로 배선을 검증한다.
 */
@SpringBootTest
@Transactional
class ChatLifecycleTest extends AbstractPostgresTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private MealRequestService mealRequestService;

    @Autowired
    private BlockService blockService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Test
    @DisplayName("accept로 매칭이 성사되면 대화가 생성(ACTIVE)되고, TOGETHER 수동 종료(endCheckIn)면 대화가 CLOSED된다")
    void accept_opensConversation_andManualEndClosesIt() {
        // given: 수신자(Bob)가 식당에 SEEKING 체크인하고
        User sender = createUser("01088880001", "chatAlice");
        User receiver = createUser("01088880002", "chatBob");
        Place place = createPlace("CHAT-E2E-001", "챗식당1");

        CheckInResponse receiverCheckIn = checkInService.createCheckIn(
                receiver.getId(), new CheckInRequest(place.getId()));
        Long receiverCheckInId = receiverCheckIn.checkInId();

        // when: 발신자(Alice)가 그 체크인에 같이먹기를 신청하고
        MealRequestResponse created = mealRequestService.create(
                sender.getId(), new MealRequestCreateRequest(receiverCheckInId, "같이 드실래요?"));
        Long mrId = created.mealRequestId();

        // and: 수신자(Bob)가 수락하면
        mealRequestService.accept(receiver.getId(), mrId);

        // then: 매칭(meal_request_id)에 대화가 생성되고 ACTIVE다.
        Conversation opened = conversationRepository.findByMealRequestId(mrId)
                .orElseThrow(() -> new AssertionError("accept 후 대화가 존재해야 한다"));
        assertThat(opened.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(opened.getFromUser().getId()).isEqualTo(sender.getId());
        assertThat(opened.getToUser().getId()).isEqualTo(receiver.getId());
        assertThat(opened.getPlace().getId()).isEqualTo(place.getId());

        // when: 매칭 한쪽(Bob)이 자기 체크인을 종료하면(TOGETHER 수동 종료 → 파트너도 함께 종료)
        checkInService.endCheckIn(receiver.getId(), receiverCheckInId);

        // then: 같은 대화가 CLOSED로 전이된다.
        Conversation closed = conversationRepository.findByMealRequestId(mrId)
                .orElseThrow(() -> new AssertionError("종료 후에도 대화 레코드는 남아있어야 한다"));
        assertThat(closed.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    @Test
    @DisplayName("매칭 후 한쪽이 상대를 차단하면 정리 과정에서 대화가 CLOSED된다")
    void block_afterMatch_closesConversation() {
        // given: 새 매칭(수신자 Dave가 체크인 → 발신자 Carol이 신청 → Dave가 수락)
        User sender = createUser("01088880003", "chatCarol");
        User receiver = createUser("01088880004", "chatDave");
        Place place = createPlace("CHAT-E2E-002", "챗식당2");

        CheckInResponse receiverCheckIn = checkInService.createCheckIn(
                receiver.getId(), new CheckInRequest(place.getId()));
        MealRequestResponse created = mealRequestService.create(
                sender.getId(), new MealRequestCreateRequest(receiverCheckIn.checkInId(), "같이 드실래요?"));
        Long mrId = created.mealRequestId();
        mealRequestService.accept(receiver.getId(), mrId);

        assertThat(conversationRepository.findByMealRequestId(mrId).orElseThrow().getStatus())
                .isEqualTo(ConversationStatus.ACTIVE);

        // when: 발신자(Carol)가 수신자(Dave)를 차단하면(차단 정리가 TOGETHER 매칭을 종료)
        blockService.block(sender.getId(), receiver.getId());

        // then: 그 매칭의 대화가 CLOSED로 전이된다.
        Conversation closed = conversationRepository.findByMealRequestId(mrId)
                .orElseThrow(() -> new AssertionError("차단 후에도 대화 레코드는 남아있어야 한다"));
        assertThat(closed.getStatus()).isEqualTo(ConversationStatus.CLOSED);
    }

    /** 온보딩 절차 없이 ACTIVE 상태 회원을 바로 저장한다(통합 테스트용 최소 프로필). */
    private User createUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return userRepository.save(user);
    }

    /** 공공데이터 마스터 기반 최소 장소를 저장한다. */
    private Place createPlace(String externalId, String name) {
        return placeRepository.save(Place.ofPublicData(
                externalId, name, "한식", "서울 어딘가", "서울 도로명",
                37.5665, 126.9780, "02-000-0000", "영업"));
    }
}
