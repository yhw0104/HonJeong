// 리뷰 작성자 닉네임을 눌렀을 때 프로필로 갈 수 없는 경우의 안내 문구(순수).
// 서버가 왜 못 여는지를 user.unavailable로 알려주고, 문구는 여기서만 정한다.
import type { AuthorUnavailable } from './api';

/**
 * 프로필을 열 수 없는 작성자를 눌렀을 때 보여줄 한 줄 안내.
 *
 * 정지 문구는 제재 사실을 제3자에게 알린다(2026-07-30 제품 결정). 중립 문구로 되돌리려면
 * 'SUSPENDED' 항목을 UNKNOWN과 같은 문구로 바꾸면 된다 — 서버 수정은 필요 없다.
 */
export function reviewAuthorUnavailableMessage(reason: AuthorUnavailable | null): string {
  if (reason === 'WITHDRAWN') return '탈퇴한 사용자입니다.';
  if (reason === 'SUSPENDED') return '정지된 사용자입니다.';
  // UNKNOWN(온보딩 중 등)과 서버가 이유를 안 준 경우 — 이유를 지어내지 않고 사실만 말한다.
  return '프로필을 볼 수 없는 사용자입니다.';
}
