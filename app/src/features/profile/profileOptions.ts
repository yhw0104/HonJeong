import { DINING_STYLE_LABEL, type DiningStyle } from '@/shared/format';

// 프로필 입력 선택지. 가입(ProfileSetup)과 수정(ProfileEdit) 두 화면이 같은 목록을 써야 한다.
//
// ★원래 두 파일에 똑같은 배열이 복붙돼 있었다. 한쪽만 고치면 "가입 때는 있던 항목이 수정에서는
//   사라지는" 상태가 되고, 그건 사용자가 프로필을 고치려다 선택이 풀리는 걸로 드러난다.

/**
 * 좋아하는 음식 선택지. **최대 3개**까지 고를 수 있다
 * (앱의 toggleFood와 백엔드 `@Size(max = 3)`, 그리고 DB의 food1/food2/food3 컬럼이 함께 정한 한도다 —
 * 늘리려면 마이그레이션이 필요하다).
 *
 * <p>★<b>기존 항목의 문자열을 바꾸거나 지우면 안 된다.</b> 사용자가 고른 값이 문자열 그대로
 * user_food_preferences에 저장돼 있어서, 여기서 이름을 바꾸면 그 사용자의 선택이 어느 칩과도
 * 맞지 않게 되고 프로필 수정 화면에서 선택이 통째로 풀린 것처럼 보인다.
 * 처음 7개(한식·일식·양식·중식·면 요리·매운맛·디저트)가 그런 이유로 원문 그대로 남아 있다.
 *
 * <p>목록 순서는 화면에 그대로 나온다 — 요리 종류 → 음식 종류 → 취향 순으로 뒀다.
 */
export const FOODS = [
  // 요리 종류
  '한식', '중식', '일식', '양식', '아시안',
  // 음식 종류
  '분식', '고기·구이', '국밥·탕', '면 요리', '해산물', '치킨', '패스트푸드',
  // 취향
  '샐러드·건강식', '카페·브런치', '디저트', '매운맛',
] as const;

/** 선택 화면(칩)용. key는 백엔드로 보낼 enum 값 그대로다 — 문구는 shared/format 한 곳에서 온다. */
export type { DiningStyle };

export const STYLES_OPT = (['TALK', 'QUIET'] as const).map((key) => ({
  key,
  label: DINING_STYLE_LABEL[key].title,
  sub: DINING_STYLE_LABEL[key].sub,
}));
