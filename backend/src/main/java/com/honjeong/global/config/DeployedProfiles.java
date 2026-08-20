package com.honjeong.global.config;

/**
 * 부팅 가드가 도는 프로파일 집합. {@code @Profile(DeployedProfiles.EXPRESSION)}으로 붙인다.
 *
 * <p><b>왜 이름을 나열하지 않는가.</b> {@code @Profile("prod")}처럼 대상을 나열하면 <b>허용이
 * 기본</b>이 된다 — 나중에 staging·canary 같은 프로파일이 생기면 그 환경은 아무도 손대지 않아도
 * 검사에서 빠지고, 빠졌다는 사실은 어디에도 드러나지 않는다. 이 가드들이 막는 것(검증 없는 소셜
 * 로그인, 심사 지침 5.1.1(v) 위반)은 <b>드러나지 않는 것이 본질</b>인 결함이라, 조용히 빠지는
 * 형태를 쓸 수 없다.
 *
 * <p>그래서 반대로 적는다 — <b>거부가 기본</b>이고, 검사를 면제받는 프로파일만 여기 이름을 올린다.
 * 새 프로파일은 자동으로 검사 대상이 된다.
 *
 * <p><b>면제 사유.</b>
 * <ul>
 *   <li>{@code local} — 노트북. 카카오 로그인만 실서버로 검증하려고 {@code OAUTH_MODE=real}만 켜는
 *       워크플로가 있는데(VS Code 실행 구성), 여기서 검사하면 카카오만 보려는 개발자에게 운영 애플
 *       서명키(.p8)를 노트북에 두라고 요구하게 된다. 로컬 DB의 계정은 심사 대상도 아니다.</li>
 *   <li>{@code test} — 자동 테스트. 외부 연동은 전부 대역이고, 컨텍스트를 띄우는 모든 테스트가
 *       자격증명을 갖춰야 하는 상황이 된다.</li>
 * </ul>
 *
 * <p>로컬 풀스택({@code docker compose up -d})은 면제가 아니다 — compose가
 * {@code SPRING_PROFILES_ACTIVE=prod}로 띄우기 때문이다. 의도한 대로다: 그 경로는 진짜 서버와
 * 같은 설정으로 뜨는지 확인하는 용도다.
 */
public final class DeployedProfiles {

    /**
     * 실제 사용자 데이터를 다루는 모든 프로파일. 스프링 프로파일 표현식이라 컴파일 상수여야 한다
     * ({@code @Profile}의 인자로 쓰인다).
     */
    public static final String EXPRESSION = "!local & !test";

    private DeployedProfiles() {
    }
}
