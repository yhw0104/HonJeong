package com.honjeong.review.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.review.domain.Review;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/**
 * 리뷰탭 응답의 작성자 정보 규칙 — 프로필로 갈 수 있는 작성자만 userId를 준다.
 *
 * <p>앱은 이 userId 유무로 닉네임을 누를 수 있는지 판단한다. ACTIVE가 아닌 작성자의 공개 프로필은
 * {@code MateProfileService.getPublicProfile}이 404로 숨기므로, id를 내려주면 눌리는 죽은 링크가 된다.
 */
class PlaceReviewResponseTest {

    private static final Long VIEWER_ID = 1L;
    private static final Long AUTHOR_ID = 7L;

    @Test
    @DisplayName("ACTIVE 작성자: userId를 채우고 unavailable은 비운다(프로필로 이동 가능)")
    void activeAuthor_hasUserId() {
        PlaceReviewResponse res = PlaceReviewResponse.from(review("연남러", UserStatus.ACTIVE), VIEWER_ID, List.of());

        assertThat(res.user().userId()).isEqualTo(AUTHOR_ID);
        assertThat(res.user().nickname()).isEqualTo("연남러");
        assertThat(res.user().unavailable()).isNull();
    }

    @Test
    @DisplayName("탈퇴한 작성자: 닉네임 '알 수 없음' + userId null + unavailable=WITHDRAWN")
    void withdrawnAuthor_hasNoUserId() {
        PlaceReviewResponse res = PlaceReviewResponse.from(review(null, UserStatus.WITHDRAWN), VIEWER_ID, List.of());

        assertThat(res.user().userId()).isNull();
        assertThat(res.user().nickname()).isEqualTo("알 수 없음");
        assertThat(res.user().unavailable()).isEqualTo(PlaceReviewResponse.AuthorUnavailable.WITHDRAWN);
    }

    @Test
    @DisplayName("정지된 작성자: userId null + unavailable=SUSPENDED(제품 결정으로 정지 사실을 안내)")
    void suspendedAuthor_hasNoUserId() {
        PlaceReviewResponse res = PlaceReviewResponse.from(review("정지된사람", UserStatus.SUSPENDED), VIEWER_ID, List.of());

        assertThat(res.user().userId()).isNull();
        assertThat(res.user().nickname()).isEqualTo("정지된사람");
        assertThat(res.user().unavailable()).isEqualTo(PlaceReviewResponse.AuthorUnavailable.SUSPENDED);
    }

    @Test
    @DisplayName("온보딩 중(PENDING) 작성자: 내부 상태를 알리지 않고 UNKNOWN으로 뭉갠다")
    void pendingAuthor_isUnknown() {
        PlaceReviewResponse res = PlaceReviewResponse.from(review(null, UserStatus.PENDING), VIEWER_ID, List.of());

        assertThat(res.user().userId()).isNull();
        assertThat(res.user().unavailable()).isEqualTo(PlaceReviewResponse.AuthorUnavailable.UNKNOWN);
    }

    @Test
    @DisplayName("내가 쓴 리뷰: mine=true이면서 userId도 함께 온다(앱이 내 프로필로 보낸다)")
    void myReview_hasUserIdAndMine() {
        PlaceReviewResponse res = PlaceReviewResponse.from(review("나", UserStatus.ACTIVE), AUTHOR_ID, List.of());

        assertThat(res.mine()).isTrue();
        assertThat(res.user().userId()).isEqualTo(AUTHOR_ID);
    }

    private static Review review(String nickname, UserStatus status) {
        User author = mock(User.class);
        when(author.getId()).thenReturn(AUTHOR_ID);
        when(author.getNickname()).thenReturn(nickname);
        when(author.getStatus()).thenReturn(status);
        Review r = mock(Review.class);
        when(r.getId()).thenReturn(42L);
        when(r.getUser()).thenReturn(author);
        when(r.getVisitedAt()).thenReturn(LocalDateTime.of(2026, 6, 25, 12, 0));
        when(r.getContent()).thenReturn("본문");
        when(r.getTasteRating()).thenReturn(5);
        when(r.getSoloFriendlyRating()).thenReturn(4);
        when(r.getTags()).thenReturn(List.of());
        when(r.isAuthenticated()).thenReturn(true);
        return r;
    }
}
