package com.honjeong.review.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.honjeong.global.common.DisplayNames;
import com.honjeong.review.domain.Review;
import com.honjeong.review.domain.ReviewTag;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/**
 * 식당 상세 리뷰탭의 리뷰 한 건 응답 데이터.
 *
 * @param reviewId 리뷰 ID
 * @param user 작성자 정보(닉네임)
 * @param visitedAt 방문 시각
 * @param content 리뷰 본문
 * @param tasteRating 맛 별점(1~5)
 * @param soloFriendlyRating 혼밥 적합도 별점(1~5)
 * @param tags 혼밥 친화 태그 목록
 * @param imageUrls 첨부 사진 URL 목록
 * @param authenticated 인증(체크인 연결) 리뷰 여부
 * @param mine 요청 사용자 본인이 쓴 리뷰인지 여부
 */
public record PlaceReviewResponse(
        Long reviewId, Author user, LocalDateTime visitedAt, String content,
        int tasteRating, int soloFriendlyRating, List<String> tags, List<String> imageUrls,
        boolean authenticated, boolean mine) {

    /**
     * 프로필을 열 수 없는 이유 — {@link Author#userId()}가 null일 때만 채워진다. 앱의 안내 문구가 이 값으로 갈린다.
     *
     * <p>{@link UserStatus}를 그대로 내려보내지 않는 이유: 노출 목적이 "왜 못 여는지"뿐이라 온보딩 중(PENDING)
     * 같은 내부 상태까지 알릴 필요가 없다. PENDING·미상은 {@code UNKNOWN}으로 뭉갠다.
     */
    public enum AuthorUnavailable { WITHDRAWN, SUSPENDED, UNKNOWN }

    /**
     * 리뷰 작성자 정보.
     *
     * @param userId 작성자 ID — <b>프로필로 이동할 수 있을 때만</b> 채운다. 탈퇴·정지 등 ACTIVE가 아닌
     *               작성자는 {@code null}이며, 앱은 이 값이 없으면 프로필로 이동하지 않고 안내만 띄운다.
     * @param nickname 작성자 닉네임
     * @param unavailable 프로필을 열 수 없는 이유. 열 수 있으면 {@code null}
     */
    public record Author(Long userId, String nickname, AuthorUnavailable unavailable) {}

    /** 기능: Review 엔티티와 사진 URL 목록을 리뷰탭 응답 DTO로 변환(내 리뷰 여부 계산 포함) */
    public static PlaceReviewResponse from(Review r, Long currentUserId, List<String> imageUrls) {
        return new PlaceReviewResponse(
                r.getId(),
                // 탈퇴자는 닉네임이 null이라 '알 수 없음'으로 표시한다(DisplayNames).
                new Author(profileLinkableId(r.getUser()),
                        DisplayNames.nicknameOrUnknown(r.getUser().getNickname()),
                        unavailableOf(r.getUser())),
                r.getVisitedAt(),
                r.getContent(),
                r.getTasteRating(),
                r.getSoloFriendlyRating(),
                r.getTags().stream().map(ReviewTag::getTag).toList(),
                imageUrls,
                r.isAuthenticated(),
                r.getUser().getId().equals(currentUserId));
    }

    /**
     * 프로필로 이동할 수 있는 작성자만 id를 돌려준다(그 외 null).
     *
     * <p>기준은 {@code MateProfileService.getPublicProfile}과 같은 <b>ACTIVE 여부</b>다. 거기서
     * ACTIVE가 아닌 사용자는 404로 존재를 숨기므로, id를 그대로 내려주면 앱에 눌리는 죽은 링크가 생긴다
     * (탈퇴자 '알 수 없음'을 눌러 404를 보는 경로). 상태 판단을 앱의 닉네임 문자열 비교에 맡기지 않으려고
     * 서버가 null로 명시한다.
     */
    private static Long profileLinkableId(User user) {
        return user.getStatus() == UserStatus.ACTIVE ? user.getId() : null;
    }

    /**
     * 프로필을 열 수 없는 이유를 정한다(열 수 있으면 null).
     *
     * <p><b>주의 — SUSPENDED를 내려보내는 것은 제재 사실을 제3자에게 알리는 일이다.</b>
     * {@code MateProfileService.getPublicProfile}이 정지 계정을 404로 숨기는 취지(제재 사유 비노출)와는
     * 상충하며, 「정지된 사용자입니다」 안내를 띄우기로 한 제품 결정(2026-07-30)에 따라 의도적으로 노출한다.
     * 중립 문구로 되돌리려면 여기서 SUSPENDED를 UNKNOWN으로 접으면 앱 수정 없이 끝난다.
     */
    private static AuthorUnavailable unavailableOf(User user) {
        if (user.getStatus() == UserStatus.ACTIVE) {
            return null;
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            return AuthorUnavailable.WITHDRAWN;
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            return AuthorUnavailable.SUSPENDED;
        }
        return AuthorUnavailable.UNKNOWN;   // PENDING(온보딩 중)·미상 — 내부 상태를 그대로 알리지 않는다
    }
}
