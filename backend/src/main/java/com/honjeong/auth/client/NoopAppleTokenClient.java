package com.honjeong.auth.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 개발용 — 애플에 아무것도 보내지 않는다. {@code honjeong.apple.mode}가 "mock"이거나 지정되지
 * 않았을 때(기본) 등록된다.
 *
 * <p>mock으로 떠 있는 상태를 조용히 두지 않는다({@link com.honjeong.push.service.NoopPushSender}와
 * 같은 이유 — 설정 주석은 서버를 띄우는 사람이 읽지 않는다는 전제). 기동 시 한 번 알린다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.apple.mode", havingValue = "mock", matchIfMissing = true)
public class NoopAppleTokenClient implements AppleTokenClient {

    private static final Logger log = LoggerFactory.getLogger(NoopAppleTokenClient.class);

    @PostConstruct
    void infoMockMode() {
        log.info("[APPLE] mock 모드로 기동합니다 — 애플 토큰을 교환·폐기하지 않습니다. "
                + "real 전환은 honjeong.apple.mode=real");
    }

    /**
     * 교환하지 않는다.
     *
     * @param authorizationCode 무시한다
     * @return 항상 null — 보관할 토큰이 없다(탈퇴 시 revoke를 건너뛴다)
     */
    @Override
    public String exchangeRefreshToken(String authorizationCode) {
        return null;
    }

    /**
     * 폐기하지 않고 시도 사실만 남긴다.
     *
     * @param refreshToken 무시한다(토큰 원문은 로그에 남기지 않는다)
     */
    @Override
    public void revoke(String refreshToken) {
        log.debug("[APPLE] mock 모드라 폐기를 건너뜁니다.");
    }
}
