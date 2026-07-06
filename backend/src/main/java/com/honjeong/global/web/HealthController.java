package com.honjeong.global.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 생존 확인용 헬스 체크 컨트롤러.
 *
 * <p>기본 경로: /api
 *
 * <p>[기존 주석] 프로젝트 세팅이 정상 동작하는지 확인하기 위한 최소 헬스 체크 엔드포인트.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 1. API 주소: GET /api/health
     * 2. 사용 화면: (앱 미사용 — 백엔드 내부용) 배포·로컬 기동 확인(curl)·인프라 헬스체크
     * 3. Request: 없음 (공개 경로 — 토큰 불필요, permitAll)
     * 4. Response: Map&lt;String,String&gt; — 항상 {"status":"UP"} 고정 응답 (ApiResponse 봉투 미사용)
     *
     * <p>[기존 주석] 헬스 체크 엔드포인트({@code GET /api/health}). 보안상 공개 경로(permitAll)다.
     * 서버가 살아 있는지 확인하는 용도로 항상 고정 응답을 반환한다.
     *
     * @return {@code {"status":"UP"}} 형태의 맵
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
