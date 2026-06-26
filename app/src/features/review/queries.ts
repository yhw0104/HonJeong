import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { createReview, updateReview, deleteReview, listPlaceReviews, fetchPlaceReviewSummary, fetchDiningHistory, type CreateReviewBody, type UpdateReviewBody } from './api';

export function usePlaceReviews(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'reviews'],
    queryFn: () => listPlaceReviews(placeId),
  });
}

export function usePlaceReviewSummary(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'review-summary'],
    queryFn: () => fetchPlaceReviewSummary(placeId),
  });
}

export function useDiningHistory() {
  return useQuery({
    queryKey: ['review', 'diningHistory'],
    queryFn: fetchDiningHistory,
  });
}

export function useCreateReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateReviewBody) => createReview(body),
    onSuccess: (_res, body) => {
      qc.invalidateQueries({ queryKey: ['place', body.placeId] }); // 상세·리뷰·집계 함께
      qc.invalidateQueries({ queryKey: ['review', 'diningHistory'] });
    },
  });
}

export function useUpdateReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { reviewId: number; body: UpdateReviewBody }) => updateReview(vars.reviewId, vars.body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['place'] }); // 모든 식당 상세·리뷰·집계
      qc.invalidateQueries({ queryKey: ['review', 'diningHistory'] });
    },
  });
}

export function useDeleteReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reviewId: number) => deleteReview(reviewId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['place'] });
      qc.invalidateQueries({ queryKey: ['review', 'diningHistory'] });
    },
  });
}
