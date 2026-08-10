import { reviewWriteTarget, reviewEditScreen } from './reviewWriteTarget';

describe('reviewWriteTarget — 새 리뷰를 쓸 때 열 화면', () => {
  it('연결할 체크인이 있으면 혼밥 화면을 열고 그 id를 그대로 넘긴다', () => {
    expect(reviewWriteTarget(7)).toEqual({ screen: 'DiningLogWrite', checkInId: 7 });
  });

  it('★없으면 일반 리뷰 화면 — 서버 답을 그대로 따르므로 화면과 저장 결과가 어긋날 수 없다', () => {
    // 여기서 혼밥 화면을 열면 혼밥 별점을 물어봐 놓고 서버가 400으로 거절한다.
    expect(reviewWriteTarget(null)).toEqual({ screen: 'ReviewWrite' });
  });

  it('체크인 id 0도 유효한 값으로 다룬다 — null만 "없음"이다', () => {
    expect(reviewWriteTarget(0)).toEqual({ screen: 'DiningLogWrite', checkInId: 0 });
  });
});

describe('reviewEditScreen — 기존 리뷰를 수정할 때 열 화면', () => {
  it('인증 리뷰는 혼밥 화면', () => {
    expect(reviewEditScreen(true)).toBe('DiningLogWrite');
  });

  it('인증 아닌 리뷰는 일반 화면 — 혼밥 별점을 새로 매길 수 없다', () => {
    expect(reviewEditScreen(false)).toBe('ReviewWrite');
  });
});
