package com.honjeong.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 모든 통합/슬라이스 테스트의 공통 베이스.
 *
 * <p>JVM 전역에서 단 하나의 PostgreSQL 컨테이너를 공유한다(static 블록에서 한 번만 start).
 * {@code @ServiceConnection}이 컨테이너의 datasource(URL/계정)를 테스트 컨텍스트에 자동 주입하므로
 * {@code application-test.yml}에는 datasource를 지정하지 않는다. 스키마는 Flyway가 적용한다.
 *
 * <p>운영과 동일한 Postgres를 쓰는 이유: 단일 활성 체크인의 <b>부분 유니크 인덱스</b>
 * ({@code check_ins(user_id) WHERE status='ACTIVE'})는 H2에서 검증할 수 없기 때문이다.
 */
@ActiveProfiles("test")
public abstract class AbstractPostgresTest {

    // JVM당 하나만 띄워 모든 테스트가 공유하는 Postgres 컨테이너. @ServiceConnection이 이 컨테이너의 datasource를 자동 주입한다.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));

    // 클래스 로딩 시 한 번만 컨테이너를 기동한다(테스트마다 재시작하지 않아 비용 절감).
    static {
        POSTGRES.start();
    }
}
