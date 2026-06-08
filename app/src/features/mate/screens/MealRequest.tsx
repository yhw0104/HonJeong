// MealRequest — 같이 먹기 신청 (원본: screens/MealRequest.jsx)
// 식당 상세에서 모달로 진입. 대상 선택 + 인사말(빠른 문구) + 보내기.
import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Pressable,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
} from 'react-native';
import { Screen, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const PEOPLE = [
  { name: '점심혼밥러', emo: '🍙', meta: '혼밥 32회 · 대화 OK', mate: true },
  { name: '조용한미식가', emo: '🍜', meta: '혼밥 18회 · 조용히', mate: false },
];
const QUICK = ['조용히 각자 먹어요 :)', '가볍게 대화 나눠요', '혼밥 입문이에요, 잘 부탁해요'];
const MAX = 40;

export function MealRequestScreen({ navigation, route }: RootStackScreenProps<'MealRequest'>) {
  const placeName = route.params?.name ?? '큰순두부 연남점';
  const [sel, setSel] = useState(0);
  const [greeting, setGreeting] = useState(QUICK[1]);

  return (
    <Screen bg={T2.bg}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {/* 상단 바 */}
        <View style={styles.topBar}>
          <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
            <Text style={styles.cancel}>취소</Text>
          </Pressable>
          <Text style={styles.topTitle}>같이 먹기 신청</Text>
          <View style={{ width: 28 }} />
        </View>

        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          {/* 식당 요약 */}
          <View style={styles.placeCard}>
            <View style={styles.placeThumb}>
              <Text style={{ fontSize: 22 }}>🍲</Text>
            </View>
            <View style={{ flex: 1, minWidth: 0 }}>
              <Text style={styles.placeName}>{placeName}</Text>
              <Text style={styles.placeMeta}>한식 · 120m · 마포구 성미산로 161-4</Text>
            </View>
          </View>

          {/* 누구에게 */}
          <View style={{ marginTop: 26 }}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>누구에게</Text>
              <Text style={styles.labelHint}>지금 혼밥 중 {PEOPLE.length}명</Text>
            </View>
            <View style={{ gap: 10, marginTop: 12 }}>
              {PEOPLE.map((p, i) => {
                const on = i === sel;
                return (
                  <Pressable
                    key={p.name}
                    onPress={() => setSel(i)}
                    style={[styles.personRow, { borderColor: on ? T2.brand : T2.border }]}
                  >
                    <View style={styles.personEmo}>
                      <Text style={{ fontSize: 21 }}>{p.emo}</Text>
                    </View>
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                        <Text style={styles.personName}>{p.name}</Text>
                        {p.mate ? (
                          <View style={styles.mateBadge}>
                            <Text style={styles.mateText}>메이트</Text>
                          </View>
                        ) : null}
                      </View>
                      <Text style={styles.personMeta}>{p.meta}</Text>
                    </View>
                    <View style={[styles.radio, { backgroundColor: on ? T2.brand : '#fff', borderColor: on ? T2.brand : T2.borderStrong }]}>
                      {on ? <Text style={styles.radioCheck}>✓</Text> : null}
                    </View>
                  </Pressable>
                );
              })}
            </View>
          </View>

          {/* 인사 한마디 */}
          <View style={{ marginTop: 26 }}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>인사 한마디</Text>
              <Text style={styles.labelHint}>
                {greeting.length} / {MAX}
              </Text>
            </View>
            <TextInput
              style={styles.greetingInput}
              value={greeting}
              onChangeText={(t) => setGreeting(t.slice(0, MAX))}
              multiline
              maxLength={MAX}
              placeholder="인사 한마디를 남겨보세요"
              placeholderTextColor={T2.textMute}
            />
            <View style={styles.quickWrap}>
              {QUICK.map((q) => {
                const on = q === greeting;
                return (
                  <Pressable
                    key={q}
                    onPress={() => setGreeting(q)}
                    style={[
                      styles.quickChip,
                      { backgroundColor: on ? T2.brandSoft : '#fff', borderColor: on ? 'rgba(255,90,31,0.2)' : T2.border },
                    ]}
                  >
                    <Text style={{ fontSize: 13, fontWeight: '600', color: on ? T2.brand : T2.textSub, letterSpacing: -0.3 }}>{q}</Text>
                  </Pressable>
                );
              })}
            </View>
          </View>
        </ScrollView>

        {/* 하단 고정 — 보내기 */}
        <View style={styles.ctaBar}>
          <Pressable style={styles.sendBtn} onPress={() => navigation.goBack()}>
            <Text style={styles.sendText}>같이 먹기 신청 보내기</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  topBar: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20 },
  cancel: { fontSize: 15, fontWeight: '600', color: T2.textSub, letterSpacing: -0.3 },
  topTitle: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },

  scroll: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 24 },

  placeCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 13,
    padding: 14,
    marginTop: 4,
    backgroundColor: '#fff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: T2.border,
  },
  placeThumb: { width: 50, height: 50, borderRadius: 12, backgroundColor: T2.mapBg, borderWidth: 1, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  placeName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  placeMeta: { fontSize: 12, color: T2.textSub, marginTop: 4 },

  labelRow: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between' },
  label: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },
  labelHint: { fontSize: 11, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },

  personRow: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14, borderRadius: 14, backgroundColor: '#fff', borderWidth: 1.5 },
  personEmo: { width: 44, height: 44, borderRadius: 22, backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  personName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  mateBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  mateText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  personMeta: { fontSize: 12, color: T2.textMute, marginTop: 4 },
  radio: { width: 24, height: 24, borderRadius: 12, borderWidth: 1.5, alignItems: 'center', justifyContent: 'center' },
  radioCheck: { color: '#fff', fontSize: 13, fontWeight: '800' },

  greetingInput: {
    marginTop: 12,
    paddingVertical: 14,
    paddingHorizontal: 16,
    minHeight: 64,
    borderRadius: 14,
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: T2.text,
    fontSize: 15,
    color: T2.text,
    lineHeight: 22,
    letterSpacing: -0.3,
    textAlignVertical: 'top',
  },
  quickWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 12 },
  quickChip: { paddingHorizontal: 13, paddingVertical: 8, borderRadius: 999, borderWidth: 1 },

  ctaBar: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 28, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: T2.border },
  sendBtn: {
    paddingVertical: 16,
    borderRadius: 12,
    backgroundColor: T2.brand,
    alignItems: 'center',
    shadowColor: T2.brand,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.28,
    shadowRadius: 18,
    elevation: 4,
  },
  sendText: { fontSize: 15, fontWeight: '800', color: '#fff', letterSpacing: -0.3 },
});
