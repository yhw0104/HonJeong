import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';
import { useDebouncedValue } from './useDebouncedValue';

jest.useFakeTimers();

// 훅 값을 밖으로 노출하는 최소 하네스(렌더 도구 부재 → react-test-renderer 직접 사용).
function setup(initial: string, delay = 300) {
  const result = { current: '' as string };
  function Probe({ value }: { value: string }) {
    result.current = useDebouncedValue(value, delay);
    return null;
  }
  let r!: TestRenderer.ReactTestRenderer;
  act(() => {
    r = TestRenderer.create(<Probe value={initial} />);
  });
  return {
    result,
    setValue: (v: string) => act(() => { r.update(<Probe value={v} />); }),
    unmount: () => act(() => { r.unmount(); }),
  };
}

describe('useDebouncedValue', () => {
  it('초기값은 즉시 반영', () => {
    const { result } = setup('a');
    expect(result.current).toBe('a');
  });

  it('delay 전엔 옛값, delay 경과 후 최신값만(중간값 스킵)', () => {
    const { result, setValue } = setup('a');
    setValue('ab');
    setValue('abc');
    expect(result.current).toBe('a'); // 아직 delay 전
    act(() => { jest.advanceTimersByTime(300); });
    expect(result.current).toBe('abc'); // 마지막 값만 반영(중간 ab 스킵)
  });

  it('타이핑이 delay 안에 이어지면 계속 옛값(마지막 변경 기준 재측정)', () => {
    const { result, setValue } = setup('a');
    setValue('ab');
    act(() => { jest.advanceTimersByTime(200); }); // delay 미만
    setValue('abc'); // 타이머 리셋
    act(() => { jest.advanceTimersByTime(200); }); // 리셋 후 200 → 아직 미만
    expect(result.current).toBe('a');
    act(() => { jest.advanceTimersByTime(100); }); // 마지막 변경 후 300 경과
    expect(result.current).toBe('abc');
  });

  it('언마운트 후 타이머 콜백은 무해(clearTimeout으로 상태 갱신 없음)', () => {
    const { setValue, unmount } = setup('a');
    setValue('ab');
    unmount();
    act(() => { jest.advanceTimersByTime(300); }); // 경고/에러 없이 지나가면 OK
  });
});
