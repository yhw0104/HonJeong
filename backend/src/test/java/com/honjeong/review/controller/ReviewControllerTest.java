package com.honjeong.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.review.dto.ReviewResponse;
import com.honjeong.review.service.ReviewService;
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * {@link ReviewController} 웹 슬라이스 테스트. HTTP 매핑·상태코드·인가·{@code @Valid}를 검증하고 로직은 서비스 모킹.
 */
@WebMvcTest(controllers = ReviewController.class)
@Import({SecurityConfig.class, WebConfig.class})
class ReviewControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private ReviewService reviewService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("POST /api/reviews: 201 + 리뷰 응답")
    void create_201() throws Exception {
        when(reviewService.createReview(eq(1L), any()))
                .thenReturn(new ReviewResponse(42L, 3L, 10L, true));

        mockMvc.perform(post("/api/reviews").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placeId":3,"tasteRating":5,"soloFriendlyRating":4,
                                 "content":"편히","tags":["1인석 많음"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewId").value(42))
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    @Test
    @DisplayName("맛 별점 누락이면 400 — 맛 별점은 두 화면 모두 필수다")
    void create_400_whenTasteRatingMissing() throws Exception {
        mockMvc.perform(post("/api/reviews").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeId\":3,\"soloFriendlyRating\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("★혼밥 별점 누락은 DTO 검증을 통과한다 — 인증 여부를 아는 건 서비스뿐이라 거기서 판단한다")
    void create_201_whenSoloRatingMissing() throws Exception {
        // 일반 리뷰 화면이 보내는 모양. 연결될 체크인이 있는지는 컨트롤러가 알 수 없으므로
        // @NotNull로 막을 수 없다 — ReviewService.createReview가 불변식을 강제한다.
        when(reviewService.createReview(eq(1L), any()))
                .thenReturn(new ReviewResponse(43L, 3L, null, false));

        mockMvc.perform(post("/api/reviews").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeId\":3,\"tasteRating\":5,\"content\":\"둘이 와도 좋아요\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test
    @DisplayName("토큰 없으면 401")
    void create_401() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placeId\":3,\"tasteRating\":5,\"soloFriendlyRating\":4}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/reviews/{id}: 200 + 수정 응답")
    void update_200() throws Exception {
        when(reviewService.updateReview(eq(1L), eq(42L), any()))
                .thenReturn(new ReviewResponse(42L, 3L, null, false));

        mockMvc.perform(patch("/api/reviews/42").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tasteRating":4,"soloFriendlyRating":3,"content":"바뀜","tags":["바테이블"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewId").value(42));
    }

    @Test
    @DisplayName("PATCH: 맛 별점 누락이면 400")
    void update_400_whenTasteRatingMissing() throws Exception {
        mockMvc.perform(patch("/api/reviews/42").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"soloFriendlyRating\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH: 혼밥 별점 없이도 수정된다 — 일반 리뷰 화면의 수정 흐름")
    void update_200_whenSoloRatingMissing() throws Exception {
        when(reviewService.updateReview(eq(1L), eq(42L), any()))
                .thenReturn(new ReviewResponse(42L, 3L, null, false));

        mockMvc.perform(patch("/api/reviews/42").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tasteRating\":4,\"content\":\"바뀜\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH: 타인 리뷰면 403")
    void update_403() throws Exception {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .when(reviewService).updateReview(eq(1L), eq(42L), any());

        mockMvc.perform(patch("/api/reviews/42").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tasteRating":4,"soloFriendlyRating":3}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{id}: 200 + success")
    void delete_200() throws Exception {
        doNothing().when(reviewService).deleteReview(eq(1L), eq(42L));

        mockMvc.perform(delete("/api/reviews/42").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE: 리뷰 없으면 404")
    void delete_404() throws Exception {
        doThrow(new BusinessException(ErrorCode.REVIEW_NOT_FOUND))
                .when(reviewService).deleteReview(eq(1L), eq(42L));

        mockMvc.perform(delete("/api/reviews/42").header("Authorization", userToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE: 토큰 없으면 401")
    void delete_401() throws Exception {
        mockMvc.perform(delete("/api/reviews/42"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/reviews: imageUrls 6장이면 400")
    void createReview_tooManyPhotos_400() throws Exception {
        String body = """
            {"placeId":1,"tasteRating":5,"soloFriendlyRating":4,
             "imageUrls":["a","b","c","d","e","f"]}
            """;
        mockMvc.perform(post("/api/reviews").header("Authorization", userToken())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }
}
