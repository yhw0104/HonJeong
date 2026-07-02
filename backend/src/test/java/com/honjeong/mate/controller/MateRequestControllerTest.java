package com.honjeong.mate.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.honjeong.global.security.JwtProvider;
import com.honjeong.mate.dto.MateRequestResponse;
import com.honjeong.mate.service.MateRequestService;

@WebMvcTest(controllers = MateRequestController.class)
@Import({SecurityConfig.class, WebConfig.class})
class MateRequestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private MateRequestService mateRequestService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("POST /api/mate-requests: 201 + PENDING")
    void create_201() throws Exception {
        when(mateRequestService.create(eq(1L), any()))
                .thenReturn(new MateRequestResponse(7L, 2L, "PENDING"));
        mockMvc.perform(post("/api/mate-requests").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"toUserId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("PATCH /api/mate-requests/9/accept: 200")
    void accept_200() throws Exception {
        when(mateRequestService.accept(1L, 9L))
                .thenReturn(new com.honjeong.mate.dto.MateRequestStatusResponse(9L, "ACCEPTED", null));
        mockMvc.perform(patch("/api/mate-requests/9/accept").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }
}
