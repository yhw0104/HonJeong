package com.honjeong.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
     * 잘못된 요청이 500으로 새어 나가는 것을 막는다 — 클라이언트 잘못을 4xx로 되돌린다.
     *
     * <p><b>왜 필요한가.</b> 아래 {@link #handleUnexpected}가 {@code Exception}을 전부 받는 최후 안전망이라,
     * 스프링이 "요청이 잘못됐다"고 알려주는 예외들까지 그리로 빨려들어가 500으로 나갔다. 그러면 서버는
     * 멀쩡한데 서버 잘못이라고 응답하게 되고, 배포 후 500 비율로 건강도를 볼 때 URL 오타 같은 것이
     * 진짜 장애와 섞인다. 스프링은 더 구체적인 핸들러를 우선 적용하므로 이 메서드가 먼저 걸린다.
     *
     * <p>다루는 예외:
     * <ul>
     *   <li>{@link MethodArgumentTypeMismatchException} — {@code /conversations/abc}처럼 경로 변수 타입 불일치</li>
     *   <li>{@link HttpMessageNotReadableException} — 본문이 깨진 JSON이거나 아예 없음</li>
     * </ul>
     *
     * @param ex 요청이 잘못됐음을 나타내는 스프링 예외
     * @return 400 상태 + INVALID_INPUT 코드 엔벨로프
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        // 내부 예외 메시지에는 파라미터 타입·파서 위치 등 구현 세부가 섞여 있어 노출하지 않는다.
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.code(), ErrorCode.INVALID_INPUT.message()));
    }

    /**
     * 경로는 존재하나 해당 HTTP 메서드를 지원하지 않는 경우를 405로 변환한다(예: 조회 전용 경로에 PUT).
     *
     * @param ex 지원 메서드 목록을 담은 예외
     * @return 405 상태 + METHOD_NOT_ALLOWED 코드 엔벨로프
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.status())
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED.code(), ErrorCode.METHOD_NOT_ALLOWED.message()));
    }

    /**
     * 매핑된 핸들러도 정적 리소스도 없는 경로를 404로 변환한다(오타 난 URL 등).
     *
     * @param ex 요청 경로를 담은 예외
     * @return 404 상태 + NOT_FOUND 코드 엔벨로프
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.status())
                .body(ApiResponse.error(ErrorCode.NOT_FOUND.code(), ErrorCode.NOT_FOUND.message()));
    }

    /**
     * 업로드 파일이 {@code spring.servlet.multipart.max-file-size}(5MB)를 넘긴 경우를 413으로 변환한다.
     *
     * <p><b>왜 필요한가.</b> 이 예외도 {@link #handleUnexpected}로 빨려들어가 500으로 나갔다. 사진이
     * 큰 것은 서버 잘못이 아니라 클라이언트 사정인데 500이 되면 두 가지가 망가진다 — 앱은 "서버
     * 오류"라는 애매한 메시지만 띄워 사용자가 무엇을 고쳐야 할지 모르고, 운영자는 500 비율로
     * 건강도를 볼 때 큰 사진 한 장을 진짜 장애와 구분하지 못한다.
     *
     * @param ex 상한을 담은 멀티파트 예외
     * @return 413 상태 + FILE_TOO_LARGE 코드 엔벨로프(사용자에게 그대로 보여줄 수 있는 메시지)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(ErrorCode.FILE_TOO_LARGE.status())
                .body(ApiResponse.error(ErrorCode.FILE_TOO_LARGE.code(), ErrorCode.FILE_TOO_LARGE.message()));
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
