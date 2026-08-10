package com.honjeong.mate.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.honjeong.badge.repository.UserBadgeRepository;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.mate.dto.PublicProfileResponse;
import com.honjeong.mate.dto.UserSearchResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserFoodPreference;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserFoodPreferenceRepository;
import com.honjeong.user.repository.UserRepository;

class MateProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final MateRepository mateRepository = mock(MateRepository.class);
    private final MateRequestRepository mateRequestRepository = mock(MateRequestRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final UserFoodPreferenceRepository foodRepository = mock(UserFoodPreferenceRepository.class);
    private final BlockRepository blockRepository = mock(BlockRepository.class);
    private final UserBadgeRepository userBadgeRepository = mock(UserBadgeRepository.class);
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC);
    private final MateProfileService service = new MateProfileService(
            userRepository, mateRepository, mateRequestRepository, checkInRepository, foodRepository,
            blockRepository, userBadgeRepository, fixedClock);

    @Test
    @DisplayName("searchUsers: 본인 제외 + 내가 보낸 PENDING이면 requestStatus=PENDING_SENT")
    void search_pendingSent() {
        User me = user(1L, "나");
        User other = user(2L, "상대");
        when(userRepository.findTop20ByNicknameContainingIgnoreCaseAndStatus(eq("상"), any()))
                .thenReturn(List.of(me, other));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.of(mock(MateRequest.class)));
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(other.getDiningStyle()).thenReturn(DiningStyle.QUIET);

        List<UserSearchResponse> res = service.searchUsers(1L, "상");
        assertThat(res).hasSize(1); // 본인(1L) 제외
        assertThat(res.get(0).userId()).isEqualTo(2L);
        assertThat(res.get(0).requestStatus()).isEqualTo("PENDING_SENT");
        assertThat(res.get(0).diningStyle()).isEqualTo("QUIET"); // 검색 카드 표시용(내 동네 대체)
    }

    @Test
    @DisplayName("닉네임 검색: 차단 상대는 결과에서 제외")
    void searchUsers_excludesBlocked() {
        User userB = user(2L, "차단상대");
        User userC = user(3L, "일반유저");
        when(userRepository.findTop20ByNicknameContainingIgnoreCaseAndStatus(eq("닉"), any()))
                .thenReturn(List.of(userB, userC));
        when(blockRepository.findCounterpartIds(1L)).thenReturn(List.of(2L));

        List<UserSearchResponse> result = service.searchUsers(1L, "닉");

        assertThat(result).extracting(UserSearchResponse::userId).containsExactly(3L);
    }

    @Test
    @DisplayName("getPublicProfile: 비메이트여도 ACTIVE 체크인이면 online=true·currentPlaceName·currentPlaceId 노출")
    void publicProfile_nonMate_onlineShown() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        // 관계상태: 양방향 모두 PENDING 없음 → NONE
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(checkInRepository.countSoloCompletedByUser(2L)).thenReturn(3L);
        when(foodRepository.findByUserId(2L)).thenReturn(Optional.empty());

        Place place = mock(Place.class);
        when(place.getName()).thenReturn("국밥집");
        when(place.getId()).thenReturn(7L);
        CheckIn active = mock(CheckIn.class);
        when(active.getPlace()).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatusIn(2L, List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE)))
                .thenReturn(Optional.of(active));

        PublicProfileResponse res = service.getPublicProfile(1L, 2L);

        assertThat(res.isMate()).isFalse();
        assertThat(res.isOnline()).isTrue();
        assertThat(res.currentPlaceName()).isEqualTo("국밥집");
        assertThat(res.currentPlaceId()).isEqualTo(7L);
        assertThat(res.requestStatus()).isEqualTo("NONE");
        assertThat(res.checkInCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getPublicProfile: 대상이 SEEKING(모집중)이어도 online=true·currentPlaceName·currentPlaceId 노출(모집중도 online)")
    void publicProfile_seeking_onlineShown() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(checkInRepository.countSoloCompletedByUser(2L)).thenReturn(0L);
        when(foodRepository.findByUserId(2L)).thenReturn(Optional.empty());

        Place place = mock(Place.class);
        when(place.getName()).thenReturn("모집중식당");
        when(place.getId()).thenReturn(9L);
        CheckIn seeking = mock(CheckIn.class);
        when(seeking.getPlace()).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatusIn(2L, List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE)))
                .thenReturn(Optional.of(seeking));

        PublicProfileResponse res = service.getPublicProfile(1L, 2L);

        assertThat(res.isOnline()).isTrue();
        assertThat(res.currentPlaceName()).isEqualTo("모집중식당");
        assertThat(res.currentPlaceId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("getPublicProfile: 메이트 + ACTIVE 체크인이면 온라인·현재장소 노출 + 선호음식 매핑, mealsTogether=0·badgeCount=대상 실카운트")
    void publicProfile_mate_online() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(true);
        when(userBadgeRepository.countByUserId(2L)).thenReturn(3L);

        Place place = mock(Place.class);
        when(place.getName()).thenReturn("혼밥국밥집");
        when(place.getId()).thenReturn(42L);
        CheckIn active = mock(CheckIn.class);
        when(active.getPlace()).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatusIn(2L, List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE)))
                .thenReturn(Optional.of(active));
        when(checkInRepository.countSoloCompletedByUser(2L)).thenReturn(5L);

        UserFoodPreference pref = mock(UserFoodPreference.class);
        when(pref.toFoods()).thenReturn(List.of("한식"));
        when(foodRepository.findByUserId(2L)).thenReturn(Optional.of(pref));

        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());

        PublicProfileResponse res = service.getPublicProfile(1L, 2L);

        assertThat(res.isMate()).isTrue();
        assertThat(res.isOnline()).isTrue();
        assertThat(res.currentPlaceName()).isEqualTo("혼밥국밥집");
        assertThat(res.currentPlaceId()).isEqualTo(42L);
        assertThat(res.preferredFoods()).containsExactly("한식");
        assertThat(res.checkInCount()).isEqualTo(5L);
        assertThat(res.mealsTogether()).isZero();
        assertThat(res.badgeCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getPublicProfile: mealsTogether = 나↔대상 실제 매칭 체크인 pairwise(countTogetherBetween)")
    void publicProfile_mealsTogether() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(checkInRepository.findByUser_IdAndStatusIn(2L, List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(checkInRepository.countSoloCompletedByUser(2L)).thenReturn(8L);
        when(foodRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(checkInRepository.countTogetherBetween(1L, 2L)).thenReturn(4L);

        PublicProfileResponse res = service.getPublicProfile(1L, 2L);

        assertThat(res.mealsTogether()).isEqualTo(4L);
    }

    @Test
    @DisplayName("getPublicProfile: 상대가 나에게 보낸 PENDING이면 requestStatus=PENDING_RECEIVED")
    void publicProfile_pendingReceived() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(checkInRepository.countSoloCompletedByUser(2L)).thenReturn(0L);
        when(foodRepository.findByUserId(2L)).thenReturn(Optional.empty());
        // 내가 보낸 건 없고(상대→나) PENDING만 존재
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.of(mock(MateRequest.class)));

        PublicProfileResponse res = service.getPublicProfile(1L, 2L);

        assertThat(res.requestStatus()).isEqualTo("PENDING_RECEIVED");
    }

    @Test
    @DisplayName("getPublicProfile: 공개 프로필 연령대 파생 양성 케이스(생년월일→20대)")
    void publicProfile_ageGroup() {
        User target = user(2L, "상대");
        when(target.getBirthDate()).thenReturn(LocalDate.of(2001, 1, 1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(checkInRepository.findByUser_IdAndStatusIn(2L, List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE)))
                .thenReturn(Optional.empty());
        when(checkInRepository.countSoloCompletedByUser(2L)).thenReturn(0L);
        when(foodRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());

        PublicProfileResponse res = service.getPublicProfile(1L, 2L);

        assertThat(res.ageGroup()).isEqualTo("20대");
    }

    @Test
    @DisplayName("getPublicProfile: 대상 사용자가 없으면 USER_NOT_FOUND")
    void publicProfile_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicProfile(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("타인 프로필: 차단 관계면 USER_NOT_FOUND(404)로 위장")
    void getPublicProfile_blockedPair_404() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(blockRepository.existsBlockBetween(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.getPublicProfile(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴한 사용자의 공개 프로필은 404로 존재를 숨긴다")
    void withdrawnUserProfileIsHidden() {
        // 실 사용자는 ACTIVE(프로필 완료)를 거쳐야 탈퇴할 수 있다 — PENDING에서 바로 withdraw하는 건 실제 전이가 아니다.
        User target = userWithId(2L);
        target.completeProfile("상대닉", Gender.NONE, LocalDate.of(1995, 1, 1), "소개", "서울 강남구",
                37.5, 127.0, DiningStyle.QUIET, null);
        target.withdraw();
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.getPublicProfile(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("정지(SUSPENDED)된 사용자의 공개 프로필도 404로 존재를 숨긴다 — fail-closed(!= ACTIVE)")
    void suspendedUserProfileIsHidden() {
        // User 도메인에 suspend() 전이 메서드가 없어(정지는 이 브랜치의 관심사가 아님) 상태만 직접 주입해 재현한다.
        User target = userWithId(2L);
        target.completeProfile("상대닉", Gender.NONE, LocalDate.of(1995, 1, 1), "소개", "서울 강남구",
                37.5, 127.0, DiningStyle.QUIET, null);
        ReflectionTestUtils.setField(target, "status", UserStatus.SUSPENDED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.getPublicProfile(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    /** 탈퇴(withdraw()) 같은 실제 상태 변화가 필요한 테스트용 — mock이 아닌 진짜 User 엔티티에 id만 강제 주입한다. */
    private User userWithId(Long id) {
        User u = User.pending(null, null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private User user(Long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        // getPublicProfile의 fail-closed 상태 가드(!= ACTIVE)가 적용되므로, 이 헬퍼로 만든 대상은
        // 기본적으로 ACTIVE로 스텁한다(대부분의 테스트는 상태 자체가 관심사가 아니다).
        lenient().when(u.getStatus()).thenReturn(UserStatus.ACTIVE);
        lenient().when(u.getNickname()).thenReturn(nickname);
        return u;
    }
}
