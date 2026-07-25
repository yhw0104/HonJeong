import { useEffect, useState } from 'react';

/**
 * value가 delayMs 동안 더 이상 바뀌지 않으면 그 값을 반환한다(디바운스).
 * 검색 입력처럼 타이핑이 멈춘 뒤 1회만 쿼리를 트리거하고 싶을 때 쓴다.
 * TextInput의 즉시 표시값은 원본 state를 쓰고, API에 넘길 값만 이 훅으로 지연시킨다.
 */
export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(id); // value가 delay 안에 또 바뀌면 이전 타이머 취소(리셋)
  }, [value, delayMs]);
  return debounced;
}
