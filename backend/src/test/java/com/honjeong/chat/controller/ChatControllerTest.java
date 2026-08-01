package com.honjeong.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.honjeong.chat.dto.ChatMessageResponse;
import com.honjeong.chat.dto.ConversationSummaryResponse;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * {@link ChatController} 웹 슬라이스 테스트. HTTP 매핑·상태코드·인가·{@code @Valid}를 검증하고 로직은 서비스 모킹.
 */
@WebMvcTest(controllers = ChatController.class)
@Import({SecurityConfig.class, WebConfig.class})
class ChatControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private ConversationService conversationService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("GET /api/conversations: 200 + 대화 목록")
    void list_200() throws Exception {
        when(conversationService.listMine(1L)).thenReturn(List.of(
                new ConversationSummaryResponse(10L, "ACTIVE", 2L, "옆자리", null,
                        "큰순두부", "같이 드실래요?", LocalDateTime.of(2026, 6, 18, 12, 0), 3L,
                        LocalDateTime.of(2026, 6, 18, 12, 5), LocalDateTime.of(2026, 6, 18, 11, 0))));

        mockMvc.perform(get("/api/conversations").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].conversationId").value(10))
                .andExpect(jsonPath("$.data[0].partnerNickname").value("옆자리"))
                .andExpect(jsonPath("$.data[0].unreadCount").value(3));
    }

    @Test
    @DisplayName("GET /api/conversations: 토큰 없으면 401")
    void list_401() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /{id}/messages: 200 + 메시지 목록")
    void messages_200() throws Exception {
        when(conversationService.messages(1L, 10L)).thenReturn(List.of(
                new ChatMessageResponse(100L, 1L, "TEXT", "안녕하세요", null,
                        LocalDateTime.of(2026, 6, 18, 12, 1))));

        mockMvc.perform(get("/api/conversations/10/messages").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].type").value("TEXT"))
                .andExpect(jsonPath("$.data[0].text").value("안녕하세요"));
    }

    @Test
    @DisplayName("GET /{id}/messages: 비참여자/없는 대화면 404")
    void messages_404() throws Exception {
        when(conversationService.messages(1L, 99L))
                .thenThrow(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        mockMvc.perform(get("/api/conversations/99/messages").header("Authorization", userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONVERSATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /{id}/messages: TEXT 유효 요청이면 200 + 생성된 메시지 (201 아님)")
    void send_200() throws Exception {
        when(conversationService.sendMessage(eq(1L), eq(10L), any()))
                .thenReturn(new ChatMessageResponse(101L, 1L, "TEXT", "반갑습니다", null,
                        LocalDateTime.of(2026, 6, 18, 12, 2)));

        mockMvc.perform(post("/api/conversations/10/messages").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TEXT\",\"text\":\"반갑습니다\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.type").value("TEXT"))
                .andExpect(jsonPath("$.data.text").value("반갑습니다"));
    }

    @Test
    @DisplayName("POST /{id}/messages: type 누락이면 400")
    void send_invalid_400() throws Exception {
        mockMvc.perform(post("/api/conversations/10/messages").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"반갑습니다\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /{id}/messages: 종료된 대화면 409 CONVERSATION_CLOSED")
    void send_closed_409() throws Exception {
        when(conversationService.sendMessage(eq(1L), eq(10L), any()))
                .thenThrow(new BusinessException(ErrorCode.CONVERSATION_CLOSED));

        mockMvc.perform(post("/api/conversations/10/messages").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TEXT\",\"text\":\"반갑습니다\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONVERSATION_CLOSED"));
    }

    @Test
    @DisplayName("POST /{id}/messages: 비참여자/없는 대화면 404")
    void send_404() throws Exception {
        when(conversationService.sendMessage(eq(1L), eq(99L), any()))
                .thenThrow(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND));

        mockMvc.perform(post("/api/conversations/99/messages").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TEXT\",\"text\":\"반갑습니다\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONVERSATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /{id}/read: 200 + 읽음 처리 위임")
    void read_200() throws Exception {
        doNothing().when(conversationService).markRead(1L, 10L);

        mockMvc.perform(post("/api/conversations/10/read").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(conversationService).markRead(1L, 10L);
    }

    @Test
    @DisplayName("POST /{id}/read: 비참여자/없는 대화면 404")
    void read_404() throws Exception {
        doThrow(new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND))
                .when(conversationService).markRead(1L, 99L);

        mockMvc.perform(post("/api/conversations/99/read").header("Authorization", userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONVERSATION_NOT_FOUND"));
    }
}
