import { apiGet, apiPost, apiPatch, apiDelete } from '@/shared/api/client';

export type CreateReviewBody = {
  placeId: number;
  checkInId?: number;
  tasteRating: number;
  soloFriendlyRating: number;
  content?: string;
  tags?: string[];
  imageUrls?: string[];
};

export type UpdateReviewBody = {
  tasteRating: number;
  soloFriendlyRating: number;
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
  soloFriendlyRating: number;
  tags: string[];
  authenticated: boolean;
  mine: boolean;
  imageUrls: string[];
};

export type PlaceReviewSummary = {
  placeId: number;
  reviewCount: number;
  avgTasteRating: number | null;
  avgSoloFriendlyRating: number | null;
  topTags: { tag: string; count: number }[];
};

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
      soloFriendlyRating: number;
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
  soloFriendlyRating: number;
  tags: string[];
  imageUrls: string[];
  authenticated: boolean;
  createdAt: string;
};

export const fetchMyReviews = () => apiGet<{ reviews: MyReview[] }>('/users/me/reviews');

export type PlacePhoto = { photoUrl: string; reviewId: number };

export const fetchPlacePhotos = (placeId: number) =>
  apiGet<PlacePhoto[]>(`/places/${placeId}/photos`);
