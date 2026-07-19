import { ApiError } from '@/shared/api/client';

// 혼밥 시작 실패 시 보여줄 문구. 진짜 충돌(이미 모집/혼밥 중)과 그 외(네트워크 실패 등)를 구분한다.
// — 백엔드가 꺼져 네트워크 실패일 때 "이미 다른 곳에서 모집 중"으로 오인시키던 문제 방지.
export function startErrorCopy(e: unknown): { title: string; message: string } {
  if (e instanceof ApiError && e.code === 'CHECKIN_ALREADY_ACTIVE') {
    return { title: '잠깐요', message: '이미 다른 곳에서 모집/혼밥 중이에요. 먼저 끝내고 다시 시도해 주세요.' };
  }
  return { title: '앗', message: '지금 시작하지 못했어요. 잠시 후 다시 시도해 주세요.' };
}
