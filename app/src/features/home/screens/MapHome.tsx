// MapHome — 지도/홈. 실제 카카오맵 위에 실데이터(마커·주변 리스트·혼밥 시작/종료·전체 카운트)를 올린다.
// 하단 탭바는 MainTabs 네비게이터가 렌더하므로 여기서는 그리지 않는다.
import React, { useEffect, useRef, useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Linking, Animated, PanResponder, Dimensions, ActivityIndicator } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { HonjeongMap, Icon, HonbabStatusBar } from '@/shared/components';
import type { HonjeongMapHandle } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { BellButton } from '@/features/notifications/BellButton';
import { useLocation } from '@/shared/location/useLocation';
import { useNearby } from '@/features/place/queries';
import { shouldOfferResearch } from '@/shared/location/research';
import type { Coord } from '@/shared/location/pickLocation';
import { useMap, useMyCheckIn, useStats, useStartCheckIn } from '@/features/checkin/queries';
import { usePromptEndCheckIn } from '@/features/checkin/usePromptEndCheckIn';
import { formatDistance } from '@/shared/format';
import type { MainTabScreenProps } from '@/navigation/types';

// 하단 시트 스냅 높이(접힘/펼침). 펼치면 화면의 82%까지 올라와 전체 리스트가 보인다.
const SHEET_COLLAPSED = 300;
const SHEET_EXPANDED = Math.round(Dimensions.get('window').height * 0.82);
const SOURCE_RANK = { default: 0, region: 1, gps: 2 } as const;

