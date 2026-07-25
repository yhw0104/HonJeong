package com.honjeong.chat.domain;

/** 대화방 상태. ACTIVE=메시지 전송 가능, CLOSED=읽기전용(영구 보관). */
public enum ConversationStatus {
    ACTIVE, CLOSED
}
