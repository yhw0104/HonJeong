// MapHome — 지도/홈 (원본: screens/MapHome.jsx)
// MapBackground 플레이스홀더 위에 핀/검색바/라이브카운터/하단시트를 오버레이.
// 하단 탭바는 MainTabs 네비게이터가 렌더하므로 여기서는 그리지 않는다.
import React, { useState } from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MapBackground, MiniPin, Icon, HonbabStatusBar } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { MainTabScreenProps } from '@/navigation/types';

const PINS = [
  { x: 120, y: 200, label: '큰순두부', mate: true },
  { x: 250, y: 160, active: true },
  { x: 180, y: 300 },
  { x: 290, y: 360, label: '혼밥하우스 · 메이트 1', mate: true },
  { x: 90, y: 380 },
  { x: 320, y: 460 },
];

type ListItem = { n: string; d: string; tag?: string; tagOn?: boolean };

const LIST: ListItem[] = [
  { n: '큰순두부 연남점', d: '한식 · 120m', tag: '메이트 2명', tagOn: true },
  { n: '혼밥의자', d: '일식 · 180m' },
  { n: '옥상국밥', d: '한식 · 240m' },
];

// 혼밥 시작 시트의 근처 식당 목록
const NEARBY: ListItem[] = [
  { n: '큰순두부 연남점', d: '한식 · 120m', tag: '메이트 2명' },
  { n: '혼밥의자', d: '일식 · 180m' },
  { n: '옥상국밥', d: '한식 · 240m' },
  { n: '연남 파스타바', d: '양식 · 300m' },
];

