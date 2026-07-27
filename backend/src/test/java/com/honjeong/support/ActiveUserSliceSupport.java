package com.honjeong.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: {@code @WebMvcTest} 슬라이스에서 {@code ActiveUserFilter}가 요구하는 {@code UserRepository} 빈을
 *    공급하고, 기본적으로 모든 사용자를 ACTIVE로 스텁한다
 * 2. 사용처: SecurityConfig를 {@code @Import}해 실제 인가·필터 체인을 태우는 컨트롤러 슬라이스 테스트들
 *
 * <p><b>왜 필요한가.</b> {@code ActiveUserFilter}는 이제 ROLE_USER 토큰이 실린 모든 요청마다 DB에서
 * {@code users.status}를 조회한다. 그런데 {@code @WebMvcTest} 슬라이스는 JPA 리포지토리를 올리지 않으므로
 * 실제 {@code UserRepository}가 없다 — 이 필터는 컴포넌트 스캔으로 슬라이스에도 끼워지는 진짜 {@code Filter} 빈이라,
 * 슬라이스가 뜨려면 어떤 형태로든 {@code UserRepository} 빈이 있어야 한다.
 *
 * <p><b>필터는 그대로 살아 있다.</b> 이 클래스는 DB 조회만 가짜로 채울 뿐, {@code ActiveUserFilter} 자체를 끄거나
 * 우회하지 않는다 — 그 필터가 실제로 요청을 통과/차단시키는 동작은 각 슬라이스 테스트가 검증하려는 진짜 보안 동작의
 * 일부이므로 그대로 두고, 여기서는 단지 "이 사용자는 ACTIVE"라는 DB 응답만 흉내 낸다.
 *
 * <p>개별 테스트가 SUSPENDED/WITHDRAWN 등 다른 상태를 검증하고 싶으면, 그 테스트 안에서
 * {@code when(userRepository.findStatusById(특정id)).thenReturn(...)}로 이 기본 스텁을 덮어쓰면 된다
 * (Mockito는 나중에 스텁한 조건이 우선한다).
 */
public abstract class ActiveUserSliceSupport {

    @MockitoBean
    protected UserRepository userRepository;

    /** 슬라이스의 기존 시나리오(유효한 access 토큰 → 200)가 그대로 통과하도록 기본값을 ACTIVE로 스텁한다. */
    @BeforeEach
    void mockActiveUserStatus() {
        when(userRepository.findStatusById(any())).thenReturn(Optional.of(UserStatus.ACTIVE));
    }
}
