import { buildGroupBody } from './buildGroupBody';

describe('buildGroupBody', () => {
  it('이름 앞뒤 공백 제거, 색 포함', () => {
    expect(buildGroupBody({ name: '  단골  ', note: '', color: '#22A65A' })).toEqual({
      name: '단골',
      color: '#22A65A',
    });
  });

  it('설명이 있으면 trim해서 포함', () => {
    expect(buildGroupBody({ name: '주말', note: ' 코스 ', color: '#FF5A1F' })).toEqual({
      name: '주말',
      note: '코스',
      color: '#FF5A1F',
    });
  });
});
