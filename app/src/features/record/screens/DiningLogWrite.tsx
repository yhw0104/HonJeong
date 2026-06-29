// DiningLogWrite — 혼밥 인증 작성(저널) (원본: screens/DiningLogWrite.jsx)
// 내 혼밥 기록의 '일기 쓰기'에서 모달로 진입. 별점/친화태그/본문 기록.
import React, { useState } from 'react';
import { Alert, Image, View, Text, TextInput, Pressable, ScrollView, StyleSheet } from 'react-native';
import { pickImages, uploadImages, remainingSlots, type PickedAsset } from '@/shared/upload/imageUpload';
import { Screen } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useCreateReview, useUpdateReview } from '@/features/review/queries';
import { buildReviewBody } from '@/features/review/reviewEdit';
import { reviewErrorMessage } from '@/features/review/reviewCopy';

const FRIENDLY = ['1인석 많음', '바테이블', '칸막이', '눈치 없음', '오래 OK'];

function Stars({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  return (
    <View style={{ flexDirection: 'row', gap: 4 }}>
      {[1, 2, 3, 4, 5].map((s) => (
        <Pressable key={s} onPress={() => onChange(s)} hitSlop={4}>
          <Text style={{ fontSize: 24, color: s <= value ? T2.brand : T2.border }}>★</Text>
        </Pressable>
      ))}
    </View>
  );
}

export function DiningLogWriteScreen({ navigation, route }: RootStackScreenProps<'DiningLogWrite'>) {
  const { placeId, placeName, checkInId, reviewId, initial } = route.params;
  const isEdit = reviewId != null;
  const createMut = useCreateReview();
  const updateMut = useUpdateReview();
  const [taste, setTaste] = useState(initial?.taste ?? 0);
  const [honbab, setHonbab] = useState(initial?.honbab ?? 0);
  const [tags, setTags] = useState<string[]>(initial?.tags ?? []);
  const [body, setBody] = useState(initial?.content ?? '');
  const [photos, setPhotos] = useState<PickedAsset[]>(
    () => (initial?.photos ?? []).map((url) => ({ uri: url, assetId: null })),
  );
  const [uploading, setUploading] = useState(false);
  const MAX_PHOTOS = 5;

  const canSave = taste >= 1 && honbab >= 1 && !createMut.isPending && !updateMut.isPending && !uploading;

  const toggleTag = (t: string) => setTags((prev) => (prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]));

  const onAddPhotos = async () => {
    const picked = await pickImages(remainingSlots(photos.length, MAX_PHOTOS));
    if (picked.length === 0) return;
    // 이미 추가한 사진(assetId 동일)은 제외 — 같은 사진 중복 추가 방지.
    const existingIds = new Set(photos.map((p) => p.assetId).filter((id): id is string => id != null));
    const fresh = picked.filter((a) => !(a.assetId && existingIds.has(a.assetId)));
    if (fresh.length === 0) {
      Alert.alert('이미 추가한 사진이에요', '같은 사진은 다시 추가할 수 없어요.');
      return;
    }
    setUploading(true);
    try {
      const urls = await uploadImages(fresh.map((a) => a.uri));
      const added: PickedAsset[] = urls.map((url, i) => ({ uri: url, assetId: fresh[i]?.assetId ?? null }));
      setPhotos((prev) => [...prev, ...added].slice(0, MAX_PHOTOS));
    } catch (e) {
      Alert.alert('사진 업로드 실패', e instanceof Error ? e.message : '다시 시도해주세요.');
    } finally {
      setUploading(false);
    }
  };
  const removePhoto = (idx: number) => setPhotos((prev) => prev.filter((_, i) => i !== idx));

  const onSave = () => {
    if (!canSave) return;
    const reviewBody = buildReviewBody({ taste, honbab, tags, body, photos: photos.map((p) => p.uri) });
    const onSuccess = () => navigation.goBack();
    const onError = (e: unknown) => Alert.alert('저장 실패', reviewErrorMessage(e));
    if (isEdit) {
      updateMut.mutate({ reviewId: reviewId!, body: reviewBody }, { onSuccess, onError });
    } else {
      createMut.mutate({ placeId, checkInId, ...reviewBody }, { onSuccess, onError });
    }
  };

  return (
    <Screen bg={T2.bg}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.close}>닫기</Text>
        </Pressable>
        <Pressable onPress={onSave} hitSlop={10} disabled={!canSave}>
          <Text style={[styles.saveBtn, !canSave && { opacity: 0.4 }]}>저장</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {/* 타이틀 */}
        <Text style={styles.h1}>{isEdit ? '리뷰 수정' : '오늘의 혼밥 기록'}</Text>

        {/* 장소 */}
        <View style={styles.placeRow}>
          <View style={styles.placeDot} />
          <Text style={styles.placeName}>{placeName}</Text>
        </View>

        {/* 별점 카드 */}
        <View style={styles.ratingCard}>
          <View style={styles.ratingRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.ratingTitle}>다시 방문하고 싶은 곳인가요?</Text>
              <Text style={styles.ratingSub}>가게 리뷰 별점에 반영돼요</Text>
            </View>
            <Stars value={taste} onChange={setTaste} />
          </View>

          <View style={styles.ratingHr} />

          <View style={styles.ratingRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.ratingTitle}>혼밥하기는 어땠나요?</Text>
              <Text style={styles.ratingSub}>혼밥 친화도에 반영돼요</Text>
            </View>
            <Stars value={honbab} onChange={setHonbab} />
          </View>

          {/* 친화 요소 태그 */}
          <View style={[styles.chipWrap, { marginTop: 16 }]}>
            {FRIENDLY.map((t) => {
              const on = tags.includes(t);
              return (
                <Pressable
                  key={t}
                  onPress={() => toggleTag(t)}
                  style={[styles.tagChip, { backgroundColor: on ? T2.brand : '#fff', borderColor: on ? T2.brand : T2.border }]}
                >
                  <Text style={{ fontSize: 12, fontWeight: '600', color: on ? '#fff' : T2.textMute }}>{t}</Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {/* 사진 (본문 위 — 먼저 보이게) */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.label}>사진 ({photos.length}/{MAX_PHOTOS})</Text>

          {photos.length === 0 ? (
            /* 사진 없을 때: 가로 긴 추가 박스 */
            <Pressable onPress={onAddPhotos} disabled={uploading} style={styles.photoAddWide}>
              <Text style={styles.photoAddWideText}>
                {uploading ? '업로드 중…' : '＋  사진을 추가해주세요'}
              </Text>
            </Pressable>
          ) : (
            /* 사진 있을 때: 가로 스크롤 + 오른쪽 하단에 겹친 동그란 ＋ 버튼 */
            <View style={{ marginTop: 12 }}>
              <ScrollView
                horizontal
                showsHorizontalScrollIndicator={false}
                style={{ marginHorizontal: -2 }}
                contentContainerStyle={{ gap: 10, paddingHorizontal: 2 }}
              >
                {photos.map((p, i) => (
                  <View key={`${p.uri}-${i}`} style={styles.photoThumb}>
                    <Image source={{ uri: p.uri }} style={styles.photoImg} />
                    <Pressable onPress={() => removePhoto(i)} hitSlop={6} style={styles.photoRemove}>
                      <Text style={styles.photoRemoveX}>×</Text>
                    </Pressable>
                  </View>
                ))}
              </ScrollView>
              {photos.length < MAX_PHOTOS && (
                <Pressable onPress={onAddPhotos} disabled={uploading} hitSlop={6} style={styles.photoAddFab}>
                  <Text style={styles.photoAddFabText}>{uploading ? '…' : '＋'}</Text>
                </Pressable>
              )}
            </View>
          )}
        </View>

        {/* 본문 */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.label}>한 줄 기록</Text>
          <TextInput
            style={styles.bodyInput}
            value={body}
            onChangeText={setBody}
            multiline
            maxLength={1000}
            placeholder="오늘의 혼밥을 기록해보세요"
            placeholderTextColor={T2.textMute}
          />
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20 },
  close: { fontSize: 14, fontWeight: '600', color: T2.textSub, letterSpacing: -0.2 },
  saveBtn: { fontSize: 14, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },

  scroll: { paddingHorizontal: 20, paddingBottom: 40 },

  h1: { fontSize: 28, fontWeight: '800', color: T2.text, letterSpacing: -1, marginTop: 8, marginBottom: 16 },

  placeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 14,
    borderTopWidth: 1,
    borderTopColor: T2.border,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  placeDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: T2.brand },
  placeName: { flex: 1, fontSize: 14, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },

  label: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },

  ratingCard: { marginTop: 28, padding: 18, borderRadius: 14, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border },
  ratingRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  ratingTitle: { fontSize: 14, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  ratingSub: { fontSize: 11, color: T2.textMute, marginTop: 2 },
  ratingHr: { height: 1, backgroundColor: T2.border, marginVertical: 16 },
  tagChip: { paddingVertical: 7, paddingHorizontal: 12, borderRadius: 999, borderWidth: 1 },

  bodyInput: {
    marginTop: 14,
    fontSize: 17,
    color: T2.text,
    lineHeight: 28,
    letterSpacing: -0.3,
    textAlignVertical: 'top',
    minHeight: 120,
    padding: 0,
  },

  photoAddWide: { marginTop: 12, height: 88, borderRadius: 14, borderWidth: 1, borderColor: T2.border, borderStyle: 'dashed', alignItems: 'center', justifyContent: 'center', backgroundColor: '#fff' },
  photoAddWideText: { fontSize: 14, fontWeight: '600', color: T2.textMute, letterSpacing: -0.3 },
  photoThumb: { width: 120, height: 120, borderRadius: 14, overflow: 'hidden' },
  photoImg: { width: '100%', height: '100%' },
  photoRemove: { position: 'absolute', top: 5, right: 5, width: 24, height: 24, borderRadius: 12, backgroundColor: 'rgba(0,0,0,0.6)', alignItems: 'center', justifyContent: 'center' },
  photoRemoveX: { color: '#fff', fontSize: 16, lineHeight: 18 },
  photoAddFab: { position: 'absolute', right: 8, bottom: 8, width: 40, height: 40, borderRadius: 20, backgroundColor: T2.brand, borderWidth: 2, borderColor: '#fff', alignItems: 'center', justifyContent: 'center' },
  photoAddFabText: { color: '#fff', fontSize: 22, lineHeight: 24, marginTop: -1 },
});
