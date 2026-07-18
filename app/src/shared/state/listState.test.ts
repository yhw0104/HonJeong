import { listState } from './listState';

describe('listState', () => {
  it('첫 로딩(데이터 없음) → loading', () =>
    expect(listState({ isLoading: true, isError: false, count: 0 })).toBe('loading'));
  it('실패(데이터 없음) → error', () =>
    expect(listState({ isLoading: false, isError: true, count: 0 })).toBe('error'));
  it('빈 결과 → empty', () =>
    expect(listState({ isLoading: false, isError: false, count: 0 })).toBe('empty'));
  it('데이터 있음 → ready', () =>
    expect(listState({ isLoading: false, isError: false, count: 3 })).toBe('ready'));
  it('데이터 있으면 백그라운드 에러여도 ready(기존 목록 유지)', () =>
    expect(listState({ isLoading: false, isError: true, count: 3 })).toBe('ready'));
  it('데이터 있으면 리페치 중이어도 ready', () =>
    expect(listState({ isLoading: true, isError: false, count: 3 })).toBe('ready'));
});
