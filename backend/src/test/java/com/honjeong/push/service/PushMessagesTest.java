package com.honjeong.push.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.push.domain.PushType;

/**
 * PushMessages 단위 테스트.
 *
 * <p>검증 목적: 배너에 실제로 찍히는 문장을 고정한다. 문구가 바뀌면 여기가 먼저 빨개져
 * 앱 {@code copy.ts}와 함께 고쳐야 한다는 사실을 상기시킨다.
 */
@DisplayName("PushMessages")
class PushMessagesTest {

    @Test
    @DisplayName("상대가 있는 알림은 닉네임을 앞에 붙인다")
    void 닉네임_접두() {
        assertThat(PushMessages.of(PushType.MEAL_REQUEST_RECEIVED, "김하늘", null).body())
                .isEqualTo("김하늘님이 같이 먹기를 신청했어요");
    }

    @Test
    @DisplayName("뱃지 획득은 상대가 없어 닉네임을 붙이지 않는다")
    void 뱃지는_닉네임_없음() {
        assertThat(PushMessages.of(PushType.BADGE_EARNED, null, null).body())
                .isEqualTo("새 뱃지를 획득했어요 🎉");
    }

    @Test
    @DisplayName("닉네임이 null이면 '누군가'로 대체한다 — 배너에 빈칸이 뜨지 않게")
    void 널_닉네임_대체() {
        assertThat(PushMessages.of(PushType.MATE_REQUEST_RECEIVED, null, null).body())
                .isEqualTo("누군가님이 메이트를 신청했어요");
    }

    @Test
    @DisplayName("채팅은 '닉네임: 내용' 형식이다")
    void 채팅_문구() {
        assertThat(PushMessages.of(PushType.CHAT_MESSAGE, "김하늘", "어디세요?").body())
                .isEqualTo("김하늘: 어디세요?");
    }

    @Test
    @DisplayName("사진 메시지는 대체 문구를 쓴다")
    void 사진_미리보기() {
        assertThat(PushMessages.chatPreview(null, true)).isEqualTo("사진을 보냈어요");
    }

    @Test
    @DisplayName("텍스트 메시지는 본문을 그대로 쓴다")
    void 텍스트_미리보기() {
        assertThat(PushMessages.chatPreview("어디세요?", false)).isEqualTo("어디세요?");
    }
}
