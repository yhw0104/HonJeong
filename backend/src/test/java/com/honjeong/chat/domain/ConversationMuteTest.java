package com.honjeong.chat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;

/**
 * Conversation의 참여자별 음소거 단위 테스트.
 *
 * <p>음소거는 참여자 id 비교만 하므로 DB가 필요 없다 — ConversationMessagingTest와 같은 방식으로
 * 연관 엔티티를 목으로 세우고 순수 단위로 검증한다(컬럼 매핑은 ChatMappingTest가 본다).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Conversation 음소거")
class ConversationMuteTest {

    private static final Long FROM_ID = 10L;
    private static final Long TO_ID = 20L;

    @Test
    @DisplayName("from 참여자가 끄면 from만 꺼지고 to는 그대로다")
    void from만_꺼진다() {
        Conversation c = 대화를_만든다();

        c.setMuted(FROM_ID, true);

        assertThat(c.isMutedBy(FROM_ID)).isTrue();
        assertThat(c.isMutedBy(TO_ID)).isFalse();
    }

    @Test
    @DisplayName("to 참여자가 끄면 to만 꺼진다")
    void to만_꺼진다() {
        Conversation c = 대화를_만든다();

        c.setMuted(TO_ID, true);

        assertThat(c.isMutedBy(TO_ID)).isTrue();
        assertThat(c.isMutedBy(FROM_ID)).isFalse();
    }

    @Test
    @DisplayName("다시 켜면 원래대로 돌아온다")
    void 다시_켤_수_있다() {
        Conversation c = 대화를_만든다();
        c.setMuted(FROM_ID, true);

        c.setMuted(FROM_ID, false);

        assertThat(c.isMutedBy(FROM_ID)).isFalse();
    }

    @Test
    @DisplayName("참여자가 아닌 사람은 음소거 상태가 false로 읽힌다 — isDeletedBy와 같은 규칙")
    void 비참여자는_false() {
        Conversation c = 대화를_만든다();

        assertThat(c.isMutedBy(99L)).isFalse();
    }

    @Test
    @DisplayName("기본값은 둘 다 켜짐(음소거 아님)이다")
    void 기본값은_알림_받음() {
        Conversation c = 대화를_만든다();

        assertThat(c.isMutedBy(FROM_ID)).isFalse();
        assertThat(c.isMutedBy(TO_ID)).isFalse();
    }

    private Conversation 대화를_만든다() {
        return Conversation.open(1L, mock(Place.class), userRef(FROM_ID), userRef(TO_ID));
    }

    private User userRef(Long id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }
}
