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
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.dto.MateAtPlace;
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
    @Mock FavoriteRepository favoriteRepository;
    @InjectMocks PlaceMateService service;

    @Test
    @DisplayName("메이트 없으면 visitedCount=0·빈 목록(메이트 쿼리 호출 안 함), 저장 수는 메이트 무관하게 반환")
    void 메이트_없음() {
        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(List.of());
        when(favoriteRepository.countDistinctSaversByPlace(9L)).thenReturn(5L);

        var res = service.getMatesAtPlace(1L, 9L);

        assertThat(res.visitedCount()).isZero();
        assertThat(res.mates()).isEmpty();
        assertThat(res.savedCount()).isEqualTo(5); // 저장 사회증거는 메이트 없어도 준다
        assertThat(res.savedMateCount()).isZero();
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
        when(favoriteRepository.countDistinctSaversByPlace(9L)).thenReturn(10L);
        when(favoriteRepository.countDistinctSaverMatesByPlace(eq(9L), anyCollection())).thenReturn(2L);

        var res = service.getMatesAtPlace(1L, 9L);

        assertThat(res.visitedCount()).isEqualTo(1); // A만 방문>0
        assertThat(res.savedCount()).isEqualTo(10);
        assertThat(res.savedMateCount()).isEqualTo(2);
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

    @Test
    @DisplayName("정렬: hereNow 동일(둘 다 false)이면 lastVisitedAt 최신이 먼저")
    void 정렬_lastVisitedAt_최신우선() {
        User a = userStub(21L, "에이");
        User b = userStub(22L, "비");
        List<Mate> mates = List.of(mateStub(a), mateStub(b));
        List<CheckInRepository.MateVisitRow> visitRows = List.of(
                visitRow(21L, 1, LocalDateTime.of(2026, 7, 10, 12, 0)),
                visitRow(22L, 1, LocalDateTime.of(2026, 7, 15, 12, 0)));
        List<Long> hereNowIds = List.of();
        List<Review> reviews = List.of();
        List<CheckInRepository.TogetherPairRow> togetherRows = List.of();

        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(mates);
        when(checkInRepository.aggregateMateVisitsAtPlace(eq(9L), anyCollection())).thenReturn(visitRows);
        when(checkInRepository.findMateIdsHereNow(eq(9L), anyCollection())).thenReturn(hereNowIds);
        when(reviewRepository.findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(eq(9L), anyCollection()))
                .thenReturn(reviews);
        when(checkInRepository.countTogetherPairsForUser(1L)).thenReturn(togetherRows);

        var res = service.getMatesAtPlace(1L, 9L);

        assertThat(res.mates()).extracting(MateAtPlace::userId).containsExactly(22L, 21L);
    }

    @Test
    @DisplayName("정렬: lastVisitedAt null(방문이력 없음)은 non-null보다 뒤로")
    void 정렬_lastVisitedAt_null은_마지막() {
        User a = userStub(23L, "에이"); // 방문이력 있음(lastVisitedAt non-null)
        User b = userStub(24L, "비"); // hereNow만, 방문이력 없음(lastVisitedAt=null)
        List<Mate> mates = List.of(mateStub(a), mateStub(b));
        List<CheckInRepository.MateVisitRow> visitRows =
                List.of(visitRow(23L, 1, LocalDateTime.of(2026, 7, 10, 12, 0)));
        List<Long> hereNowIds = List.of(23L, 24L); // 둘 다 hereNow=true로 맞춰 lastVisitedAt만 비교되게 함
        List<Review> reviews = List.of();
        List<CheckInRepository.TogetherPairRow> togetherRows = List.of();

        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(mates);
        when(checkInRepository.aggregateMateVisitsAtPlace(eq(9L), anyCollection())).thenReturn(visitRows);
        when(checkInRepository.findMateIdsHereNow(eq(9L), anyCollection())).thenReturn(hereNowIds);
        when(reviewRepository.findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(eq(9L), anyCollection()))
                .thenReturn(reviews);
        when(checkInRepository.countTogetherPairsForUser(1L)).thenReturn(togetherRows);

        var res = service.getMatesAtPlace(1L, 9L);

        assertThat(res.mates()).extracting(MateAtPlace::userId).containsExactly(23L, 24L);
    }

    @Test
    @DisplayName("정렬: hereNow·lastVisitedAt 동률이면 userId 오름차순(결정적 tie-break)")
    void 정렬_동률이면_userId_오름차순() {
        User a = userStub(30L, "에이");
        User b = userStub(25L, "비");
        List<Mate> mates = List.of(mateStub(a), mateStub(b)); // 입력 순서를 일부러 내림차순으로
        List<CheckInRepository.MateVisitRow> visitRows = List.of(); // 방문이력 없음 → 둘 다 lastVisitedAt=null
        List<Long> hereNowIds = List.of(30L, 25L); // 둘 다 hereNow=true → 동률
        List<Review> reviews = List.of();
        List<CheckInRepository.TogetherPairRow> togetherRows = List.of();

        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(mates);
        when(checkInRepository.aggregateMateVisitsAtPlace(eq(9L), anyCollection())).thenReturn(visitRows);
        when(checkInRepository.findMateIdsHereNow(eq(9L), anyCollection())).thenReturn(hereNowIds);
        when(reviewRepository.findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(eq(9L), anyCollection()))
                .thenReturn(reviews);
        when(checkInRepository.countTogetherPairsForUser(1L)).thenReturn(togetherRows);

        var res = service.getMatesAtPlace(1L, 9L);

        assertThat(res.mates()).extracting(MateAtPlace::userId).containsExactly(25L, 30L);
    }

    @Test
    @DisplayName("together: countTogetherPairsForUser 결과에 없는 메이트는 togetherCount=0")
    void together_기본값_0() {
        User a = userStub(11L, "에이");
        User b = userStub(12L, "비");
        List<Mate> mates = List.of(mateStub(a), mateStub(b));
        List<CheckInRepository.MateVisitRow> visitRows = List.of(
                visitRow(11L, 1, LocalDateTime.of(2026, 7, 18, 12, 0)),
                visitRow(12L, 1, LocalDateTime.of(2026, 7, 17, 12, 0)));
        List<Long> hereNowIds = List.of();
        List<Review> reviews = List.of();
        List<CheckInRepository.TogetherPairRow> togetherRows = List.of(togetherRow(11L, 3)); // 12L은 없음

        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(mates);
        when(checkInRepository.aggregateMateVisitsAtPlace(eq(9L), anyCollection())).thenReturn(visitRows);
        when(checkInRepository.findMateIdsHereNow(eq(9L), anyCollection())).thenReturn(hereNowIds);
        when(reviewRepository.findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(eq(9L), anyCollection()))
                .thenReturn(reviews);
        when(checkInRepository.countTogetherPairsForUser(1L)).thenReturn(togetherRows);

        var res = service.getMatesAtPlace(1L, 9L);

        var bOut = res.mates().stream().filter(m -> m.userId() == 12L).findFirst().orElseThrow();
        assertThat(bOut.togetherCount()).isZero();
    }

    @Test
    @DisplayName("리뷰: 같은 메이트 리뷰 2건(최신순 DESC로 옴)이면 최신(첫) 건을 채택(dedup)")
    void 리뷰_dedup_최신건_채택() {
        User a = userStub(11L, "에이");
        List<Mate> mates = List.of(mateStub(a));
        List<CheckInRepository.MateVisitRow> visitRows =
                List.of(visitRow(11L, 2, LocalDateTime.of(2026, 7, 18, 12, 0)));
        List<Long> hereNowIds = List.of();
        // DESC로 이미 정렬돼서 온다고 가정 — 첫 건이 최신, putIfAbsent라 첫 건이 채택돼야 함
        List<Review> reviews = List.of(
                reviewStub(a, 5, "최신 리뷰"),
                reviewStubDiscarded(a)); // 오래된 리뷰 — putIfAbsent에 밀려 필드가 읽히지 않아야 함
        List<CheckInRepository.TogetherPairRow> togetherRows = List.of();

        when(mateRepository.findMatesWithUserByUserId(1L)).thenReturn(mates);
        when(checkInRepository.aggregateMateVisitsAtPlace(eq(9L), anyCollection())).thenReturn(visitRows);
        when(checkInRepository.findMateIdsHereNow(eq(9L), anyCollection())).thenReturn(hereNowIds);
        when(reviewRepository.findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(eq(9L), anyCollection()))
                .thenReturn(reviews);
        when(checkInRepository.countTogetherPairsForUser(1L)).thenReturn(togetherRows);

        var res = service.getMatesAtPlace(1L, 9L);

        var aOut = res.mates().get(0);
        assertThat(aOut.soloFriendlyRating()).isEqualTo(5);
        assertThat(aOut.reviewContent()).isEqualTo("최신 리뷰");
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

    // dedup 테스트에서 putIfAbsent에 밀려 버려지는(= getSoloFriendlyRating/getContent가 호출되지 않는) 두 번째
    // 리뷰용 — getUser()만 실제로 호출되므로, 안 쓰이는 스텁을 넣으면 strict-stubs가 UnnecessaryStubbingException을 던진다.
    private Review reviewStubDiscarded(User user) {
        Review r = mock(Review.class);
        when(r.getUser()).thenReturn(user);
        return r;
    }

    private CheckInRepository.TogetherPairRow togetherRow(long partnerId, long cnt) {
        CheckInRepository.TogetherPairRow row = mock(CheckInRepository.TogetherPairRow.class);
        when(row.getPartnerId()).thenReturn(partnerId);
        when(row.getCnt()).thenReturn(cnt);
        return row;
    }
}
