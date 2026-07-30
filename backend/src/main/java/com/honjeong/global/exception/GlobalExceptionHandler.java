package com.honjeong.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.honjeong.global.common.ApiResponse;

/**
 * 컨트롤러 전역에서 발생한 예외를 가로채 공통 응답 엔벨로프({@code {success:false, error:{code,message}}})로
 * 변환하는 advice. 각 핸들러가 예외 종류별로 HTTP 상태·코드·메시지를 결정한다.
 *
 * <p>사용처: 스프링 MVC가 전 컨트롤러에 자동 적용한다(직접 참조 없음). 필터 단계의 401/403은
 * SecurityConfig가 별도로 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 규칙 위반 예외를 처리한다.
     *
     * <p>예외가 들고 있는 {@link ErrorCode}로 HTTP 상태와 코드를, 예외 메시지로 본문 메시지를 채운다.
     *
     * @param ex 서비스 계층에서 던진 비즈니스 예외(ErrorCode·메시지 보유)
     * @return ErrorCode의 상태 + {success:false, error:{code, message}} 엔벨로프
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity.status(ec.status()).body(ApiResponse.error(ec.code(), ex.getMessage()));
    }

    /**
     * {@code @Valid} 요청 본문 검증 실패를 400 INVALID_INPUT으로 변환한다.
     *
     * <p>첫 번째 필드 에러 메시지를 노출하고, 필드 에러가 없으면 기본 메시지를 사용한다.
     *
     * @param ex 바인딩/검증 실패 예외
     * @return 400 상태 + INVALID_INPUT 코드 엔벨로프
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // 검증 실패한 필드 중 첫 번째의 메시지를 꺼낸다. 없으면(이론상) 공통 기본 메시지로 대체.
        var fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : ErrorCode.INVALID_INPUT.message();
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.code(), message));
    }

    /**
     * 필수 {@code @RequestParam} 누락을 400 INVALID_INPUT으로 변환한다(예: nickname-check의 nickname 누락).
     *
     * @param ex 누락된 파라미터 정보를 담은 예외
     * @return 400 상태 + {@code "<파라미터명> 파라미터가 필요합니다."} 메시지 엔벨로프
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.code(), ex.getParameterName() + " 파라미터가 필요합니다."));
    }

    /**
     * 위에서 잡지 못한 모든 예외를 최종적으로 받아 500 INTERNAL_ERROR로 변환한다(최후의 안전망).
     *
     * <p>내부 예외 메시지는 노출하지 않고 ErrorCode의 일반 메시지만 내려준다.
     *
     * @param ex 처리되지 않은 임의의 예외
     * @return 500 상태 + INTERNAL_ERROR 엔벨로프
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message()));
    }
}
