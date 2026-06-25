// DiningLogWrite — 혼밥 인증 작성(저널) (원본: screens/DiningLogWrite.jsx)
// 내 혼밥 기록의 '일기 쓰기'에서 모달로 진입. 별점/친화태그/본문 기록.
import React, { useState } from 'react';
import { Alert, View, Text, TextInput, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useCreateReview } from '@/features/review/queries';
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
  const { placeId, placeName, checkInId } = route.params;
  const createMut = useCreateReview();
  const [taste, setTaste] = useState(0);
  const [honbab, setHonbab] = useState(0);
  const [tags, setTags] = useState<string[]>([]);
  const [body, setBody] = useState('');

  const canSave = taste >= 1 && honbab >= 1 && !createMut.isPending;

  const toggleTag = (t: string) => setTags((prev) => (prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]));

  const onSave = () => {
    if (!canSave) return;
    createMut.mutate(
      { placeId, checkInId, tasteRating: taste, soloFriendlyRating: honbab,
        content: body.trim() || undefined, tags },
      {
        onSuccess: () => navigation.goBack(),
        onError: (e) => Alert.alert('저장 실패', reviewErrorMessage(e)),
      },
    );
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
        <Text style={styles.h1}>오늘의 혼밥 기록</Text>

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

        {/* 본문 */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.label}>한 줄 기록</Text>
          <TextInput
            style={styles.bodyInput}
            value={body}
            onChangeText={setBody}
            multiline
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
});
