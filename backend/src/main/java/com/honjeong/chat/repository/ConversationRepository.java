package com.honjeong.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.chat.domain.Conversation;

/**
 * 매칭 대화방 데이터 접근 — 매칭(meal_request)당 단건 조회, 사용자별 대화 목록 (대상 테이블: conversations).
 * <p>대화방은 meal_request_id UNIQUE FK로 매칭 1건당 1개만 존재한다. 목록 조회는 place·fromUser·toUser를
 * fetch join해 장소명·상대 닉네임 조회 시 N+1을 막는다.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 매칭(같이먹기 신청) id로 대화방 단건 조회(신규 매칭 시 기존 대화 존재 여부 확인용).
     *
     * @param mealRequestId 매칭(같이먹기 신청) id
     * @return 대화방 또는 빈 Optional
     */
    Optional<Conversation> findByMealRequestId(Long mealRequestId);

    /**
     * 사용자가 참여한(from 또는 to) 대화 목록을 마지막 활동순으로 조회.
     * <p>정렬 기준은 "마지막 활동 시각" — 메시지가 있으면 lastMessageAt, 아직 없으면(방금 매칭) createdAt을 쓴다.
     * 목록 오른쪽에 표시되는 시각과 정렬 순서가 항상 일치한다.
     *
     * @param userId 조회할 사용자 id
     * @return 참여 대화 목록(마지막 활동순)
     */
    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.place
            JOIN FETCH c.fromUser
            JOIN FETCH c.toUser
            WHERE c.fromUser.id = :userId OR c.toUser.id = :userId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC, c.id DESC
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);
}
