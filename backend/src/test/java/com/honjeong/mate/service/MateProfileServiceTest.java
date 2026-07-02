package com.honjeong.mate.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.mate.dto.UserSearchResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.user.domain.User;
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

    private User user(Long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        lenient().when(u.getNickname()).thenReturn(nickname);
        return u;
    }
}
