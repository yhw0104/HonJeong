import { ApiError } from '@/shared/api/client';
import type { CheckIn } from './api';

type Deps = {
  start: (placeId: number) => Promise<CheckIn>;
  getMine: () => Promise<CheckIn | null>;
  end: (checkInId: number) => Promise<CheckIn>;
};

/**
 * 체크인 시작. 다른 장소가 ACTIVE라 409(CHECKIN_ALREADY_ACTIVE)면 기존을 종료하고 1회만 재시도한다.
 * getMine() 결과가 SEEKING/TOGETHER 등 ACTIVE가 아닌 non-null이면(실제 충돌) 재시도해도 또 409이므로
 * 재시도하지 않고 원래 에러를 그대로 던진다(확정 실패한 POST를 두 번 쏘지 않는다).
 * 그 외(non-409) 에러는 그대로 던진다. (deps 주입 → 순수 단위테스트 가능)
 */
export async function startCheckInWithRecovery(placeId: number, deps: Deps): Promise<CheckIn> {
  try {
    return await deps.start(placeId);
  } catch (e) {
    if (e instanceof ApiError && e.code === 'CHECKIN_ALREADY_ACTIVE') {
      const mine = await deps.getMine();
      if (mine && mine.status !== 'ACTIVE') throw e;
      if (mine) await deps.end(mine.checkInId);
      return await deps.start(placeId);
    }
    throw e;
  }
}
