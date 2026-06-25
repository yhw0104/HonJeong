import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { createReview, listPlaceReviews, fetchPlaceReviewSummary, fetchDiningHistory, type CreateReviewBody } from './api';

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
