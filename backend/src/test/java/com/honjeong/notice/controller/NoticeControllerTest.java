package com.honjeong.notice.controller;

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
import com.honjeong.notice.dto.NoticesResponse;
import com.honjeong.notice.service.NoticeService;

@WebMvcTest(controllers = NoticeController.class)
@Import({SecurityConfig.class, WebConfig.class})
class NoticeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private NoticeService noticeService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("GET /api/notices: 200 + 목록")
    void list_200() throws Exception {
        var item = new NoticesResponse.Item(1L, "UPDATE", "새 기능 안내", "본문", true,
                LocalDateTime.of(2026, 7, 1, 10, 0));
        when(noticeService.getNotices()).thenReturn(new NoticesResponse(List.of(item)));

        mockMvc.perform(get("/api/notices").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notices[0].title").value("새 기능 안내"))
                .andExpect(jsonPath("$.data.notices[0].category").value("UPDATE"))
                .andExpect(jsonPath("$.data.notices[0].pinned").value(true));
    }

    @Test
    @DisplayName("GET /api/notices: 토큰 없으면 401")
    void list_401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/notices")).andExpect(status().isUnauthorized());
    }
}
