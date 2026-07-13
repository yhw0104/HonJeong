package com.honjeong.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 전 도메인이 공유하는 에러 코드 enum — 각 상수가 HTTP 상태와 기본 사용자 메시지를 함께 정의한다.
 * BusinessException에 담겨 던져지고 GlobalExceptionHandler·SecurityConfig가 이 값으로 에러 응답을 만든다.
 *
 * <p>[기존 주석] 도메인 공통 에러 코드. {@code code()}는 enum 이름을 그대로 노출한다(예: CHECKIN_ALREADY_ACTIVE).
 * 각 슬라이스에서 도메인별 코드를 추가한다.
 */
public enum ErrorCode {

    // 공통 — 어느 도메인에서나 쓰는 범용 에러(잘못된 입력 400 / 미인증 401 / 권한부족 403 / 없음 404 / 충돌 409)
    /** 요청 값 검증 실패(@Valid 실패·필수 파라미터 누락 등) — 400 */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    /** 인증 실패 — 토큰 없음·위조·만료 등 — 401 */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    /** 인가 실패 — 역할(ROLE) 부족 등 권한 없음 — 403 */
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    /** 범용 리소스 없음 — 404 */
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    /** 범용 상태 충돌 — 409 */
    CONFLICT(HttpStatus.CONFLICT, "요청이 충돌했습니다."),

