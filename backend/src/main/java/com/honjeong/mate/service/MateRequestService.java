package com.honjeong.mate.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

@Service
public class MateRequestService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String ROLE_RECEIVED = "received";
    private static final String ROLE_SENT = "sent";

    private final MateRequestRepository mateRequestRepository;
    private final MateRepository mateRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public MateRequestService(MateRequestRepository mateRequestRepository, MateRepository mateRepository,
            UserRepository userRepository, Clock clock) {
        this.mateRequestRepository = mateRequestRepository;
        this.mateRepository = mateRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public MateRequestResponse create(Long userId, MateRequestCreateRequest request) {
        Long toUserId = request.toUserId();
        if (userId.equals(toUserId)) {
            throw new BusinessException(ErrorCode.MATE_SELF);
        }
        if (!userRepository.existsById(toUserId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (mateRepository.existsByUser_IdAndMateUser_Id(userId, toUserId)) {
            throw new BusinessException(ErrorCode.MATE_ALREADY);
        }
        try {
            User fromRef = userRepository.getReferenceById(userId);
            User toRef = userRepository.getReferenceById(toUserId);
            MateRequest saved = mateRequestRepository.saveAndFlush(MateRequest.create(fromRef, toRef, now()));
            return MateRequestResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.MATE_REQUEST_DUPLICATE);
        }
    }

    @Transactional
    public MateRequestStatusResponse accept(Long userId, Long id) {
        MateRequest mr = loadPendingForReceiver(userId, id);
        mr.accept(now());
        User a = mr.getFromUser();
        User b = mr.getToUser();
        mateRepository.save(Mate.create(a, b, now()));
        mateRepository.save(Mate.create(b, a, now()));
        return MateRequestStatusResponse.from(mr);
    }

    @Transactional
    public MateRequestStatusResponse decline(Long userId, Long id) {
        MateRequest mr = loadPendingForReceiver(userId, id);
        mr.decline(now());
        return MateRequestStatusResponse.from(mr);
    }

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

    @Transactional(readOnly = true)
    public List<MateRequestListItemResponse> getMateRequests(Long userId, String role, String status) {
        boolean sent = parseRole(role);
        MateRequestStatus statusFilter = parseStatus(status);
        List<MateRequest> result = sent
                ? mateRequestRepository.findSent(userId, statusFilter)
                : mateRequestRepository.findReceived(userId, statusFilter);
        return result.stream().map(MateRequestListItemResponse::from).toList();
    }

    private boolean parseRole(String role) {
        if (role == null || role.isBlank() || ROLE_RECEIVED.equals(role)) {
            return false;
        }
        if (ROLE_SENT.equals(role)) {
            return true;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 role입니다.");
    }

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

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
