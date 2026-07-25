package com.honjeong.meal.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.badge.service.BadgeService;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestListItemResponse;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: 같이먹기 신청 비즈니스 로직 — 신청 생성(대상 ACTIVE·opt-in·자기/차단/중복 검증), 수락(체크인 TOGETHER 매칭 전이), 거절, 받은/보낸 목록 조회
 * 2. 사용 Controller: MealRequestController
 *
 * <p>[기존 주석] 같이먹기 신청 도메인 서비스. 신청 생성(대상 검증·opt-in·자기/중복 차단)·수락·거절·목록을 담당한다.
 * 모든 시각은 주입된 {@link Clock}을 Asia/Seoul로 환산해 KST로 통일한다(CheckInService와 동일).
 */
@Service
public class MealRequestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String ROLE_RECEIVED = "received";
    private static final String ROLE_SENT = "sent";

    private final MealRequestRepository mealRequestRepository;
    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockRepository blockRepository;
    private final Clock clock;
    private final BadgeService badgeService;
    private final ConversationService conversationService;

    public MealRequestService(MealRequestRepository mealRequestRepository, CheckInRepository checkInRepository,
            UserRepository userRepository, NotificationService notificationService,
            BlockRepository blockRepository, Clock clock, BadgeService badgeService,
            ConversationService conversationService) {
        this.mealRequestRepository = mealRequestRepository;
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.blockRepository = blockRepository;
        this.clock = clock;
        this.badgeService = badgeService;
        this.conversationService = conversationService;
    }

    /**
     * 기능: 같이먹기 신청 생성 — 대상 체크인 SEEKING(모집중) 검증, 자기 자신·수신 거부(opt-in)·차단 관계·중복 신청 차단 후 PENDING 신청 저장 + 수신자에게 알림 발행
     * Request: userId — 신청자 사용자 ID, request — MealRequestCreateRequest(toCheckInId 대상 체크인 id, message 인사 한마디)
     * Response: MealRequestResponse — 생성된 신청(신청 id·대상 체크인 id·인사말·상태 PENDING)
     *
     * <p>[기존 주석] 같이먹기 신청을 생성한다. 대상 체크인이 SEEKING(모집중)이 아니면 404, 자기 자신이면 409, 수신 거부면 403,
     * 중복(유니크 위반)이면 409로 처리한다. place_id는 대상 체크인의 장소에서 파생한다.
     *
     * @param userId  신청자 id
     * @param request 대상 체크인 id·인사말
     * @return 생성된 신청 응답
     */
    @Transactional
    public MealRequestResponse create(Long userId, MealRequestCreateRequest request) {
        CheckIn target = checkInRepository.findById(request.toCheckInId())
                .filter(c -> c.getStatus() == CheckInStatus.SEEKING)
                .orElseThrow(() -> new BusinessException(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));

        User receiver = target.getUser();
        if (receiver.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.MEALREQUEST_SELF);
        }
        if (!receiver.isAllowMealRequest()) {
            throw new BusinessException(ErrorCode.MEALREQUEST_OPT_OUT);
        }
        // 차단 관계(어느 방향이든)면 신청 불가 — 메시지는 차단 사실을 드러내지 않는다.
        if (blockRepository.existsBlockBetween(userId, receiver.getId())) {
            throw new BusinessException(ErrorCode.USER_BLOCKED);
        }

        try {
            User fromRef = userRepository.getReferenceById(userId);
            // saveAndFlush로 즉시 INSERT를 실행해 유니크 위반이 트랜잭션 커밋 전에 발생하도록 강제한다 — 동시 요청에도 catch가 신뢰성 있게 동작한다.
            MealRequest saved = mealRequestRepository.saveAndFlush(
                    MealRequest.create(fromRef, target, target.getPlace(), request.message(), now()));
            notificationService.publish(receiver.getId(), NotificationType.MEAL_REQUEST_RECEIVED, userId);
            return MealRequestResponse.from(saved);
        } catch (DataIntegrityViolationException e) {              // 중복 신청(유니크 위반)
            throw new BusinessException(ErrorCode.MEALREQUEST_DUPLICATE);
        }
    }

    /**
     * 기능: 신청 수락 — 상태를 ACCEPTED로 전이하고 수신자 체크인 SEEKING→TOGETHER, 발신자에게 새 TOGETHER 체크인 생성, 같은 대상의 나머지 PENDING 자동 거절 + 발신자에게 수락 알림 발행
     * Request: userId — 요청 사용자 ID(수신자여야 함), id — 신청 ID
     * Response: MealRequestStatusResponse — 수락 결과(신청 id·상태 ACCEPTED·응답 시각)
     *
     * <p>[기존 주석] 신청을 수락한다. 없으면 404, 수신자가 아니면 403, 이미 응답했으면 409, 대상이 더는 모집중이 아니면
     * MEALREQUEST_TARGET_ENDED(409). 수락 시 매칭 전이가 함께 일어난다 — 수신자 체크인은 SEEKING→TOGETHER로
     * 바뀌고, 발신자에게는 새 TOGETHER 체크인이 insert된다(발신자가 기존에 다른 곳 SEEKING이면 취소하고 ACTIVE면 종료,
     * 이미 TOGETHER면 MEALREQUEST_SENDER_BUSY). 같은 대상으로 온 나머지 PENDING 신청은 자동 DECLINED 처리한다.
     *
     * @param userId 요청 회원 id(수신자여야 함)
     * @param id     신청 id
     * @return 수락 결과
     */
    @Transactional
    public MealRequestStatusResponse accept(Long userId, Long id) {
        MealRequest mr = loadPendingForReceiver(userId, id);

        CheckIn target = mr.getToCheckIn();
        if (target.getStatus() != CheckInStatus.SEEKING) {
            throw new BusinessException(ErrorCode.MEALREQUEST_TARGET_ENDED);
        }

        LocalDateTime now = now();
        mr.accept(now);
        target.matchTogether(mr.getId(), now);              // 수신자 SEEKING → TOGETHER

        Long senderId = mr.getFromUser().getId();
        checkInRepository.findByUser_IdAndStatusIn(senderId,
                        List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE, CheckInStatus.TOGETHER))
                .ifPresent(existing -> {
                    if (existing.getStatus() == CheckInStatus.TOGETHER) {
                        throw new BusinessException(ErrorCode.MEALREQUEST_SENDER_BUSY);
                    }
                    if (existing.getStatus() == CheckInStatus.SEEKING) {
                        existing.cancel(now);                // 발신자 모집 의도는 취소(이력 미집계)
                    } else {
                        existing.end(now);                    // 발신자가 혼밥중이었으면 그 식사는 종료
                    }
                    checkInRepository.flush();               // INSERT 전 UPDATE 반영(유니크 인덱스)
                });

        try {
            checkInRepository.save(CheckIn.startTogether(
                    userRepository.getReferenceById(senderId), target.getPlace(), mr.getId(), now));
        } catch (DataIntegrityViolationException e) {        // 경쟁: 발신자에 두 번째 TOGETHER
            throw new BusinessException(ErrorCode.MEALREQUEST_SENDER_BUSY);
        }

        mealRequestRepository.expireOtherPending(target.getId(), mr.getId(), now); // 다른 PENDING은 자리가 차서 만료
        conversationService.open(mr.getId(), senderId, userId, target.getPlace().getId()); // 매칭 성사 → 대화 개설
        badgeService.checkAndAward(userId, true);    // 수신자 같이먹기 뱃지
        badgeService.checkAndAward(senderId, true);  // 발신자 같이먹기 뱃지
        notificationService.publish(senderId, NotificationType.MEAL_REQUEST_ACCEPTED, userId);
        return MealRequestStatusResponse.from(mr);
    }

    /**
     * 기능: 신청 거절 — 상태를 DECLINED로 전이하고 응답 시각 기록
     * Request: userId — 요청 사용자 ID(수신자여야 함), id — 신청 ID
     * Response: MealRequestStatusResponse — 거절 결과(신청 id·상태 DECLINED·응답 시각)
     *
     * <p>[기존 주석] 신청을 거절한다. 가드는 {@link #accept}와 동일하다.
     *
     * @param userId 요청 회원 id(수신자여야 함)
     * @param id     신청 id
     * @return 거절 결과
     */
    @Transactional
    public MealRequestStatusResponse decline(Long userId, Long id) {
        MealRequest mr = loadPendingForReceiver(userId, id);
        mr.decline(now());
        return MealRequestStatusResponse.from(mr);
    }

    /**
     * 기능: 신청자가 자신이 보낸 PENDING 신청을 철회(WITHDRAWN) — 발신자 권한·PENDING 검사 후 전이.
     * Request: userId — 요청 사용자 ID(발신자여야 함), id — 신청 ID
     * Response: MealRequestStatusResponse — 철회 결과(신청 id·상태 WITHDRAWN·응답 시각)
     *
     * @param userId 요청 회원 id(발신자여야 함)
     * @param id     신청 id
     * @return 철회 결과
     * @throws BusinessException MEALREQUEST_NOT_FOUND(404)/FORBIDDEN(비발신자 403)/MEALREQUEST_ALREADY_RESPONDED(409)
     */
    @Transactional
    public MealRequestStatusResponse withdraw(Long userId, Long id) {
        MealRequest mr = loadPendingForSender(userId, id);
        mr.withdraw(now());
        return MealRequestStatusResponse.from(mr);
    }

    /**
     * 기능: 응답(수락/거절) 가능한 PENDING 신청을 수신자 권한 검사와 함께 로드
     * <p>[기존 주석] 응답 가능한 신청을 로드한다 — 없음 404 / 비수신자 403 / 이미 응답 409.
     */
    private MealRequest loadPendingForReceiver(Long userId, Long id) {
        MealRequest mr = mealRequestRepository.findWithReceiverById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEALREQUEST_NOT_FOUND));
        if (!mr.isReceivedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!mr.isPending()) {
            throw new BusinessException(ErrorCode.MEALREQUEST_ALREADY_RESPONDED);
        }
        return mr;
    }

    /**
     * 기능: 철회 가능한 PENDING 신청을 발신자 권한 검사와 함께 로드 — 없음 404 / 비발신자 403 / 이미 응답 409.
     */
    private MealRequest loadPendingForSender(Long userId, Long id) {
        MealRequest mr = mealRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEALREQUEST_NOT_FOUND));
        if (!mr.isSentBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!mr.isPending()) {
            throw new BusinessException(ErrorCode.MEALREQUEST_ALREADY_RESPONDED);
        }
        return mr;
    }

    /**
     * 기능: 받은/보낸 같이먹기 신청 목록 조회 — role·status 파싱 검증 후 차단 관계 상대는 상호 은닉(제외)해 조회
     * Request: userId — 요청 사용자 ID, role — "received"(기본)|"sent", status — 상태 필터 문자열(선택)
     * Response: List&lt;MealRequestListItemResponse&gt; — 신청 목록(createdAt 내림차순)
     *
     * <p>[기존 주석] 받은/보낸 신청 목록을 조회한다. role=received(기본)|sent, status는 선택 필터.
     *
     * @param userId 회원 id
     * @param role   "received"(기본)|"sent" — 그 외는 400
     * @param status 상태 필터 문자열(선택) — 잘못되면 400
     * @return 신청 목록(createdAt 내림차순)
     */
    @Transactional(readOnly = true)
    public List<MealRequestListItemResponse> getMealRequests(Long userId, String role, String status) {
        boolean sent = parseRole(role);
        MealRequestStatus statusFilter = parseStatus(status);
        // 차단 관계(양방향) 유저는 같이먹기 신청 목록에서 상호 은닉한다(FR-108).
        List<Long> excluded = blockRepository.findExclusionIds(userId);
        List<MealRequest> result = sent
                ? mealRequestRepository.findSent(userId, statusFilter, excluded)
                : mealRequestRepository.findReceived(userId, statusFilter, excluded);
        return result.stream().map(MealRequestListItemResponse::from).toList();
    }

    /**
     * 기능: role 쿼리 문자열을 sent 여부(boolean)로 변환
     * <p>[기존 주석] role 문자열을 sent 여부로 변환한다. null/빈/"received" → false, "sent" → true, 그 외 → 400.
     */
    private boolean parseRole(String role) {
        if (role == null || role.isBlank() || ROLE_RECEIVED.equals(role)) {
            return false;
        }
        if (ROLE_SENT.equals(role)) {
            return true;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 role입니다.");
    }

    /**
     * 기능: status 쿼리 문자열을 MealRequestStatus enum으로 변환
     * <p>[기존 주석] status 문자열을 enum으로 변환한다. null/빈 → null(전체), 잘못된 값 → 400.
     */
    private MealRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MealRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 status입니다.");
        }
    }

    /**
     * 기능: 주입된 Clock 기준 현재 시각을 KST LocalDateTime으로 반환
     * <p>[기존 주석] 현재 시각을 KST LocalDateTime으로 반환한다.
     */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
