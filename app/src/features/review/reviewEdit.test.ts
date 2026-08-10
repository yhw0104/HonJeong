import { buildReviewBody } from './reviewEdit';

describe('buildReviewBody — 작성/수정 폼 상태를 요청 바디로', () => {
  it('별점·태그·본문을 매핑(본문 trim)', () => {
    expect(buildReviewBody({ taste: 4, honbab: 5, tags: ['바테이블'], body: ' 좋다 ' })).toEqual({
      tasteRating: 4,
      soloFriendlyRating: 5,
      content: '좋다',
      tags: ['바테이블'],
    });
  });

  it('★혼밥 별점이 없으면 null 그대로 보낸다 — 일반 리뷰 화면', () => {
    // 값을 채워 보내면 서버가 400을 준다(혼밥 별점은 인증 리뷰만 가질 수 있다).
    // 0 같은 걸 대신 넣으면 조용히 혼밥 친화도를 오염시킨다.
    const body = buildReviewBody({ taste: 4, honbab: null, tags: [], body: '둘이 와도 좋아요' });
    expect(body.soloFriendlyRating).toBeNull();
    expect(body.tags).toEqual([]);
  });

  it('빈 본문은 content undefined', () => {
    expect(buildReviewBody({ taste: 4, honbab: 5, tags: [], body: '   ' }).content).toBeUndefined();
  });

  it('photos를 imageUrls로 전달한다', () => {
    const body = buildReviewBody({ taste: 5, honbab: 4, tags: [], body: '', photos: ['u1', 'u2'] });
    expect(body.imageUrls).toEqual(['u1', 'u2']);
  });
});
