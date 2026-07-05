package com.honjeong.review.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.review.dto.DiningHistoryResponse;
import com.honjeong.review.dto.MyReviewsResponse;
import com.honjeong.review.service.ReviewService;

@WebMvcTest(controllers = DiningHistoryController.class)
@Import({SecurityConfig.class, WebConfig.class})
class DiningHistoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private ReviewService reviewService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("GET /api/users/me/reviews: 200 + 목록")
    void myReviews_200() throws Exception {
        var item = new MyReviewsResponse.Item(5L, 10L, "국밥집", LocalDateTime.now(), "맛있다",
                4, 5, List.of("혼밥환영"), List.of(), false, LocalDateTime.now());
        when(reviewService.getMyReviews(1L)).thenReturn(new MyReviewsResponse(List.of(item)));
        mockMvc.perform(get("/api/users/me/reviews").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews[0].placeName").value("국밥집"))
                .andExpect(jsonPath("$.data.reviews[0].authenticated").value(false));
    }

    @Test
    @DisplayName("GET /api/users/me/dining-history: 200")
    void diningHistory_200() throws Exception {
        when(reviewService.getDiningHistory(1L)).thenReturn(new DiningHistoryResponse(
                new DiningHistoryResponse.Summary(1, 1, 1, 1), List.of()));
        mockMvc.perform(get("/api/users/me/dining-history").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.totalCheckIns").value(1));
    }
}
