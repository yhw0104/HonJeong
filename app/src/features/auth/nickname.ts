// 닉네임 검증 규칙 — 가입(ProfileSetup)·편집(ProfileEdit)이 공유하는 순수 로직.
// 규칙: 2–10자, 자음/모음 낱자만은 불가, 서버 중복확인(available)을 통과해야 제출 가능.
import { T2 } from '@/shared/theme';

/** 닉네임 중복확인 상태. idle=확인 전(2자 미만 포함), invalid=자음/모음 낱자만. */
export type NickStatus = 'idle' | 'checking' | 'available' | 'taken' | 'invalid' | 'error';

/** 닉네임 최소/최대 길이(가입 힌트 "2–10자"와 동일 — 가입·편집 공통). */
export const NICKNAME_MIN = 2;
export const NICKNAME_MAX = 10;

/** 완성된 글자 없이 자음/모음 낱자(ㄱ-ㅎ, ㅏ-ㅣ)만으로 이뤄졌는지(예: 'ㅋㅋ'). */
export const isOnlyJamo = (s: string) => /^[ㄱ-ㅎㅏ-ㅣ]+$/.test(s);

/** 중복확인 상태별 표시 문구·색 — 가입·편집 화면 공통. */
export const NICK_HINT: Record<NickStatus, { text: string; color: string }> = {
  idle: { text: '', color: T2.textMute },
  checking: { text: '확인 중…', color: T2.textMute },
  available: { text: '사용 가능', color: T2.brand },
  taken: { text: '이미 사용 중', color: '#E1493F' },
  invalid: { text: '자음·모음만으로는 안 돼요', color: '#E1493F' },
  error: { text: '확인 실패', color: T2.textMute },
};

/**
 * 입력에 대해 서버 확인 전 단계를 판정한다.
 * - 2자 미만 → idle, 자모만 → invalid (둘 다 서버 호출 없음)
 * - 기존 닉네임과 동일 → available (편집 화면: 자기 닉네임은 중복이 아님)
 * - 그 외 → 서버 중복확인(check) 필요
 */
export function precheckNickname(
  raw: string,
  currentNickname?: string | null,
): { action: 'set'; status: NickStatus } | { action: 'check' } {
  const trimmed = raw.trim();
  if (trimmed.length < NICKNAME_MIN) return { action: 'set', status: 'idle' };
  if (isOnlyJamo(trimmed)) return { action: 'set', status: 'invalid' };
  if (currentNickname != null && trimmed === currentNickname) return { action: 'set', status: 'available' };
  return { action: 'check' };
}

/** CTA/저장 버튼 게이팅: 2자 이상이고 중복확인을 통과(available)했을 때만 제출 가능. */
export function canSubmitNickname(raw: string, status: NickStatus): boolean {
  return raw.trim().length >= NICKNAME_MIN && status === 'available';
}
