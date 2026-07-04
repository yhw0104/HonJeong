// MateProfile — 메이트 프로필(다른 사람) (원본: screens/MateProfile.jsx)
// Mates 목록에서 진입. 프로필/통계/선호/성향 + 하단 CTA(메이트/같이먹기).
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { Screen, Avatar, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useUserProfile, useSendMateRequest, useDeleteMate } from '@/features/mate/queries';
import { mateErrorMessage } from '@/features/mate/mateCopy';

export function MateProfileScreen({ navigation, route }: RootStackScreenProps<'MateProfile'>) {
  const { userId } = route.params;
  const { data: p, isLoading, isError } = useUserProfile(userId);
  const send = useSendMateRequest();
  const del = useDeleteMate();

  if (isLoading) {
    return (
      <Screen bg={T2.bg} edges={['top']}>
        <View style={styles.header}>
          <Pressable onPress={() => navigation.goBack()} hitSlop={10} style={styles.headerBtn}>
            <Icon name="chevronLeft" size={22} color={T2.text} />
          </Pressable>
        </View>
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
          <ActivityIndicator color={T2.brand} />
        </View>
      </Screen>
    );
  }

  if (isError || !p) {
    return (
      <Screen bg={T2.bg} edges={['top']}>
        <View style={styles.header}>
          <Pressable onPress={() => navigation.goBack()} hitSlop={10} style={styles.headerBtn}>
            <Icon name="chevronLeft" size={22} color={T2.text} />
          </Pressable>
        </View>
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
          <Text style={{ color: T2.textMute, fontSize: 14 }}>프로필을 불러올 수 없어요.</Text>
        </View>
      </Screen>
    );
  }

  const diningStyleLabel =
    p.diningStyle === 'TALK'
      ? { emoji: '💬', title: '도란도란 대화하며', sub: '가볍게 이야기 나누는 게 좋아요' }
      : p.diningStyle === 'QUIET'
      ? { emoji: '🤫', title: '조용히 각자 편하게', sub: '말 없이 각자 편안하게 드세요' }
      : null;

  const mealEnabled = p.isOnline && p.currentPlaceId != null;

  return (
    <Screen bg={T2.bg} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10} style={styles.headerBtn}>
          <Icon name="chevronLeft" size={22} color={T2.text} />
        </Pressable>
        <View style={styles.headerBtn}>
          <View style={styles.dotsRow}>
            {[0, 1, 2].map((d) => (
              <View key={d} style={styles.dot} />
            ))}
          </View>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 프로필 헤더 */}
        <View style={styles.profile}>
          <Avatar uri={p.profileImageUrl} name={p.nickname ?? ''} size={84} />
          <View style={styles.nameRow}>
            <Text style={styles.name}>{p.nickname ?? '(이름 없음)'}</Text>
            {p.mealsTogether > 0 && (
              <View style={styles.togetherBadge}>
                <Text style={styles.togetherText}>같이 {p.mealsTogether}회</Text>
              </View>
            )}
          </View>
          {p.isOnline && (
            <View style={styles.nowPill}>
              <View style={styles.nowDot} />
              <Text style={styles.nowText}>지금 혼밥 중</Text>
              {p.currentPlaceName ? (
                <Text style={styles.nowPlace}>· {p.currentPlaceName}</Text>
              ) : null}
            </View>
          )}
          {/* 내 동네 표기는 제거(설정 기능 없음) — 성향은 아래 전용 카드에서 이미 보여준다. */}
          {p.introduction ? <Text style={styles.bio}>{p.introduction}</Text> : null}
        </View>

        {/* 통계 */}
        <View style={styles.statsCard}>
          <View style={styles.statCell}>
            <Text style={styles.statNum}>{p.checkInCount}</Text>
            <Text style={styles.statLabel}>혼밥</Text>
          </View>
          <View style={[styles.statCell, styles.statDivider]}>
            <Text style={[styles.statNum, { color: T2.brand }]}>{p.mealsTogether}</Text>
            <Text style={styles.statLabel}>함께 먹음</Text>
          </View>
          <View style={[styles.statCell, styles.statDivider]}>
            <Text style={styles.statNum}>{p.badgeCount}</Text>
            <Text style={styles.statLabel}>뱃지</Text>
          </View>
        </View>

        {/* 좋아하는 음식 */}
        {p.preferredFoods.length > 0 && (
          <View style={{ marginTop: 28 }}>
            <Text style={styles.sectionLabel}>좋아하는 음식</Text>
            <View style={styles.chipWrap}>
              {p.preferredFoods.map((f) => (
                <View key={f} style={styles.foodChip}>
                  <Text style={styles.foodText}>{f}</Text>
                </View>
              ))}
            </View>
          </View>
        )}

        {/* 같이 먹을 때 */}
        {diningStyleLabel && (
          <View style={{ marginTop: 28 }}>
            <Text style={styles.sectionLabel}>같이 먹을 때</Text>
            <View style={styles.styleCard}>
              <Text style={{ fontSize: 22 }}>{diningStyleLabel.emoji}</Text>
              <View>
                <Text style={styles.styleTitle}>{diningStyleLabel.title}</Text>
                <Text style={styles.styleSub}>{diningStyleLabel.sub}</Text>
              </View>
            </View>
          </View>
        )}
      </ScrollView>

      {/* 하단 CTA */}
      <View style={styles.ctaBar}>
        {/* 좌측: 메이트 관계 상태별 버튼 */}
        {p.isMate ? (
          <Pressable
            style={styles.mateBtn}
            disabled={del.isPending}
            onPress={() =>
              del.mutate(p.userId, {
                onError: (err) => Alert.alert('오류', mateErrorMessage(err)),
              })
            }
          >
            <Text style={{ color: T2.brand, fontSize: 16 }}>✓</Text>
            <Text style={styles.mateBtnText}>{del.isPending ? '처리 중…' : '메이트 해제'}</Text>
          </Pressable>
        ) : p.requestStatus === 'PENDING_SENT' ? (
          <Pressable style={styles.mateBtn} disabled>
            <Text style={styles.mateBtnText}>신청함</Text>
          </Pressable>
        ) : p.requestStatus === 'PENDING_RECEIVED' ? (
          <Pressable style={styles.mateBtn} disabled>
            <Text style={styles.mateBtnText}>메이트 신청 받음</Text>
          </Pressable>
        ) : (
          <Pressable
            style={styles.mateBtn}
            disabled={send.isPending}
            onPress={() =>
              send.mutate(p.userId, {
                onError: (err) => Alert.alert('오류', mateErrorMessage(err)),
              })
            }
          >
            <Text style={styles.mateBtnText}>{send.isPending ? '신청 중…' : '+ 메이트 신청'}</Text>
          </Pressable>
        )}

        {/* 우측: 같이 먹기 신청 */}
        <Pressable
          style={[styles.mealBtn, !mealEnabled && styles.mealBtnDisabled]}
          disabled={!mealEnabled}
          onPress={() => {
            if (mealEnabled) {
              navigation.navigate('MealRequest', {
                placeId: p.currentPlaceId!,
                placeName: p.currentPlaceName ?? '',
              });
            }
          }}
          accessibilityRole="button"
        >
          <Text style={[styles.mealBtnText, !mealEnabled && styles.mealBtnTextDisabled]}>
            같이 먹기 신청
          </Text>
        </Pressable>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 12 },
  headerBtn: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  dotsRow: { flexDirection: 'row', gap: 3 },
  dot: { width: 3.5, height: 3.5, borderRadius: 2, backgroundColor: T2.text },

  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 24 },

  profile: { alignItems: 'center', paddingTop: 8 },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 14 },
  name: { fontSize: 22, fontWeight: '800', color: T2.text, letterSpacing: -0.6 },
  togetherBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  togetherText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  nowPill: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 10, paddingVertical: 6, paddingHorizontal: 12, borderRadius: 999, backgroundColor: 'rgba(34,166,90,0.1)' },
  nowDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: C.open },
  nowText: { fontSize: 12, fontWeight: '700', color: C.openDark, letterSpacing: -0.2 },
  nowPlace: { fontSize: 12, color: T2.textSub, letterSpacing: -0.2 },
  sub: { fontSize: 13, color: T2.textMute, marginTop: 10, letterSpacing: -0.2 },
  bio: { fontSize: 14, color: T2.textSub, marginTop: 14, lineHeight: 22, letterSpacing: -0.3, textAlign: 'center', maxWidth: 280 },

  statsCard: { flexDirection: 'row', marginTop: 24, paddingVertical: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  statCell: { flex: 1, alignItems: 'center' },
  statDivider: { borderLeftWidth: 1, borderLeftColor: T2.border },
  statNum: { fontSize: 22, fontWeight: '800', letterSpacing: -0.6, color: T2.text },
  statLabel: { fontSize: 11, color: T2.textMute, marginTop: 4 },

  sectionLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginBottom: 12 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  foodChip: { paddingHorizontal: 14, paddingVertical: 9, borderRadius: 999, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.borderStrong },
  foodText: { fontSize: 13, fontWeight: '600', color: T2.text, letterSpacing: -0.2 },

  styleCard: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderRadius: 14, backgroundColor: T2.text },
  styleTitle: { fontSize: 15, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  styleSub: { fontSize: 12, color: 'rgba(255,255,255,0.6)', marginTop: 2 },

  ctaBar: { flexDirection: 'row', gap: 10, paddingHorizontal: 16, paddingTop: 12, paddingBottom: 28, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: T2.border },
  mateBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 18, paddingVertical: 16, borderRadius: 12, backgroundColor: T2.bg },
  mateBtnText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  mealBtn: { flex: 1, paddingVertical: 16, borderRadius: 12, backgroundColor: T2.brand, alignItems: 'center' },
  mealBtnDisabled: { backgroundColor: T2.border },
  mealBtnText: { fontSize: 15, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  mealBtnTextDisabled: { color: T2.textMute },
});
