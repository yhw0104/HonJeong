package com.honjeong.push.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.push.domain.Platform;
import com.honjeong.push.repository.DeviceTokenRepository;

/**
 * 기기 토큰 등록·해제.
 *
 * <p>사용처: DeviceTokenController.
 *
 * <p>등록은 UPSERT다 — 토큰은 기기에 붙는 값이라 같은 토큰의 주인이 바뀔 수 있다
 * (한 휴대폰을 두 사람이 번갈아 쓰는 경우). 새 행을 만들면 UNIQUE 제약에 걸리고,
 * 주인을 안 바꾸면 이전 사용자의 알림이 다음 사용자 폰에 계속 간다.
 */
@Service
public class DeviceTokenService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final Clock clock;
    private final int stalenessDays;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository, Clock clock,
            @Value("${honjeong.push.staleness-days:60}") int stalenessDays) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.clock = clock;
        this.stalenessDays = stalenessDays;
    }

    /**
     * 토큰 등록(또는 주인·플랫폼 갱신).
     *
     * <p>DB의 원자적 UPSERT 한 방으로 처리한다 — 앱 시작 시 등록과 토큰 갱신이 거의 동시에 뜨는
     * 경합이 실재한다. 사유는 {@link DeviceTokenRepository#upsert} Javadoc 참조.
     *
     * <p><b>설치 ID가 오면 같은 기기의 옛 토큰을 먼저 지운다.</b> 로그아웃 때 FCM 폐기가 실패해
     * 주인 없이 남은 토큰은 기기에 값이 없어 다시는 지목할 수 없는데, 설치 ID로는 지목된다.
     * 순서가 중요하다 — 지우고 넣어야 방금 넣은 행을 자기가 지우는 일이 없다.
     *
     * <p>설치 ID가 <b>없으면 아무것도 지우지 않는다</b>. 서버가 앱보다 먼저 배포되므로 한동안
     * 설치 ID를 보내지 않는 구버전 앱이 계속 등록한다 — 그 등록은 기존 동작 그대로여야 한다.
     *
     * @param userId         토큰의 새 주인
     * @param token          FCM 등록 토큰
     * @param platform       기기 플랫폼
     * @param installationId 앱 설치 식별자(구버전 앱은 null)
     */
    @Transactional
    public void register(Long userId, String token, Platform platform, String installationId) {
        if (installationId != null) {
            deviceTokenRepository.deleteByInstallationIdAndTokenNot(installationId, token);
        }
        deviceTokenRepository.upsert(userId, token, platform.name(), now(), installationId);
    }

    /**
     * 토큰 해제(로그아웃).
     *
     * <p>내 토큰이 아니거나 이미 없으면 <b>조용히 넘어간다.</b> 이 호출이 실패하면
     * 앱의 로그아웃이 막히는데, 로그아웃을 막을 만한 이유가 아니다.
     *
     * @param userId 요청 사용자
     * @param token  해제할 토큰
     */
    @Transactional
    public void unregister(Long userId, String token) {
        deviceTokenRepository.findByToken(token)
                .filter(t -> t.getUser().getId().equals(userId))
                .ifPresent(t -> deviceTokenRepository.deleteByToken(token));
    }

    /**
     * staleness window를 벗어난 기기 토큰을 지운다(청소 스케줄러가 주기 호출).
     *
     * <p><b>무엇을 지우는가.</b> 로그아웃은 서버 삭제와 FCM 폐기 두 단계로 토큰을 끊는데, 둘 다
     * 실패하면 그 토큰은 FCM에 살아 있고 우리 DB에도 남아 있는데 기기에는 없다 — 다시는 지목해
     * 지울 수 없다. 그러면 그 폰을 넘겨받은 사람의 잠금화면에 이전 사용자의 알림이 계속 뜬다.
     * 앱은 시작할 때마다 재등록하므로, 재등록이 {@code stalenessDays}만큼 끊긴 행이 곧 그런 토큰이다.
     *
     * <p><b>왜 window가 짧으면 안 되는가.</b> 오래 앱을 안 연 휴면 사용자의 토큰까지 지우면
     * 푸시가 끊기고, 그러면 앱을 열 이유가 없어져 영영 돌아오지 않는다. Firebase가 window를
     * 넉넉히 잡으라고 하는 것이 이 때문이다.
     *
     * @return 삭제된 행 수
     */
    @Transactional
    public int sweepStale() {
        int deleted = deviceTokenRepository.deleteAllByLastRegisteredAtBefore(now().minusDays(stalenessDays));
        if (deleted > 0) {
            // 0건이면 남기지 않는다 — 24시간마다 도는 작업이라 대부분의 날에는 소음이 된다.
            log.info("[push] staleness 청소 — {}일 이상 재등록되지 않은 기기 토큰 {}건 삭제", stalenessDays, deleted);
        }
        return deleted;
    }

    /**
     * 지금 시각(KST).
     *
     * @return 전역 Clock을 한국 시간대로 읽은 현재 시각
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(KST));
    }
}
