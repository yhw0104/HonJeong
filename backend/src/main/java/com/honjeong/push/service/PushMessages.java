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

    /** 채팅 미리보기 최대 길이(자). 말줄임표를 포함한 길이다. */
    public static final int MAX_PREVIEW_LENGTH = 100;

    private static final String TITLE = "혼정";
    private static final String ELLIPSIS = "…";

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
            // 여기서 한 번 더 자른다. 호출자가 chatPreview()를 거쳐 오는 것이 관례지만, 관례는
            // 다음 호출 지점이 생기는 순간 깨진다 — 상한 초과는 그 사용자의 푸시를 통째로 끊는
            // 사고(아래 truncate Javadoc)라 규약이 아니라 코드로 막는다. 이미 잘린 값은 그대로 통과한다.
            case CHAT_MESSAGE -> who + ": " + truncate(preview);
        };
        return new PushMessage(TITLE, body, type, null);
    }

    /**
     * 채팅 미리보기 문구. 이미지 메시지는 본문이 없으므로 대체 문구를 쓴다.
     *
     * @param text    텍스트 메시지 본문(이미지면 null)
     * @param isImage 이미지 메시지인가
     * @return 배너에 넣을 미리보기(길면 말줄임표로 끝난다)
     */
    public static String chatPreview(String text, boolean isImage) {
        if (isImage) {
            return "사진을 보냈어요";
        }
        return truncate(text);
    }

    /**
     * 미리보기를 {@value #MAX_PREVIEW_LENGTH}자 이하로 자른다. 이미 짧으면 그대로 돌려준다.
     *
     * <p><b>반드시 자른다.</b> {@code SendMessageRequest.text}의 상한은 1000자다. 최악은
     * <b>순수 한글 1000자</b>로, UTF-8에서 글자당 3바이트라 약 3000바이트가 되고 여기에 닉네임과
     * 나머지 페이로드 필드가 더해지면 FCM 상한(4096바이트)에 닿는다.
     *
     * <p>★ 이모지가 섞이면 오히려 <b>줄어든다</b> — 이모지는 4바이트지만 Java {@code String}에서
     * char 두 개(서로게이트 쌍)를 쓰므로 char당 2바이트꼴이다. 즉 같은 글자 수라면 한글보다 가볍다.
     * (이 파일의 이전 주석은 반대로 적혀 있었다. 위험한 쪽은 이모지가 아니라 순수 한글 장문이다.)
     *
     * <p>넘기면 FCM이 {@code INVALID_ARGUMENT}를 돌려주는데, 그건 <b>토큰이 아니라 우리 페이로드가
     * 잘못됐다는 뜻</b>이라 무효 토큰으로 오판하면 메시지 한 건이 그 사용자의 푸시를 통째로 끊는다
     * ({@code FcmPushSender.isPermanentlyInvalid} 주석 참조).
     *
     * <p>{@value #MAX_PREVIEW_LENGTH}자를 고른 근거: 배너는 어차피 두 줄만 그린다. 자르면 잠금화면에
     * 노출되는 대화 내용의 범위도 함께 줄어든다.
     *
     * @param text 자를 원본(null 허용 — 그대로 null을 돌려준다)
     * @return 상한 이하로 자른 문자열(잘렸으면 말줄임표로 끝난다)
     */
    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, cutIndex(text)) + ELLIPSIS;
    }

    /**
     * 말줄임표 한 글자를 뺀 자리에서 자르되, 서로게이트 쌍(이모지)을 반토막 내지 않는다.
     *
     * <p>이모지는 char 두 개다. 경계에서 앞쪽 char만 남기면 짝 잃은 서로게이트가 되고, UTF-8로
     * 직렬화될 때 깨진 바이트가 되어 FCM이 다시 {@code INVALID_ARGUMENT}를 돌려줄 수 있다.
     *
     * @param text 자를 원본(길이가 상한을 넘는 것이 보장된 값)
     * @return substring의 끝 인덱스(이 인덱스 바로 앞 char까지 남긴다)
     */
    private static int cutIndex(String text) {
        int end = MAX_PREVIEW_LENGTH - ELLIPSIS.length();
        return Character.isHighSurrogate(text.charAt(end - 1)) ? end - 1 : end;
    }
}
