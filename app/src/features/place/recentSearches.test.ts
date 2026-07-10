import { MAX_RECENT, nextRecent, removeItem } from './recentSearches';

test('새 검색어를 목록 맨 앞에 추가한다', () => {
  expect(nextRecent(['김밥'], '초밥')).toEqual(['초밥', '김밥']);
});

test('빈 문자열·공백만 있는 검색어는 무시하고 원본을 그대로 둔다', () => {
  const list = ['김밥'];
  expect(nextRecent(list, '')).toBe(list);
  expect(nextRecent(list, '   ')).toBe(list);
});

test('앞뒤 공백은 trim해서 저장한다', () => {
  expect(nextRecent([], '  초밥  ')).toEqual(['초밥']);
});

test('이미 있는 검색어를 다시 검색하면 중복 없이 맨 앞으로 이동한다', () => {
  expect(nextRecent(['초밥', '김밥', '파스타'], '김밥')).toEqual(['김밥', '초밥', '파스타']);
});

test('최대 MAX_RECENT개만 유지하고 가장 오래된 것을 버린다', () => {
  const full = ['8', '7', '6', '5', '4', '3', '2', '1']; // 8개(맨 앞=최신)
  expect(full).toHaveLength(MAX_RECENT);
  const got = nextRecent(full, '9');
  expect(got).toHaveLength(MAX_RECENT);
  expect(got[0]).toBe('9');
  expect(got).not.toContain('1'); // 가장 오래된 것 잘림
});

test('removeItem은 해당 검색어만 지우고 순서를 유지한다', () => {
  expect(removeItem(['초밥', '김밥', '파스타'], '김밥')).toEqual(['초밥', '파스타']);
});

test('removeItem은 없는 검색어면 원본을 그대로 반환한다', () => {
  expect(removeItem(['초밥', '김밥'], '없음')).toEqual(['초밥', '김밥']);
});
