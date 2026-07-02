package com.honjeong.mate.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserFoodPreference;
import com.honjeong.user.repository.UserFoodPreferenceRepository;
import com.honjeong.user.repository.UserRepository;

class MateProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final MateRepository mateRepository = mock(MateRepository.class);
    private final MateRequestRepository mateRequestRepository = mock(MateRequestRepository.class);
    private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
    private final UserFoodPreferenceRepository foodRepository = mock(UserFoodPreferenceRepository.class);
    private final MateProfileService service = new MateProfileService(
            userRepository, mateRepository, mateRequestRepository, checkInRepository, foodRepository);

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

        List<UserSearchResponse> res = service.searchUsers(1L, "상");
        assertThat(res).hasSize(1); // 본인(1L) 제외
        assertThat(res.get(0).userId()).isEqualTo(2L);
        assertThat(res.get(0).requestStatus()).isEqualTo("PENDING_SENT");
    }

    @Test
    @DisplayName("getPublicProfile: 비메이트면 온라인 상태 미노출 + checkInRepository 온라인조회 호출 안 함(프라이버시)")
    void publicProfile_nonMate_onlineHidden() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        // 관계상태: 양방향 모두 PENDING 없음 → NONE
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(1L, 2L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(mateRequestRepository.findByFromUser_IdAndToUser_IdAndStatus(2L, 1L, MateRequestStatus.PENDING))
                .thenReturn(Optional.empty());
        when(checkInRepository.countByUser_Id(2L)).thenReturn(3L);
        when(foodRepository.findByUserId(2L)).thenReturn(Optional.empty());

        PublicProfileResponse res = service.getPublicProfile(1L, 2L);

        assertThat(res.isMate()).isFalse();
        assertThat(res.isOnline()).isFalse();
        assertThat(res.currentPlaceName()).isNull();
        assertThat(res.requestStatus()).isEqualTo("NONE");
        assertThat(res.checkInCount()).isEqualTo(3L);
        // 비메이트일 때 온라인(현재 체크인) 조회는 절대 호출되지 않아야 한다
        verify(checkInRepository, never()).findByUser_IdAndStatus(any(), any());
    }

    @Test
    @DisplayName("getPublicProfile: 메이트 + ACTIVE 체크인이면 온라인·현재장소 노출 + 선호음식 매핑, mealsTogether·badgeCount=0")
    void publicProfile_mate_online() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(true);

        Place place = mock(Place.class);
        when(place.getName()).thenReturn("혼밥국밥집");
        CheckIn active = mock(CheckIn.class);
        when(active.getPlace()).thenReturn(place);
        when(checkInRepository.findByUser_IdAndStatus(2L, CheckInStatus.ACTIVE)).thenReturn(Optional.of(active));
        when(checkInRepository.countByUser_Id(2L)).thenReturn(5L);

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
        assertThat(res.preferredFoods()).containsExactly("한식");
        assertThat(res.checkInCount()).isEqualTo(5L);
        assertThat(res.mealsTogether()).isZero();
        assertThat(res.badgeCount()).isZero();
    }

    @Test
    @DisplayName("getPublicProfile: 상대가 나에게 보낸 PENDING이면 requestStatus=PENDING_RECEIVED")
    void publicProfile_pendingReceived() {
        User target = user(2L, "상대");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(mateRepository.existsByUser_IdAndMateUser_Id(1L, 2L)).thenReturn(false);
        when(checkInRepository.countByUser_Id(2L)).thenReturn(0L);
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
    @DisplayName("getPublicProfile: 대상 사용자가 없으면 USER_NOT_FOUND")
    void publicProfile_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicProfile(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User user(Long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        lenient().when(u.getNickname()).thenReturn(nickname);
        return u;
    }
}
