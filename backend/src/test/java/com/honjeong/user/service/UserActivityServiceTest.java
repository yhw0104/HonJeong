package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.dto.ActivitySummaryResponse;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock private CheckInRepository checkInRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private MateRepository mateRepository;
    @InjectMocks private UserActivityService service;

    @Test
    @DisplayName("getActivitySummary: 체크인·리뷰·즐겨찾기·메이트 카운트를 모두 실데이터로 집계")
    void getActivitySummary_aggregates_withMateCount() {
        when(checkInRepository.countByUser_Id(1L)).thenReturn(12L);
        when(reviewRepository.countByUser_Id(1L)).thenReturn(8L);
        when(favoriteRepository.countDistinctPlaceByUserId(1L)).thenReturn(5L);
        when(mateRepository.countByUser_Id(1L)).thenReturn(3L);

        ActivitySummaryResponse res = service.getActivitySummary(1L);

        assertThat(res.checkInCount()).isEqualTo(12L);
        assertThat(res.reviewCount()).isEqualTo(8L);
        assertThat(res.favoriteCount()).isEqualTo(5L);
        assertThat(res.mateCount()).isEqualTo(3L);
    }
}
