// Avatar 렌더 분기 결정(순수). 사진이 있으면 사진, 없으면 앱 아이콘 하나로 통일한다.
// (이니셜/👤 폴백이 화면마다 갈려 같은 사람이 다르게 보이던 문제를 2026-07-30에 통일)
export type AvatarMode = 'image' | 'fallback';

export function avatarMode(uri?: string | null): AvatarMode {
  return uri ? 'image' : 'fallback';
}
