package com.honjeong.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.block.repository.BlockRepository;
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
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * MealRequestService 단위 테스트(순수 Mockito + 고정 Clock). 신청 생성 분기(404/409/403/중복)·응답·목록 라우팅을 검증한다.
 * 엔티티 id·상태가 필요한 비교는 mock 엔티티 스텁으로 해결한다(DB 없이).
 */
class MealRequestServiceTest {

    private final MealRequestRepository mealRequestRepository = mock(MealRequestRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-18T03:00:00Z"), ZoneOffset.UTC);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final BlockRepository blockRepository = mock(BlockRepository.class);
    private final MealRequestService service =
            new MealRequestService(mealRequestRepository, checkInRepository, userRepository,
                    notificationService, blockRepository, clock);

    private final LocalDateTime nowKst = LocalDateTime.of(2026, 6, 18, 12, 0);

    private CheckIn targetCheckIn(long checkInId, long ownerId, boolean allowMealRequest, long placeId) {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        when(owner.isAllowMealRequest()).thenReturn(allowMealRequest);
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(placeId);
        CheckIn ci = mock(CheckIn.class);
        when(ci.getId()).thenReturn(checkInId);
        when(ci.getStatus()).thenReturn(CheckInStatus.SEEKING); // create의 대상 SEEKING 필터를 통과시킨다.
        when(ci.getUser()).thenReturn(owner);
        when(ci.getPlace()).thenReturn(place);
        return ci;
    }

    private MealRequestCreateRequest request(long toCheckInId) {
        return new MealRequestCreateRequest(toCheckInId, "같이 드실래요?");
    }

    private MealRequest pendingRequest(long receiverId) {
        User sender = mock(User.class);
        when(sender.getId()).thenReturn(1L);
        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(receiverId);
        CheckIn ci = mock(CheckIn.class);
        when(ci.getId()).thenReturn(3L);
        when(ci.getUser()).thenReturn(receiver);
        when(ci.getStatus()).thenReturn(CheckInStatus.SEEKING); // accept의 대상 SEEKING 가드를 통과시킨다.
        when(ci.getPlace()).thenReturn(mock(Place.class));
        return MealRequest.create(sender, ci, mock(Place.class), "msg", nowKst);
    }

    /** 실 엔티티(mock 아님)로 매칭 전이 테스트용 Place를 만든다 — id만 리플렉션으로 주입. */
    private Place place(long id) {
        Place p = Place.ofPublicData("ext" + id, "식당", "한식", "서울", "도로명", 37.5, 127.0, null, "영업");
        setId(p, id);
        return p;
    }

    /** 실 엔티티(mock 아님)로 매칭 전이 테스트용 User 참조를 만든다 — id만 리플렉션으로 주입. */
    private User userRef(long id) {
        User u = User.pending("010" + id, null);
        setId(u, id);
        return u;
    }

    /** 엔티티의 id 필드를 리플렉션으로 주입한다(IDENTITY 채번이라 mock 없이 검증하려면 필요). */
    private void setId(Object target, long id) {
        try {
            var f = target.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("create: 정상이면 PENDING 신청을 저장하고 응답 반환")
    void create_success() {
        CheckIn target = targetCheckIn(10L, 2L, true, 3L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(mealRequestRepository.saveAndFlush(any(MealRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        MealRequestResponse res = service.create(1L, request(10L));

        assertThat(res.toCheckInId()).isEqualTo(10L);
        assertThat(res.message()).isEqualTo("같이 드실래요?");
        assertThat(res.status()).isEqualTo("PENDING");
        verify(mealRequestRepository).saveAndFlush(any(MealRequest.class));
        verify(notificationService).publish(2L, NotificationType.MEAL_REQUEST_RECEIVED, 1L);
    }

    @Test
    @DisplayName("create: 대상 체크인 없으면 TARGET_CHECKIN_NOT_AVAILABLE(404)")
    void create_targetNotFound() {
        when(checkInRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));
        verify(mealRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create: 대상 체크인이 ENDED면 TARGET_CHECKIN_NOT_AVAILABLE(404)")
    void create_targetEnded() {
        CheckIn ended = mock(CheckIn.class);
        when(ended.getStatus()).thenReturn(CheckInStatus.ENDED);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(ended));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("create: 대상 체크인이 SEEKING이 아니면(혼밥중=ACTIVE) TARGET_CHECKIN_NOT_AVAILABLE(404)")
    void create_targetNotSeeking_throwsTargetCheckInNotAvailable() {
        CheckIn activeTarget = CheckIn.startSeeking(userRef(2L), place(3L), nowKst);
        activeTarget.dineAlone(nowKst);                      // ACTIVE(혼밥중) — 모집중 아님
        setId(activeTarget, 10L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(activeTarget));

        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TARGET_CHECKIN_NOT_AVAILABLE));
        verify(mealRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create: 대상 주인이 나 자신이면 MEALREQUEST_SELF(409)")
    void create_self() {
        CheckIn target = targetCheckIn(10L, 1L, true, 3L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_SELF));
        verify(mealRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create: 대상이 수신 거부면 MEALREQUEST_OPT_OUT(403)")
    void create_optOut() {
        CheckIn target = targetCheckIn(10L, 2L, false, 3L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_OPT_OUT));
        verify(mealRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("차단 관계면 신청 불가(USER_BLOCKED) + 알림 미발행")
    void create_blockedPair_throws() {
        CheckIn target = targetCheckIn(10L, 2L, true, 3L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        when(blockRepository.existsBlockBetween(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_BLOCKED);
        verify(notificationService, never()).publish(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("create: 중복 신청(유니크 위반)이면 MEALREQUEST_DUPLICATE(409)")
    void create_duplicate() {
        CheckIn target = targetCheckIn(10L, 2L, true, 3L);
        when(checkInRepository.findById(10L)).thenReturn(Optional.of(target));
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(mealRequestRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq violation"));
        assertThatThrownBy(() -> service.create(1L, request(10L)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_DUPLICATE));
    }

    @Test
    @DisplayName("accept: 수신자가 PENDING을 수락하면 ACCEPTED·respondedAt 기록")
    void accept_success() {
        MealRequest mr = pendingRequest(2L);
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));

        MealRequestStatusResponse res = service.accept(2L, 7L);

        assertThat(res.status()).isEqualTo("ACCEPTED");
        assertThat(res.respondedAt()).isEqualTo(nowKst);
        verify(checkInRepository).save(any(CheckIn.class));
        verify(mealRequestRepository).expireOtherPending(eq(3L), any(), eq(nowKst));
        verify(notificationService).publish(1L, NotificationType.MEAL_REQUEST_ACCEPTED, 2L);
    }

    @Test
    @DisplayName("decline: 수신자가 PENDING을 거절하면 DECLINED")
    void decline_success() {
        MealRequest mr = pendingRequest(2L);
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));

        MealRequestStatusResponse res = service.decline(2L, 7L);

        assertThat(res.status()).isEqualTo("DECLINED");
        assertThat(res.respondedAt()).isEqualTo(nowKst);
    }

    @Test
    @DisplayName("decline: 이미 응답한 신청이면 MEALREQUEST_ALREADY_RESPONDED(409)")
    void decline_alreadyResponded() {
        MealRequest mr = pendingRequest(2L);
        mr.decline(nowKst.minusMinutes(5));
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));
        assertThatThrownBy(() -> service.decline(2L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_ALREADY_RESPONDED));
    }

    @Test
    @DisplayName("withdraw: 발신자가 PENDING 신청을 철회하면 WITHDRAWN")
    void withdraw_success() {
        MealRequest mr = pendingRequest(2L); // sender=1, receiver=2
        when(mealRequestRepository.findById(7L)).thenReturn(Optional.of(mr));

        MealRequestStatusResponse res = service.withdraw(1L, 7L);

        assertThat(res.status()).isEqualTo("WITHDRAWN");
        assertThat(res.respondedAt()).isEqualTo(nowKst);
    }

    @Test
    @DisplayName("withdraw: 발신자가 아니면(수신자 등) FORBIDDEN")
    void withdraw_notSender() {
        MealRequest mr = pendingRequest(2L);
        when(mealRequestRepository.findById(7L)).thenReturn(Optional.of(mr));
        assertThatThrownBy(() -> service.withdraw(2L, 7L)) // 2L = 수신자(발신자 아님)
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("withdraw: 이미 응답된 신청이면 MEALREQUEST_ALREADY_RESPONDED")
    void withdraw_alreadyResponded() {
        MealRequest mr = pendingRequest(2L);
        mr.accept(nowKst.minusMinutes(5)); // 이미 ACCEPTED
        when(mealRequestRepository.findById(7L)).thenReturn(Optional.of(mr));
        assertThatThrownBy(() -> service.withdraw(1L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_ALREADY_RESPONDED));
    }

    @Test
    @DisplayName("accept: 신청 없으면 MEALREQUEST_NOT_FOUND(404)")
    void accept_notFound() {
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.accept(2L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_NOT_FOUND));
    }

    @Test
    @DisplayName("accept: 수신자가 아니면 FORBIDDEN(403)")
    void accept_notReceiver() {
        MealRequest mr = pendingRequest(2L);
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));
        assertThatThrownBy(() -> service.accept(99L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("accept: 이미 응답한 신청이면 MEALREQUEST_ALREADY_RESPONDED(409)")
    void accept_alreadyResponded() {
        MealRequest mr = pendingRequest(2L);
        mr.accept(nowKst.minusMinutes(5));
        when(mealRequestRepository.findWithReceiverById(7L)).thenReturn(Optional.of(mr));
        assertThatThrownBy(() -> service.accept(2L, 7L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_ALREADY_RESPONDED));
    }

    @Test
    @DisplayName("accept: 수신자 SEEKING→TOGETHER 전이 + 발신자 TOGETHER insert + 다른 PENDING 정리")
    void accept_matches() {
        // given: 수신자(2L)의 SEEKING(모집중) 체크인 대상 + 수락자=수신자(2L), 발신자=1L
        Place p = place(10L);
        CheckIn target = CheckIn.startSeeking(userRef(2L), p, nowKst);       // 수신자 체크인(SEEKING)
        MealRequest mr = MealRequest.create(userRef(1L), target, p, "hi", nowKst); // 발신자 1L → target
        setId(mr, 5L);
        setId(target, 3L);                                                  // 테스트 헬퍼로 id 주입
        when(mealRequestRepository.findWithReceiverById(5L)).thenReturn(Optional.of(mr));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.empty()); // 발신자 체크인 없음
        when(userRepository.getReferenceById(1L)).thenReturn(userRef(1L));

        service.accept(2L, 5L);

        // then: 요청 ACCEPTED, 수신자 TOGETHER, 발신자 TOGETHER 저장, 다른 PENDING 정리 호출
        assertThat(mr.getStatus()).isEqualTo(MealRequestStatus.ACCEPTED);
        assertThat(target.getStatus()).isEqualTo(CheckInStatus.TOGETHER);
        ArgumentCaptor<CheckIn> saved = ArgumentCaptor.forClass(CheckIn.class);
        verify(checkInRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(CheckInStatus.TOGETHER);
        assertThat(saved.getValue().getMealRequestId()).isEqualTo(5L);
        verify(mealRequestRepository).expireOtherPending(3L, 5L, nowKst);
    }

    @Test
    @DisplayName("accept: 수신자 체크인이 더는 SEEKING(모집중)이 아니면 MEALREQUEST_TARGET_ENDED")
    void accept_targetEnded() {
        Place p = place(10L);
        CheckIn target = CheckIn.start(userRef(2L), p, nowKst);
        target.end(nowKst); // 이미 종료(ACTIVE에서 시작해 end로 ENDED — SEEKING이면 end가 no-op이라 여기선 ACTIVE 경유가 맞다)
        MealRequest mr = MealRequest.create(userRef(1L), target, p, null, nowKst);
        setId(mr, 5L);
        when(mealRequestRepository.findWithReceiverById(5L)).thenReturn(Optional.of(mr));

        assertThatThrownBy(() -> service.accept(2L, 5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_TARGET_ENDED));
    }

    @Test
    @DisplayName("accept: 발신자가 이미 TOGETHER면 MEALREQUEST_SENDER_BUSY")
    void accept_senderBusy() {
        Place p = place(10L);
        CheckIn target = CheckIn.startSeeking(userRef(2L), p, nowKst);
        MealRequest mr = MealRequest.create(userRef(1L), target, p, null, nowKst);
        setId(mr, 5L);
        CheckIn senderTogether = CheckIn.startTogether(userRef(1L), place(99L), 8L, nowKst);
        when(mealRequestRepository.findWithReceiverById(5L)).thenReturn(Optional.of(mr));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.of(senderTogether));

        assertThatThrownBy(() -> service.accept(2L, 5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.MEALREQUEST_SENDER_BUSY));
    }

    @Test
    @DisplayName("accept: 발신자가 다른 곳 ACTIVE면 그 ACTIVE를 종료하고 TOGETHER insert")
    void accept_endsSenderActive() {
        Place p = place(10L);
        CheckIn target = CheckIn.startSeeking(userRef(2L), p, nowKst);
        MealRequest mr = MealRequest.create(userRef(1L), target, p, null, nowKst);
        setId(mr, 5L);
        setId(target, 3L);
        CheckIn senderActive = CheckIn.start(userRef(1L), place(99L), nowKst);
        when(mealRequestRepository.findWithReceiverById(5L)).thenReturn(Optional.of(mr));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.of(senderActive));
        when(userRepository.getReferenceById(1L)).thenReturn(userRef(1L));

        service.accept(2L, 5L);

        assertThat(senderActive.getStatus()).isEqualTo(CheckInStatus.ENDED);
        // flush가 save(INSERT)보다 먼저 호출돼야 한다 — 그래야 유니크 인덱스(uq_check_ins_current_user)
        // 위반 없이 기존 ACTIVE 종료(UPDATE)가 새 TOGETHER(INSERT)보다 먼저 DB에 반영된다.
        InOrder inOrder = inOrder(checkInRepository);
        inOrder.verify(checkInRepository).flush();
        inOrder.verify(checkInRepository).save(any(CheckIn.class));
    }

    @Test
    @DisplayName("accept: 발신자가 기존 SEEKING(모집중)이면 취소(CANCELLED)하고 새 TOGETHER insert")
    void accept_cancelsSenderSeeking() {
        Place p = place(10L);
        CheckIn target = CheckIn.startSeeking(userRef(2L), p, nowKst);
        MealRequest mr = MealRequest.create(userRef(1L), target, p, null, nowKst);
        setId(mr, 5L);
        setId(target, 3L);
        CheckIn senderSeeking = CheckIn.startSeeking(userRef(1L), place(99L), nowKst);
        when(mealRequestRepository.findWithReceiverById(5L)).thenReturn(Optional.of(mr));
        when(checkInRepository.findByUser_IdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(Optional.of(senderSeeking));
        when(userRepository.getReferenceById(1L)).thenReturn(userRef(1L));

        service.accept(2L, 5L);

        assertThat(senderSeeking.getStatus()).isEqualTo(CheckInStatus.CANCELLED);
        InOrder inOrder = inOrder(checkInRepository);
        inOrder.verify(checkInRepository).flush();
        inOrder.verify(checkInRepository).save(any(CheckIn.class));
    }

    @Test
    @DisplayName("getMealRequests: role 기본(null·received)→findReceived, sent→findSent 라우팅")
    void list_roleRouting() {
        when(mealRequestRepository.findReceived(eq(1L), isNull(), anyList())).thenReturn(List.of());
        when(mealRequestRepository.findSent(eq(1L), isNull(), anyList())).thenReturn(List.of());

        service.getMealRequests(1L, "received", null);
        service.getMealRequests(1L, null, null); // null → received 기본
        verify(mealRequestRepository, times(2)).findReceived(eq(1L), isNull(), anyList());

        service.getMealRequests(1L, "sent", null);
        verify(mealRequestRepository).findSent(eq(1L), isNull(), anyList());
    }

    @Test
    @DisplayName("getMealRequests: status를 enum으로 변환해 전달")
    void list_statusFilter() {
        when(mealRequestRepository.findReceived(eq(1L), eq(MealRequestStatus.PENDING), anyList()))
                .thenReturn(List.of());
        service.getMealRequests(1L, "received", "PENDING");
        verify(mealRequestRepository).findReceived(eq(1L), eq(MealRequestStatus.PENDING), anyList());
    }

    @Test
    @DisplayName("getMealRequests: 차단 상호 은닉 — blockRepository의 제외 id를 리포지토리에 그대로 전달한다")
    void list_passesExclusionIdsFromBlockRepository() {
        when(blockRepository.findExclusionIds(1L)).thenReturn(List.of(9L, 10L));
        when(mealRequestRepository.findReceived(1L, null, List.of(9L, 10L))).thenReturn(List.of());

        service.getMealRequests(1L, "received", null);

        verify(mealRequestRepository).findReceived(1L, null, List.of(9L, 10L));
    }

    @Test
    @DisplayName("getMealRequests: 엔티티를 목록 DTO로 매핑한다(fromUser·toUser·placeId·placeName·message·status·createdAt)")
    void list_mapsToDto() {
        User from = mock(User.class);
        when(from.getId()).thenReturn(20L);
        when(from.getNickname()).thenReturn("옆자리");
        User to = mock(User.class);
        when(to.getId()).thenReturn(30L);
        when(to.getNickname()).thenReturn("수신자");
        CheckIn ci = mock(CheckIn.class);
        when(ci.getUser()).thenReturn(to);
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(3L);
        when(place.getName()).thenReturn("큰순두부");
        MealRequest mr = MealRequest.create(from, ci, place, "같이 드실래요?", nowKst);
        when(mealRequestRepository.findReceived(eq(1L), isNull(), anyList())).thenReturn(List.of(mr));

        List<MealRequestListItemResponse> result = service.getMealRequests(1L, "received", null);

        assertThat(result).hasSize(1);
        MealRequestListItemResponse item = result.get(0);
        assertThat(item.fromUser().userId()).isEqualTo(20L);
        assertThat(item.fromUser().nickname()).isEqualTo("옆자리");
        assertThat(item.toUser().userId()).isEqualTo(30L);
        assertThat(item.toUser().nickname()).isEqualTo("수신자");
        assertThat(item.placeId()).isEqualTo(3L);
        assertThat(item.placeName()).isEqualTo("큰순두부");
        assertThat(item.message()).isEqualTo("같이 드실래요?");
        assertThat(item.status()).isEqualTo("PENDING");
        assertThat(item.createdAt()).isEqualTo(nowKst);
    }

    @Test
    @DisplayName("getMealRequests: 잘못된 role이면 INVALID_INPUT(400)")
    void list_badRole() {
        assertThatThrownBy(() -> service.getMealRequests(1L, "garbage", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("getMealRequests: 잘못된 status면 INVALID_INPUT(400)")
    void list_badStatus() {
        assertThatThrownBy(() -> service.getMealRequests(1L, "received", "NOPE"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
