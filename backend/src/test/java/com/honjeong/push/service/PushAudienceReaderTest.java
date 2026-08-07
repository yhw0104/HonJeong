package com.honjeong.push.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.global.common.DisplayNames;
import com.honjeong.push.domain.DeviceToken;
import com.honjeong.push.repository.DeviceTokenRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * PushAudienceReader 단위 테스트 — 발송 1단계(조회)가 뽑아 오는 값을 본다.
 *
 * <p>가장 중요한 단언은 <b>닉네임이 {@link DisplayNames}를 통과하는가</b>다. 통과하지 않으면
 * 탈퇴한 상대의 원래 닉네임이 <b>잠금화면 배너에</b> 그대로 뜬다 — 07-30에 리뷰·대화·신청이력·
 * 알림 네 곳에서 통일한 표시 규칙이 푸시에서만 깨지는 것이고, 화면과 달리 배너는 되돌릴 수 없다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PushAudienceReader")
class PushAudienceReaderTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private PushAudienceReader reader;

    @Test
    @DisplayName("토큰이 0건이면 EMPTY를 주고 사용자 조회조차 하지 않는다")
    void 토큰이_없으면_빈_값() {
        given(deviceTokenRepository.findAllByUser_Id(7L)).willReturn(List.of());

        assertThat(reader.read(7L, 9L).isEmpty()).isTrue();
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("토큰의 id와 값을 함께 싣는다 — id는 기록 구간이 재조회에 쓴다")
    void 토큰의_id와_값을_싣는다() {
        DeviceToken token = deviceToken(3L, "tok-a");
        User actor = user("김하늘");
        given(deviceTokenRepository.findAllByUser_Id(7L)).willReturn(List.of(token));
        given(userRepository.findById(9L)).willReturn(Optional.of(actor));

        PushAudience audience = reader.read(7L, 9L);

        assertThat(audience.tokens()).containsExactly(new PushAudience.TokenRef(3L, "tok-a"));
        assertThat(audience.tokenValues()).containsExactly("tok-a");
    }

    @Test
    @DisplayName("탈퇴해 닉네임이 없는 상대는 '알 수 없음'으로 바꿔 싣는다 — 배너에 원래 닉네임이 뜨면 안 된다")
    void 탈퇴자는_알_수_없음() {
        DeviceToken token = deviceToken(3L, "tok-a");
        User withdrawn = user(null);
        given(deviceTokenRepository.findAllByUser_Id(7L)).willReturn(List.of(token));
        given(userRepository.findById(9L)).willReturn(Optional.of(withdrawn));

        assertThat(reader.read(7L, 9L).actorNickname()).isEqualTo(DisplayNames.UNKNOWN);
    }

    @Test
    @DisplayName("상대가 없으면(뱃지) 닉네임을 조회하지 않는다")
    void 뱃지는_상대_조회를_건너뛴다() {
        DeviceToken token = deviceToken(3L, "tok-a");
        given(deviceTokenRepository.findAllByUser_Id(7L)).willReturn(List.of(token));

        assertThat(reader.read(7L, null).actorNickname()).isNull();
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("상대를 못 찾아도 터지지 않는다 — 닉네임 없이 보낸다('누군가'로 표시된다)")
    void 상대를_못_찾으면_닉네임_없음() {
        DeviceToken token = deviceToken(3L, "tok-a");
        given(deviceTokenRepository.findAllByUser_Id(7L)).willReturn(List.of(token));
        given(userRepository.findById(9L)).willReturn(Optional.empty());

        assertThat(reader.read(7L, 9L).actorNickname()).isNull();
    }

    /**
     * id가 채워진 DeviceToken 목.
     *
     * <p>{@code DeviceToken.of}는 저장 전 객체라 id가 null이다. 여기서 검증하려는 것이
     * "id를 실어 보내는가"이므로 목으로 id를 만들어 준다.
     *
     * <p><b>given(...) 안에서 부르지 말 것</b> — 스터빙 안에서 또 스터빙하면 Mockito가
     * 미완성 스터빙(UnfinishedStubbingException)으로 본다. 먼저 지역 변수로 받아 둔다.
     *
     * @param id    device_tokens.id
     * @param value FCM 등록 토큰
     * @return 그 값을 돌려주는 목
     */
    private static DeviceToken deviceToken(Long id, String value) {
        DeviceToken token = mock(DeviceToken.class);
        given(token.getId()).willReturn(id);
        given(token.getToken()).willReturn(value);
        return token;
    }

    private static User user(String nickname) {
        User user = mock(User.class);
        given(user.getNickname()).willReturn(nickname);
        return user;
    }
}
