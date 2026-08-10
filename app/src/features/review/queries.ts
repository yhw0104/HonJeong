import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { createReview, updateReview, deleteReview, listPlaceReviews, fetchPlaceReviewSummary, fetchReviewContext, fetchDiningHistory, fetchMyReviews, fetchPlacePhotos, type CreateReviewBody, type UpdateReviewBody } from './api';

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

/**
 * '리뷰 쓰기'를 누르면 어느 화면을 열지 — 지금 쓰면 혼밥 인증으로 연결될 체크인이 있는가.
 *
 * 식당 상세에 들어올 때 미리 걸어 둔다(버튼을 누를 때 기다리지 않게). 체크인 상태는 수시로
 * 바뀌므로 캐시를 오래 들고 있지 않는다 — 화면에 머무는 동안 시작·종료할 수 있다.
 */
export function useReviewContext(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'review-context'],
    queryFn: () => fetchReviewContext(placeId),
    staleTime: 0,
  });
}

export function useDiningHistory() {
  return useQuery({
    queryKey: ['review', 'diningHistory'],
    queryFn: fetchDiningHistory,
  });
}

export function useMyReviews() {
  return useQuery({ queryKey: ['review', 'mine'], queryFn: fetchMyReviews });
}

export function useCreateReview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateReviewBody) => createReview(body),
    onSuccess: (_res, body) => {
      qc.invalidateQueries({ queryKey: ['place', body.placeId] }); // 상세·리뷰·집계 함께
      qc.invalidateQueries({ queryKey: ['review', 'diningHistory'] });
      qc.invalidateQueries({ queryKey: ['review', 'mine'] });
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
      qc.invalidateQueries({ queryKey: ['review', 'mine'] });
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
      qc.invalidateQueries({ queryKey: ['review', 'mine'] });
    },
  });
}

export function usePlacePhotos(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'photos'],
    queryFn: () => fetchPlacePhotos(placeId),
  });
}
