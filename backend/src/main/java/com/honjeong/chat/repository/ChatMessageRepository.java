package com.honjeong.chat.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.chat.domain.ChatMessage;

/**
 * 매칭 대화 메시지 데이터 접근 — 대화방별 메시지 목록(시간순), 안읽음 개수 집계 (대상 테이블: chat_messages).
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 대화방의 메시지를 오래된순(id 오름차순=작성순)으로 전부 조회.
     *
     * @param conversationId 대화방 id
     * @return 대화방 메시지 목록(작성순)
     */
    List<ChatMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    /**
     * 대화방에서 상대가 내 마지막 읽음 시각 이후 보낸 메시지 수(안읽음 뱃지용). 내가 보낸 메시지는 항상 제외한다.
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

    /**
     * 여러 대화방의 "마지막 메시지"를 한 번의 쿼리로 조회(대화 목록 미리보기 배치용).
     * <p>목록 화면이 대화마다 메시지를 다시 읽던 N+1을 없앤다. 마지막 메시지 = id 최대값(작성순 = id 오름차순).
     * 메시지가 하나도 없는 대화방은 결과에 포함되지 않으므로, 호출부는 "없으면 미리보기 null"로 처리한다.
     * 빈 목록을 넘기면 IN () 이 되어 DB마다 동작이 다르므로 호출부에서 빈 목록을 걸러 호출한다.
     *
     * @param conversationIds 대화방 id 목록(비어 있지 않아야 함)
     * @return 대화방별 마지막 메시지(메시지 없는 대화방은 제외)
     */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.conversation.id IN :conversationIds
              AND m.id = (SELECT MAX(m2.id) FROM ChatMessage m2 WHERE m2.conversation.id = m.conversation.id)
            """)
    List<ChatMessage> findLastMessagesByConversationIds(@Param("conversationIds") List<Long> conversationIds);
}
