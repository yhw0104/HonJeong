package com.honjeong.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 애플 로그인의 두 반쪽이 같은 모드인지 부팅 때 확인한다. 어긋나면 기동을 실패시킨다(fail-closed).
 *
 * <p><b>왜 필요한가.</b> 애플 기능 하나가 서로 무관한 두 설정으로 갈려 있다.
 * {@code honjeong.oauth.mode}는 로그인 쪽(ID 토큰 검증 —
 * {@code AppleOAuthVerifier}·{@code DelegatingOAuthVerifier})을, {@code honjeong.apple.mode}는
 * 토큰 쪽(교환·폐기 — {@code RealAppleTokenClient}/{@code NoopAppleTokenClient})을 켠다. 그래서
 * <b>{@code oauth.mode=real} + {@code apple.mode=mock}</b>이라는 조합이 아무 저항 없이 만들어진다.
 * 그 조합에서 서버는 이렇게 동작한다:
 *
 * <ul>
 *   <li>애플 로그인은 완벽하게 성공한다(검증기는 real이다).</li>
 *   <li>가입은 매번 {@code apple_refresh_token = null}로 저장된다(Noop이 늘 null을 준다).</li>
 *   <li>탈퇴는 폐기할 토큰이 없어 애플에 아무 것도 보내지 않는다.</li>
 *   <li>에러도, 실패한 요청도 없다 — 서버는 완전히 건강해 보인다.</li>
 * </ul>
 *
 * <p>즉 App Store 심사 지침 5.1.1(v)(계정 삭제를 제공하는 앱은 애플 토큰도 폐기해야 한다)를
 * 위반하는 상태가 <b>버그가 아니라 설정값 하나로</b> 만들어지고, 아무 신호도 남기지 않는다. 게다가
 * 이 조합은 자격증명이 없어 기동이 실패할 때 가장 먼저 손이 가는 회피책이다 — 그 회피가 통하면
 * {@code RealAppleTokenClient}의 fail-closed 가드가 통째로 무력해진다. 그래서 여기서 막는다.
 *
 * <p>이름을 바꿔 두 설정을 한 이름으로 합치는 방법도 있지만, 네 파일과 yml을 건드리면서 동작은
 * 하나도 달라지지 않는다. 대신 "어긋난 조합만 부팅에서 거부"한다.
 *
 * <p>반대 조합({@code oauth.mode=mock} + {@code apple.mode=real})은 막지 않는다 — mock 검증기로는
 * 애플 계정이 만들어지지 않아 폐기 의무 자체가 생기지 않고, 토큰 클라이언트만 실연동으로 두고
 * 확인하고 싶은 개발 상황이 있다.
 *
 * <p>★<b>{@code prod} 프로파일에서만 검사한다</b>({@code WebSocketConfig}의 {@code @Profile("!test")}와
 * 같은 종류의 프로파일 절단). 이 가드가 막으려는 <b>규정 위반은 운영에서만 발생한다</b> — 심사에
 * 걸리는 계정도, 5.1.1(v)가 말하는 "사용자의 애플 토큰"도 운영 서버에만 있다. 반면 로컬은 카카오
 * 로그인을 실서버로 검증하려고 {@code OAUTH_MODE=real}만 켜는 워크플로가 이미 있고(VS Code 실행
 * 구성), 전 프로파일에서 검사하면 <b>카카오만 보려는 개발자에게 운영 애플 서명키(.p8)를 노트북에
 * 두라고 요구하게 된다</b>. 아무 이득 없이 비밀만 퍼뜨리는 거래라 받지 않는다.
 *
 * <p>로컬 풀스택 경로({@code docker compose up -d})는 그대로 검사 대상이다 — compose가
 * {@code SPRING_PROFILES_ACTIVE=prod}로 띄우기 때문이다. 다만 그 경로는 이 가드 이전에도 이미
 * 실자격증명 없이는 뜨지 않았다: prod 기본값이 {@code push.mode=real}(FCM 자격증명 없으면 기동
 * 실패)이고 {@code apple.mode=real}(APPLE_* 없으면 {@code AppleClientSecretFactory}에서 기동 실패)이다.
 * 그래서 이 가드가 compose 사용자에게 새로 요구하는 것은 없고, 오직 <b>"APPLE_MODE=mock으로
 * 되돌려 그 실패를 우회하는 길"</b>만 닫는다.
 */
@Component
@Profile("prod")
@ConditionalOnProperty(name = "honjeong.oauth.mode", havingValue = "real")
public class AppleModeConsistencyCheck {

    /**
     * @param appleMode {@code honjeong.apple.mode} — 기본값을 mock으로 둔다(application.yml의 기본과
     *        같은 값). 설정이 아예 없는 상황도 "어긋난 조합"으로 잡히도록 하기 위함이다.
     * @throws IllegalArgumentException {@code honjeong.apple.mode}가 real이 아닐 때
     */
    public AppleModeConsistencyCheck(@Value("${honjeong.apple.mode:mock}") String appleMode) {
        Assert.isTrue("real".equals(appleMode),
                "honjeong.oauth.mode=real인데 honjeong.apple.mode=" + appleMode + "입니다. "
                        + "이 조합에서는 애플 로그인만 정상 동작하고, 가입은 매번 apple_refresh_token 없이 "
                        + "저장되며, 탈퇴해도 애플에 폐기를 보내지 않습니다 — 오류도 실패 요청도 없이 "
                        + "App Store 심사 지침 5.1.1(v)를 위반하는 상태가 됩니다. "
                        + "APPLE_MODE=real로 두고 APPLE_CLIENT_ID·APPLE_TEAM_ID·APPLE_KEY_ID·"
                        + "APPLE_PRIVATE_KEY_BASE64를 채워주세요. "
                        + "자격증명이 없어 기동이 실패하더라도 APPLE_MODE=mock으로 되돌리면 안 됩니다"
                        + "(그게 바로 이 가드가 막는 상태입니다). ★OAUTH_MODE=mock으로 내리는 것도 "
                        + "답이 아닙니다 — 운영에서 그 값은 소셜 토큰을 검증하지 않는다는 뜻이고, "
                        + "임의 문자열로 계정을 만들 수 있게 됩니다(2026-07-27에 실제로 있었던 우회). "
                        + "이 검사는 prod 프로파일에서만 돕니다. 애플 자격증명 없이 카카오만 실검증하려면 "
                        + "prod가 아닌 프로파일(local)로 띄우세요.");
    }
}
