package com.honjeong.push.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.repository.DeviceTokenRepository;

/**
 * 발송 3단계 — <b>기록</b>. 발송 결과를 DB에 남긴다(산 토큰은 사용 시각 갱신, 죽은 토큰은 삭제).
 *
 * <p>사용처: {@link PushSendTask}.
 *
 * <p><b>왜 별도 빈인가.</b> {@link PushAudienceReader} Javadoc 참조 — 자기호출은 프록시를 타지
 * 않아 {@code @Transactional}이 무효가 된다. 이 클래스가 별도 빈이어야 여기서 여는 트랜잭션이
 * <b>FCM HTTP가 끝난 뒤에</b> 열리고, 그 사이 DB 커넥션을 붙잡고 있지 않게 된다.
 *
 * <p><b>왜 id로 재조회하는가.</b> 1단계에서 읽은 엔티티는 그 트랜잭션이 끝나면 detached라
 * 더티체킹이 돌지 않는다 — 그 상태로 {@code markUsed}를 불러 봐야 메모리 위 객체만 바뀌고
 * UPDATE는 나가지 않는다(조용히 아무 일도 안 일어난다). 그래서 id 목록을 받아 이 트랜잭션에서
 * 다시 읽는다. 벌크 UPDATE 대신 재조회를 쓰는 이유는 {@code updated_at} 감사 갱신을 살리기
 * 위해서다(JPQL 벌크 UPDATE는 {@code @LastModifiedDate}를 건너뛴다).
 */
@Component
public class PushDeliveryRecorder {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DeviceTokenRepository deviceTokenRepository;
    private final Clock clock;

    public PushDeliveryRecorder(DeviceTokenRepository deviceTokenRepository, Clock clock) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.clock = clock;
    }

    /**
     * 발송 결과를 반영한다.
     *
     * <p>★ {@code liveTokenIds}는 "<b>성공</b>한 토큰"이 아니라 "<b>영구 무효가 아닌</b> 토큰"이다.
     * {@link PushSender#send}는 죽은 토큰 목록만 돌려주므로 일시 실패
     * ({@code UNAVAILABLE}·{@code INTERNAL}·{@code QUOTA_EXCEEDED}·{@code INVALID_ARGUMENT})와
     * 성공을 구분할 수 없다. 그래서 {@code last_used_at}은 실제로 <b>마지막 발송 시도 시각</b>이다.
     * 지금은 이 컬럼을 읽는 곳이 없지만, 나중에 "N일 이상 미사용 토큰 정리" 같은 것을 붙일 때
     * 이 차이를 모르면 죽은 토큰이 영원히 살아 있는 것으로 보인다.
     *
     * @param liveTokenIds 영구 무효가 아닌 토큰의 id — {@code last_used_at}을 지금으로 갱신한다
     * @param deadTokens   영구 무효로 판정된 토큰 문자열 — 삭제한다. 안 지우면 계정마다 쓰레기가
     *                     쌓여 실패 호출만 늘어난다
     * @param ownerId      발송 시점의 토큰 주인 — 삭제를 이 사용자의 행으로 한정한다
     *                     ({@code DeviceTokenRepository.deleteByTokenAndUserId} Javadoc 참조)
     */
    @Transactional
    public void recordResult(List<Long> liveTokenIds, List<String> deadTokens, Long ownerId) {
        if (!liveTokenIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
            for (DeviceToken token : deviceTokenRepository.findAllById(liveTokenIds)) {
                token.markUsed(now);
            }
        }
        deadTokens.forEach(token -> deviceTokenRepository.deleteByTokenAndUserId(token, ownerId));
    }
}
