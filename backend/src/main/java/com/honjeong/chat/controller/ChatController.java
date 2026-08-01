package com.honjeong.chat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.chat.dto.ChatMessageResponse;
import com.honjeong.chat.dto.ConversationSummaryResponse;
import com.honjeong.chat.dto.SendMessageRequest;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

import jakarta.validation.Valid;

/**
 * 매칭 대화(match chat) HTTP 엔드포인트 — 목록/메시지 조회/전송/읽음 처리.
 *
 * <p>사용처: 프론트 채팅 화면(대화 목록·메시지 스레드·전송·읽음 동기화).
 */
@RestController
@RequestMapping("/api/conversations")
public class ChatController {

    private final ConversationService conversationService;

    public ChatController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ApiResponse<List<ConversationSummaryResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(conversationService.listMine(userId));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(conversationService.messages(userId, id));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<ChatMessageResponse> send(@CurrentUserId Long userId, @PathVariable Long id,
                                                 @Valid @RequestBody SendMessageRequest req) {
        return ApiResponse.success(conversationService.sendMessage(userId, id, req));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@CurrentUserId Long userId, @PathVariable Long id) {
        conversationService.markRead(userId, id);
        return ApiResponse.success(null);
    }

    /**
     * 대화방을 내 목록에서만 삭제한다(소프트 삭제).
     *
     * <p>사용 화면: 대화 목록(ConversationList)의 스와이프 삭제. 종료된 대화만 삭제할 수 있다.
     *
     * @param userId 인증 사용자 ID
     * @param id     대화방 id
     * @return 빈 성공 응답
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        conversationService.deleteForMe(userId, id);
        return ApiResponse.success(null);
    }
}
