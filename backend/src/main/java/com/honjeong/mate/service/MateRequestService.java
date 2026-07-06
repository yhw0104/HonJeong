package com.honjeong.mate.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.mate.dto.MateRequestCreateRequest;
import com.honjeong.mate.dto.MateRequestListItemResponse;
import com.honjeong.mate.dto.MateRequestResponse;
import com.honjeong.mate.dto.MateRequestStatusResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: 메이트 신청 생성·수락·거절·취소와 신청 목록 조회 비즈니스 로직
 * 2. 사용 Controller: MateRequestController
 */
@Service
public class MateRequestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String ROLE_RECEIVED = "received";
    private static final String ROLE_SENT = "sent";

    private final MateRequestRepository mateRequestRepository;
    private final MateRepository mateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BlockRepository blockRepository;
    private final Clock clock;

    public MateRequestService(MateRequestRepository mateRequestRepository, MateRepository mateRepository,
            UserRepository userRepository, NotificationService notificationService,
            BlockRepository blockRepository, Clock clock) {
        this.mateRequestRepository = mateRequestRepository;
        this.mateRepository = mateRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.blockRepository = blockRepository;
        this.clock = clock;
    }

    /**
     * 기능: 메이트 신청 생성 — 자기 자신·미존재 사용자·차단 관계·이미 메이트·중복 PENDING 방지, 수신자에게 알림 발행
     * Request: userId — 신청자 ID, request — MateRequestCreateRequest(toUserId: 상대 사용자 ID)
     * Response: MateRequestResponse — 생성된 신청 ID·상대 ID·상태(PENDING)
     */
    @Transactional
    public MateRequestResponse create(Long userId, MateRequestCreateRequest request) {
        Long toUserId = request.toUserId();
        if (userId.equals(toUserId)) {
            throw new BusinessException(ErrorCode.MATE_SELF);
        }
        if (!userRepository.existsById(toUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (blockRepository.existsBlockBetween(userId, toUserId)) {
            throw new BusinessException(ErrorCode.USER_BLOCKED);
        }
        if (mateRepository.existsByUser_IdAndMateUser_Id(userId, toUserId)) {
            throw new BusinessException(ErrorCode.MATE_ALREADY);
        }
        try {
            User fromRef = userRepository.getReferenceById(userId);
            User toRef = userRepository.getReferenceById(toUserId);
            MateRequest saved = mateRequestRepository.saveAndFlush(MateRequest.create(fromRef, toRef, now()));
            notificationService.publish(toUserId, NotificationType.MATE_REQUEST_RECEIVED, userId);
            return MateRequestResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.MATE_REQUEST_DUPLICATE);
        }
    }

    /**
     * 기능: 받은 신청 수락 — 양방향 Mate 2행 멱등 저장, 역방향 PENDING 신청도 함께 수락 처리, 발신자에게 알림 발행
     * Request: userId — 수락하는 사용자(수신자) ID, id — 신청 ID
     * Response: MateRequestStatusResponse — 신청 ID·상태(ACCEPTED)·응답 시각
     */
    @Transactional
    public MateRequestStatusResponse accept(Long userId, Long id) {
        MateRequest mr = loadPendingForReceiver(userId, id);
        mr.accept(now());
        User a = mr.getFromUser();
        User b = mr.getToUser();
        // 멱등 저장: 이미 존재하는 방향은 skip(uq_mates_pair 위반 방지)
        if (!mateRepository.existsByUser_IdAndMateUser_Id(a.getId(), b.getId())) {
            mateRepository.save(Mate.create(a, b, now()));
        }
        if (!mateRepository.existsByUser_IdAndMateUser_Id(b.getId(), a.getId())) {
            mateRepository.save(Mate.create(b, a, now()));
        }
        // 역방향 PENDING 신청(b→a)이 있으면 함께 ACCEPTED로 닫음
        mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(b.getId(), a.getId(), MateRequestStatus.PENDING)
                .ifPresent(rev -> rev.accept(now()));
        // 알림은 수락된 신청의 발신자에게만 — 자동수락된 역방향 신청의 발신자는 수락자 본인이라 불필요(스펙).
        notificationService.publish(mr.getFromUser().getId(), NotificationType.MATE_REQUEST_ACCEPTED, userId);
        return MateRequestStatusResponse.from(mr);
    }

    /**
     * 기능: 받은 신청 거절 (수신자만 가능, PENDING 상태만 처리)
     * Request: userId — 거절하는 사용자(수신자) ID, id — 신청 ID
     * Response: MateRequestStatusResponse — 신청 ID·상태(DECLINED)·응답 시각
     */
    @Transactional
    public MateRequestStatusResponse decline(Long userId, Long id) {
        MateRequest mr = loadPendingForReceiver(userId, id);
        mr.decline(now());
        return MateRequestStatusResponse.from(mr);
    }

    /**
     * 기능: 보낸 신청 취소 (발신자만 가능, PENDING 상태만 처리)
     * Request: userId — 취소하는 사용자(발신자) ID, id — 신청 ID
     * Response: MateRequestStatusResponse — 신청 ID·상태(CANCELED)·응답 시각
     */
    @Transactional
    public MateRequestStatusResponse cancel(Long userId, Long id) {
        MateRequest mr = mateRequestRepository.findWithUsersById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATE_REQUEST_NOT_FOUND));
        if (!mr.isSentBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!mr.isPending()) {
            throw new BusinessException(ErrorCode.MATE_REQUEST_ALREADY_RESPONDED);
        }
        mr.cancel(now());
        return MateRequestStatusResponse.from(mr);
    }

    /** 기능: 신청 조회 후 수신자 본인·PENDING 상태 검증 (아니면 FORBIDDEN/MATE_REQUEST_ALREADY_RESPONDED 예외) */
    private MateRequest loadPendingForReceiver(Long userId, Long id) {
        MateRequest mr = mateRequestRepository.findWithUsersById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATE_REQUEST_NOT_FOUND));
        if (!mr.isReceivedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!mr.isPending()) {
            throw new BusinessException(ErrorCode.MATE_REQUEST_ALREADY_RESPONDED);
        }
        return mr;
    }

    /**
     * 기능: 받은/보낸 메이트 신청 목록 조회 (상태 필터 선택, 차단 상대 상호 은닉)
     * Request: userId — 사용자 ID, role — received|sent(기본 received), status — 상태 필터(선택, 예: PENDING)
     * Response: List<MateRequestListItemResponse> — 신청 목록(최신순)
     */
    @Transactional(readOnly = true)
    public List<MateRequestListItemResponse> getMateRequests(Long userId, String role, String status) {
        boolean sent = parseRole(role);
        MateRequestStatus statusFilter = parseStatus(status);
        // 차단 관계(양방향) 유저는 메이트 신청 목록에서 상호 은닉한다(FR-108).
        List<Long> excluded = blockRepository.findExclusionIds(userId);
        List<MateRequest> result = sent
                ? mateRequestRepository.findSent(userId, statusFilter, excluded)
                : mateRequestRepository.findReceived(userId, statusFilter, excluded);
        return result.stream().map(MateRequestListItemResponse::from).toList();
    }

    /** 기능: role 파라미터를 sent 여부 boolean으로 파싱 (received|sent 외 값은 INVALID_INPUT 예외) */
    private boolean parseRole(String role) {
        if (role == null || role.isBlank() || ROLE_RECEIVED.equals(role)) {
            return false;
        }
        if (ROLE_SENT.equals(role)) {
            return true;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 role입니다.");
    }

    /** 기능: status 파라미터를 MateRequestStatus로 파싱 (빈 값이면 null=전체, 잘못된 값은 INVALID_INPUT 예외) */
    private MateRequestStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MateRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 status입니다.");
        }
    }

    /** 기능: 주입된 Clock 기준 현재 시각을 KST LocalDateTime으로 변환 */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
