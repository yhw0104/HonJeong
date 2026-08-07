package com.honjeong.push.service;

import com.honjeong.push.domain.PushType;

/**
 * 푸시 배너 문구 조립. <b>서버측 문구는 전부 여기서만 만든다.</b>
 *
 * <p>★ 알려진 중복: 같은 사건의 문구가 두 곳에 산다. 알림함 문구는 앱이 만들고
 * ({@code app/src/features/notifications/copy.ts}), 푸시 배너는 OS가 그리므로 서버가
 * 완성된 문장을 보내야 한다. 제거할 수 없는 중복이므로 <b>문구를 고칠 때는 두 파일을
 * 같은 커밋에서 고친다.</b> 한쪽만 고치면 알림함과 배너가 다른 말을 하게 된다.
 *
 * <p>사용처: PushSendTask, ConversationService(채팅 미리보기).
 */
public final class PushMessages {

    private static final String TITLE = "혼정";

    private PushMessages() {
    }

    /**
     * 종류별 배너 문구를 만든다.
     *
     * @param type          푸시 종류
     * @param actorNickname 상대 닉네임(DisplayNames를 통과한 값 — 탈퇴자는 '알 수 없음').
     *                      BADGE_EARNED는 상대가 없어 null이 들어온다
     * @param preview       채팅 메시지 미리보기(CHAT_MESSAGE일 때만 사용, 그 외 null)
     * @return 제목·본문·종류가 채워진 메시지(conversationId는 호출자가 채운다)
     */
    public static PushMessage of(PushType type, String actorNickname, String preview) {
        String who = actorNickname == null ? "누군가" : actorNickname;
        String body = switch (type) {
            case MEAL_REQUEST_RECEIVED -> who + "님이 같이 먹기를 신청했어요";
            case MEAL_REQUEST_ACCEPTED -> who + "님이 같이 먹기를 수락했어요";
            case MEAL_MATCH_CANCELLED -> who + "님이 같이 먹기 약속을 취소했어요";
            case MATE_REQUEST_RECEIVED -> who + "님이 메이트를 신청했어요";
            case MATE_REQUEST_ACCEPTED -> who + "님이 메이트를 수락했어요";
            case BADGE_EARNED -> "새 뱃지를 획득했어요 🎉";
            case CHAT_MESSAGE -> who + ": " + preview;
        };
        return new PushMessage(TITLE, body, type, null);
    }

    /**
     * 채팅 미리보기 문구. 이미지 메시지는 본문이 없으므로 대체 문구를 쓴다.
     *
     * @param text    텍스트 메시지 본문(이미지면 null)
     * @param isImage 이미지 메시지인가
     * @return 배너에 넣을 미리보기
     */
    public static String chatPreview(String text, boolean isImage) {
        return isImage ? "사진을 보냈어요" : text;
    }
}
