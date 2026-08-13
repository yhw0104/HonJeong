package com.honjeong.chat.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.honjeong.global.common.DisplayNames;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.push.domain.PushType;
import com.honjeong.push.service.PushDispatcher;
import com.honjeong.push.service.PushMessages;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 매칭(meal_request) 성사·종료에 연동되는 대화방 생성·닫힘 코어 (대상 테이블: conversations).
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
    private final ChatMessageRepository chatMessageRepository;
    private final BlockRepository blockRepository;
    private final PushDispatcher pushDispatcher;
    private final Clock clock;
    private final ChatEventBroadcaster chatEventBroadcaster;

    public ConversationService(ConversationRepository conversationRepository,
                                PlaceRepository placeRepository,
                                UserRepository userRepository,
                                ChatMessageRepository chatMessageRepository,
                                BlockRepository blockRepository,
                                PushDispatcher pushDispatcher, Clock clock,
                                ChatEventBroadcaster chatEventBroadcaster) {
        this.conversationRepository = conversationRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.blockRepository = blockRepository;
        this.pushDispatcher = pushDispatcher;
        this.clock = clock;
        this.chatEventBroadcaster = chatEventBroadcaster;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }

    /**
     * 매칭 성사 시 대화방 생성(멱등 — 이미 있으면 무시).
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
     * 같이먹기 종료 시 대화방 닫기(멱등 — 없거나 이미 CLOSED면 무해).
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
     * TOGETHER /me 응답용 — 매칭의 대화 id 조회(없으면 null).
     *
     * @param mealRequestId 매칭된 같이먹기 신청 id
     * @return 대화방 id(없으면 null)
     */
    @Transactional(readOnly = true)
    public Long findIdByMealRequestId(Long mealRequestId) {
        return conversationRepository.findByMealRequestId(mealRequestId)
                .map(Conversation::getId).orElse(null);
    }

    /**
     * 대화방에 메시지 전송(TEXT/IMAGE) — 저장 후 대화의 lastMessageAt을 갱신하고 발신자를 읽음 처리.
     * <p>비참여자는 {@link ErrorCode#CONVERSATION_NOT_FOUND}로 위장(존재 여부를 노출하지 않음).
     * CLOSED 대화는 {@link ErrorCode#CONVERSATION_CLOSED}. 타입별 필수값이 비어있으면 {@link ErrorCode#INVALID_INPUT}.
     *
     * <p>저장이 끝나면 상대에게 {@link com.honjeong.push.domain.PushType#CHAT_MESSAGE} 푸시를 예약한다
     * (커밋 후 발송). 서로 차단한 사이거나 <b>수신자가</b> 이 대화를 음소거했으면 보내지 않는다.
     *
     * <p>동시에 {@link ChatEventBroadcaster}로 양쪽 소켓에도 새 메시지를 민다(역시 커밋 후). 차단은 푸시와
     * 같은 조건을 공유하지만, 뮤트는 소켓 쪽에는 적용하지 않는다 — 뮤트는 "알림 끄기"지 "화면 갱신 끄기"가
     * 아니라서, 뮤트한 대화를 열어 두고 있어도 새 메시지는 떠야 한다.
     *
     * @param userId         발신자(참여자) id
     * @param conversationId 대화방 id
     * @param req            메시지 타입·내용
     * @return 저장된 메시지 DTO
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long conversationId, SendMessageRequest req) {
        Conversation conv = loadParticipating(conversationId, userId);
        if (!conv.isActive()) {
            throw new BusinessException(ErrorCode.CONVERSATION_CLOSED);
        }
        LocalDateTime now = now();
        ChatMessage msg;
        if (req.type() == MessageType.TEXT) {
            if (req.text() == null || req.text().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            msg = ChatMessage.text(conv, userId, req.text().trim(), now);
        } else {
            if (req.imageUrl() == null || req.imageUrl().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            msg = ChatMessage.image(conv, userId, req.imageUrl(), now);
        }
        chatMessageRepository.save(msg);
        conv.touch(now);
        conv.markRead(userId, now); // 보낸 사람은 읽은 것으로

        // 상대에게 푸시. 채팅은 알림함에 쌓지 않으므로(메시지마다 쌓으면 도배된다)
        // NotificationService.publish()를 거치지 않고 발송기를 직접 부른다.
        Long partnerId = conv.partnerOf(userId);
        boolean blocked = blockRepository.existsBlockBetween(userId, partnerId);
        if (!blocked && !conv.isMutedBy(partnerId)) {
            boolean isImage = req.type() != MessageType.TEXT;
            String preview = PushMessages.chatPreview(isImage ? null : req.text().trim(), isImage);
            pushDispatcher.dispatch(partnerId, PushType.CHAT_MESSAGE, userId, conv.getId(), preview);
        }

        // 저장된 메시지를 양쪽 소켓으로 민다. 차단은 푸시와 같은 조건을 쓰고, 뮤트는 보지 않는다
        // (뮤트는 알림을 끄는 것이지 화면 갱신을 끄는 것이 아니다 — ChatEventBroadcaster 참조).
        ChatMessageResponse response = ChatMessageResponse.from(msg);
        chatEventBroadcaster.broadcastMessage(userId, partnerId, blocked, conv.getId(), response);
        return response;
    }

    /**
     * 대화방의 메시지 전체를 작성순으로 조회.
     *
     * @param userId         조회자(참여자) id
     * @param conversationId 대화방 id
     * @return 작성순 메시지 목록
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(Long userId, Long conversationId) {
        loadParticipating(conversationId, userId);
        return chatMessageRepository.findByConversationIdOrderByIdAsc(conversationId)
                .stream().map(ChatMessageResponse::from).toList();
    }

    /**
     * 대화방을 읽음 처리(내 lastReadAt=now).
     *
     * @param userId         읽는 사용자(참여자) id
     * @param conversationId 대화방 id
     */
    @Transactional
    public void markRead(Long userId, Long conversationId) {
        loadParticipating(conversationId, userId).markRead(userId, now());
    }

    /**
     * 이 대화의 푸시 알림을 켜거나 끈다.
     *
     * <p>비참여자는 {@link ErrorCode#CONVERSATION_NOT_FOUND}로 위장한다(403이 아니다 —
     * 내가 속하지 않은 대화의 존재를 노출하지 않는다). 삭제와 달리 상태 제한이 없어
     * CLOSED 대화도 끄고 켤 수 있다.
     *
     * @param userId         요청 참여자 id
     * @param conversationId 대화방 id
     * @param muted          true면 푸시를 받지 않는다
     */
    @Transactional
    public void setMuted(Long userId, Long conversationId, boolean muted) {
        loadParticipating(conversationId, userId).setMuted(userId, muted);
    }

    /**
     * 대화방을 내 목록에서만 삭제한다(소프트 삭제) — 상대 목록과 메시지는 그대로 남는다.
     *
     * <p>{@code chat_messages}는 지우지 않는다. 신고가 접수됐을 때 조사할 근거가 남아야 하기 때문이다.
     *
     * <p>종료(CLOSED)된 대화만 삭제할 수 있다. 진행 중인 대화는 '매칭 깨기'로 먼저 종료해야 한다 —
     * CLOSED는 이미 읽기전용이라 삭제 후 상대가 메시지를 보내는 상황이 발생하지 않는다.
     *
     * <p>이미 삭제한 대화를 다시 삭제해도 성공한다(멱등).
     *
     * @param userId         요청 회원 id(참여자여야 함)
     * @param conversationId 대화방 id
     * @throws BusinessException 대화가 없거나 참여자가 아닐 때(CONVERSATION_NOT_FOUND),
     *                           진행 중일 때(CONVERSATION_NOT_CLOSED)
     */
    @Transactional
    public void deleteForMe(Long userId, Long conversationId) {
        Conversation conv = loadParticipating(conversationId, userId);
        if (conv.isActive()) {
            throw new BusinessException(ErrorCode.CONVERSATION_NOT_CLOSED);
        }
        conv.deleteBy(userId, now());
    }

    /**
     * 내가 참여한 대화 목록(마지막 활동순) — 상대 정보·안읽음 수·마지막 메시지 미리보기 포함,
     * 차단 상대·내가 지운 대화는 제외.
     * <p>정렬 기준은 {@link ConversationRepository#findAllForUser}와 동일한 "마지막 활동 시각"
     * (메시지가 있으면 lastMessageAt, 없으면 createdAt). 내가 목록에서 지운(소프트 삭제) 대화는
     * 애초에 조회되지 않는다({@link Conversation#deleteBy}) — 상대 목록에는 그대로 남는다.
     * <p>차단(어느 방향이든) 관계인 상대와의 대화는 목록에서 숨긴다(spec §7) — 혼밥러 목록의 상호 은닉(FR-108)과
     * 같은 패턴({@link BlockRepository#findExclusionIds}, 빈 결과는 -1L 센티널). 대화는 CLOSED로 남아 이력은
     * 보존되지만(차단 정리가 이미 대화를 닫아둔다), 목록·닉네임·프로필 사진 노출만 막는다.
     *
     * @param userId 조회할 사용자 id
     * @return 대화 목록 요약(차단 상대·내가 지운 대화 제외)
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> listMine(Long userId) {
        List<Long> excluded = blockRepository.findExclusionIds(userId);
        List<Conversation> mine = conversationRepository.findAllForUser(userId).stream()
                .filter(c -> !excluded.contains(partnerOf(c, userId).getId()))
                .toList();
        if (mine.isEmpty()) {
            return List.of(); // 빈 IN 절로 미리보기 쿼리를 부르지 않는다
        }
        Map<Long, String> previews = previewsOf(mine);
        return mine.stream()
                .map(c -> {
                    var partner = partnerOf(c, userId);
                    long unread = chatMessageRepository.countUnread(c.getId(), userId, c.lastReadAtFor(userId));
                    return new ConversationSummaryResponse(
                            c.getId(), c.getStatus().name(),
                            partner.getId(),
                            // 탈퇴자는 닉네임이 null이라 '알 수 없음'으로 표시한다(DisplayNames).
                            DisplayNames.nicknameOrUnknown(partner.getNickname()), partner.getProfileImageUrl(),
                            c.getPlace().getName(),
                            previews.get(c.getId()), // 마지막 메시지 미리보기(메시지 없으면 null)
                            c.getLastMessageAt(), unread,
                            c.lastReadAtFor(partner.getId()), // 상대가 마지막 읽은 시각(내 메시지 읽음 표시용)
                            c.getCreatedAt(), // 매칭 시각 — 메시지가 없을 때 목록에 표시할 fallback
                            c.isMutedBy(userId)); // 내가 이 대화의 푸시를 껐는지(상대 설정이 아니다)
                }).toList();
    }

    /** 대화 상대(내가 fromUser면 toUser, 아니면 fromUser). */
    private User partnerOf(Conversation c, Long userId) {
        return c.getFromUser().getId().equals(userId) ? c.getToUser() : c.getFromUser();
    }

    private Conversation loadParticipating(Long conversationId, Long userId) {
        return conversationRepository.findById(conversationId)
                .filter(c -> c.isParticipant(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));
    }

    /**
     * 목록의 모든 대화방에 대한 마지막 메시지 미리보기를 한 번의 쿼리로 만든다(대화마다 메시지를 다시 읽던 N+1 제거).
     * 메시지가 없는 대화방은 맵에 없으므로 {@code get}이 null → 미리보기 없음.
     */
    private Map<Long, String> previewsOf(List<Conversation> conversations) {
        List<Long> ids = conversations.stream().map(Conversation::getId).toList();
        Map<Long, String> previews = new HashMap<>();
        for (ChatMessage last : chatMessageRepository.findLastMessagesByConversationIds(ids)) {
            previews.put(last.getConversation().getId(),
                    last.getType() == MessageType.IMAGE ? "사진" : last.getText());
        }
        return previews;
    }
}
