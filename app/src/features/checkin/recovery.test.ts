import { startCheckInWithRecovery } from './recovery';
import { ApiError } from '@/shared/api/client';
import type { CheckIn } from './api';

const made = (over: Partial<CheckIn> = {}): CheckIn => ({
  checkInId: 1, placeId: 9, status: 'ACTIVE', startedAt: 'x', endedAt: null, ...over,
});

it('정상이면 그대로 시작 결과 반환', async () => {
  const start = jest.fn().mockResolvedValue(made({ placeId: 9 }));
  const res = await startCheckInWithRecovery(9, { start, getMine: jest.fn(), end: jest.fn() });
  expect(res.placeId).toBe(9);
  expect(start).toHaveBeenCalledTimes(1);
});

it('409면 기존 종료 후 1회 재시도', async () => {
  const start = jest.fn()
    .mockRejectedValueOnce(new ApiError(409, 'CHECKIN_ALREADY_ACTIVE', '이미 진행 중'))
    .mockResolvedValueOnce(made({ checkInId: 2, placeId: 9 }));
  const getMine = jest.fn().mockResolvedValue(made({ checkInId: 1, placeId: 5 }));
  const end = jest.fn().mockResolvedValue(made({ checkInId: 1, status: 'ENDED' }));

  const res = await startCheckInWithRecovery(9, { start, getMine, end });

  expect(end).toHaveBeenCalledWith(1);
  expect(start).toHaveBeenCalledTimes(2);
  expect(res.checkInId).toBe(2);
});

it('409가 아닌 에러는 그대로 던진다', async () => {
  const start = jest.fn().mockRejectedValue(new ApiError(500, 'INTERNAL_ERROR', 'x'));
  await expect(
    startCheckInWithRecovery(9, { start, getMine: jest.fn(), end: jest.fn() }),
  ).rejects.toBeInstanceOf(ApiError);
});

it('409인데 기존이 SEEKING이면 재시도하지 않고 원래 에러를 그대로 던진다', async () => {
  const conflict = new ApiError(409, 'CHECKIN_ALREADY_ACTIVE', '이미 진행 중');
  const start = jest.fn().mockRejectedValue(conflict);
  const getMine = jest.fn().mockResolvedValue(made({ checkInId: 1, placeId: 5, status: 'SEEKING' }));
  const end = jest.fn();

  await expect(startCheckInWithRecovery(9, { start, getMine, end })).rejects.toBe(conflict);

  expect(end).not.toHaveBeenCalled();
  expect(start).toHaveBeenCalledTimes(1); // 확정 실패한 POST를 두 번 쏘지 않는다
});

it('409인데 기존이 TOGETHER여도 재시도하지 않고 원래 에러를 그대로 던진다', async () => {
  const conflict = new ApiError(409, 'CHECKIN_ALREADY_ACTIVE', '이미 진행 중');
  const start = jest.fn().mockRejectedValue(conflict);
  const getMine = jest.fn().mockResolvedValue(made({ checkInId: 1, placeId: 5, status: 'TOGETHER' }));
  const end = jest.fn();

  await expect(startCheckInWithRecovery(9, { start, getMine, end })).rejects.toBe(conflict);

  expect(end).not.toHaveBeenCalled();
  expect(start).toHaveBeenCalledTimes(1);
});
