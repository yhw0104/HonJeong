import { sortByMode } from './sortFavorites';

const items = [{ name: '초밥천국' }, { name: '가마솥밥' }, { name: '노란식탁' }];

test('등록순은 원래 순서를 그대로 유지한다', () => {
  expect(sortByMode(items, 'registered').map((i) => i.name)).toEqual(['초밥천국', '가마솥밥', '노란식탁']);
});

test('이름순은 가나다순으로 정렬하고 원본 배열은 건드리지 않는다', () => {
  expect(sortByMode(items, 'name').map((i) => i.name)).toEqual(['가마솥밥', '노란식탁', '초밥천국']);
  expect(items.map((i) => i.name)).toEqual(['초밥천국', '가마솥밥', '노란식탁']);
});
