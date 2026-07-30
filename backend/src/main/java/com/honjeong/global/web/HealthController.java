package com.honjeong.global.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 생존 확인용 헬스 체크 컨트롤러 — 프로젝트 세팅이 정상 동작하는지 확인하는 최소 엔드포인트.
 *
 * <p>기본 경로: /api
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 서버가 살아 있는지 확인하는 용도로 항상 고정 응답을 반환한다.
     *
     * <p>사용 화면: 앱에서는 쓰지 않는다(백엔드 내부용) — 배포·로컬 기동 확인(curl)과 인프라 헬스체크.
     *
     * <p><b>인증:</b> 공개 경로(permitAll)라 토큰이 필요 없다. 공통 {@code ApiResponse} 봉투도 쓰지 않는다.
     *
     * @return {@code {"status":"UP"}} 형태의 맵
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
