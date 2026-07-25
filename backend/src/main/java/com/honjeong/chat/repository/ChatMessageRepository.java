package com.honjeong.chat.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.chat.domain.ChatMessage;

/**
 * 1. 기능: 매칭 대화 메시지 데이터 접근 — 대화방별 메시지 목록(시간순), 안읽음 개수 집계 (대상 테이블: chat_messages)
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 기능: 대화방의 메시지를 오래된순(id 오름차순=작성순)으로 전부 조회
     * 쿼리: SELECT m.* FROM chat_messages m WHERE m.conversation_id = :conversationId ORDER BY m.id ASC
     * Request: conversationId — 대화방 id / Response: List&lt;ChatMessage&gt; — 대화방 메시지 목록(작성순)
     *
     * @param conversationId 대화방 id
     * @return 대화방 메시지 목록(작성순)
     */
    List<ChatMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    /**
     * 기능: 대화방에서 상대가 내 마지막 읽음 시각 이후 보낸 메시지 수(안읽음 뱃지용). 내가 보낸 메시지는 항상 제외한다.
     * 쿼리: SELECT COUNT(m.id) FROM chat_messages m WHERE m.conversation_id = :conversationId AND m.sender_user_id &lt;&gt; :userId
     *       AND (CAST(:lastReadAt AS timestamp) IS NULL OR m.created_at &gt; :lastReadAt)
     *       (lastReadAt를 timestamp로 CAST — Postgres가 "? IS NULL" 단독 사용만으로는 파라미터 타입을 추론 못해 타입힌트 필요)
     * Request: conversationId — 대화방 id, userId — 안읽음을 셀 기준 사용자(나) id, lastReadAt — 내 마지막 읽음 시각(한 번도 안 읽었으면 null)
     * Response: long — 안읽음 메시지 개수
     *
     * <p>lastReadAt이 null이면(한 번도 읽지 않음) 상대가 보낸 메시지 전부를 안읽음으로 센다.
     *
     * @param conversationId 대화방 id
     * @param userId         안읽음을 셀 기준 사용자(나) id
     * @param lastReadAt     내 마지막 읽음 시각(null이면 한 번도 안 읽음)
     * @return 안읽음 메시지 개수
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.senderUserId <> :userId
              AND (CAST(:lastReadAt AS timestamp) IS NULL OR m.createdAt > :lastReadAt)
            """)
    long countUnread(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("lastReadAt") LocalDateTime lastReadAt);
}
