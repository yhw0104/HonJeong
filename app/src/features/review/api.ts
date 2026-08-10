import { apiGet, apiPost, apiPatch, apiDelete } from '@/shared/api/client';

export type CreateReviewBody = {
  placeId: number;
  checkInId?: number;
  tasteRating: number;
  /** 혼밥 적합도 별점. **혼밥 인증 리뷰만 보낼 수 있다** — 연결될 체크인이 없는데 보내면 서버가 400을 준다. */
  soloFriendlyRating?: number | null;
  content?: string;
  tags?: string[];
  imageUrls?: string[];
};

export type UpdateReviewBody = {
  tasteRating: number;
  /** 혼밥 적합도 별점. **혼밥 인증 리뷰만 보낼 수 있다** — 연결될 체크인이 없는데 보내면 서버가 400을 준다. */
  soloFriendlyRating?: number | null;
  content?: string;
  tags?: string[];
  imageUrls?: string[];
};

export type CreateReviewResult = {
  reviewId: number;
  placeId: number;
  checkInId: number | null;
  authenticated: boolean;
};

/** 리뷰 작성자 프로필을 열 수 없는 이유(서버 판단) — 열 수 있으면 null이 온다. */
export type AuthorUnavailable = 'WITHDRAWN' | 'SUSPENDED' | 'UNKNOWN';

export type PlaceReview = {
  reviewId: number;
  /** userId는 프로필로 이동할 수 있는 작성자에게만 온다 — 탈퇴·정지는 null이고 unavailable에 이유가 담긴다. */
  user: { userId: number | null; nickname: string; unavailable: AuthorUnavailable | null };
  visitedAt: string;
  content: string | null;
  tasteRating: number;
  /** 혼밥 인증 리뷰가 아니면 null — 표시할 때 별점 줄을 숨긴다. */
  soloFriendlyRating: number | null;
  tags: string[];
  authenticated: boolean;
  mine: boolean;
  imageUrls: string[];
};

export type PlaceReviewSummary = {
  placeId: number;
  /** 전체 리뷰 수(맛 별점 기준). */
  reviewCount: number;
  /**
   * 혼밥 적합도를 평가한 리뷰 수. **'혼밥러 N명 평가'의 N은 이 값이다** —
   * 혼밥 별점은 혼밥 인증 리뷰만 갖기 때문에 전체 리뷰 수를 쓰면 부풀려진다.
   */
  soloRatedCount: number;
  avgTasteRating: number | null;
  /** 혼밥 평가가 하나도 없으면 null — 리뷰는 있는데 전부 인증이 아닌 경우가 여기 해당한다. */
  avgSoloFriendlyRating: number | null;
  topTags: { tag: string; count: number }[];
};

/** 리뷰를 쓰기 전 서버에 묻는 것 — 지금 쓰면 혼밥 인증으로 연결될 체크인이 있는가. */
export type ReviewContext = { linkableCheckInId: number | null };

/**
 * 어느 작성 화면을 열지 정하기 위한 사전 조회.
 *
 * 여기서 받은 id를 작성 요청에 그대로 되돌려 보내야 한다 — 서버는 스스로 체크인을 찾지 않으므로,
 * 안 보내면 인증이 붙지 않는다.
 */
export const fetchReviewContext = (placeId: number) =>
  apiGet<ReviewContext>(`/places/${placeId}/review-context`);

export type DiningHistory = {
  summary: { totalCheckIns: number; totalReviews: number; distinctPlaces: number; thisMonthCheckIns: number };
  entries: {
    checkInId: number;
    placeId: number;
    placeName: string;
    visitedAt: string;
    status: 'ACTIVE' | 'ENDED';
    review: {
      reviewId: number;
      content: string | null;
      tasteRating: number;
      /** 혼밥 인증 리뷰가 아니면 null. */
      soloFriendlyRating: number | null;
      tags: string[];
      imageUrls: string[];
    } | null;
  }[];
};

export const createReview = (body: CreateReviewBody) => apiPost<CreateReviewResult>('/reviews', body);

export const updateReview = (reviewId: number, body: UpdateReviewBody) =>
  apiPatch<CreateReviewResult>(`/reviews/${reviewId}`, body);

export const deleteReview = (reviewId: number) => apiDelete<null>(`/reviews/${reviewId}`);

export const listPlaceReviews = (placeId: number) =>
  apiGet<PlaceReview[]>(`/places/${placeId}/reviews`);

export const fetchPlaceReviewSummary = (placeId: number) =>
  apiGet<PlaceReviewSummary>(`/places/${placeId}/review-summary`);

export const fetchDiningHistory = () => apiGet<DiningHistory>('/users/me/dining-history');

export type MyReview = {
  reviewId: number;
  placeId: number;
  placeName: string;
  visitedAt: string;
  content: string | null;
  tasteRating: number;
  /** 혼밥 인증 리뷰가 아니면 null — 표시할 때 별점 줄을 숨긴다. */
  soloFriendlyRating: number | null;
  tags: string[];
  imageUrls: string[];
  authenticated: boolean;
  createdAt: string;
};

export const fetchMyReviews = () => apiGet<{ reviews: MyReview[] }>('/users/me/reviews');

export type PlacePhoto = { photoUrl: string; reviewId: number };

export const fetchPlacePhotos = (placeId: number) =>
  apiGet<PlacePhoto[]>(`/places/${placeId}/photos`);
