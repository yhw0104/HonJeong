// MyReviews — 내가 쓴 리뷰(인증+일반 전체). 더보기 '내가 쓴 리뷰'에서 진입.
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, ActivityIndicator, Alert, Image } from 'react-native';
import { Screen, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useMyReviews, useDeleteReview } from '@/features/review/queries';
import type { MyReview } from '@/features/review/api';

export function MyReviewsScreen({ navigation }: RootStackScreenProps<'MyReviews'>) {
  const { data, isLoading, isError, isSuccess } = useMyReviews();
  const delMut = useDeleteReview();

  const confirmDelete = (reviewId: number) =>
    Alert.alert('리뷰 삭제', '이 리뷰를 삭제할까요? 되돌릴 수 없어요.', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: () => delMut.mutate(reviewId) },
    ]);

  const reviews = data?.reviews ?? [];

  if (isLoading)
    return (
      <Screen bg={T2.bg} edges={['top']}>
        <MoreHeader title="내가 쓴 리뷰" onBack={() => navigation.goBack()} />
        <View style={{ padding: 40, alignItems: 'center' }}>
          <ActivityIndicator color={T2.brand} />
        </View>
      </Screen>
    );

  if (isError)
    return (
      <Screen bg={T2.bg} edges={['top']}>
        <MoreHeader title="내가 쓴 리뷰" onBack={() => navigation.goBack()} />
        <View style={{ padding: 40 }}>
          <Text style={{ color: T2.textMute }}>리뷰를 불러오지 못했어요.</Text>
        </View>
      </Screen>
    );

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="내가 쓴 리뷰" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {isSuccess && reviews.length === 0 && (
          <Text style={{ padding: 24, color: T2.textMute, textAlign: 'center' }}>아직 쓴 리뷰가 없어요.</Text>
        )}

        <View style={{ gap: 10 }}>
          {reviews.map((r: MyReview) => (
            <Pressable
              key={r.reviewId}
              style={styles.card}
              onPress={() => navigation.navigate('RestaurantDetail', { placeId: r.placeId, name: r.placeName })}
            >
              <View style={{ flex: 1, minWidth: 0 }}>
                <View style={styles.topRow}>
                  <Text style={styles.place}>{r.placeName}</Text>
                  {r.authenticated && (
                    <View style={styles.authBadge}>
                      <Text style={styles.authBadgeText}>✓ 혼밥</Text>
                    </View>
                  )}
                </View>
                <Text style={styles.date}>{formatDot(r.createdAt)}</Text>

                {r.content ? <Text style={styles.note}>{r.content}</Text> : null}

                <View style={styles.ratingRow}>
                  <View style={styles.tasteChip}>
                    <Text style={styles.tasteText}>맛 ★ {r.tasteRating.toFixed(1)}</Text>
                  </View>
                  {/* 혼밥 별점은 혼밥 인증 리뷰만 갖는다 — 없으면 칩 자체를 안 그린다. */}
                  {r.soloFriendlyRating != null && (
                    <View style={styles.honbabChip}>
                      <Text style={styles.honbabText}>혼밥 ★ {r.soloFriendlyRating.toFixed(1)}</Text>
                    </View>
                  )}
                </View>

                {r.tags.length > 0 && (
                  <View style={styles.tagRow}>
                    {r.tags.map((tag) => (
                      <View key={tag} style={styles.tagChip}>
                        <Text style={styles.tagText}>#{tag}</Text>
                      </View>
                    ))}
                  </View>
                )}

                {r.imageUrls.length > 0 && (
                  <View style={styles.photoRow}>
                    {r.imageUrls.map((uri) => (
                      <Image key={uri} source={{ uri }} style={styles.photo} />
                    ))}
                  </View>
                )}

                <View style={styles.actionRow}>
                  <Pressable
                    hitSlop={6}
                    onPress={() =>
                      navigation.navigate('DiningLogWrite', {
                        placeId: r.placeId,
                        placeName: r.placeName,
                        reviewId: r.reviewId,
                        initial: {
                          taste: r.tasteRating,
                          honbab: r.soloFriendlyRating,
                          tags: r.tags,
                          content: r.content ?? '',
                          photos: r.imageUrls,
                        },
                      })
                    }
                  >
                    <Text style={styles.actionEdit}>수정</Text>
                  </Pressable>
                  <Pressable hitSlop={6} onPress={() => confirmDelete(r.reviewId)}>
                    <Text style={styles.actionDelete}>삭제</Text>
                  </Pressable>
                </View>
              </View>
            </Pressable>
          ))}
        </View>
      </ScrollView>
    </Screen>
  );
}

// ── 헬퍼 ──────────────────────────────────────────────────────────────────────

const formatDot = (iso: string) => {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`;
};

// ── 스타일 ────────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 40 },

  card: { flexDirection: 'row', gap: 14, padding: 14, backgroundColor: '#fff', borderRadius: 16, borderWidth: 1, borderColor: T2.border },
  topRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  place: { fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  date: { fontSize: 11, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 },
  note: { fontSize: 12, color: T2.textSub, lineHeight: 18, marginTop: 5, letterSpacing: -0.2 },
  ratingRow: { flexDirection: 'row', gap: 6, marginTop: 10 },
  tasteChip: { backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, paddingHorizontal: 7, paddingVertical: 3, borderRadius: 6 },
  tasteText: { fontSize: 11, fontWeight: '700', color: T2.textSub },
  honbabChip: { backgroundColor: T2.brandSoft, paddingHorizontal: 7, paddingVertical: 3, borderRadius: 6 },
  honbabText: { fontSize: 11, fontWeight: '700', color: T2.brand },

  authBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  authBadgeText: { fontSize: 10, fontWeight: '700', color: T2.brand },

  tagRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 },
  tagChip: { backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, paddingHorizontal: 7, paddingVertical: 3, borderRadius: 6 },
  tagText: { fontSize: 11, fontWeight: '600', color: T2.textSub },

  photoRow: { flexDirection: 'row', gap: 6, marginTop: 8 },
  photo: { width: 64, height: 64, borderRadius: 8 },

  actionRow: { flexDirection: 'row', gap: 16, marginTop: 10 },
  actionEdit: { fontSize: 12, fontWeight: '700', color: T2.textSub },
  actionDelete: { fontSize: 12, fontWeight: '700', color: '#d11' },
});
