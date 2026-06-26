import type { UpdateReviewBody } from './api';

/** 작성/수정 화면 폼 상태. taste=맛 별점, honbab=혼밥친화 별점. */
export type ReviewForm = { taste: number; honbab: number; tags: string[]; body: string };

/** 폼 상태를 리뷰 요청 바디로 변환한다(작성·수정 공용). 빈 본문은 undefined. */
export function buildReviewBody(form: ReviewForm): UpdateReviewBody {
  return {
    tasteRating: form.taste,
    soloFriendlyRating: form.honbab,
    content: form.body.trim() || undefined,
    tags: form.tags,
  };
}
