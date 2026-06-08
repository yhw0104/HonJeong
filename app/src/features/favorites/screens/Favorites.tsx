// Favorites — 즐겨찾기 (원본: screens/Favorites.jsx)
// 그룹목록 ↔ 그룹상세(식당목록)를 useState로 토글(화면 내 전환). "새 그룹 만들기"는 NewGroup 모달.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { MainTabScreenProps } from '@/navigation/types';

type Group = { id: string; name: string; note: string; emojis: string[] };
type Place = { n: string; cat: string; dist: string; addr: string; visited: boolean };

const GROUPS: Group[] = [
  { id: 'wish', name: '가보고 싶은 곳', note: '혼밥 도전 리스트', emojis: ['🍜', '🍣', '🥘'] },
  { id: 'regular', name: '혼밥 단골', note: '편하게 가는 곳', emojis: ['🍲', '🍱'] },
  { id: 'office', name: '회사 근처 점심', note: '연남 · 합정', emojis: ['🍙', '🍛', '🍔'] },
];

const PLACES: Record<string, Place[]> = {
  wish: [
    { n: '혼밥의자', cat: '일식', dist: '650m', addr: '서대문구 연희로11가길 22 1층', visited: false },
    { n: '연남 파스타바', cat: '양식', dist: '320m', addr: '마포구 동교로 38-12 2층', visited: false },
  ],
  regular: [
    { n: '큰순두부 연남점', cat: '한식', dist: '120m', addr: '마포구 성미산로 161-4', visited: true },
    { n: '옥상국밥', cat: '한식', dist: '480m', addr: '마포구 양화로 64 3층', visited: true },
  ],
  office: [{ n: '큰순두부 연남점', cat: '한식', dist: '120m', addr: '마포구 성미산로 161-4', visited: true }],
};

export function FavoritesScreen({ navigation }: MainTabScreenProps<'Favorites'>) {
  const [openGroup, setOpenGroup] = useState<string | null>(null);
  const current = GROUPS.find((g) => g.id === openGroup);

  return (
    <Screen bg={T2.bg} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.h1}>즐겨찾기</Text>
        <View style={styles.segment}>
          <Text style={styles.segLabel}>내 장소</Text>
          <Text style={styles.segCount}>{GROUPS.length}</Text>
          <View style={styles.segUnderline} />
        </View>
      </View>
      <View style={styles.divider} />

      <ScrollView>
        {openGroup === null ? (
          <View>
            {GROUPS.map((g) => {
              const count = (PLACES[g.id] || []).length;
              return (
                <Pressable key={g.id} style={styles.groupRow} onPress={() => setOpenGroup(g.id)}>
                  <View style={styles.groupThumb}>
                    <Text style={{ fontSize: 22 }}>{g.emojis[0]}</Text>
                  </View>
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Text style={styles.groupName}>{g.name}</Text>
                    <View style={styles.metaRow}>
                      <Text style={styles.metaStrong}>{count}곳</Text>
                      <Text style={styles.metaDot}>·</Text>
                      <Text style={styles.metaSub} numberOfLines={1}>
                        {g.note}
                      </Text>
                    </View>
                  </View>
                  <Icon name="chevronRight" size={18} color={T2.textMute} />
                </Pressable>
              );
            })}

            {/* 새 그룹 만들기 */}
            <Pressable style={styles.newRow} onPress={() => navigation.navigate('NewGroup')}>
              <View style={styles.newPlus}>
                <Text style={styles.newPlusText}>+</Text>
              </View>
              <Text style={styles.newLabel}>새 그룹 만들기</Text>
            </Pressable>
          </View>
        ) : (
          <View>
            <Pressable style={styles.backRow} onPress={() => setOpenGroup(null)}>
              <Icon name="chevronLeft" size={18} color={T2.text} />
              <Text style={styles.backTitle}>{current?.name}</Text>
              <Text style={styles.backCount}>{(PLACES[openGroup] || []).length}</Text>
            </Pressable>
            {(PLACES[openGroup] || []).map((p, i) => (
              <View key={i} style={styles.placeRow}>
                <View style={styles.placeThumb}>
                  <Text style={{ fontSize: 20 }}>🍽</Text>
                </View>
                <View style={{ flex: 1, minWidth: 0 }}>
                  <View style={styles.placeNameRow}>
                    <Text style={styles.placeName} numberOfLines={1}>
                      {p.n}
                    </Text>
                    {p.visited ? (
                      <View style={styles.visitedBadge}>
                        <Text style={styles.visitedText}>다녀옴</Text>
                      </View>
                    ) : null}
                  </View>
                  <View style={styles.metaRow}>
                    <Text style={styles.metaSub}>{p.cat}</Text>
                    <Text style={styles.metaDot}>·</Text>
                    <Text style={styles.metaStrong}>{p.dist}</Text>
                  </View>
                  <Text style={styles.placeAddr} numberOfLines={1}>
                    {p.addr}
                  </Text>
                </View>
              </View>
            ))}
          </View>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: 20, paddingTop: 12 },
  h1: { fontSize: 28, fontWeight: '800', color: T2.text, letterSpacing: -1 },
  segment: { marginTop: 18, flexDirection: 'row', alignItems: 'center', gap: 6, paddingBottom: 12, alignSelf: 'flex-start' },
  segLabel: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  segCount: { fontSize: 12, fontWeight: '700', color: T2.brand },
  segUnderline: { position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, backgroundColor: T2.brand },
  divider: { height: 1, backgroundColor: T2.border },

  groupRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingVertical: 14,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  groupThumb: {
    width: 52,
    height: 52,
    borderRadius: 12,
    backgroundColor: T2.mapBg,
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  groupName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 4 },
  metaStrong: { fontSize: 12, fontWeight: '700', color: T2.text },
  metaDot: { fontSize: 12, color: T2.textMute },
  metaSub: { flex: 1, fontSize: 12, color: T2.textSub },

  newRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 18, paddingHorizontal: 20 },
  newPlus: {
    width: 40,
    height: 40,
    borderRadius: 10,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: T2.borderStrong,
    alignItems: 'center',
    justifyContent: 'center',
  },
  newPlusText: { color: T2.textSub, fontSize: 22, fontWeight: '300' },
  newLabel: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },

  backRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  backTitle: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  backCount: { fontSize: 12, fontWeight: '700', color: T2.brand, marginLeft: 2 },

  placeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingVertical: 14,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  placeThumb: {
    width: 52,
    height: 52,
    borderRadius: 12,
    backgroundColor: T2.mapBg,
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeNameRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  placeName: { flexShrink: 1, fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  visitedBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  visitedText: { fontSize: 10, fontWeight: '700', color: T2.textSub },
  placeAddr: { fontSize: 12, color: T2.textMute, marginTop: 4, letterSpacing: -0.2 },
});
