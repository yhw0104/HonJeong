package com.honjeong.chat.dto;

import java.time.LocalDateTime;

/**
 * 읽음 이벤트(서버 → 앱).
 *
 * <p>{@code readerUserId} 하나로 앱이 두 경우를 가른다.
 * <ul>
 *   <li><b>나</b> — 그 대화의 안읽음 수를 0으로(내 다른 기기의 배지가 사라진다)</li>
 *   <li><b>상대</b> — {@code partnerLastReadAt}을 갱신(내 마지막 메시지에 '읽음'이 뜬다)</li>
 * </ul>
 *
 * @param type           항상 "read"
 * @param conversationId 대화방 id
 * @param readerUserId   읽은 사람
 * @param readAt         읽은 시각
 */
public record WsReadEvent(String type, Long conversationId, Long readerUserId, LocalDateTime readAt) {

    public static WsReadEvent of(Long conversationId, Long readerUserId, LocalDateTime readAt) {
        return new WsReadEvent("read", conversationId, readerUserId, readAt);
    }
}
