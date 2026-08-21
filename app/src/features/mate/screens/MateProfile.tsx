// MateProfile — 메이트 프로필(다른 사람) (원본: screens/MateProfile.jsx)
// Mates 목록에서 진입. 프로필/통계/선호/성향 + 하단 CTA(메이트/같이먹기).
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, ActivityIndicator, Alert, Modal } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Screen, Avatar, Icon, PhotoViewer } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useUserProfile, useSendMateRequest, useDeleteMate } from '@/features/mate/queries';
import { mateErrorMessage } from '@/features/mate/mateCopy';
import { ageGenderLabel, DINING_STYLE_LABEL } from '@/shared/format';
import { useBlockUser } from '@/features/safety/queries';

export function MateProfileScreen({ navigation, route }: RootStackScreenProps<'MateProfile'>) {
  const { userId } = route.params;
  const { data: p, isLoading, isError } = useUserProfile(userId);
  const send = useSendMateRequest();
  const del = useDeleteMate();
  const blockMut = useBlockUser();

  // 우상단 … 메뉴: Alert 대신 버튼 아래 드롭다운 카드로 표시(백드롭 탭으로 닫기).
  const [menuOpen, setMenuOpen] = useState(false);
  const [photo, setPhoto] = useState<string | null>(null); // 프로필 사진 확대 뷰어 대상
  const insets = useSafeAreaInsets();

  const openReport = () => {
    setMenuOpen(false);
    navigation.navigate('ReportForm', {
      targetType: 'USER',
      targetId: userId,
      targetNickname: p?.nickname ?? '이 사용자',
    });
  };

  const confirmBlock = () => {
    setMenuOpen(false);
    const nickname = p?.nickname ?? '이 사용자';
    Alert.alert('차단', `${nickname}님을 차단할까요?\n서로의 프로필과 혼밥 현황이 보이지 않게 돼요.`, [
      { text: '취소', style: 'cancel' },
      {
        text: '차단',
        style: 'destructive',
        // 차단하면 이 프로필 자체가 404가 되므로 목록으로 되돌아간다.
        onPress: () =>
          blockMut.mutate(userId, {
            onSuccess: () => navigation.goBack(),
            onError: () => Alert.alert('차단 실패', '잠시 후 다시 시도해주세요.'),
          }),
      },
    ]);
  };

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

  // 문구는 shared/format의 DINING_STYLE_LABEL 한 곳에서만 정한다(내 프로필·선택 화면과 같은 문장).
  // 아이콘만 이 화면의 표현이라 여기서 붙인다. diningStyle이 없으면 이 블록 자체를 그리지 않는다.
  const diningStyleLabel =
    p.diningStyle === 'TALK' || p.diningStyle === 'QUIET'
      ? { icon: 'chat' as const, ...DINING_STYLE_LABEL[p.diningStyle] }
      : null;

  const mealEnabled = p.isOnline && p.currentPlaceId != null;

  return (
    <Screen bg={T2.bg} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10} style={styles.headerBtn}>
          <Icon name="chevronLeft" size={22} color={T2.text} />
        </Pressable>
        <Pressable onPress={() => setMenuOpen(true)} hitSlop={10} style={styles.headerBtn}>
          <View style={styles.dotsRow}>
            {[0, 1, 2].map((d) => (
              <View key={d} style={styles.dot} />
            ))}
          </View>
        </Pressable>
      </View>

      {/* … 드롭다운 메뉴 — 버튼 아래 카드, 바깥 탭으로 닫힘 */}
      <Modal visible={menuOpen} transparent animationType="fade" onRequestClose={() => setMenuOpen(false)}>
        <Pressable style={styles.menuBackdrop} onPress={() => setMenuOpen(false)}>
          <Pressable style={[styles.menuCard, { top: insets.top + 48 }]} onPress={() => {}}>
            <Pressable style={styles.menuItem} onPress={openReport}>
              <Text style={styles.menuItemText}>신고하기</Text>
            </Pressable>
            <View style={styles.menuDivider} />
            <Pressable style={styles.menuItem} onPress={confirmBlock}>
              <Text style={[styles.menuItemText, styles.menuItemDanger]}>차단하기</Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 프로필 헤더 */}
        <View style={styles.profile}>
          {/* 사진이 있을 때만 눌러서 크게 볼 수 있다 — 폴백(앱 아이콘)은 확대할 이유가 없다. */}
          <Pressable
            onPress={() => p.profileImageUrl && setPhoto(p.profileImageUrl)}
            disabled={!p.profileImageUrl}
            accessibilityRole={p.profileImageUrl ? 'button' : undefined}
            accessibilityLabel={p.profileImageUrl ? '프로필 사진 크게 보기' : undefined}
          >
            <Avatar uri={p.profileImageUrl} size={84} />
          </Pressable>
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
          {/* 상대가 어떤 사람인지 기본 신호: "20대 여성". 성향은 아래 전용 카드에서 보여준다. */}
          {ageGenderLabel(p.ageGroup, p.gender) ? (
            <Text style={styles.sub}>{ageGenderLabel(p.ageGroup, p.gender)}</Text>
          ) : null}
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
              <Icon name={diningStyleLabel.icon} size={22} color={T2.brand} />
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

      <PhotoViewer uri={photo} onClose={() => setPhoto(null)} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 12 },
  headerBtn: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  dotsRow: { flexDirection: 'row', gap: 3 },
  dot: { width: 3.5, height: 3.5, borderRadius: 2, backgroundColor: T2.text },

  menuBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.12)' },
  menuCard: {
    position: 'absolute',
    right: 12,
    minWidth: 148,
    backgroundColor: '#fff',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: T2.border,
    paddingVertical: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.12,
    shadowRadius: 16,
    elevation: 6,
  },
  menuItem: { paddingVertical: 13, paddingHorizontal: 16 },
  menuItemText: { fontSize: 14, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  menuItemDanger: { color: '#E1493F' },
  menuDivider: { height: 1, backgroundColor: T2.border, marginHorizontal: 8 },

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