export function MapHomeScreen({ navigation }: MainTabScreenProps<'MapHome'>) {
  const insets = useSafeAreaInsets();
  const [honbabOn, setHonbabOn] = useState(false);
  const [picking, setPicking] = useState(false);
  const [placeName, setPlaceName] = useState('큰순두부 연남점');

  const startHonbab = (name: string) => {
    setPlaceName(name);
    setPicking(false);
    setHonbabOn(true);
  };

  return (
    <View style={styles.root}>
      <MapBackground />

      {/* 핀 */}
      {PINS.map((p, i) => (
        <MiniPin key={i} {...p} />
      ))}

      {/* 내 위치 */}
      <View style={styles.myLocation} />

      {/* 상단 검색 + 길찾기 + (혼밥 중 상태 카드) + 라이브 카운터 */}
      <View style={[styles.topWrap, { top: insets.top + 8 }]}>
        <View style={styles.searchRow}>
          <View style={styles.search}>
            <Icon name="search" size={16} color={T2.text} />
            <Text style={styles.searchPlaceholder}>장소, 음식, 메이트</Text>
          </View>
          <View style={styles.navBtn}>
            <Icon name="navigate" size={20} color="#fff" />
          </View>
        </View>

        {/* 혼밥 중 상태 카드 — 검색창 바로 아래 */}
        {honbabOn && (
          <HonbabStatusBar
            place={placeName}
            onEnd={() => setHonbabOn(false)}
            style={{ marginTop: 10 }}
          />
        )}
      </View>

      {/* 줌 컨트롤 */}
      <View style={styles.zoom}>
        {['+', '−', '◎'].map((s, i, a) => (
          <View key={s} style={[styles.zoomBtn, i < a.length - 1 && styles.zoomDivider]}>
            <Text style={styles.zoomText}>{s}</Text>
          </View>
        ))}
      </View>

      {/* 하단 시트 */}
      <View style={styles.sheet}>
        <View style={styles.handle} />
        <View style={[styles.sheetHead, styles.sheetHeadRow]}>
          <View style={{ flex: 1 }}>
            <View style={styles.liveRow}>
              <View style={styles.pulseSm}>
                <View style={styles.pulseHaloSm} />
                <View style={styles.pulseDotSm} />
              </View>
              <Text style={styles.liveTag}>지금 · 실시간</Text>
            </View>
            <Text style={styles.sheetTitle}>
              연남동에서 <Text style={{ color: T2.brand }}>27명</Text>이{'\n'}혼자 식사 중
            </Text>
            <Text style={styles.sheetSub}>
              메이트 모집 중 <Text style={{ color: T2.text, fontWeight: '700' }}>3명</Text> · 가게 8곳
            </Text>
          </View>

          {/* 혼밥 시작 → 식당 선택 시트 / 혼밥 중이면 종료 토글 */}
          {honbabOn ? (
            <Pressable style={styles.honbabBtnOn} onPress={() => setHonbabOn(false)}>
              <View style={styles.honbabPulse}>
                <View style={styles.honbabHalo} />
                <View style={styles.honbabDot} />
              </View>
              <Text style={styles.honbabBtnOnText}>혼밥 중</Text>
            </Pressable>
          ) : (
            <Pressable style={styles.honbabBtn} onPress={() => setPicking(true)}>
              <Text style={styles.honbabEmoji}>🍚</Text>
              <Text style={styles.honbabBtnText}>혼밥 시작</Text>
            </Pressable>
          )}
        </View>

        {LIST.map((r, i) => (
          <Pressable
            key={i}
            style={[styles.listRow, i === 0 && styles.listRowFirst]}
            onPress={() => navigation.navigate('RestaurantDetail', { name: r.n })}
          >
            <View style={{ flex: 1 }}>
              <Text style={styles.listName}>{r.n}</Text>
              <View style={styles.listMetaRow}>
                <Text style={styles.listMeta}>{r.d}</Text>
                {r.tag && (
                  <>
                    <Text style={styles.dot}>·</Text>
                    <Text style={[styles.listTag, { color: r.tagOn ? T2.brand : T2.textSub, fontWeight: r.tagOn ? '700' : '500' }]}>
                      {r.tagOn ? '● ' : ''}
                      {r.tag}
                    </Text>
                  </>
                )}
              </View>
            </View>
          </Pressable>
        ))}
      </View>

      {/* 식당 선택 시트 — 혼밥 시작 전 어디서 먹는지 선택 */}
      {picking && (
        <>
          <Pressable style={styles.scrim} onPress={() => setPicking(false)} />
          <View style={[styles.pickSheet, { paddingBottom: insets.bottom + 24 }]}>
            <View style={styles.handle} />
            <View style={{ paddingHorizontal: 20, paddingBottom: 4 }}>
              <Text style={styles.pickTitle}>어디서 혼밥 중이세요?</Text>
              <Text style={styles.pickSub}>선택한 식당에 ‘혼밥 중’으로 표시돼요</Text>
            </View>
            <View style={{ marginTop: 14 }}>
              {NEARBY.map((p, i) => (
                <Pressable
                  key={p.n}
                  style={[styles.pickRow, i === 0 && styles.pickRowFirst]}
                  onPress={() => startHonbab(p.n)}
                >
                  <View style={styles.pickIcon}>
                    <Text style={styles.pickIconEmoji}>🍽</Text>
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.pickName}>{p.n}</Text>
                    <Text style={styles.pickMeta}>{p.d}{p.tag ? ` · ${p.tag}` : ''}</Text>
                  </View>
                  <Icon name="chevronRight" size={18} color={T2.textMute} />
                </Pressable>
              ))}
            </View>
            <View style={styles.pickSearch}>
              <Text style={styles.pickSearchText}>직접 검색해서 찾기</Text>
            </View>
          </View>
        </>
      )}
    </View>
  );
}

