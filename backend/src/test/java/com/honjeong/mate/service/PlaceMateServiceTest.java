package com.honjeong.mate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.dto.PlaceMatesResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.review.domain.Review;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.domain.User;

@ExtendWith(MockitoExtension.class)
class PlaceMateServiceTest {

    @Mock MateRepository mateRepository;
    @Mock CheckInRepository checkInRepository;
    @Mock ReviewRepository reviewRepository;
    @InjectMocks PlaceMateService service;

    @Test
    @DisplayName("메이트 없으면 visitedCount=0·빈 목록(쿼리 호출 안 함)")
    void 메이트_없음() {
        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(List.of());

        var res = service.getMatesAtPlace(1L, 9L);

        assertThat(res.visitedCount()).isZero();
        assertThat(res.mates()).isEmpty();
        verify(checkInRepository, never()).aggregateMateVisitsAtPlace(any(), any());
    }

    @Test
    @DisplayName("조립: 방문+리뷰+같이먹음 병합, hereNow 우선 정렬, visitedCount=방문>0 메이트 수")
    void 조립_정렬() {
        // 메이트 A(id 11, 방문2·리뷰★5·같이3), B(id 12, 방문0·지금 여기·리뷰없음)
        // 스텁 헬퍼(mateStub/visitRow/reviewStub/togetherRow)는 내부에서 when()을 호출하므로,
        // 바깥 when(...).thenReturn(...) 체인이 열려 있는 동안 인자로 바로 넣으면 Mockito가
        // "Unfinished stubbing"으로 오해한다 — 리스트를 먼저 지역 변수로 만들어 둔다.
        User a = userStub(11L, "에이");
        User b = userStub(12L, "비");
        List<Mate> mates = List.of(mateStub(a), mateStub(b));
        List<CheckInRepository.MateVisitRow> visitRows =
                List.of(visitRow(11L, 2, LocalDateTime.of(2026, 7, 18, 12, 0)));
        List<Long> hereNowIds = List.of(12L);
        List<Review> reviews = List.of(reviewStub(a, 5, "조용해요"));
        List<CheckInRepository.TogetherPairRow> togetherRows = List.of(togetherRow(11L, 3));

        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(mates);
        when(checkInRepository.aggregateMateVisitsAtPlace(eq(9L), anyCollection())).thenReturn(visitRows);
        when(checkInRepository.findMateIdsHereNow(eq(9L), anyCollection())).thenReturn(hereNowIds);
        when(reviewRepository.findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(eq(9L), anyCollection()))
                .thenReturn(reviews);
        when(checkInRepository.countTogetherPairsForUser(1L)).thenReturn(togetherRows);

        var res = service.getMatesAtPlace(1L, 9L);

        assertThat(res.visitedCount()).isEqualTo(1); // A만 방문>0
        assertThat(res.mates()).hasSize(2);
        // hereNow(B) 우선
        assertThat(res.mates().get(0).userId()).isEqualTo(12L);
        assertThat(res.mates().get(0).hereNow()).isTrue();
        assertThat(res.mates().get(0).visitCount()).isZero();
        var aOut = res.mates().get(1);
        assertThat(aOut.userId()).isEqualTo(11L);
        assertThat(aOut.soloFriendlyRating()).isEqualTo(5);
        assertThat(aOut.reviewContent()).isEqualTo("조용해요");
        assertThat(aOut.togetherCount()).isEqualTo(3);
        assertThat(aOut.visitCount()).isEqualTo(2);
    }

    private User userStub(long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getNickname()).thenReturn(nickname);
        return u;
    }

    private Mate mateStub(User mateUser) {
        Mate m = mock(Mate.class);
        when(m.getMateUser()).thenReturn(mateUser);
        return m;
    }

    private CheckInRepository.MateVisitRow visitRow(long userId, long visitCount, LocalDateTime lastVisitedAt) {
        CheckInRepository.MateVisitRow row = mock(CheckInRepository.MateVisitRow.class);
        when(row.getUserId()).thenReturn(userId);
        when(row.getVisitCount()).thenReturn(visitCount);
        when(row.getLastVisitedAt()).thenReturn(lastVisitedAt);
        return row;
    }

    private Review reviewStub(User user, int soloFriendlyRating, String content) {
        Review r = mock(Review.class);
        when(r.getUser()).thenReturn(user);
        when(r.getSoloFriendlyRating()).thenReturn(soloFriendlyRating);
        when(r.getContent()).thenReturn(content);
        return r;
    }

    private CheckInRepository.TogetherPairRow togetherRow(long partnerId, long cnt) {
        CheckInRepository.TogetherPairRow row = mock(CheckInRepository.TogetherPairRow.class);
        when(row.getPartnerId()).thenReturn(partnerId);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }
}
