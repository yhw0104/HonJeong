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

export type PlaceReview = {
  reviewId: number;
  user: { nickname: string };
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

export type PlacePhoto = { photoUrl: string; reviewId: number };

export const fetchPlacePhotos = (placeId: number) =>
  apiGet<PlacePhoto[]>(`/places/${placeId}/photos`);
