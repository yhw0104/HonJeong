package com.honjeong.global.exception;

/**
 * 1. 기능: 비즈니스 규칙 위반을 나타내는 공통 런타임 예외 — 담고 있는 ErrorCode가 HTTP 상태·코드·기본 메시지를 결정
 * 2. 사용처: 전 도메인 Service 계층에서 던짐(CheckInService·MealRequestService 등), GlobalExceptionHandler가 잡아 공통 응답 봉투로 변환, CurrentUserIdArgumentResolver(401)
 *
 * <p>[기존 주석] 비즈니스 규칙 위반을 나타내는 예외. {@link ErrorCode}가 HTTP 상태·코드·기본 메시지를 결정한다.
 * 서비스 계층에서 던지고 {@code GlobalExceptionHandler}가 공통 엔벨로프로 변환한다.
 */
public class BusinessException extends RuntimeException {

    // 이 예외가 어떤 에러(상태·코드·기본 메시지)인지 결정하는 코드. GlobalExceptionHandler가 이 값으로 응답을 만든다.
    private final ErrorCode errorCode;

    /**
     * 기능: 에러 코드의 기본 메시지를 그대로 예외 메시지로 사용해 생성한다
     * Request: errorCode — 위반한 비즈니스 규칙에 대응하는 에러 코드
     * Response: 없음(생성자)
     *
     * <p>[기존 주석] 에러 코드의 기본 메시지를 그대로 예외 메시지로 사용해 생성한다.
     *
     * @param errorCode 위반한 비즈니스 규칙에 대응하는 에러 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * 기능: 에러 코드는 유지하되 상황에 맞는 사용자 정의 메시지로 덮어써 생성한다
     * Request: errorCode — 대응 에러 코드(HTTP 상태·코드명 결정), message — 기본 메시지 대신 노출할 구체적 메시지
     * Response: 없음(생성자)
     *
     * <p>[기존 주석] 에러 코드는 유지하되 상황에 맞는 사용자 정의 메시지로 덮어써 생성한다.
     *
     * @param errorCode 위반한 비즈니스 규칙에 대응하는 에러 코드(HTTP 상태·코드명 결정)
     * @param message 기본 메시지 대신 노출할 구체적 메시지
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 기능: 이 예외가 담고 있는 에러 코드 조회
     * Request: 없음
     * Response: ErrorCode — 핸들러가 HTTP 상태·코드명을 뽑는 데 사용
     *
     * <p>[기존 주석] 이 예외가 담고 있는 에러 코드를 반환한다(핸들러가 HTTP 상태·코드명을 뽑는 데 사용).
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