    // 인증·계정 — 토큰 재발급, 휴대폰 인증, 약관 동의, 닉네임/사용자 조회 등 가입·로그인 흐름의 에러
    /** 리프레시 토큰이 없거나 만료·폐기됨(재로그인 필요) — 401 */
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    /** 휴대폰 인증번호 불일치 — 400 */
    PHONE_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    /** 휴대폰 인증번호 유효시간 초과 — 400 */
    PHONE_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    /** 휴대폰 인증번호 검증 시도 횟수 초과 — 429 */
    PHONE_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다."),
    /** 휴대폰 인증을 완료하지 않고 다음 단계 요청 — 400 */
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "휴대폰 인증이 필요합니다."),
    /** 필수 약관 미동의 상태로 가입 진행 — 400 */
    TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "필수 약관에 동의해야 합니다."),
    /** 닉네임 중복(가입·프로필 수정 시) — 409 */
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    /** 사용자 없음(탈퇴·잘못된 id) — 404 */
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 장소 — 존재하지 않는 장소 조회
    /** 장소(places) 없음 — 잘못된 placeId 조회 — 404 */
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "식당을 찾을 수 없습니다."),

    // 체크인 — 단일 활성 제약 충돌(다른 장소 ACTIVE)·대상 없음
    /** 단일 활성 체크인 제약 위반 — 이미 ACTIVE 체크인이 있는데 새 체크인 시도 — 409 */
    CHECKIN_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 진행 중인 체크인이 있습니다."),
    /** 체크인 없음 — 잘못된 checkInId 조회 — 404 */
    CHECKIN_NOT_FOUND(HttpStatus.NOT_FOUND, "체크인을 찾을 수 없습니다."),
    /** ACTIVE 상태가 아닌 체크인을 취소/종료 시도 — 409 */
    CHECKIN_NOT_ACTIVE(HttpStatus.CONFLICT, "진행 중인 혼밥만 취소할 수 있습니다."),
    /** SEEKING 상태가 아닌 체크인에 dineAlone(혼자 먹기 시작) 시도 — 409 */
    CHECKIN_NOT_SEEKING(HttpStatus.CONFLICT, "모집 중인 체크인이 아닙니다."),
    /** TOGETHER 상태가 아닌 체크인에 매칭 깨기(leaveMatch) 시도 — 409 */
    CHECKIN_NOT_TOGETHER(HttpStatus.CONFLICT, "같이 먹는 중이 아닙니다."),

    // 같이먹기 — 대상 체크인 상태·opt-in·자기신청/중복·응답완료 충돌
    /** 같이먹기 신청 없음 — 잘못된 requestId — 404 */
    MEALREQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "같이먹기 신청을 찾을 수 없습니다."),
    /** 신청 대상 체크인이 없거나 이미 ACTIVE가 아님 — 404 */
    TARGET_CHECKIN_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "대상 체크인이 없거나 이미 종료되었습니다."),
    /** 자기 자신의 체크인에 같이먹기 신청 — 409 */
    MEALREQUEST_SELF(HttpStatus.CONFLICT, "자기 자신에게는 신청할 수 없습니다."),
    /** 상대가 같이먹기 수신 opt-in을 꺼 둠 — 403 */
    MEALREQUEST_OPT_OUT(HttpStatus.FORBIDDEN, "상대가 같이먹기 신청을 받지 않습니다."),
    /** 같은 대상에게 이미 대기 중인 신청 존재 — 409 */
    MEALREQUEST_DUPLICATE(HttpStatus.CONFLICT, "이미 신청한 대상입니다."),
    /** 이미 수락/거절된 신청에 재응답 시도 — 409 */
    MEALREQUEST_ALREADY_RESPONDED(HttpStatus.CONFLICT, "이미 응답한 신청입니다."),
    /** 수락 시점에 상대(신청자) 체크인이 이미 종료됨 — 409 */
    MEALREQUEST_TARGET_ENDED(HttpStatus.CONFLICT, "상대가 이미 혼밥을 종료했어요."),
    /** 신청자가 이미 다른 사람과 TOGETHER 상태 — 409 */
    MEALREQUEST_SENDER_BUSY(HttpStatus.CONFLICT, "이미 다른 사람과 식사 중입니다."),

    // 메이트 — 신청/수락/거절/취소·관계
    /** 본인에게 메이트 신청 — 400 */
    MATE_SELF(HttpStatus.BAD_REQUEST, "본인에게는 메이트 신청을 할 수 없습니다."),
    /** 이미 메이트 관계인 상대에게 재신청 — 409 */
    MATE_ALREADY(HttpStatus.CONFLICT, "이미 메이트인 상대입니다."),
    /** 같은 상대에게 대기 중인 메이트 신청 존재 — 409 */
    MATE_REQUEST_DUPLICATE(HttpStatus.CONFLICT, "이미 신청한 상대입니다."),
    /** 메이트 신청 없음 — 잘못된 requestId — 404 */
    MATE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "메이트 신청을 찾을 수 없습니다."),
    /** 이미 수락/거절/취소된 메이트 신청에 재처리 시도 — 409 */
    MATE_REQUEST_ALREADY_RESPONDED(HttpStatus.CONFLICT, "이미 처리된 신청입니다."),
    /** 메이트 관계 없음(끊기 등 대상 조회 실패) — 404 */
    MATE_NOT_FOUND(HttpStatus.NOT_FOUND, "메이트 관계를 찾을 수 없습니다."),

    // 알림
    /** 알림 없음 — 잘못된 notificationId 또는 남의 알림 — 404 */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),

    // 리뷰
    /** 리뷰 없음 — 잘못된 reviewId — 404 */
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    /** 같은 체크인(방문)에 이미 리뷰 작성함 — 409 */
    REVIEW_DUPLICATE_CHECKIN(HttpStatus.CONFLICT, "이미 이 방문에 리뷰를 남겼어요."),

    // 즐겨찾기
    /** 즐겨찾기 그룹 없음 — 잘못된 groupId 또는 남의 그룹 — 404 */
    FAVORITE_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "즐겨찾기 그룹을 찾을 수 없습니다."),
    /** 기본 그룹 삭제 시도 — 400 */
    DEFAULT_GROUP_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "기본 그룹은 삭제할 수 없습니다."),

    // 차단/신고 — FR-108 안전장치
    /** 자기 자신 차단 시도 — 400 */
    BLOCK_SELF(HttpStatus.BAD_REQUEST, "자기 자신은 차단할 수 없습니다."),
    /** 이미 차단한 상대를 재차단 — 409 */
    BLOCK_ALREADY(HttpStatus.CONFLICT, "이미 차단한 상대입니다."),
    /** 차단 내역 없음(해제 시 대상 조회 실패) — 404 */
    BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "차단 내역을 찾을 수 없습니다."),
    /** 차단 관계인 상대에게 신청 시도(사유는 노출하지 않음) — 403 */
    USER_BLOCKED(HttpStatus.FORBIDDEN, "신청할 수 없는 상대예요."),
    /** 신고 대상(사용자·리뷰) 없음 — 404 */
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."),
    /** 자기 자신(또는 자기 리뷰) 신고 시도 — 400 */
    REPORT_SELF(HttpStatus.BAD_REQUEST, "자기 자신(또는 내가 쓴 리뷰)은 신고할 수 없습니다."),

    // 예기치 못한 서버 내부 오류(처리되지 않은 예외 → 500)
    /** 처리되지 않은 모든 예외의 최종 안전망 — 500 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    // 각 enum 상수가 들고 다니는 HTTP 상태와 기본 메시지(응답 변환의 출처)
    private final HttpStatus status;
    private final String message;

    /**
     * 기능: 각 에러 코드를 HTTP 상태·기본 메시지와 함께 정의하는 enum 생성자
     * Request: status — 이 에러에 대응하는 HTTP 상태, message — 기본 사용자 메시지
     * Response: 없음(생성자)
     *
     * <p>[기존 주석] 각 에러 코드를 HTTP 상태·기본 메시지와 함께 정의하는 enum 생성자.
     *
     * @param status 이 에러에 대응하는 HTTP 상태
     * @param message 기본 사용자 메시지
     */
    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * 기능: 이 에러의 HTTP 상태 조회
     * Request: 없음
     * Response: HttpStatus — 응답에 쓸 HTTP 상태(예: 404 NOT_FOUND)
     *
     * <p>[기존 주석] 이 에러의 HTTP 상태를 반환한다(예: 404 NOT_FOUND).
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * 기능: 클라이언트에 노출할 코드 문자열 조회 — enum 이름을 그대로 사용
     * Request: 없음
     * Response: String — 코드 문자열(예: "NOT_FOUND")
     *
     * <p>[기존 주석] 클라이언트에 노출할 코드 문자열을 반환한다 — enum 이름을 그대로 사용한다(예: "NOT_FOUND").
     */
    public String code() {
        return name();
    }

    /**
     * 기능: 이 에러의 기본 사용자 메시지 조회
     * Request: 없음
     * Response: String — 기본 메시지
     *
     * <p>[기존 주석] 이 에러의 기본 메시지를 반환한다.
     */
    public String message() {
        return message;
    }
}
