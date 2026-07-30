package com.honjeong.global.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.honjeong.global.exception.ErrorCode;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 보안 필터 단계(컨트롤러 진입 전)의 에러를 공통 응답 엔벨로프 JSON으로 직접 직렬화한다.
 *
 * <p>사용처: SecurityConfig(401/403 핸들러), ActiveUserFilter.
 *
 * <p>이 시점엔 {@code GlobalExceptionHandler}(@RestControllerAdvice)가 동작하지 않으므로 문자열로 직접 만든다.
 */
public final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    /**
     * 주어진 에러 코드로 응답의 상태와 본문을 채운다.
     *
     * @param response 응답 객체
     * @param code 내려줄 에러 코드
     * @throws IOException 응답 본문 쓰기에 실패했을 때
     */
    public static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        HttpStatus status = code.status();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"" + code.code() + "\",\"message\":\"" + code.message() + "\"}}");
    }
}
