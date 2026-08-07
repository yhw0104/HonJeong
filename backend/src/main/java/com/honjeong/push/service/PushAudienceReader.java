package com.honjeong.push.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.global.common.DisplayNames;
import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.user.repository.UserRepository;

/**
 * 발송 1단계 — <b>조회</b>. 누구에게 보낼지(토큰)와 배너에 뭐라고 쓸지(닉네임)를 읽는다.
 *
 * <p>사용처: {@link PushSendTask}.
 *
 * <p><b>왜 별도 빈인가.</b> 발송(FCM HTTP)이 DB 트랜잭션 안에서 일어나면 안 되기 때문에
 * 조회·발송·기록을 세 구간으로 나눴는데(스펙 §2 — 커넥션 점유), 같은 클래스 안에서 나누면
 * 자기호출이라 스프링 프록시를 타지 않아 {@code @Transactional}이 <b>아무 일도 하지 않는다</b>
 * ({@code @Async}가 별도 빈이어야 했던 것과 같은 이유). 그래서 클래스를 나눴다.
 *
 * <p>{@code readOnly = true}인 이유: 이 구간은 정말로 읽기만 한다. 쓰기는 3단계
 * ({@link PushDeliveryRecorder})가 자기 트랜잭션에서 한다.
 */
@Component
public class PushAudienceReader {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    public PushAudienceReader(DeviceTokenRepository deviceTokenRepository, UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * 발송 재료를 읽는다.
     *
     * <p>닉네임은 {@link DisplayNames}를 통과시킨다 — 탈퇴한 상대의 원래 닉네임이 배너에 뜨면
     * 07-30에 통일한 표시 규칙이 푸시에서만 깨진다.
     *
     * @param recipientId 받는 사람
     * @param actorId     상대 id(BADGE_EARNED는 null)
     * @return 토큰이 없으면 {@link PushAudience#EMPTY}, 있으면 토큰·닉네임이 채워진 값
     */
    @Transactional(readOnly = true)
    public PushAudience read(Long recipientId, Long actorId) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUser_Id(recipientId);
        if (tokens.isEmpty()) {
            return PushAudience.EMPTY; // 푸시 권한을 안 준 사용자 — 보낼 곳이 없다
        }
        String nickname = actorId == null ? null
                : userRepository.findById(actorId)
                        .map(u -> DisplayNames.nicknameOrUnknown(u.getNickname()))
                        .orElse(null);
        return new PushAudience(
                tokens.stream().map(t -> new PushAudience.TokenRef(t.getId(), t.getToken())).toList(),
                nickname);
    }
}
