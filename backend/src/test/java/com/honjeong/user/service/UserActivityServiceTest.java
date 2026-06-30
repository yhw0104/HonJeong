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
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.dto.ActivitySummaryResponse;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock private CheckInRepository checkInRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @InjectMocks private UserActivityService service;

    @Test
    @DisplayName("getActivitySummary: 체크인·리뷰·즐겨찾기 카운트를 모으고 메이트는 0")
    void getActivitySummary_aggregates_mateZero() {
        when(checkInRepository.countByUser_Id(1L)).thenReturn(12L);
        when(reviewRepository.countByUser_Id(1L)).thenReturn(8L);
        when(favoriteRepository.countDistinctPlaceByUserId(1L)).thenReturn(5L);

        ActivitySummaryResponse res = service.getActivitySummary(1L);

        assertThat(res.checkInCount()).isEqualTo(12L);
        assertThat(res.reviewCount()).isEqualTo(8L);
        assertThat(res.favoriteCount()).isEqualTo(5L);
        assertThat(res.mateCount()).isEqualTo(0L);
    }
}
