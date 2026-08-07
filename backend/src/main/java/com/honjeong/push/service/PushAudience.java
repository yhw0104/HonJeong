package com.honjeong.push.service;

import java.util.List;

/**
 * 발송에 필요한 재료 — <b>조회 트랜잭션에서 뽑아 트랜잭션 밖으로 들고 나가는 값</b>이다.
 *
 * <p>엔티티가 아니라 이 record를 넘기는 이유: 조회 트랜잭션이 끝나면 {@code DeviceToken}은
 * detached라 지연 로딩이 터진다. 발송 구간(HTTP)과 기록 구간(다른 트랜잭션)이 쓰는 값만
 * 미리 복사해 두면 그 사고가 구조적으로 불가능해진다.
 *
 * @param tokens        수신자의 기기 토큰들(발송에 쓰는 값 + 나중에 재조회할 id)
 * @param actorNickname 배너에 찍을 상대 닉네임({@code DisplayNames}를 통과한 값). 상대가 없으면 null
 */
public record PushAudience(List<TokenRef> tokens, String actorNickname) {

    /** 보낼 곳이 없는 수신자(푸시 권한을 안 준 사용자). */
    public static final PushAudience EMPTY = new PushAudience(List.of(), null);

    /**
     * 기기 토큰 한 건.
     *
     * @param id    device_tokens.id — 기록 구간에서 재조회할 때 쓴다
     * @param value FCM 등록 토큰 — 발송에 쓴다
     */
    public record TokenRef(Long id, String value) {
    }

    /**
     * 보낼 곳이 하나도 없는가.
     *
     * @return 토큰이 0건이면 true
     */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    /**
     * 발송기에 넘길 토큰 문자열들.
     *
     * @return FCM 등록 토큰 목록
     */
    public List<String> tokenValues() {
        return tokens.stream().map(TokenRef::value).toList();
    }

    /**
     * 죽은 토큰을 뺀 나머지의 id — 기록 구간에서 {@code markUsed} 대상이다.
     *
     * @param deadTokens 발송기가 무효로 판정한 토큰 문자열들
     * @return 살아 있는 토큰의 id 목록
     */
    public List<Long> liveIdsExcluding(List<String> deadTokens) {
        return tokens.stream()
                .filter(t -> !deadTokens.contains(t.value()))
                .map(TokenRef::id)
                .toList();
    }
}
