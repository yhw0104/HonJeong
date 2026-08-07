package com.honjeong.push.service;

import com.honjeong.push.domain.PushType;

/**
 * 기기로 보낼 푸시 한 건.
 *
 * @param title          배너 제목(OS가 그린다)
 * @param body           배너 본문(OS가 그린다)
 * @param type           앱이 이동·무효화 판단에 쓰는 종류
 * @param conversationId 채팅일 때만 채워지는 대화방 id(그 외 null)
 */
public record PushMessage(String title, String body, PushType type, Long conversationId) {
}
