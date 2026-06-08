// DiningLogWrite — 혼밥 인증 작성(저널) (원본: screens/DiningLogWrite.jsx)
// 내 혼밥 기록의 '일기 쓰기'에서 모달로 진입. 사진/기분/별점/친화태그/한 줄 기록.
import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, ImagePlaceholder } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const MOODS = ['편안', '행복', '맛있음', '집중', '조금 어색', '쓸쓸'];
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

export function DiningLogWriteScreen({ navigation }: RootStackScreenProps<'DiningLogWrite'>) {
  const [mood, setMood] = useState('편안');
  const [taste, setTaste] = useState(4);
  const [honbab, setHonbab] = useState(5);
  const [tags, setTags] = useState<string[]>(['1인석 많음', '바테이블', '눈치 없음']);
  const [body, setBody] = useState(
    '창가 바테이블에서 순두부 한 그릇.\n점심에 1인석이 절반이나 비어있어서 눈치 볼 일이 없었다.\n다음엔 비빔밥도 시켜봐야지.'
  );

  const toggleTag = (t: string) => setTags((prev) => (prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]));

  return (
    <Screen bg={T2.bg}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.close}>닫기</Text>
        </Pressable>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.saveBtn}>저장</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {/* 날짜/메타 + 타이틀 */}
        <Text style={styles.meta}>2026.05.22 · FRI · 12:34</Text>
        <Text style={styles.h1}>
          오늘의{'\n'}
          <Text style={{ color: T2.brand }}>32번째</Text> 혼밥
        </Text>

        {/* 장소 */}
        <View style={styles.placeRow}>
          <View style={styles.placeDot} />
          <Text style={styles.placeName}>큰순두부 연남점</Text>
          <Text style={styles.placeMeta}>마포구 · 한식</Text>
        </View>

        {/* 사진 */}
        <View style={styles.photoRow}>
          <View style={{ flex: 2 }}>
            <ImagePlaceholder w="100%" h={232} radius={10} tag="순두부 한 그릇" />
          </View>
          <View style={{ flex: 1, gap: 6 }}>
            <ImagePlaceholder w="100%" h={113} radius={10} tag="2" />
            <View style={styles.addPhoto}>
              <Text style={styles.addPhotoText}>+</Text>
            </View>
          </View>
        </View>

        {/* 기분 */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.label}>오늘의 기분</Text>
          <View style={styles.chipWrap}>
            {MOODS.map((m) => {
              const on = mood === m;
              return (
                <Pressable
                  key={m}
                  onPress={() => setMood(m)}
                  style={[styles.moodChip, { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border }]}
                >
                  <Text style={{ fontSize: 13, fontWeight: '600', color: on ? '#fff' : T2.text }}>{m}</Text>
                </Pressable>
              );
            })}
          </View>
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

  meta: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 1 },
  h1: { fontSize: 36, fontWeight: '800', color: T2.text, letterSpacing: -1.2, marginTop: 4, lineHeight: 40 },

  placeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 14,
    marginTop: 20,
    borderTopWidth: 1,
    borderTopColor: T2.border,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  placeDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: T2.brand },
  placeName: { flex: 1, fontSize: 14, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  placeMeta: { fontSize: 12, color: T2.textMute },

  photoRow: { flexDirection: 'row', gap: 6, marginTop: 20 },
  addPhoto: {
    height: 113,
    borderRadius: 10,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: T2.borderStrong,
    alignItems: 'center',
    justifyContent: 'center',
  },
  addPhotoText: { fontSize: 22, color: T2.textMute },

  label: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  moodChip: { paddingVertical: 9, paddingHorizontal: 14, borderRadius: 999, borderWidth: 1 },

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
