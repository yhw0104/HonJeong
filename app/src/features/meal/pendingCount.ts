import type { MealRequestListItem } from './api';

/**
 * 하단탭 뱃지에 쓸 "내 응답을 기다리는 받은 신청" 수.
 *
 * <p>★목록 길이를 그대로 쓰면 안 된다. 받은 신청 목록에는 이미 수락·거절·만료·철회된 것도
 * 함께 들어 있어서, 어제 거절한 신청 넷 때문에 오늘도 빨간 4가 떠 있게 된다. 뱃지는
 * "지금 네가 할 일이 있다"는 신호라, 그 신호가 거짓이면 사용자는 뱃지를 무시하기 시작한다.
 * 채팅 탭의 뱃지가 '안 읽은 메시지 수'인 것과 같은 성격이다.
 *
 * <p>같은 화면의 세그먼트("받은 신청 N")는 <b>목록 길이</b>를 쓴다 — 그건 신호가 아니라
 * 목록의 크기를 말하는 자리라서 의미가 다르다.
 */
export function pendingReceivedCount(list: MealRequestListItem[]): number {
  return list.filter((r) => r.status === 'PENDING').length;
}
