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
import com.honjeong.review.dto.PlacePhotoResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.dto.ReviewContextResponse;
import com.honjeong.review.service.ReviewService;
import com.honjeong.support.ActiveUserSliceSupport;

@WebMvcTest(controllers = PlaceReviewController.class)
@Import({SecurityConfig.class, WebConfig.class})
class PlaceReviewControllerTest extends ActiveUserSliceSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private ReviewService reviewService;

    @Test
    @DisplayName("GET /api/places/{id}/reviews: 200 + 목록 + mine")
    void list_200() throws Exception {
        when(reviewService.getPlaceReviews(3L, 1L)).thenReturn(List.of(
                new PlaceReviewResponse(42L, new PlaceReviewResponse.Author(7L, "연남러", null),
                        LocalDateTime.of(2026, 6, 25, 12, 0), "편히", 5, 4, List.of("1인석 많음"), List.of(), true, true)));

        mockMvc.perform(get("/api/places/3/reviews")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reviewId").value(42))
                .andExpect(jsonPath("$.data[0].user.userId").value(7))
                .andExpect(jsonPath("$.data[0].user.nickname").value("연남러"))
                .andExpect(jsonPath("$.data[0].authenticated").value(true))
                .andExpect(jsonPath("$.data[0].mine").value(true));
    }

    @Test
    @DisplayName("GET /api/places/{id}/review-context: 200 + 연결될 체크인 ID")
    void reviewContext_200() throws Exception {
        when(reviewService.getReviewContext(1L, 3L)).thenReturn(new ReviewContextResponse(7L));

        mockMvc.perform(get("/api/places/3/review-context")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkableCheckInId").value(7));
    }

    @Test
    @DisplayName("GET /api/places/{id}/review-context: 연결할 체크인이 없으면 null — 앱은 일반 리뷰 화면을 연다")
    void reviewContext_200_null() throws Exception {
        when(reviewService.getReviewContext(1L, 3L)).thenReturn(new ReviewContextResponse(null));

        mockMvc.perform(get("/api/places/3/review-context")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkableCheckInId").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/places/{id}/photos: 200, 평탄화 사진 목록")
    void photos_200() throws Exception {
        when(reviewService.getPlacePhotos(1L)).thenReturn(List.of(new PlacePhotoResponse("p1", 10L)));
        mockMvc.perform(get("/api/places/1/photos")
                        .header("Authorization", "Bearer " + jwtProvider.createAccessToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].photoUrl").value("p1"));
    }
}
