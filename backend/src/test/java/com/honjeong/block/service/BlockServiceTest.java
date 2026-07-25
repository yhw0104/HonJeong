package com.honjeong.block.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.block.domain.Block;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

class BlockServiceTest {

    private final BlockRepository blockRepository = mock(BlockRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MateRepository mateRepository = mock(MateRepository.class);
    private final MateRequestRepository mateRequestRepository = mock(MateRequestRepository.class);
    private final MealRequestRepository mealRequestRepository = mock(MealRequestRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final ConversationService conversationService = mock(ConversationService.class);

    private final BlockService service = new BlockService(blockRepository, userRepository,
            mateRepository, mateRequestRepository, mealRequestRepository, checkInRepository, clock,
            conversationService);

    private User user(long id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    @Test
    @DisplayName("자기 자신 차단 → BLOCK_SELF")
    void block_self_throws() {
        assertThatThrownBy(() -> service.block(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BLOCK_SELF);
    }

    @Test
    @DisplayName("대상 없음 → USER_NOT_FOUND")
    void block_targetMissing_throws() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.block(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("중복 차단(유니크 위반) → BLOCK_ALREADY")
    void block_duplicate_throws() {
        User target = user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(blockRepository.saveAndFlush(any(Block.class))).thenThrow(new DataIntegrityViolationException("dup"));
        assertThatThrownBy(() -> service.block(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BLOCK_ALREADY);
    }

    @Test
    @DisplayName("차단 성공 시 자동 정리 — 메이트 양방향 삭제 + PENDING 신청 종결 + 같이먹기 PENDING 정리")
    void block_cleansUpRelations() {
        User target = user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        Mate forward = mock(Mate.class);
        Mate backward = mock(Mate.class);
        when(mateRepository.findByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(Optional.of(forward));
        when(mateRepository.findByUser_IdAndMateUser_Id(2L, 1L)).thenReturn(Optional.of(backward));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyList())).thenReturn(Optional.empty());

        service.block(1L, 2L);

        verify(blockRepository).saveAndFlush(any(Block.class));
        verify(mateRepository).delete(forward);
        verify(mateRepository).delete(backward);
        verify(mateRequestRepository).resolvePendingBetween(eq(1L), eq(2L),
                eq(MateRequestStatus.CANCELED), any(LocalDateTime.class));
        verify(mateRequestRepository).resolvePendingBetween(eq(2L), eq(1L),
                eq(MateRequestStatus.DECLINED), any(LocalDateTime.class));
        verify(mealRequestRepository).expirePendingBetween(eq(1L), eq(2L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("차단 상대와 TOGETHER 매칭 중이면 양쪽 체크인 종료")
    void block_endsTogetherPairWithBlockedUser() {
        User target = user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.findByUser_IdAndMateUser_Id(anyLong(), anyLong())).thenReturn(Optional.empty());

        CheckIn mine = mock(CheckIn.class);
        when(mine.getStatus()).thenReturn(CheckInStatus.TOGETHER);
        when(mine.getMealRequestId()).thenReturn(77L);
        CheckIn partners = mock(CheckIn.class);
        User partnerUser = user(2L);
        when(partners.getUser()).thenReturn(partnerUser);
        User myUser = user(1L);
        when(mine.getUser()).thenReturn(myUser);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyList())).thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(77L)).thenReturn(List.of(mine, partners));

        service.block(1L, 2L);

        verify(mine).end(any(LocalDateTime.class));
        verify(partners).end(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("TOGETHER지만 파트너가 차단 대상이 아니면 종료 안 함")
    void block_keepsTogetherWithOthers() {
        User target = user(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.findByUser_IdAndMateUser_Id(anyLong(), anyLong())).thenReturn(Optional.empty());

        CheckIn mine = mock(CheckIn.class);
        when(mine.getStatus()).thenReturn(CheckInStatus.TOGETHER);
        when(mine.getMealRequestId()).thenReturn(77L);
        User myUser = user(1L);
        when(mine.getUser()).thenReturn(myUser);
        CheckIn partners = mock(CheckIn.class);
        User other = user(9L);
        when(partners.getUser()).thenReturn(other);
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyList())).thenReturn(Optional.of(mine));
        when(checkInRepository.findTogetherByMealRequestId(77L)).thenReturn(List.of(mine, partners));

        service.block(1L, 2L);

        verify(mine, never()).end(any());
        verify(partners, never()).end(any());
    }

    @Test
    @DisplayName("해제: 내역 없으면 BLOCK_NOT_FOUND, 있으면 delete")
    void unblock() {
        when(blockRepository.findByBlocker_IdAndBlocked_Id(1L, 2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.unblock(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BLOCK_NOT_FOUND);

        Block b = mock(Block.class);
        when(blockRepository.findByBlocker_IdAndBlocked_Id(1L, 2L)).thenReturn(Optional.of(b));
        service.unblock(1L, 2L);
        verify(blockRepository).delete(b);
    }
}
