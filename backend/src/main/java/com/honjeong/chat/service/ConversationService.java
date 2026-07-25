package com.honjeong.chat.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.chat.domain.Conversation;
import com.honjeong.chat.repository.ConversationRepository;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: 매칭(meal_request) 성사·종료에 연동되는 대화방 생성·닫힘 코어 (대상 테이블: conversations)
 *
 * <p>매칭 수락 시 {@link #open}으로 대화방을 만들고, 같이먹기 종료(수동 종료·타임아웃 등) 시 {@link #close}로 닫는다.
 * 둘 다 멱등 — 매칭 이벤트가 중복 발생해도(재시도 등) 안전하게 반복 호출할 수 있다. Task 4가 수락/종료/차단 흐름에서
 * 이 두 메서드를 호출해 연결한다.
 */
@Service
public class ConversationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ConversationRepository conversationRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public ConversationService(ConversationRepository conversationRepository,
                                PlaceRepository placeRepository,
                                UserRepository userRepository, Clock clock) {
        this.conversationRepository = conversationRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }

    /**
     * 기능: 매칭 성사 시 대화방 생성(멱등 — 이미 있으면 무시)
     * Request: mealRequestId — 매칭된 같이먹기 신청 id, fromUserId/toUserId — 대화 참여자 id,
     *          placeId — 만남 장소 id / Response: 없음
     *
     * <p>연관 엔티티는 프록시({@code getReferenceById})로만 참조한다 — 이 시점에 Place·User 전체를
     * 로딩할 필요가 없다(존재는 매칭 흐름 상위 단계에서 이미 검증됨).
     *
     * @param mealRequestId 매칭된 같이먹기 신청 id
     * @param fromUserId    신청자(대화 개설자) id
     * @param toUserId      수신자(상대) id
     * @param placeId       만남 장소 id
     */
    @Transactional
    public void open(Long mealRequestId, Long fromUserId, Long toUserId, Long placeId) {
        if (conversationRepository.findByMealRequestId(mealRequestId).isPresent()) {
            return;
        }
        conversationRepository.save(Conversation.open(
                mealRequestId,
                placeRepository.getReferenceById(placeId),
                userRepository.getReferenceById(fromUserId),
                userRepository.getReferenceById(toUserId)));
    }

    /**
     * 기능: 같이먹기 종료 시 대화방 닫기(멱등 — 없거나 이미 CLOSED면 무해)
     * Request: mealRequestId — 매칭된 같이먹기 신청 id / Response: 없음
     *
     * @param mealRequestId 매칭된 같이먹기 신청 id
     */
    @Transactional
    public void close(Long mealRequestId) {
        conversationRepository.findByMealRequestId(mealRequestId)
                .filter(Conversation::isActive)
                .ifPresent(Conversation::close);
    }

    /**
     * 기능: TOGETHER /me 응답용 — 매칭의 대화 id 조회(없으면 null)
     * Request: mealRequestId — 매칭된 같이먹기 신청 id / Response: Long — 대화방 id(없으면 null)
     *
     * @param mealRequestId 매칭된 같이먹기 신청 id
     * @return 대화방 id(없으면 null)
     */
    @Transactional(readOnly = true)
    public Long findIdByMealRequestId(Long mealRequestId) {
        return conversationRepository.findByMealRequestId(mealRequestId)
                .map(Conversation::getId).orElse(null);
    }
}