const shadow = {
  shadowColor: '#000',
  shadowOffset: { width: 0, height: 2 },
  shadowOpacity: 0.08,
  shadowRadius: 12,
  elevation: 3,
};

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: T2.mapBg },

  myLocation: {
    position: 'absolute',
    left: '50%',
    top: '45%',
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: '#171717',
    borderWidth: 3,
    borderColor: '#fff',
  },

  topWrap: { position: 'absolute', left: 16, right: 16 },
  searchRow: { flexDirection: 'row', gap: 8 },
  search: {
    flex: 1,
    height: 48,
    backgroundColor: '#fff',
    borderRadius: 14,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    gap: 10,
    ...shadow,
  },
  searchPlaceholder: { flex: 1, color: T2.textMute, fontSize: 14, letterSpacing: -0.3 },
  navBtn: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: T2.brand,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: T2.brand,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 12,
    elevation: 4,
  },

  zoom: {
    position: 'absolute',
    right: 16,
    bottom: 320,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#fff',
    ...shadow,
  },
  zoomBtn: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  zoomDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  zoomText: { fontSize: 18, color: T2.text },

  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#fff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 12,
    paddingBottom: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.06,
    shadowRadius: 24,
    elevation: 8,
  },
  handle: { width: 36, height: 4, borderRadius: 2, backgroundColor: '#E5E5E5', alignSelf: 'center', marginBottom: 16 },
  sheetHead: { paddingHorizontal: 20, paddingBottom: 12 },
  liveRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  pulseSm: { width: 7, height: 7, alignItems: 'center', justifyContent: 'center' },
  pulseHaloSm: { position: 'absolute', width: 13, height: 13, borderRadius: 7, backgroundColor: T2.brand, opacity: 0.2 },
  pulseDotSm: { width: 7, height: 7, borderRadius: 4, backgroundColor: T2.brand },
  liveTag: { fontSize: 11, fontWeight: '700', color: T2.brand, letterSpacing: 0.5 },
  sheetTitle: { fontSize: 26, fontWeight: '800', color: T2.text, letterSpacing: -0.8, marginTop: 4, lineHeight: 30 },
  sheetSub: { fontSize: 12, color: T2.textMute, marginTop: 6, letterSpacing: -0.2 },

  listRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  listRowFirst: { borderTopWidth: 1, borderTopColor: T2.border },
  listName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  listMetaRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 3 },
  listMeta: { fontSize: 12, color: T2.textSub },
  dot: { color: T2.textMute, fontSize: 12 },
  listTag: { fontSize: 12 },

  // 시트 헤더 + 혼밥 시작 버튼
  sheetHeadRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  honbabBtn: {
    flexShrink: 0,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingHorizontal: 20,
    paddingVertical: 15,
    borderRadius: 12,
    backgroundColor: T2.brand,
    shadowColor: T2.brand,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.32,
    shadowRadius: 16,
    elevation: 5,
  },
  honbabEmoji: { fontSize: 16 },
  honbabBtnText: { fontSize: 14, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  honbabBtnOn: {
    flexShrink: 0,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 7,
    paddingHorizontal: 18,
    paddingVertical: 15,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: T2.brand,
  },
  honbabBtnOnText: { fontSize: 14, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 },
  honbabPulse: { width: 8, height: 8, alignItems: 'center', justifyContent: 'center' },
  honbabHalo: { position: 'absolute', width: 14, height: 14, borderRadius: 7, backgroundColor: T2.brand, opacity: 0.25 },
  honbabDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: T2.brand },

  // 식당 선택 시트
  scrim: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 60, backgroundColor: 'rgba(10,10,10,0.4)' },
  pickSheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 61,
    backgroundColor: '#fff',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingTop: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -8 },
    shadowOpacity: 0.18,
    shadowRadius: 30,
    elevation: 12,
  },
  pickTitle: { fontSize: 20, fontWeight: '800', color: T2.text, letterSpacing: -0.5 },
  pickSub: { fontSize: 13, color: T2.textMute, marginTop: 5, letterSpacing: -0.3 },
  pickRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 13,
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  pickRowFirst: { borderTopWidth: 1, borderTopColor: T2.border },
  pickIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: T2.bg,
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pickIconEmoji: { fontSize: 18 },
  pickName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  pickMeta: { fontSize: 12, color: T2.textSub, marginTop: 3, letterSpacing: -0.2 },
  pickSearch: {
    marginHorizontal: 20,
    marginTop: 16,
    paddingVertical: 14,
    borderRadius: 12,
    borderWidth: 1.5,
    borderStyle: 'dashed',
    borderColor: T2.borderStrong,
    alignItems: 'center',
  },
  pickSearchText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
});