export function MapHomeScreen({ navigation }: MainTabScreenProps<'MapHome'>) {
  const insets = useSafeAreaInsets();
  const [picking, setPicking] = useState(false);
  const mapRef = useRef<HonjeongMapHandle>(null);

  const { coord, source, permission, requestAgain } = useLocation({ watch: true });

  // 검색 기준점(anchor): 주변 목록·마커·지도 center는 이것만 본다. 실시간 coord는 파란 점 전용.
  // 최초 좌표로 초기화하고, 위치 출처가 더 나은 단계로 승격될 때(기본→내동네→GPS) 한 번씩 따라 올린다
  // (기존 'GPS 잡히면 지도 이동' 체감 유지 + 권한 거부 상태에서 내동네가 늦게 로드되는 경우 포함).
  const [anchor, setAnchor] = useState<Coord | null>(null);
  const prevRankRef = useRef(SOURCE_RANK[source]);
  useEffect(() => {
    const prevRank = prevRankRef.current;
    const rank = SOURCE_RANK[source];
    prevRankRef.current = rank;
    setAnchor((a) => (a == null || rank > prevRank ? coord : a));
  }, [coord, source]);
  const searchAt = anchor ?? coord;

  const stats = useStats();
  const markers = useMap(searchAt);
  const nearby = useNearby(searchAt, 1000, true, false); // 폴링 없음 — 재검색 버튼으로 갱신
  const myCheckIn = useMyCheckIn();
  const startMut = useStartCheckIn();
  const promptEnd = usePromptEndCheckIn();

  const honbabOn = !!myCheckIn.data; // ACTIVE 또는 TOGETHER — 종료/취소되면 useMyCheckIn이 null을 반환
  const nearbyList = nearby.data?.content ?? [];
  const myPlaceName =
    markers.data?.find((m) => m.placeId === myCheckIn.data?.placeId)?.name ??
    nearbyList.find((p) => p.placeId === myCheckIn.data?.placeId)?.name ??
    '혼밥 중';

  const goDetail = (placeId: number, name?: string) =>
    navigation.navigate('RestaurantDetail', { placeId, name });

  const startHonbab = (placeId: number) => {
    setPicking(false);
    startMut.mutate(placeId);
  };
  const endHonbab = () => {
    if (myCheckIn.data) promptEnd(myCheckIn.data);
  };

  // 내 위치로: 진짜 GPS가 있으면 지도 이동, 없으면(거부/실패) 권한을 다시 요청한다.
  // 재요청으로 GPS가 잡히면 coord가 바뀌어 지도 center가 따라 이동한다.
  const recenterToMe = () => {
    if (source === 'gps') mapRef.current?.recenter();
    else requestAgain();
  };

  // 기준점에서 200m 이상 이동(진짜 GPS일 때만)하면 '이 위치에서 재검색' 노출.
  const offerResearch = anchor != null && shouldOfferResearch(coord, anchor, source);
  const researchHere = () => setAnchor(coord); // queryKey 변경 → 목록·마커 재조회 + 지도 center 이동

  // 드래그로 펼치는 하단 시트: 핸들/헤더를 위로 끌면 펼침, 아래로 끌면 접힘.
  const sheetH = useRef(new Animated.Value(SHEET_COLLAPSED)).current;
  const expandedRef = useRef(false);
  const snapSheet = (expand: boolean) => {
    expandedRef.current = expand;
    Animated.spring(sheetH, {
      toValue: expand ? SHEET_EXPANDED : SHEET_COLLAPSED,
      useNativeDriver: false,
      bounciness: 2,
    }).start();
  };
  const sheetPan = useRef(
    PanResponder.create({
      // 탭은 통과시키고(버튼 동작), 세로 드래그일 때만 시트를 잡는다.
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_, g) => Math.abs(g.dy) > 4,
      onPanResponderMove: (_, g) => {
        const base = expandedRef.current ? SHEET_EXPANDED : SHEET_COLLAPSED;
        sheetH.setValue(Math.min(SHEET_EXPANDED, Math.max(SHEET_COLLAPSED, base - g.dy)));
      },
      onPanResponderRelease: (_, g) => {
        if (g.dy < -50) snapSheet(true);
        else if (g.dy > 50) snapSheet(false);
        else snapSheet(expandedRef.current);
      },
    }),
  ).current;

  return (
    <View style={styles.root}>
      <HonjeongMap
        ref={mapRef}
        center={searchAt}
        // '내 위치' 파란 점은 진짜 GPS일 때만 — 폴백 좌표(연남동 기본 등)를 내 위치처럼 보이게 하지 않는다.
        myLocation={source === 'gps' ? coord : null}
        markers={(markers.data ?? []).map((m) => ({
          placeId: m.placeId,
          latitude: m.latitude,
          longitude: m.longitude,
          activeCount: m.activeCount,
        }))}
        onMarkerPress={(placeId) => goDetail(placeId)}
      />

      {/* 상단 검색 + (위치 안내) + (혼밥 중 상태 카드) */}
      <View style={[styles.topWrap, { top: insets.top + 8 }]}>
        <View style={styles.searchRow}>
          <Pressable style={styles.search} onPress={() => navigation.navigate('PlaceSearch')}>
            <Icon name="search" size={16} color={T2.text} />
            <Text style={styles.searchPlaceholder}>혼밥집 검색</Text>
            {/* 종은 검색창 안 오른쪽 끝 — 자체 Pressable이라 탭이 검색 이동과 분리된다 */}
            <BellButton />
          </Pressable>
          <Pressable style={styles.navBtn} onPress={recenterToMe}>
            <Icon name="navigate" size={20} color="#fff" />
          </Pressable>
        </View>

        {permission === 'denied' && (
          <Pressable style={styles.locBanner} onPress={() => Linking.openSettings()}>
            <Text style={styles.locBannerText}>위치를 못 받아 연남동 기준으로 표시 중 · 위치 켜기</Text>
          </Pressable>
        )}

        {honbabOn && (
          <HonbabStatusBar
            place={myPlaceName}
            together={myCheckIn.data?.status === 'TOGETHER'}
            partnerNickname={myCheckIn.data?.partnerNickname}
            onEnd={endHonbab}
            style={{ marginTop: 10 }}
          />
        )}
      </View>

      {/* 줌 +/− · 내 위치로 이동 */}
      <View style={styles.zoom}>
        <Pressable style={[styles.zoomBtn, styles.zoomDivider]} onPress={() => mapRef.current?.zoomIn()}>
          <Text style={styles.zoomText}>+</Text>
        </Pressable>
        <Pressable style={[styles.zoomBtn, styles.zoomDivider]} onPress={() => mapRef.current?.zoomOut()}>
          <Text style={styles.zoomText}>−</Text>
        </Pressable>
        <Pressable style={styles.zoomBtn} onPress={recenterToMe}>
          <Text style={styles.zoomText}>◎</Text>
        </Pressable>
      </View>

      {/* 이동 감지 시 지도 하단 가운데 '이 위치에서 재검색' */}
      {offerResearch && (
        <View style={styles.researchWrap} pointerEvents="box-none">
          <Pressable style={styles.researchBtn} onPress={researchHere} disabled={nearby.isFetching}>
            {nearby.isFetching ? (
              <ActivityIndicator size="small" color={T2.brand} />
            ) : (
              <Text style={styles.researchText}>↻ 이 위치에서 재검색</Text>
            )}
          </Pressable>
        </View>
      )}

      {/* 하단 시트 (핸들·헤더를 위로 드래그하면 펼쳐져 전체 리스트가 보임) */}
      <Animated.View style={[styles.sheet, { height: sheetH }]}>
        <View {...sheetPan.panHandlers}>
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
              지금 <Text style={{ color: T2.brand }}>{stats.data?.activeCount ?? '–'}명</Text>이{'\n'}혼자 식사 중
            </Text>
            <Text style={styles.sheetSub}>내 주변 가게 {nearbyList.length}곳</Text>
          </View>

          {/* 혼밥 시작 → 식당 선택 시트 / 혼밥 중이면 종료 토글 */}
          {honbabOn ? (
            <Pressable style={styles.honbabBtnOn} onPress={endHonbab}>
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
        </View>

        <ScrollView style={styles.sheetList} showsVerticalScrollIndicator={false}>
          {nearbyList.map((r, i) => (
            <Pressable
              key={r.placeId}
              style={[styles.listRow, i === 0 && styles.listRowFirst]}
              onPress={() => goDetail(r.placeId, r.name)}
            >
              <View style={{ flex: 1 }}>
                <Text style={styles.listName}>{r.name}</Text>
                <View style={styles.listMetaRow}>
                  <Text style={styles.listMeta}>
                    {[r.category, formatDistance(r.distanceMeters)].filter(Boolean).join(' · ')}
                  </Text>
                  {r.activeCount > 0 && (
                    <>
                      <Text style={styles.dot}>·</Text>
                      <Text style={[styles.listTag, { color: T2.brand, fontWeight: '700' }]}>● 혼밥 {r.activeCount}</Text>
                    </>
                  )}
                </View>
              </View>
            </Pressable>
          ))}
        </ScrollView>
      </Animated.View>

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
            <ScrollView style={styles.pickList} showsVerticalScrollIndicator={false}>
              {nearbyList.map((p, i) => (
                <Pressable
                  key={p.placeId}
                  style={[styles.pickRow, i === 0 && styles.pickRowFirst]}
                  onPress={() => startHonbab(p.placeId)}
                >
                  <View style={styles.pickIcon}>
                    <Text style={styles.pickIconEmoji}>🍽</Text>
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.pickName}>{p.name}</Text>
                    <Text style={styles.pickMeta}>{[p.category, formatDistance(p.distanceMeters)].filter(Boolean).join(' · ')}</Text>
                  </View>
                  <Icon name="chevronRight" size={18} color={T2.textMute} />
                </Pressable>
              ))}
            </ScrollView>
            <Pressable
              style={styles.pickSearch}
              onPress={() => {
                setPicking(false);
                navigation.navigate('PlaceSearch');
              }}
            >
              <Text style={styles.pickSearchText}>직접 검색해서 찾기</Text>
            </Pressable>
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
  locBanner: {
    marginTop: 10,
    backgroundColor: '#FFF1ED',
    borderRadius: 12,
    paddingVertical: 10,
    paddingHorizontal: 14,
  },
  locBannerText: { fontSize: 12, color: T2.brand, fontWeight: '700', letterSpacing: -0.2 },

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

  researchWrap: { position: 'absolute', left: 0, right: 0, bottom: SHEET_COLLAPSED + 12, alignItems: 'center' },
  researchBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 150,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 999,
    backgroundColor: '#fff',
    ...shadow,
  },
  researchText: { fontSize: 13, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 },

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
  sheetList: { flex: 1 },
  pickList: { marginTop: 14, maxHeight: 340 },
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
