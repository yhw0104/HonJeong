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
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.dto.MateRequestCreateRequest;
import com.honjeong.mate.dto.MateRequestResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

class MateRequestServiceTest {

    private final MateRequestRepository mateRequestRepository = mock(MateRequestRepository.class);
    private final MateRepository mateRepository = mock(MateRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-02T03:00:00Z"), ZoneOffset.UTC);
    private final MateRequestService service =
            new MateRequestService(mateRequestRepository, mateRepository, userRepository, clock);

    @Test
    @DisplayName("create: 자기 자신에게 신청하면 MATE_SELF")
    void create_self() {
        assertThatThrownBy(() -> service.create(1L, new MateRequestCreateRequest(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MATE_SELF);
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
    @DisplayName("accept: 수신자가 수락하면 mates 양방향 2행 저장")
    void accept_createsBidirectionalMates() {
        User from = mockUser(1L);
        User to = mockUser(2L);
        MateRequest mr = MateRequest.create(from, to, null);
        when(mateRequestRepository.findWithUsersById(10L)).thenReturn(Optional.of(mr));

        service.accept(2L, 10L); // userId=2L(=to)가 수락

        verify(mateRepository, times(2)).save(any(Mate.class));
        assertThat(mr.getStatus().name()).isEqualTo("ACCEPTED");
    }

    private User mockUser(Long id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        return u;
    }
}
