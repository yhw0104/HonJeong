// MealRequest — 같이 먹기 신청 (식당상세에서 모달 진입)
// 모집중(같이 먹을 사람 구하는 중) 목록(useSeekers)에서 한 명 선택 + 인사말 → POST /meal-requests.
import React, { useState } from 'react';
import {
  View, Text, TextInput, Pressable, ScrollView, ActivityIndicator,
  KeyboardAvoidingView, Platform, Alert, StyleSheet,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Screen, Icon, Avatar } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useSeekers } from '@/features/checkin/queries';
import { useCreateMealRequest } from '@/features/meal/queries';
import { mealErrorMessage } from '@/features/meal/mealCopy';
import { formatElapsed } from '@/shared/format';
import type { RootStackScreenProps } from '@/navigation/types';

const QUICK = ['조용히 각자 먹어요 :)', '가볍게 대화 나눠요', '혼밥 입문이에요, 잘 부탁해요'];
const MAX = 40;

export function MealRequestScreen({ navigation, route }: RootStackScreenProps<'MealRequest'>) {
  const { placeId, placeName } = route.params;
  const seekers = useSeekers(placeId);
  const create = useCreateMealRequest();
  const insets = useSafeAreaInsets();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [greeting, setGreeting] = useState(QUICK[1]);

  const list = seekers.data ?? [];
  const send = () => {
    if (selectedId == null) return;
    create.mutate(
      { toCheckInId: selectedId, message: greeting.trim() || undefined },
      {
        onSuccess: () => {
          Alert.alert('신청을 보냈어요', '상대가 수락하면 같이먹기 탭에서 확인할 수 있어요.', [
            { text: '확인', onPress: () => navigation.goBack() },
          ]);
        },
        onError: (e) => Alert.alert('신청 실패', mealErrorMessage(e)),
      },
    );
  };

  return (
    // bottom edge는 Screen이 아니라 아래 CTA바가 직접 처리한다 — SafeAreaView가 아래 여백을 잡으면
    // 흰 버튼바 밑에 크림색(T2.bg) 띠가 남아 두 톤으로 갈린다(ChatRoom과 같은 처리).
    <Screen bg={T2.bg} edges={['top', 'left', 'right']}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.topBar}>
          <Pressable onPress={() => navigation.goBack()} hitSlop={10} accessibilityRole="button">
            <Text style={styles.cancel}>취소</Text>
          </Pressable>
          <Text style={styles.topTitle}>같이 먹기 신청</Text>
          <View style={{ width: 28 }} />
        </View>

        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled" keyboardDismissMode="on-drag">
          <View style={styles.placeCard}>
            {/* 이모지 대신 브랜드 핀 타일 — 받은 신청 카드의 핀 아이콘과 같은 언어. */}
            <View style={styles.placeThumb}><Icon name="pin" size={22} color={T2.brand} /></View>
            <View style={{ flex: 1, minWidth: 0 }}>
              <Text style={styles.placeName}>{placeName}</Text>
              <Text style={styles.placeMeta}>지금 모집 중 {list.length}명</Text>
            </View>
          </View>

          <View style={{ marginTop: 26 }}>
            <Text style={styles.label}>누구에게</Text>
            {seekers.isLoading ? (
              <ActivityIndicator style={{ marginTop: 16 }} color={T2.brand} />
            ) : seekers.isError ? (
              <Text style={styles.stateText}>모집중 목록을 불러오지 못했어요.</Text>
            ) : list.length === 0 ? (
              <Text style={styles.stateText}>지금 모집 중인 사람이 없어요.</Text>
            ) : (
              <View style={{ gap: 10, marginTop: 12 }}>
                {list.map((d) => {
                  const on = d.checkInId === selectedId;
                  return (
                    <Pressable
                      key={d.checkInId}
                      onPress={() => setSelectedId(d.checkInId)}
                      style={[styles.personRow, { borderColor: on ? T2.brand : T2.border }]}
                      accessibilityRole="button"
                    >
                      <Avatar uri={d.profileImageUrl} size={44} />
                      <View style={{ flex: 1, minWidth: 0 }}>
                        <Text style={styles.personName}>{d.nickname}</Text>
                        <Text style={styles.personMeta}>{formatElapsed(d.elapsedMinutes)}</Text>
                      </View>
                      <View style={[styles.radio, { backgroundColor: on ? T2.brand : '#fff', borderColor: on ? T2.brand : T2.borderStrong }]}>
                        {on ? <Text style={styles.radioCheck}>✓</Text> : null}
                      </View>
                    </Pressable>
                  );
                })}
              </View>
            )}
          </View>

          <View style={{ marginTop: 26 }}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>인사 한마디</Text>
              <Text style={styles.labelHint}>{greeting.length} / {MAX}</Text>
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
                  <Pressable key={q} onPress={() => setGreeting(q)}
                    style={[styles.quickChip, { backgroundColor: on ? T2.brandSoft : '#fff', borderColor: on ? 'rgba(255,90,31,0.2)' : T2.border }]}>
                    <Text style={{ fontSize: 13, fontWeight: '600', color: on ? T2.brand : T2.textSub, letterSpacing: -0.3 }}>{q}</Text>
                  </Pressable>
                );
              })}
            </View>
          </View>
        </ScrollView>

        <View style={[styles.ctaBar, { paddingBottom: 16 + insets.bottom }]}>
          <Pressable
            style={[styles.sendBtn, { opacity: selectedId == null || create.isPending ? 0.5 : 1 }]}
            onPress={send}
            disabled={selectedId == null || create.isPending}
            accessibilityRole="button"
          >
            <Text style={styles.sendText}>{create.isPending ? '보내는 중…' : '같이 먹기 신청 보내기'}</Text>
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
  placeCard: { flexDirection: 'row', alignItems: 'center', gap: 13, padding: 14, marginTop: 4, backgroundColor: '#fff', borderRadius: 16, borderWidth: 1, borderColor: T2.border },
  placeThumb: { width: 46, height: 46, borderRadius: 12, backgroundColor: T2.brandSoft, alignItems: 'center', justifyContent: 'center' },
  placeName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  placeMeta: { fontSize: 12, color: T2.textSub, marginTop: 4 },
  labelRow: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between' },
  label: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },
  labelHint: { fontSize: 11, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
  stateText: { fontSize: 13, color: T2.textMute, marginTop: 14 },
  personRow: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14, borderRadius: 14, backgroundColor: '#fff', borderWidth: 1.5 },
  personName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  personMeta: { fontSize: 12, color: T2.textMute, marginTop: 4 },
  radio: { width: 24, height: 24, borderRadius: 12, borderWidth: 1.5, alignItems: 'center', justifyContent: 'center' },
  radioCheck: { color: '#fff', fontSize: 13, fontWeight: '800' },
  greetingInput: { marginTop: 12, paddingVertical: 14, paddingHorizontal: 16, minHeight: 64, borderRadius: 14, backgroundColor: '#fff', borderWidth: 1.5, borderColor: T2.text, fontSize: 15, color: T2.text, lineHeight: 22, letterSpacing: -0.3, textAlignVertical: 'top' },
  quickWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 12 },
  quickChip: { paddingHorizontal: 13, paddingVertical: 8, borderRadius: 999, borderWidth: 1 },
  // paddingBottom은 렌더에서 준다(16 + 하단 안전영역) — 흰색이 화면 맨 아래까지 이어지게.
  ctaBar: { paddingHorizontal: 16, paddingTop: 12, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: T2.border },
  sendBtn: { paddingVertical: 16, borderRadius: 12, backgroundColor: T2.brand, alignItems: 'center' },
  sendText: { fontSize: 15, fontWeight: '800', color: '#fff', letterSpacing: -0.3 },
});
