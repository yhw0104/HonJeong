import type { UpdateReviewBody } from './api';

/**
 * 작성/수정 화면 폼 상태. taste=맛 별점, honbab=혼밥친화 별점. photos=업로드 완료된 url 또는 로컬 uri.
 *
 * `honbab`은 **혼밥 인증 리뷰에서만 값이 있다**. 일반 리뷰 화면은 이 별점을 묻지 않으므로 null이고,
 * 그대로 null로 보내야 한다 — 값을 채워 보내면 서버가 400을 준다(혼밥 친화도 오염 방지 불변식).
 */
export type ReviewForm = {
  taste: number; honbab: number | null; tags: string[]; body: string; photos: string[];
};

/** 폼 상태를 리뷰 요청 바디로 변환한다(작성·수정 공용). 빈 본문은 undefined. */
export function buildReviewBody(form: ReviewForm): UpdateReviewBody {
  return {
    tasteRating: form.taste,
    soloFriendlyRating: form.honbab,
    content: form.body.trim() || undefined,
    tags: form.tags,
    imageUrls: form.photos,
  };
}
