package com.honjeong.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * 배포 프로파일에서 소셜 토큰 검증이 실제로 켜져 있는지 부팅 때 확인한다. 꺼져 있으면 기동을
 * 실패시킨다(fail-closed).
 *
 * <p><b>무엇을 막는가.</b> {@code honjeong.oauth.mode=mock}이면 {@code MockOAuthVerifier}가 대표
 * 검증기가 된다. 이 구현은 카카오·애플이 서명한 ID 토큰을 검증하지 않고 <b>받은 문자열을 그대로
 * 소셜 식별자로 신뢰</b>한다. 즉 배포 서버가 이 모드로 떠 있으면 누구든 임의 문자열을
 * {@code POST /api/auth/oauth/{provider}}에 넣어 계정을 만들 수 있고, 남의 소셜 식별자를 알면
 * 그 계정으로 그대로 들어갈 수 있다. 이 프로젝트에서 결과가 가장 큰 설정값 하나다.
 *
 * <p><b>왜 지금 필요한가.</b> 2026-07-27에 이 상태가 실제로 열려 있었다. prod yml은 그때도
 * {@code ${OAUTH_MODE:real}}로 real을 기본값으로 선언하고 있었지만, {@code docker-compose.yml}이
 * {@code OAUTH_MODE:-mock}으로 그 선언을 덮어써 서버가 조용히 mock으로 떴다. 그래서 <b>yml의
 * 기본값을 지키는 것만으로는 부족하다</b> — 환경변수 한 줄이 그 위에 얹히고, 얹혔다는 신호는
 * 아무 데도 남지 않는다(오류도 없고 요청도 전부 200이다). 지금 그 자리는 compose 파일의 주석이
 * 지키고 있는데, 주석은 부팅을 막지 못한다. 여기서 값 자체를 확인한다.
 *
 * <p>{@code AppleModeConsistencyCheck}가 이 검사의 짝이다 — 그쪽은 "oauth는 real인데 apple만
 * mock"인 어긋난 조합을 막고, 여기는 그 전제(oauth=real) 자체를 세운다. 둘 다 mock이면 이 검사가
 * 먼저 걸린다(그쪽은 {@code @ConditionalOnProperty}로 oauth=real일 때만 등록된다). 운영자가 먼저
 * 봐야 할 메시지가 더 심각한 쪽이라 순서가 맞다.
 *
 * <p>검사 대상 프로파일과 그 이유는 {@link DeployedProfiles}에 있다.
 */
@Component
@Profile(DeployedProfiles.EXPRESSION)
public class OAuthRealModeCheck {

    /**
     * @param oauthMode {@code honjeong.oauth.mode} — 기본값을 mock으로 둔다(application.yml의 기본과
     *        같은 값). 설정이 통째로 빠진 새 환경도 "검증 없음"으로 뜨지 못하게 하기 위함이다.
     * @throws IllegalArgumentException real이 아닐 때
     */
    public OAuthRealModeCheck(@Value("${honjeong.oauth.mode:mock}") String oauthMode) {
        Assert.isTrue("real".equals(oauthMode),
                "honjeong.oauth.mode=" + oauthMode + "입니다. 배포 프로파일에서는 real이어야 합니다. "
                        + "이 값이 real이 아니면 서버는 카카오·애플이 서명한 ID 토큰을 검증하지 않고 "
                        + "받은 문자열을 그대로 신뢰합니다 — 아무나 임의 문자열로 계정을 만들거나 "
                        + "남의 소셜 식별자로 그 계정에 들어올 수 있습니다(2026-07-27에 실제로 열려 "
                        + "있던 상태입니다). OAUTH_MODE=real로 두고 KAKAO_APP_KEY를 채워주세요. "
                        + "카카오 키가 없어 기동이 실패하더라도 OAUTH_MODE=mock으로 되돌리면 안 됩니다"
                        + "(그게 바로 이 가드가 막는 상태입니다). "
                        + "이 검사는 local·test를 제외한 모든 프로파일에서 돕니다 — 배포 환경이라면 "
                        + "카카오 키를 채우는 것 말고 다른 답이 없습니다. 노트북에서 카카오 키 없이 "
                        + "서버만 띄워 화면을 보려는 경우에만 local 프로파일(./gradlew bootRun)을 쓰세요.");
    }
}
