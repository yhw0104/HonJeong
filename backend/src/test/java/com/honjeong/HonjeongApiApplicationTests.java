package com.honjeong;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.honjeong.support.AbstractPostgresTest;

/**
 * 부트스트랩 스모크 테스트 — Testcontainers PostgreSQL 위에서 Flyway(V1) 적용 후
 * 전체 스프링 컨텍스트가 정상 기동하는지 검증한다(Slice 0 게이트).
 */
@SpringBootTest
class HonjeongApiApplicationTests extends AbstractPostgresTest {

	// 컨텍스트가 끝까지 로딩되면(예외 없이) 통과하는 스모크 테스트 — 본문이 비어 있어도 기동 자체가 검증 대상이다.
	@Test
	void contextLoads() {
	}

}
