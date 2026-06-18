package com.honjeong.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataIntegrityViolationException;

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
    private final MealRequestService service =
            new MealRequestService(mealRequestRepository, checkInRepository, userRepository, clock);

    private final LocalDateTime nowKst = LocalDateTime.of(2026, 6, 18, 12, 0);

    private CheckIn targetCheckIn(long checkInId, long ownerId, boolean allowMealRequest, long placeId) {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        when(owner.isAllowMealRequest()).thenReturn(allowMealRequest);
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(placeId);
        CheckIn ci = mock(CheckIn.class);
        when(ci.getId()).thenReturn(checkInId);
        when(ci.getStatus()).thenReturn(CheckInStatus.ACTIVE);
        when(ci.getUser()).thenReturn(owner);
        when(ci.getPlace()).thenReturn(place);
        return ci;
    }

    private MealRequestCreateRequest request(long toCheckInId) {
        return new MealRequestCreateRequest(toCheckInId, "같이 드실래요?");
    }

    private MealRequest pendingRequest(long receiverId) {
        User receiver = mock(User.class);
        when(receiver.getId()).thenReturn(receiverId);
        CheckIn ci = mock(CheckIn.class);
        when(ci.getUser()).thenReturn(receiver);
        return MealRequest.create(mock(User.class), ci, mock(Place.class), "msg", nowKst);
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

        MealRequestStatusResponse res = service.accept(2L, 7L);

        assertThat(res.status()).isEqualTo("ACCEPTED");
        assertThat(res.respondedAt()).isEqualTo(nowKst);
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
    @DisplayName("getMealRequests: role 기본(null·received)→findReceived, sent→findSent 라우팅")
    void list_roleRouting() {
        when(mealRequestRepository.findReceived(1L, null)).thenReturn(List.of());
        when(mealRequestRepository.findSent(1L, null)).thenReturn(List.of());

        service.getMealRequests(1L, "received", null);
        service.getMealRequests(1L, null, null); // null → received 기본
        verify(mealRequestRepository, times(2)).findReceived(1L, null);

        service.getMealRequests(1L, "sent", null);
        verify(mealRequestRepository).findSent(1L, null);
    }

    @Test
    @DisplayName("getMealRequests: status를 enum으로 변환해 전달")
    void list_statusFilter() {
        when(mealRequestRepository.findReceived(1L, MealRequestStatus.PENDING)).thenReturn(List.of());
        service.getMealRequests(1L, "received", "PENDING");
        verify(mealRequestRepository).findReceived(1L, MealRequestStatus.PENDING);
    }

    @Test
    @DisplayName("getMealRequests: 엔티티를 목록 DTO로 매핑한다(닉네임·placeId·message·status·createdAt)")
    void list_mapsToDto() {
        User from = mock(User.class);
        when(from.getNickname()).thenReturn("옆자리");
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(3L);
        MealRequest mr = MealRequest.create(from, mock(CheckIn.class), place, "같이 드실래요?", nowKst);
        when(mealRequestRepository.findReceived(1L, null)).thenReturn(List.of(mr));

        List<MealRequestListItemResponse> result = service.getMealRequests(1L, "received", null);

        assertThat(result).hasSize(1);
        MealRequestListItemResponse item = result.get(0);
        assertThat(item.fromUser().nickname()).isEqualTo("옆자리");
        assertThat(item.placeId()).isEqualTo(3L);
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
