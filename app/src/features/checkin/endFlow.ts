import type { CheckIn } from './api';

/** 혼밥 종료 시 "정말 혼밥했나요?" 확인을 띄우는 경과시간 임계값(분). */
export const CANCEL_PROMPT_WINDOW_MIN = 30;

/**
 * 종료 탭 시 취소 확인(prompt)을 띄울지, 바로 종료(end)할지 결정한다.
 * ACTIVE이고 경과가 임계값 미만이면 prompt, 그 외(30분 이상·TOGETHER 등)는 end.
 */
export function decideEndAction(checkIn: CheckIn, nowMs: number): 'prompt' | 'end' {
  if (checkIn.status !== 'ACTIVE') return 'end';
  const elapsedMin = (nowMs - new Date(checkIn.startedAt).getTime()) / 60000;
  return elapsedMin < CANCEL_PROMPT_WINDOW_MIN ? 'prompt' : 'end';
}
