package com.honjeong.mate.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.repository.MateRepository;
import java.util.Optional;
import com.honjeong.mate.domain.Mate;

class MateServiceTest {

    private final MateRepository mateRepository = mock(MateRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-02T03:00:00Z"), ZoneOffset.UTC);
    private final MateService service = new MateService(mateRepository, checkInRepository, clock);

    @Test
    @DisplayName("deleteMate: 관계 없으면 MATE_NOT_FOUND")
    void deleteMate_notFound() {
        when(mateRepository.findByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteMate(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MATE_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteMate: 양방향 2행 삭제")
    void deleteMate_bidirectional() {
        when(mateRepository.findByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(Optional.of(mock(Mate.class)));
        when(mateRepository.findByUser_IdAndMateUser_Id(2L, 1L)).thenReturn(Optional.of(mock(Mate.class)));
        service.deleteMate(1L, 2L);
        verify(mateRepository, times(2)).delete(any(Mate.class));
    }

    @Test
    @DisplayName("getMyMates: 메이트 없으면 빈 목록(체크인 조회 안 함)")
    void getMyMates_empty() {
        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(List.of());
        assertThat(service.getMyMates(1L)).isEmpty();
        verifyNoInteractions(checkInRepository);
    }
}
