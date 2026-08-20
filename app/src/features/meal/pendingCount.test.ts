import { pendingReceivedCount } from './pendingCount';
import type { MealRequestListItem, MealRequestStatus } from './api';

const req = (status: MealRequestStatus): MealRequestListItem =>
  ({ mealRequestId: 1, status, createdAt: '2026-08-20T12:00:00' }) as MealRequestListItem;

describe('pendingReceivedCount', () => {
  it('PENDING만 센다', () => {
    expect(pendingReceivedCount([req('PENDING'), req('PENDING')])).toBe(2);
  });

  it('★응답이 끝난 신청은 세지 않는다 — 어제 거절한 것 때문에 오늘도 뱃지가 떠 있으면 안 된다', () => {
    const list = [req('ACCEPTED'), req('DECLINED'), req('EXPIRED'), req('WITHDRAWN')];
    expect(pendingReceivedCount(list)).toBe(0);
  });

  it('섞여 있으면 PENDING 개수만 돌려준다', () => {
    expect(pendingReceivedCount([req('PENDING'), req('DECLINED'), req('PENDING'), req('ACCEPTED')])).toBe(2);
  });

  it('빈 목록은 0', () => {
    expect(pendingReceivedCount([])).toBe(0);
  });
});
