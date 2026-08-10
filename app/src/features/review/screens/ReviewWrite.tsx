// ReviewWrite — 혼밥 인증이 아닌 리뷰 작성/수정(같이먹기 후 · 체크인 없이 쓰는 경우).
//
// 왜 화면이 둘인가: 혼밥 적합도 별점과 친화 태그는 **혼밥 인증 리뷰만** 가질 수 있다. 그래야
// 식당의 혼밥 친화도가 혼자 먹어본 사람의 평가로만 채워진다(예전엔 같이 먹은 사람의 점수도
// 똑같은 한 표로 섞였다). 그 규칙을 사람에게 보여주는 형태가 이 화면이다 — 묻지 않는다.
//
// 어느 화면을 열지는 서버가 정한다(reviewWriteTarget · GET /places/{id}/review-context).
// 여기서 혼밥 별점을 채워 보내면 서버가 400으로 거절한다.
import React, { useRef, useState } from 'react';
import { Alert, View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { type PickedAsset } from '@/shared/upload/imageUpload';
import { Screen } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useCreateReview, useUpdateReview } from '../queries';
import { buildReviewBody } from '../reviewEdit';
import { reviewErrorMessage } from '../reviewCopy';
import { ReviewPhotoPicker } from '../components/ReviewPhotoPicker';
import { ReviewBodyInput } from '../components/ReviewBodyInput';

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

export function ReviewWriteScreen({ navigation, route }: RootStackScreenProps<'ReviewWrite'>) {
  const { placeId, placeName, reviewId, initial } = route.params;
  const isEdit = reviewId != null;
  const createMut = useCreateReview();
  const updateMut = useUpdateReview();
  const [taste, setTaste] = useState(initial?.taste ?? 0);
  const [body, setBody] = useState(initial?.content ?? '');
  const [photos, setPhotos] = useState<PickedAsset[]>(
    () => (initial?.photos ?? []).map((url) => ({ uri: url, assetId: null })),
  );
  const [uploading, setUploading] = useState(false);
  const scrollRef = useRef<ScrollView>(null);

  const canSave = taste >= 1 && !createMut.isPending && !updateMut.isPending && !uploading;

  const onSave = () => {
    if (!canSave) return;
    // ★keepSolo — 이 화면이 만드는 리뷰는 혼밥 값을 갖지 않지만, **예전에 쓰인** 인증 없는 리뷰에는
    // 값이 남아 있다(과거 데이터를 지우지 않기로 했다). 수정은 받은 값을 그대로 덮어쓰므로,
    // 화면에 띄우지 않은 채 들고 있다가 그대로 되돌려 보내야 그 값이 살아남는다.
    const reviewBody = buildReviewBody({
      taste,
      honbab: initial?.keepSolo?.honbab ?? null,
      tags: initial?.keepSolo?.tags ?? [],
      body,
      photos: photos.map((p) => p.uri),
    });
    const onSuccess = () => navigation.goBack();
    const onError = (e: unknown) => Alert.alert('저장 실패', reviewErrorMessage(e));
    if (isEdit) {
      updateMut.mutate({ reviewId: reviewId!, body: reviewBody }, { onSuccess, onError });
    } else {
      // checkInId를 보내지 않는다 — 서버는 스스로 체크인을 찾지 않으므로 일반 리뷰로 저장된다.
      createMut.mutate({ placeId, ...reviewBody }, { onSuccess, onError });
    }
  };

  return (
    <Screen bg={T2.bg}>
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.close}>닫기</Text>
        </Pressable>
        <Pressable onPress={onSave} hitSlop={10} disabled={!canSave}>
          <Text style={[styles.saveBtn, !canSave && { opacity: 0.4 }]}>저장</Text>
        </Pressable>
      </View>

      {/* automaticallyAdjustKeyboardInsets — 본문이 맨 아래라 키보드에 가린다(DiningLogWrite와 같은 이유). */}
      <ScrollView
        ref={scrollRef}
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        automaticallyAdjustKeyboardInsets
      >
        <Text style={styles.h1}>{isEdit ? '리뷰 수정' : '리뷰 쓰기'}</Text>

        <View style={styles.placeRow}>
          <View style={styles.placeDot} />
          <Text style={styles.placeName}>{placeName}</Text>
        </View>

        <View style={styles.ratingCard}>
          <View style={styles.ratingRow}>
            <View style={{ flex: 1 }}>
              <Text style={styles.ratingTitle}>다시 방문하고 싶은 곳인가요?</Text>
              <Text style={styles.ratingSub}>가게 리뷰 별점에 반영돼요</Text>
            </View>
            <Stars value={taste} onChange={setTaste} />
          </View>
        </View>

        {/* 혼밥 항목이 왜 없는지 알려준다 — 없는 이유를 모르면 빠진 것처럼 보인다. */}
        <Text style={styles.notice}>혼밥 친화도와 태그는 혼밥 인증을 하면 남길 수 있어요</Text>

        <ReviewPhotoPicker
          photos={photos}
          onChange={setPhotos}
          uploading={uploading}
          onUploadingChange={setUploading}
        />

        {/* 본문 — 화면의 마지막 내용이어야 한다(ReviewBodyInput이 끝까지 스크롤한다) */}
        <ReviewBodyInput value={body} onChangeText={setBody} scrollRef={scrollRef} />
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

  ratingCard: { marginTop: 28, padding: 18, borderRadius: 14, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border },
  ratingRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  ratingTitle: { fontSize: 14, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  ratingSub: { fontSize: 11, color: T2.textMute, marginTop: 2 },

  notice: { marginTop: 12, fontSize: 12, color: T2.textMute, letterSpacing: -0.2, lineHeight: 18 },
});
