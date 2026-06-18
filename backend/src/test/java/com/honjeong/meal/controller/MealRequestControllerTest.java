package com.honjeong.meal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

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
import com.honjeong.meal.dto.MealRequestListItemResponse;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.service.MealRequestService;

/**
 * {@link MealRequestController} 웹 슬라이스 테스트. HTTP 매핑·상태코드·인가·{@code @Valid}를 검증하고 로직은 서비스 모킹.
 */
@WebMvcTest(controllers = MealRequestController.class)
@Import({SecurityConfig.class, WebConfig.class})
class MealRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private MealRequestService mealRequestService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("POST /api/meal-requests: 201 + 신청 응답")
    void create_201() throws Exception {
        when(mealRequestService.create(eq(1L), any()))
                .thenReturn(new MealRequestResponse(7L, 10L, "같이 드실래요?", "PENDING"));

        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10,\"message\":\"같이 드실래요?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mealRequestId").value(7))
                .andExpect(jsonPath("$.data.toCheckInId").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST: toCheckInId 누락이면 400")
    void create_invalid() throws Exception {
        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST: 토큰 없으면 401")
    void create_401() throws Exception {
        mockMvc.perform(post("/api/meal-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST: 온보딩 토큰이면 403")
    void create_onboarding_403() throws Exception {
        mockMvc.perform(post("/api/meal-requests")
                        .header("Authorization", "Bearer " + jwtProvider.createOnboardingToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST: 수신 거부면 403 MEALREQUEST_OPT_OUT")
    void create_403() throws Exception {
        when(mealRequestService.create(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.MEALREQUEST_OPT_OUT));
        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEALREQUEST_OPT_OUT"));
    }

    @Test
    @DisplayName("POST: 중복이면 409 MEALREQUEST_DUPLICATE")
    void create_409() throws Exception {
        when(mealRequestService.create(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.MEALREQUEST_DUPLICATE));
        mockMvc.perform(post("/api/meal-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toCheckInId\":10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEALREQUEST_DUPLICATE"));
    }

    @Test
    @DisplayName("GET /api/meal-requests: received 기본, 200 + 목록")
    void list_200() throws Exception {
        when(mealRequestService.getMealRequests(eq(1L), eq("received"), any()))
                .thenReturn(List.of(new MealRequestListItemResponse(
                        7L, new MealRequestListItemResponse.FromUser("옆자리"), 3L, "같이 드실래요?", "PENDING",
                        LocalDateTime.of(2026, 6, 18, 12, 5))));

        mockMvc.perform(get("/api/meal-requests").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mealRequestId").value(7))
                .andExpect(jsonPath("$.data[0].fromUser.nickname").value("옆자리"))
                .andExpect(jsonPath("$.data[0].placeId").value(3))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /{id}/accept: 200 + ACCEPTED")
    void accept_200() throws Exception {
        when(mealRequestService.accept(1L, 7L))
                .thenReturn(new MealRequestStatusResponse(7L, "ACCEPTED", LocalDateTime.of(2026, 6, 18, 12, 10)));

        mockMvc.perform(patch("/api/meal-requests/7/accept").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("PATCH /{id}/accept: 수신자 아니면 403")
    void accept_403() throws Exception {
        when(mealRequestService.accept(1L, 7L))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN));
        mockMvc.perform(patch("/api/meal-requests/7/accept").header("Authorization", userToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/accept: 없으면 404")
    void accept_404() throws Exception {
        when(mealRequestService.accept(1L, 99L))
                .thenThrow(new BusinessException(ErrorCode.MEALREQUEST_NOT_FOUND));
        mockMvc.perform(patch("/api/meal-requests/99/accept").header("Authorization", userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEALREQUEST_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /{id}/decline: 200 + DECLINED")
    void decline_200() throws Exception {
        when(mealRequestService.decline(1L, 7L))
                .thenReturn(new MealRequestStatusResponse(7L, "DECLINED", LocalDateTime.of(2026, 6, 18, 12, 10)));

        mockMvc.perform(patch("/api/meal-requests/7/decline").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }
}
