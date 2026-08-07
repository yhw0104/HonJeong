package com.honjeong.push.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.notification.domain.NotificationType;

/**
 * 두 enum이 1:1로 맞물려 있는지 강제하는 회귀 테스트.
 *
 * <p><b>왜 필요한가.</b> {@link PushType#from}은 {@code valueOf(type.name())}이다. 나중에
 * {@link NotificationType}에만 값을 추가하고 여기에 빠뜨리면 그 알림이 발행되는 순간
 * {@code IllegalArgumentException}이 난다 — 알림함 저장까지 끝난 뒤에. 그 예외가 어디서
 * 잡히느냐에 따라 신청 자체가 롤백될 수도 있다(PushDispatcher Javadoc).
 *
 * <p>이 테스트는 그 사고를 <b>enum에 값을 추가한 커밋에서</b> 잡는다. 런타임이 아니라.
 */
@DisplayName("PushType ↔ NotificationType 사상")
class PushTypeTest {

    @Test
    @DisplayName("NotificationType의 모든 값이 PushType으로 사상된다")
    void 알림함_종류는_전부_사상된다() {
        assertThatCode(() -> Arrays.stream(NotificationType.values()).forEach(PushType::from))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("사상 결과는 이름이 같은 값이다")
    void 같은_이름으로_사상된다() {
        for (NotificationType type : NotificationType.values()) {
            assertThat(PushType.from(type).name()).isEqualTo(type.name());
        }
    }

    @Test
    @DisplayName("CHAT_MESSAGE를 빼면 PushType은 NotificationType과 정확히 같은 집합이다 — 반대 방향도 막는다")
    void 채팅을_빼면_두_집합이_같다() {
        assertThat(Arrays.stream(PushType.values())
                .filter(t -> t != PushType.CHAT_MESSAGE)
                .map(Enum::name).toList())
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(NotificationType.values()).map(Enum::name).toList());
    }
}
