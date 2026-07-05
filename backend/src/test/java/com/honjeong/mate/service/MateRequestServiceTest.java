package com.honjeong.mate.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.mate.dto.MateRequestCreateRequest;
import com.honjeong.mate.dto.MateRequestResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

class MateRequestServiceTest {

    private final MateRequestRepository mateRequestRepository = mock(MateRequestRepository.class);
    private final MateRepository mateRepository = mock(MateRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-02T03:00:00Z"), ZoneOffset.UTC);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final BlockRepository blockRepository = mock(BlockRepository.class);
    private final MateRequestService service =
            new MateRequestService(mateRequestRepository, mateRepository, userRepository,
                    notificationService, blockRepository, clock);

    @Test
    @DisplayName("create: 자기 자신에게 신청하면 MATE_SELF")
    void create_self() {
        assertThatThrownBy(() -> service.create(1L, new MateRequestCreateRequest(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MATE_SELF);
    }

    @Test
    @DisplayName("차단 관계면 신청 불가(USER_BLOCKED) + 알림 미발행")
    void create_blockedPair_throws() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(blockRepository.existsBlockBetween(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, new MateRequestCreateRequest(2L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_BLOCKED);
        verify(notificationService, never()).publish(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("create: 이미 메이트면 MATE_ALREADY")
    void create_alreadyMate() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new MateRequestCreateRequest(2L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MATE_ALREADY);
    }

    @Test
    @DisplayName("create: 정상이면 PENDING 저장 후 응답")
    void create_success() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(userRepository.getReferenceById(anyLong())).thenReturn(mock(User.class));
        when(mateRequestRepository.saveAndFlush(any(MateRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        MateRequestResponse res = service.create(1L, new MateRequestCreateRequest(2L));
        assertThat(res.status()).isEqualTo("PENDING");
        verify(mateRequestRepository).saveAndFlush(any(MateRequest.class));
        verify(notificationService).publish(2L, NotificationType.MATE_REQUEST_RECEIVED, 1L);
    }

    @Test
    @DisplayName("create: 부분유니크 충돌이면 MATE_REQUEST_DUPLICATE")
    void create_duplicate() {
        when(userRepository.existsById(2L)).thenReturn(true);
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(userRepository.getReferenceById(anyLong())).thenReturn(mock(User.class));
        when(mateRequestRepository.saveAndFlush(any(MateRequest.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));
        assertThatThrownBy(() -> service.create(1L, new MateRequestCreateRequest(2L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MATE_REQUEST_DUPLICATE);
    }

    @Test
    @DisplayName("accept: 수신자가 수락하면 mates 양방향 2행 저장 (역방향 없음)")
    void accept_createsBidirectionalMates() {
        User from = mockUser(1L);
        User to = mockUser(2L);
        MateRequest mr = MateRequest.create(from, to, null);
        when(mateRequestRepository.findWithUsersById(10L)).thenReturn(Optional.of(mr));
        when(mateRepository.existsByUser_IdAndMateUser_Id(anyLong(), anyLong())).thenReturn(false);
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());

        service.accept(2L, 10L); // userId=2L(=to)가 수락

        verify(mateRepository, times(2)).save(any(Mate.class));
        assertThat(mr.getStatus().name()).isEqualTo("ACCEPTED");
        verify(notificationService).publish(1L, NotificationType.MATE_REQUEST_ACCEPTED, 2L);
    }

    @Test
    @DisplayName("accept: 상호신청(A→B, B→A 둘 다 PENDING) 시 한쪽 수락으로 역방향도 ACCEPTED, mates 멱등 저장, 알림은 수락된 신청의 발신자에게 1건만")
    void accept_mutualPending_resolvesReverseAndSavesMatesIdempotently() {
        User a = mockUser(1L);
        User b = mockUser(2L);
        // A→B 요청 (id=10), B가 수락
        MateRequest mrAtoB = MateRequest.create(a, b, null);
        // B→A 역방향 PENDING 요청
        MateRequest mrBtoA = MateRequest.create(b, a, null);

        when(mateRequestRepository.findWithUsersById(10L)).thenReturn(Optional.of(mrAtoB));
        when(mateRepository.existsByUser_IdAndMateUser_Id(anyLong(), anyLong())).thenReturn(false);
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.of(mrBtoA));

        service.accept(2L, 10L);

        // mates는 양방향 각 1회씩(총 2회) 저장
        verify(mateRepository, times(2)).save(any(Mate.class));
        // 역방향 요청도 ACCEPTED
        assertThat(mrBtoA.getStatus()).isEqualTo(MateRequestStatus.ACCEPTED);
        // 원래 요청도 ACCEPTED
        assertThat(mrAtoB.getStatus()).isEqualTo(MateRequestStatus.ACCEPTED);
        // 알림은 수락된 신청(A→B)의 발신자(A=1L)에게 1건만 — 자동수락된 역방향(B→A)의 발신자(B=2L)는 수락자 본인이라 제외
        verify(notificationService, times(1)).publish(anyLong(), any(NotificationType.class), anyLong());
        verify(notificationService).publish(1L, NotificationType.MATE_REQUEST_ACCEPTED, 2L);
    }

    @Test
    @DisplayName("accept: 이미 메이트인 상태에서 잔존 PENDING 재수락 시 mates save 없이 ALREADY_RESPONDED(존재 체크 skip)")
    void accept_alreadyMatesExistSkipsSave() {
        User a = mockUser(1L);
        User b = mockUser(2L);
        MateRequest mr = MateRequest.create(a, b, null);
        when(mateRequestRepository.findWithUsersById(10L)).thenReturn(Optional.of(mr));
        // 양방향 모두 이미 존재
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(true);
        when(mateRepository.existsByUser_IdAndMateUser_Id(2L, 1L)).thenReturn(true);
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());

        service.accept(2L, 10L);

        // 이미 존재하므로 save는 한 번도 호출 안 됨
        verify(mateRepository, never()).save(any(Mate.class));
    }

    private User mockUser(Long id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        return u;
    }
}
