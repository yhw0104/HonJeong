package com.honjeong.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.review.domain.Review;
import com.honjeong.review.dto.ReviewCreateRequest;
import com.honjeong.review.dto.ReviewResponse;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

class ReviewServiceTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final PlaceService placeService = mock(PlaceService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-25T03:00:00Z"), ZoneOffset.UTC);
    private final ReviewService service =
            new ReviewService(reviewRepository, checkInRepository, placeService, userRepository, clock);

    private Place place(long id) {
        Place p = mock(Place.class);
        when(p.getId()).thenReturn(id);
        return p;
    }

    private ReviewCreateRequest req(Long checkInId, List<String> tags) {
        return new ReviewCreateRequest(3L, checkInId, 5, 4, "편히 먹었다", tags);
    }

    @Test
    @DisplayName("checkInId 없으면 24h 내 최근 체크인을 자동 인증 연결")
    void create_autoLinksRecentCheckIn() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        CheckIn recent = mock(CheckIn.class);
        when(recent.getId()).thenReturn(7L);
        when(recent.getStartedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 11, 0));
        when(checkInRepository.findRecentForReview(eqL(1L), eqL(3L), any())).thenReturn(Optional.of(recent));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse res = service.createReview(1L, req(null, List.of("1인석 많음")));

        assertThat(res.placeId()).isEqualTo(3L);
        assertThat(res.authenticated()).isTrue();
        assertThat(res.checkInId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("미허용 태그면 INVALID_INPUT")
    void create_rejectsUnknownTag() {
        Place p = place(3L);  // place()는 when() 내부를 쓰므로 thenReturn 인자로 직접 쓰면 nested-when 오류 발생
        when(placeService.getById(3L)).thenReturn(p);

        assertThatThrownBy(() -> service.createReview(1L, req(null, List.of("우주최고"))))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("checkInId가 타인 소유면 FORBIDDEN")
    void create_forbidsOthersCheckIn() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        CheckIn others = mock(CheckIn.class);
        when(others.isOwnedBy(1L)).thenReturn(false);
        when(checkInRepository.findById(99L)).thenReturn(Optional.of(others));

        assertThatThrownBy(() -> service.createReview(1L, req(99L, List.of())))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("checkInId 지정했으나 place 불일치면 인증 없는 일반 리뷰로 저장")
    void create_ignoresCheckInWhenPlaceMismatch() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Place otherPlace = mock(Place.class);
        when(otherPlace.getId()).thenReturn(999L);
        CheckIn ci = mock(CheckIn.class);
        when(ci.isOwnedBy(1L)).thenReturn(true);
        when(ci.getPlace()).thenReturn(otherPlace);
        when(checkInRepository.findById(99L)).thenReturn(Optional.of(ci));

        ReviewResponse res = service.createReview(1L, req(99L, List.of()));

        assertThat(res.authenticated()).isFalse();
        assertThat(res.checkInId()).isNull();
    }

    @Test
    @DisplayName("checkInId 지정했으나 DB에 없으면 인증 없는 일반 리뷰로 저장")
    void create_ignoresCheckInWhenNotFound() {
        Place p = place(3L);
        when(placeService.getById(3L)).thenReturn(p);
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkInRepository.findById(99L)).thenReturn(Optional.empty());

        ReviewResponse res = service.createReview(1L, req(99L, List.of()));

        assertThat(res.authenticated()).isFalse();
        assertThat(res.checkInId()).isNull();
    }

    private static Long eqL(long v) { return org.mockito.ArgumentMatchers.eq(v); }
}
