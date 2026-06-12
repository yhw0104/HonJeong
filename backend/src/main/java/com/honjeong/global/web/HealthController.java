package com.honjeong.global.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 세팅이 정상 동작하는지 확인하기 위한 최소 헬스 체크 엔드포인트.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 헬스 체크 엔드포인트({@code GET /api/health}). 보안상 공개 경로(permitAll)다.
     * 서버가 살아 있는지 확인하는 용도로 항상 고정 응답을 반환한다.
     *
     * @return {@code {"status":"UP"}} 형태의 맵
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
