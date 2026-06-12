package com.honjeong.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 응답을 감싸는 공통 응답 엔벨로프. 성공: {@code {success:true, data:...}}, 실패: {@code {success:false, error:{code,message}}}.
 * 직접 생성하지 않고 정적 팩토리({@link #success}/{@link #error})로 만든다.
 * {@code @JsonInclude(NON_NULL)} 덕분에 값이 null인 필드는 JSON 직렬화에서 빠지므로,
 * 성공 응답엔 {@code error} 키가, 실패 응답엔 {@code data} 키가 노출되지 않는다.
 *
 * @param <T> 성공 시 data에 담기는 본문 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    /**
     * 성공 응답을 만든다. {@code success=true}, 본문은 {@code data}, {@code error}는 null이라 직렬화에서 빠진다.
     *
     * @param data 응답 본문(없을 수 있어 null 허용)
     * @param <T> 본문 타입
     * @return success=true 형태의 엔벨로프
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 실패 응답을 만든다. {@code success=false}, {@code data}는 null이라 직렬화에서 빠지고 {@code error}에 코드·메시지를 담는다.
     *
     * @param code 에러 식별 코드(보통 {@link com.honjeong.global.exception.ErrorCode#code()})
     * @param message 클라이언트에 보여줄 에러 메시지
     * @return success=false 형태의 엔벨로프
     */
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }

    /** 실패 응답의 {@code error} 객체 — 에러 코드와 메시지를 담는 중첩 레코드. */
    public record ApiError(String code, String message) {
    }
}
