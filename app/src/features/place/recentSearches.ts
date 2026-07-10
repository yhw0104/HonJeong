// 최근 검색어 — 검색 결과를 탭했을 때 그 검색어를 기기에 로컬 저장한다(서버 전송 없음).
// 순수 로직(nextRecent/removeItem)은 TDD로 검증하고, SecureStore I/O·상태는 useRecentSearches 훅이 감싼다.
import { useCallback, useEffect, useState } from 'react';
import * as SecureStore from 'expo-secure-store';

/** 유지할 최근 검색어 최대 개수. */
export const MAX_RECENT = 8;

/** SecureStore 키(영숫자·-·_·. 만 허용). */
const KEY = 'recent_searches';

/** 검색어를 목록 맨 앞에 넣는다: trim·빈문자열 무시, 중복 제거 후 맨 앞, 최대 MAX_RECENT개. */
export function nextRecent(list: string[], term: string): string[] {
  const t = term.trim();
  if (!t) return list;
  return [t, ...list.filter((x) => x !== t)].slice(0, MAX_RECENT);
}

/** 목록에서 특정 검색어만 제거(순서 유지). */
export function removeItem(list: string[], term: string): string[] {
  return list.filter((x) => x !== term);
}

async function load(): Promise<string[]> {
  try {
    const raw = await SecureStore.getItemAsync(KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((x): x is string => typeof x === 'string') : [];
  } catch {
    return []; // 파싱·읽기 실패는 빈 목록으로(편의 기능이라 조용히 폴백)
  }
}

function save(list: string[]): void {
  // 저장 실패는 무시(다음 성공 저장이 복구). 최근 검색어는 편의 기능이라 사용자에게 알리지 않는다.
  SecureStore.setItemAsync(KEY, JSON.stringify(list)).catch(() => {});
}

/** 최근 검색어 상태 + 조작. 마운트 시 로드하고, add/remove/clear가 상태와 저장소를 함께 갱신한다. */
export function useRecentSearches() {
  const [recent, setRecent] = useState<string[]>([]);

  useEffect(() => {
    let alive = true;
    load().then((list) => {
      if (alive) setRecent(list);
    });
    return () => {
      alive = false;
    };
  }, []);

  const add = useCallback((term: string) => {
    setRecent((cur) => {
      const next = nextRecent(cur, term);
      if (next !== cur) save(next);
      return next;
    });
  }, []);

  const remove = useCallback((term: string) => {
    setRecent((cur) => {
      const next = removeItem(cur, term);
      save(next);
      return next;
    });
  }, []);

  const clear = useCallback(() => {
    setRecent([]);
    save([]);
  }, []);

  return { recent, add, remove, clear };
}
