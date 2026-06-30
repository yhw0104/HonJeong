// Avatar 렌더 분기 결정(순수). uri>이름>기본 우선순위.
export type AvatarMode = 'image' | 'initial' | 'default';

export function avatarMode(uri?: string | null, name?: string | null): AvatarMode {
  if (uri) return 'image';
  if (name && Array.from(name)[0]) return 'initial';
  return 'default';
}
