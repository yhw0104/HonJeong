// 리뷰를 쓸 때 어느 화면을 열 것인가.
//
// 혼밥 인증 리뷰만 혼밥 별점·태그를 가질 수 있어서 작성 화면이 둘이다. 어느 쪽인지는
// **서버만 안다** — 앱은 끝난 체크인을 모르기 때문이다(/check-ins/me는 현재 것만 준다).
// 그래서 GET /places/{id}/review-context로 받은 답을 그대로 따른다.
import type { RootStackParamList } from '@/navigation/types';

/** 열어야 할 화면과 그 화면에 넘길 파라미터 조각. */
export type ReviewWriteTarget =
  | { screen: 'DiningLogWrite'; checkInId: number }
  | { screen: 'ReviewWrite' };

/**
 * 새 리뷰를 쓸 때 열 화면을 정한다. (순수)
 *
 * ★받은 id를 그대로 되돌려 준다는 게 핵심이다. 서버는 스스로 체크인을 찾지 않으므로,
 * 이 id를 작성 요청에 실어 보내야만 인증이 붙는다 — 화면에서 혼밥 별점을 물어봤는데
 * 저장 결과가 일반 리뷰가 되는(또는 그 반대) 어긋남이 구조적으로 생길 수 없다.
 *
 * @param linkableCheckInId 서버가 알려준 '지금 쓰면 연결될 체크인'. 없으면 null
 * @returns 열 화면(+ 필요한 checkInId)
 */
export function reviewWriteTarget(linkableCheckInId: number | null): ReviewWriteTarget {
  if (linkableCheckInId == null) return { screen: 'ReviewWrite' };
  return { screen: 'DiningLogWrite', checkInId: linkableCheckInId };
}

/** 기존 리뷰를 수정할 때 열 화면. 인증 여부는 서버가 이미 판정해 내려준다(불변이다). */
export function reviewEditScreen(authenticated: boolean): keyof Pick<RootStackParamList, 'DiningLogWrite' | 'ReviewWrite'> {
  return authenticated ? 'DiningLogWrite' : 'ReviewWrite';
}
