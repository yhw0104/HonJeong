package com.honjeong.push.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    private final DeviceTokenRepository deviceTokenRepository;
    private final Clock clock;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository, Clock clock) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.clock = clock;
    }

    /**
     * 토큰 등록(또는 주인·플랫폼 갱신).
     *
     * <p>DB의 원자적 UPSERT 한 방으로 처리한다 — 앱 시작 시 등록과 토큰 갱신이 거의 동시에 뜨는
     * 경합이 실재한다. 사유는 {@link DeviceTokenRepository#upsert} Javadoc 참조.
     *
     * @param userId   토큰의 새 주인
     * @param token    FCM 등록 토큰
     * @param platform 기기 플랫폼
     */
    @Transactional
    public void register(Long userId, String token, Platform platform) {
        deviceTokenRepository.upsert(userId, token, platform.name(), now());
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
     * 지금 시각(KST).
     *
     * @return 전역 Clock을 한국 시간대로 읽은 현재 시각
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(KST));
    }
}
