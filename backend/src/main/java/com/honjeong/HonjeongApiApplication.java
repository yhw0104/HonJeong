package com.honjeong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 1. 기능: 혼정 백엔드(Spring Boot) 애플리케이션 진입점 — 컴포넌트 스캔·자동 구성을 켜고 서버를 기동한다
 * 2. 사용처: JVM 부트스트랩(./gradlew bootRun, 실행 JAR) — 코드에서 직접 참조하지 않음
 */
@SpringBootApplication
public class HonjeongApiApplication {

	/**
	 * 기능: 스프링 컨텍스트를 초기화하고 내장 웹 서버(:8080)를 기동한다
	 * Request: args — JVM 커맨드라인 인자(프로파일 등 스프링 부트 인자로 전달)
	 * Response: 없음(void)
	 */
	public static void main(String[] args) {
		SpringApplication.run(HonjeongApiApplication.class, args);
	}

}
