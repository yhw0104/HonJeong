package com.honjeong.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.chat.domain.Conversation;

/**
 * 1. 기능: 매칭 대화방 데이터 접근 — 매칭(meal_request)당 단건 조회, 사용자별 대화 목록 (대상 테이블: conversations)
 *
 * <p>대화방은 meal_request_id UNIQUE FK로 매칭 1건당 1개만 존재한다. 목록 조회는 place·fromUser·toUser를
 * fetch join해 장소명·상대 닉네임 조회 시 N+1을 막는다.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 기능: 매칭(같이먹기 신청) id로 대화방 단건 조회(신규 매칭 시 기존 대화 존재 여부 확인용)
     * 쿼리: SELECT c.* FROM conversations c WHERE c.meal_request_id = :mealRequestId
     * Request: mealRequestId — 매칭된 같이먹기 신청 id / Response: Optional&lt;Conversation&gt; — 대화방 또는 빈 Optional
     *
     * @param mealRequestId 매칭(같이먹기 신청) id
     * @return 대화방 또는 빈 Optional
     */
    Optional<Conversation> findByMealRequestId(Long mealRequestId);

    /**
     * 기능: 사용자가 참여한(from 또는 to) 대화 목록을 최근 메시지순으로 조회
     * 쿼리: SELECT c.* FROM conversations c JOIN places ON c.place_id JOIN users ON c.from_user_id JOIN users ON c.to_user_id
     *       WHERE c.from_user_id = :userId OR c.to_user_id = :userId
     *       ORDER BY c.last_message_at DESC NULLS LAST, c.id DESC
     *       (place·fromUser·toUser 전부 JOIN FETCH — 장소명·상대 닉네임 N+1 방지)
     * Request: userId — 조회할 사용자 id / Response: List&lt;Conversation&gt; — 참여 대화 목록(최근 메시지순)
     *
     * <p>아직 메시지가 없어 lastMessageAt이 null인 대화(방금 매칭 성사)는 목록 맨 뒤로 보낸다(NULLS LAST).
     *
     * @param userId 조회할 사용자 id
     * @return 참여 대화 목록(최근 메시지순)
     */
    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.place
            JOIN FETCH c.fromUser
            JOIN FETCH c.toUser
            WHERE c.fromUser.id = :userId OR c.toUser.id = :userId
            ORDER BY c.lastMessageAt DESC NULLS LAST, c.id DESC
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);
}
