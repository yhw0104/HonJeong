package com.honjeong.report.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.honjeong.global.security.JwtProvider;
import com.honjeong.report.dto.MyReportResponse;
import com.honjeong.report.dto.ReportCreateResponse;
import com.honjeong.report.service.ReportService;
import com.honjeong.support.ActiveUserSliceSupport;

@WebMvcTest(controllers = ReportController.class)
@Import({SecurityConfig.class, WebConfig.class})
class ReportControllerTest extends ActiveUserSliceSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private ReportService reportService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("POST /api/reports: 201 + reportId")
    void create_201() throws Exception {
        when(reportService.create(eq(1L), any())).thenReturn(new ReportCreateResponse(7L, "RECEIVED"));

        mockMvc.perform(post("/api/reports").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"USER\",\"targetId\":2,\"reasonCode\":\"SPAM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportId").value(7));
    }

    @Test
    @DisplayName("GET /api/reports: 200 + 내 신고 내역")
    void list_200() throws Exception {
        List<MyReportResponse> stub = List.of(new MyReportResponse(
                7L, "USER", "상대", "SPAM", null, "RECEIVED", LocalDateTime.now()));
        when(reportService.getMyReports(1L)).thenReturn(stub);

        mockMvc.perform(get("/api/reports").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].targetNickname").value("상대"));
    }

    @Test
    @DisplayName("POST /api/reports: reasonCode 공백 → 400")
    void create_400_blankReasonCode() throws Exception {
        mockMvc.perform(post("/api/reports").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"USER\",\"targetId\":2,\"reasonCode\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reportService);
    }
}
