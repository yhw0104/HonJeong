package com.honjeong.review.dto;

/**
 * 리뷰 작성 화면을 고르기 위한 사전 조회 응답.
 *
 * <p>앱은 이 값으로 <b>혼밥 리뷰 화면</b>(혼밥 별점·친화 태그를 묻는다)과 <b>일반 리뷰 화면</b>
 * (묻지 않는다) 중 하나를 연다. 그리고 여기서 받은 id를 작성 요청에 그대로 되돌려 보내므로,
 * 화면에서 물어본 것과 서버가 저장하는 것이 어긋날 수 없다.
 *
 * @param linkableCheckInId 지금 이 식당에 리뷰를 쓰면 혼밥 인증으로 연결될 체크인 ID.
 *                          없으면 null(→ 앱은 일반 리뷰 화면을 연다)
 */
public record ReviewContextResponse(Long linkableCheckInId) {}
