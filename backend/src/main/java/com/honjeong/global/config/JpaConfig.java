package com.honjeong.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 감사(Auditing) 기능을 켜는 설정 클래스.
 * {@code @EnableJpaAuditing}이 있어야 {@code @CreatedDate}/{@code @LastModifiedDate}가 동작하므로,
 * {@link com.honjeong.global.common.BaseTimeEntity}의 created_at·updated_at 자동 채움이 가능해진다.
 * 이 어노테이션이 없으면 감사 컬럼이 null로 남는다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
