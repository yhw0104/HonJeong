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

    @Test
    @DisplayName("상한과 같은 길이면 자르지 않는다 — 경계")
    void 상한_경계는_자르지_않는다() {
        String exact = "가".repeat(PushMessages.MAX_PREVIEW_LENGTH);

        assertThat(PushMessages.chatPreview(exact, false)).isEqualTo(exact);
    }

    @Test
    @DisplayName("상한을 한 글자라도 넘으면 잘라서 말줄임표를 붙인다 — 경계")
    void 상한을_넘으면_자른다() {
        String tooLong = "가".repeat(PushMessages.MAX_PREVIEW_LENGTH + 1);

        String preview = PushMessages.chatPreview(tooLong, false);

        assertThat(preview).hasSize(PushMessages.MAX_PREVIEW_LENGTH).endsWith("…");
        assertThat(preview).startsWith("가".repeat(PushMessages.MAX_PREVIEW_LENGTH - 1));
    }

    @Test
    @DisplayName("1000자(요청 상한)를 넣어도 100자로 잘린다 — FCM 페이로드 4096바이트 초과 방지")
    void 요청_상한_1000자도_잘린다() {
        String maxRequest = "한".repeat(1000);

        assertThat(PushMessages.chatPreview(maxRequest, false))
                .hasSize(PushMessages.MAX_PREVIEW_LENGTH);
    }

    @Test
    @DisplayName("자르는 자리가 이모지 한가운데여도 반토막 내지 않는다 — 깨진 문자가 다시 INVALID_ARGUMENT를 부른다")
    void 이모지를_반토막_내지_않는다() {
        // 자르는 자리(99번째 char)에 이모지의 앞 char이 오도록 배치한다 → 그대로 자르면 쌍이 쪼개진다.
        String text = "가".repeat(PushMessages.MAX_PREVIEW_LENGTH - 2) + "🎉" + "나".repeat(10);

        String preview = PushMessages.chatPreview(text, false);

        // 이모지를 통째로 버리고 그 앞에서 끊는다 — 짝 잃은 서로게이트가 남으면 안 된다.
        assertThat(preview).isEqualTo("가".repeat(PushMessages.MAX_PREVIEW_LENGTH - 2) + "…");
        assertThat(Character.isSurrogate(preview.charAt(preview.length() - 2))).isFalse();
    }

    @Test
    @DisplayName("잘린 미리보기가 그대로 배너 본문에 들어간다")
    void 잘린_미리보기가_본문에_들어간다() {
        String preview = PushMessages.chatPreview("나".repeat(300), false);

        assertThat(PushMessages.of(PushType.CHAT_MESSAGE, "김하늘", preview).body())
                .isEqualTo("김하늘: " + preview)
                .hasSizeLessThan(120);
    }

    @Test
    @DisplayName("chatPreview를 거치지 않고 of()에 직접 긴 값을 줘도 잘린다 — 새 호출 지점이 상한을 다시 열지 못하게")
    void of가_직접_받은_긴_값도_자른다() {
        // 절단이 chatPreview에만 있으면 "호출자가 먼저 chatPreview를 거친다"는 관례에 기대게 되는데,
        // 관례는 다음 호출 지점이 생기는 순간 깨진다. 상한 초과는 그 사용자의 푸시를 통째로 끊으므로
        // (INVALID_ARGUMENT → 토큰 삭제로 오판했던 것이 I-3) 규약이 아니라 코드로 막는다.
        String raw = "한".repeat(1000);

        String body = PushMessages.of(PushType.CHAT_MESSAGE, "김하늘", raw).body();

        assertThat(body).isEqualTo("김하늘: " + "한".repeat(PushMessages.MAX_PREVIEW_LENGTH - 1) + "…");
    }
}
