package com.honjeong.global.security;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 정식 회원(ROLE_USER) 토큰으로 들어온 요청마다 users.status가 ACTIVE인지 확인해, 아니면 401로 끊는다.
 *
 * <p>사용처: SecurityConfig 필터 체인(BearerTokenAuthenticationFilter 뒤).
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

    private static final Logger log = LoggerFactory.getLogger(ActiveUserFilter.class);

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
            String subject = ((Jwt) auth.getPrincipal()).getSubject();
            UserStatus status = resolveStatus(subject);
            if (status != UserStatus.ACTIVE) {
                if (status != null) {
                    // 상태 조회는 정상적으로 됐지만 ACTIVE가 아님(정지·탈퇴·온보딩 중 남은 토큰) — 흔히 벌어지는
                    // 정상적인 401 사유라 DEBUG로만 남긴다. 토큰 원문은 남기지 않는다.
                    log.debug("비활성 계정 접근 차단: userId={}, status={}", subject, status);
                }
                SecurityContextHolder.clearContext();
                SecurityErrorWriter.write(response, ErrorCode.ACCOUNT_INACTIVE);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * JWT sub(문자열)를 userId로 파싱해 상태를 조회한다 — sub이 숫자가 아니거나 그 id의 회원 행이 없으면
     * 예외를 던지지 않고 "확인 불가"(null)로 취급해 fail-closed 처리한다.
     *
     * <p>{@link JwtProvider}가 만드는 access 토큰의 sub은 항상 {@code String.valueOf(long)}이고 그 id는
     * 정상적으로 존재하는 회원이라 두 실패 경로 모두 정상 경로로는 도달하지 않는다. 그럼에도 보안 필터는
     * 입력을 신뢰하지 않고 실패 시 열어주는 대신 닫아야 한다({@code NumberFormatException}이 그대로
     * 새어나가면 500으로 응답해 인증 실패가 인증 우회처럼 보이는 스택트레이스를 노출하게 된다).
     *
     * <p>두 실패 경로는 로그 레벨을 다르게 남긴다 — 둘 다 "일상적인 정지/탈퇴"와는 성격이 다르고(서명은
     * 유효한 요청인데 우리 쪽 데이터/토큰 발급 경로가 어긋난 정황), 서명 키 로테이션이나 상태 조회 자체가
     * 깨지는 사고가 나면 사용자 전원이 이 분기로 몰려 로그로만 원인을 구분할 수 있기 때문이다.
     *
     * @param subject JWT의 sub 클레임(정상적으로는 userId 문자열)
     * @return 해당 userId의 상태(없거나 sub이 숫자가 아니면 null)
     */
    private UserStatus resolveStatus(String subject) {
        Long userId;
        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException e) {
            // sub이 숫자가 아님 — 정상 발급 경로로는 나오지 않는 값이라 위조·손상된 토큰일 가능성이 있다.
            // 토큰 원문은 로깅하지 않는다.
            log.warn("JWT sub이 숫자가 아니라 상태 확인 불가(위조 또는 손상된 토큰일 수 있음)");
            return null;
        }
        Optional<UserStatus> found = userRepository.findStatusById(userId);
        if (found.isEmpty()) {
            // 서명은 유효한데 그 userId의 회원 행이 없음 — 정상 흐름에서는 발생하지 않는다. 이게 대량으로
            // 찍히면 정지 물결이 아니라 상태 조회 자체가 깨진 사고(마이그레이션·서명 키 로테이션 등)다.
            log.warn("userId={}에 해당하는 사용자 행이 없어 상태 확인 불가", userId);
            return null;
        }
        return found.get();
    }
}
