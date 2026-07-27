package com.honjeong.global.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.honjeong.global.exception.ErrorCode;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 1. 기능: 정식 회원(ROLE_USER) 토큰으로 들어온 요청마다 users.status가 ACTIVE인지 확인해, 아니면 401로 끊는다
 * 2. 사용처: SecurityConfig 필터 체인(BearerTokenAuthenticationFilter 뒤)
 *
 * <p><b>왜 필요한가.</b> JWT는 무상태라 발급된 access 토큰(TTL 1시간)을 서버가 회수할 수 없다. 이 필터가 없으면
 * 탈퇴 직후에도 남은 토큰으로 최대 1시간 API를 쓸 수 있고, 정지(SUSPENDED) 제재도 이미 로그인한 사용자에게
 * 아무 효력이 없다.
 *
 * <p><b>왜 인자 리졸버가 아니라 필터인가.</b> {@code @CurrentUserId}를 쓰지 않는 핸들러(파일 업로드·역지오코딩·
 * 장소 검색·공지 등)가 있어 리졸버에 넣으면 구멍이 남는다.
 *
 * <p>온보딩 토큰(ROLE_ONBOARDING)은 검사하지 않는다 — 그 단계의 회원은 PENDING이 정상이다.
 */
@Component
public class ActiveUserFilter extends OncePerRequestFilter {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;

    public ActiveUserFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isUserToken = auth != null
                && auth.getPrincipal() instanceof Jwt
                && auth.getAuthorities().stream().anyMatch(a -> ROLE_USER.equals(a.getAuthority()));
        if (isUserToken) {
            UserStatus status = resolveStatus(((Jwt) auth.getPrincipal()).getSubject());
            if (status != UserStatus.ACTIVE) {
                SecurityContextHolder.clearContext();
                SecurityErrorWriter.write(response, ErrorCode.ACCOUNT_INACTIVE);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 기능: JWT sub(문자열)를 userId로 파싱해 상태를 조회 — sub이 숫자가 아니면(위조·손상 등) 예외를 던지지 않고
     * "확인 불가"로 취급해 fail-closed 처리한다
     *
     * <p>{@link JwtProvider}가 만드는 access 토큰의 sub은 항상 {@code String.valueOf(long)}이라 정상 경로로는
     * 도달하지 않지만, 보안 필터는 입력을 신뢰하지 않고 실패 시 열어주는 대신 닫아야 한다({@code NumberFormatException}이
     * 그대로 새어나가면 500으로 응답해 인증 실패가 인증 우회처럼 보이는 스택트레이스를 노출하게 된다).
     *
     * @param subject JWT의 sub 클레임(정상적으로는 userId 문자열)
     * @return 해당 userId의 상태(없거나 sub이 숫자가 아니면 null)
     */
    private UserStatus resolveStatus(String subject) {
        try {
            return userRepository.findStatusById(Long.valueOf(subject)).orElse(null);
        } catch (NumberFormatException e) {
            return null; // 숫자가 아닌 sub은 "상태 확인 불가"로 취급 — 아래에서 ACTIVE가 아니므로 401
        }
    }
}
