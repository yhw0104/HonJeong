package com.honjeong.auth.dto;

/**
 * 약관 동의 요청 본문. {@code POST /api/auth/terms}에서 받는다.
 *
 * <p>다섯 항목 모두 단순 boolean이라 별도 검증 어노테이션은 없고, 대신 필수 4종(age·service·privacy·location)이 모두 true인지는
 * 서비스 계층({@code AuthService.agreeTerms})에서 검사해 하나라도 false면 {@code TERMS_REQUIRED}로 거절한다.
 *
 * @param age       만 14세 이상 확인(필수 — 14세 미만은 법정대리인 동의 필요).
 * @param service   서비스 이용약관 동의 여부(필수).
 * @param privacy   개인정보 처리방침 동의 여부(필수).
 * @param location  위치기반서비스 약관 동의 여부(필수 — 혼밥 체크인이 위치 기반이라 필요).
 * @param marketing 마케팅 정보 수신 동의 여부(선택 — false여도 통과).
 */
public record TermsRequest(boolean age, boolean service, boolean privacy, boolean location, boolean marketing) {
}
