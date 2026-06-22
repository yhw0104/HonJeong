import { ApiError } from '@/shared/api/client';
import type { CheckIn } from './api';

type Deps = {
  start: (placeId: number) => Promise<CheckIn>;
  getMine: () => Promise<CheckIn | null>;
  end: (checkInId: number) => Promise<CheckIn>;
};

/**
 * 체크인 시작. 다른 장소가 ACTIVE라 409(CHECKIN_ALREADY_ACTIVE)면 기존을 종료하고 1회만 재시도한다.
 * 그 외 에러는 그대로 던진다. (deps 주입 → 순수 단위테스트 가능)
 */
export async function startCheckInWithRecovery(placeId: number, deps: Deps): Promise<CheckIn> {
  try {
    return await deps.start(placeId);
  } catch (e) {
    if (e instanceof ApiError && e.code === 'CHECKIN_ALREADY_ACTIVE') {
      const mine = await deps.getMine();
      if (mine && mine.status === 'ACTIVE') await deps.end(mine.checkInId);
      return await deps.start(placeId);
    }
    throw e;
  }
}
