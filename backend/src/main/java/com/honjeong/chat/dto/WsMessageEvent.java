package com.honjeong.chat.dto;

/**
 * 새 메시지 이벤트(서버 → 앱).
 *
 * <p>{@code message}는 REST 응답과 <b>같은</b> {@link ChatMessageResponse}다 —
 * 두 경로가 다른 모양을 내려보내면 앱에서 처리가 갈린다.
 *
 * @param type           항상 "message"
 * @param conversationId 이 메시지가 속한 대화방
 * @param message        메시지 본문
 */
public record WsMessageEvent(String type, Long conversationId, ChatMessageResponse message) {

    public static WsMessageEvent of(Long conversationId, ChatMessageResponse message) {
        return new WsMessageEvent("message", conversationId, message);
    }
}
