import type { CheckInStatus } from './api';

/** 상태바가 그릴 모드. seeking=모집중(혼자먹기/그만두기), dining=혼밥중(끝내기), together=같이먹는중(끝내기). */
export type CheckInMode = 'seeking' | 'dining' | 'together';

export function checkInMode(status: CheckInStatus): CheckInMode {
  if (status === 'SEEKING') return 'seeking';
  if (status === 'TOGETHER') return 'together';
  return 'dining';
}
