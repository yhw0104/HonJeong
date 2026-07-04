package com.honjeong.meal.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 같이먹기 신청 도메인 서비스. 신청 생성(대상 검증·opt-in·자기/중복 차단)·수락·거절·목록을 담당한다.
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
    private final Clock clock;

    public MealRequestService(MealRequestRepository mealRequestRepository, CheckInRepository checkInRepository,
            UserRepository userRepository, NotificationService notificationService, Clock clock) {
        this.mealRequestRepository = mealRequestRepository;
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * 같이먹기 신청을 생성한다. 대상 체크인이 ACTIVE가 아니면 404, 자기 자신이면 409, 수신 거부면 403,
     * 중복(유니크 위반)이면 409로 처리한다. place_id는 대상 체크인의 장소에서 파생한다.
     *
     * @param userId  신청자 id
     * @param request 대상 체크인 id·인사말
     * @return 생성된 신청 응답
     */
    @Transactional
    public MealRequestResponse create(Long userId, MealRequestCreateRequest request) {
        CheckIn target = checkInRepository.findById(request.toCheckInId())
                .filter(c -> c.getStatus() == CheckInStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));

        User receiver = target.getUser();
        if (receiver.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.MEALREQUEST_SELF);
        }
        if (!receiver.isAllowMealRequest()) {
            throw new BusinessException(ErrorCode.MEALREQUEST_OPT_OUT);
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
     * 신청을 수락한다. 없으면 404, 수신자가 아니면 403, 이미 응답했으면 409, 대상이 이미 혼밥을 끝냈으면
     * MEALREQUEST_TARGET_ENDED(409). 수락 시 매칭 전이가 함께 일어난다 — 수신자 체크인은 ACTIVE→TOGETHER로
     * 바뀌고, 발신자에게는 새 TOGETHER 체크인이 insert된다(발신자가 기존에 다른 곳 ACTIVE면 먼저 종료하고,
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
        if (target.getStatus() != CheckInStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MEALREQUEST_TARGET_ENDED);
        }

        LocalDateTime now = now();
        mr.accept(now);
        target.matchTogether(mr.getId(), now);              // 수신자 ACTIVE → TOGETHER

        Long senderId = mr.getFromUser().getId();
        checkInRepository.findByUser_IdAndStatusIn(senderId,
                        List.of(CheckInStatus.ACTIVE, CheckInStatus.TOGETHER))
                .ifPresent(existing -> {
                    if (existing.getStatus() == CheckInStatus.TOGETHER) {
                        throw new BusinessException(ErrorCode.MEALREQUEST_SENDER_BUSY);
                    }
                    existing.end(now);                       // 발신자 기존 ACTIVE 종료
                    checkInRepository.flush();               // INSERT 전 UPDATE 반영(유니크 인덱스)
                });

        try {
            checkInRepository.save(CheckIn.startTogether(
                    userRepository.getReferenceById(senderId), target.getPlace(), mr.getId(), now));
        } catch (DataIntegrityViolationException e) {        // 경쟁: 발신자에 두 번째 TOGETHER
            throw new BusinessException(ErrorCode.MEALREQUEST_SENDER_BUSY);
        }

        mealRequestRepository.declineOtherPending(target.getId(), mr.getId(), now); // 다른 PENDING 정리
        notificationService.publish(senderId, NotificationType.MEAL_REQUEST_ACCEPTED, userId);
        return MealRequestStatusResponse.from(mr);
    }

    /**
     * 신청을 거절한다. 가드는 {@link #accept}와 동일하다.
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

    /** 응답 가능한 신청을 로드한다 — 없음 404 / 비수신자 403 / 이미 응답 409. */
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
     * 받은/보낸 신청 목록을 조회한다. role=received(기본)|sent, status는 선택 필터.
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
        List<MealRequest> result = sent
                ? mealRequestRepository.findSent(userId, statusFilter)
                : mealRequestRepository.findReceived(userId, statusFilter);
        return result.stream().map(MealRequestListItemResponse::from).toList();
    }

    /** role 문자열을 sent 여부로 변환한다. null/빈/"received" → false, "sent" → true, 그 외 → 400. */
    private boolean parseRole(String role) {
        if (role == null || role.isBlank() || ROLE_RECEIVED.equals(role)) {
            return false;
        }
        if (ROLE_SENT.equals(role)) {
            return true;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 role입니다.");
    }

    /** status 문자열을 enum으로 변환한다. null/빈 → null(전체), 잘못된 값 → 400. */
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

    /** 현재 시각을 KST LocalDateTime으로 반환한다. */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
