// 혼밥 친화도 별점(0~5) → 사람이 읽는 한 줄 문구. 리뷰가 없거나 점수 미상이면 '아직 평가가 없어요'.
export function soloFriendlyLabel(rating: number | null, reviewCount: number): string {
  if (reviewCount === 0 || rating == null) return '아직 평가가 없어요';
  if (rating >= 4.5) return '혼밥하기 아주 좋아요';
  if (rating >= 3.5) return '혼밥하기 좋아요';
  if (rating >= 2.5) return '혼밥하기 무난해요';
  return '혼밥은 조금 아쉬워요';
}
