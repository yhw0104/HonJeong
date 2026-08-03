// MapHome — 지도/홈. 실제 카카오맵 위에 실데이터(마커·주변 리스트·혼밥 시작/종료·전체 카운트)를 올린다.
// 하단 탭바는 MainTabs 네비게이터가 렌더하므로 여기서는 그리지 않는다.
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Linking, Animated, PanResponder, Dimensions, ActivityIndicator, Image, type LayoutChangeEvent } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { HonjeongMap, Icon, HonbabStatusBar, StateView } from '@/shared/components';
import type { HonjeongMapHandle } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { BellButton } from '@/features/notifications/BellButton';
import { useLocation } from '@/shared/location/useLocation';
import { useNearby } from '@/features/place/queries';
import type { Coord } from '@/shared/location/pickLocation';
import { listState } from '@/shared/state/listState';
import { useMyCheckIn, useStats, useStartCheckIn, useDineAlone, useCancelCheckIn } from '@/features/checkin/queries';
import { EndHonbabSheet } from '@/features/checkin/components/EndHonbabSheet';
import type { CheckIn } from '@/features/checkin/api';
import { checkInMode } from '@/features/checkin/statusView';
import { formatDistance } from '@/shared/format';
import { distanceMeters } from '@/shared/location/distance';
import type { MainTabScreenProps } from '@/navigation/types';

const HONJEONG_ICON = require('../../../../assets/honjeong-icon.png'); // 같이 먹기 버튼 아이콘(혼정 로고)

// 하단 시트 스냅 높이(접힘/펼침). 펼치면 화면의 82%까지 올라와 전체 리스트가 보인다.
// 시트 접힘 높이는 헤더를 실측해 정한다(onHeadLayout). 이 값은 실측 전 첫 프레임용 임시값일 뿐이다.
const SHEET_COLLAPSED_FALLBACK = 150;
const SHEET_VPADDING = 12 + 8; // styles.sheet의 paddingTop + paddingBottom — 실측 헤더에 더해야 안 잘린다
const SHEET_EXPANDED = Math.round(Dimensions.get('window').height * 0.82);
const SOURCE_RANK = { default: 0, region: 1, gps: 2 } as const;

// 하단 주변 목록 정렬 옵션. rating은 맛 별점 기준(리뷰 수 → 거리로 tie-break).
type SortKey = 'distance' | 'reviews' | 'rating';
const SORT_OPTIONS: { key: SortKey; label: string }[] = [
  { key: 'distance', label: '거리순' },
  { key: 'reviews', label: '리뷰 많은순' },
  { key: 'rating', label: '별점순' },
];

export function MapHomeScreen({ navigation }: MainTabScreenProps<'MapHome'>) {
  const insets = useSafeAreaInsets();
  const [picking, setPicking] = useState(false);
  const [clusterIds, setClusterIds] = useState<number[] | null>(null); // 묶음 마커(같은 좌표 여러 식당) 탭 시 그 목록
  const [sortKey, setSortKey] = useState<SortKey>('distance'); // 하단 목록 정렬(거리순/리뷰순/별점순)
  const mapRef = useRef<HonjeongMapHandle>(null);

  const { coord, source, permission, requestAgain } = useLocation({ watch: true });

  // 검색 기준점(anchor): 주변 목록·마커·지도 center는 이것만 본다. 실시간 coord는 파란 점 전용.
  // 최초 좌표로 초기화하고, 위치 출처가 더 나은 단계로 승격될 때(기본→내동네→GPS) 한 번씩 따라 올린다
  // (기존 'GPS 잡히면 지도 이동' 체감 유지 + 권한 거부 상태에서 내동네가 늦게 로드되는 경우 포함).
  const [anchor, setAnchor] = useState<Coord | null>(null);
  const [mapCenter, setMapCenter] = useState<Coord | null>(null); // 사용자가 드래그한 현재 지도 중심(재검색 판정용)
  const prevRankRef = useRef(SOURCE_RANK[source]);
  useEffect(() => {
    const prevRank = prevRankRef.current;
    const rank = SOURCE_RANK[source];
    prevRankRef.current = rank;
    setAnchor((a) => (a == null || rank > prevRank ? coord : a));
  }, [coord, source]);
  const searchAt = anchor ?? coord;
  // 목록 거리 표기는 검색 기준점(anchor)이 아니라 '내 위치(GPS)' 기준으로 — 진짜 GPS일 때만 '내 위치에서'.
  const myGps = source === 'gps' ? coord : null;
  // 목록·식당선택 시트·묶음마커 시트 공통 거리 표기. GPS 있으면 '내 위치에서 Nkm', 없으면 검색기준(백엔드) 거리.
  const distanceLabel = (p: { latitude: number; longitude: number; distanceMeters: number }) =>
    myGps
      ? `내 위치에서 ${formatDistance(distanceMeters(myGps, { lat: p.latitude, lng: p.longitude }))}`
      : formatDistance(p.distanceMeters);

  const stats = useStats();
  // 사회적 증거: '지금 혼밥 중'은 지금 식당에서 혼자인 모두(모집중+혼밥중). 그 중 일부가 같이 먹을 사람을 찾는 중.
  const honbabTotal = stats.data ? stats.data.seekingCount + stats.data.activeCount : null;
  const seekingNow = stats.data?.seekingCount ?? 0;
  const nearby = useNearby(searchAt, 1000, true, true); // 마커·목록 공통 소스. anchor를 폴링해 카운트만 갱신, anchor 고정이라 파란 점이 움직여도 목록은 안 튐(재검색 버튼으로만 기준점 이동)
  const myCheckIn = useMyCheckIn();
  const startMut = useStartCheckIn();
  const dineAloneMut = useDineAlone();
  const cancelMut = useCancelCheckIn();
  const [ending, setEnding] = useState<CheckIn | null>(null); // 종료 시트(밀어서 완료) 대상

  const honbabOn = !!myCheckIn.data; // SEEKING/ACTIVE/TOGETHER — 종료/취소되면 null
  const nearbyList = nearby.data?.content ?? [];
  // 하단 목록 정렬 — 반경 내 페이지(백엔드 거리순)를 선택 기준으로 재정렬. tie-break은 안정적인 백엔드 거리값(목록 안 튐).
  const sortedList = useMemo(() => {
    const arr = [...nearbyList];
    if (sortKey === 'reviews') {
      arr.sort((a, b) => b.reviewCount - a.reviewCount || a.distanceMeters - b.distanceMeters);
    } else if (sortKey === 'rating') {
      arr.sort((a, b) =>
        (b.avgTasteRating ?? -1) - (a.avgTasteRating ?? -1)
        || b.reviewCount - a.reviewCount
        || a.distanceMeters - b.distanceMeters);
    } else {
      arr.sort((a, b) => a.distanceMeters - b.distanceMeters);
    }
    return arr;
  }, [nearbyList, sortKey]);
  const nearbySt = listState({ isLoading: nearby.isLoading, isError: nearby.isError, count: nearbyList.length });
  // 체크인 응답이 식당 이름을 직접 담아준다 — 지도를 옮겨 주변 목록이 바뀌어도 상태바 이름이 '내 식당'으로 떨어지지 않게.
  const myPlaceName = myCheckIn.data?.placeName ?? '내 식당';

  const goDetail = (placeId: number, name?: string) =>
    navigation.navigate('RestaurantDetail', { placeId, name });

  const startHonbab = (placeId: number) => {
    if (startMut.isPending) return; // 연타로 두 번 시작되는 것 차단
    setPicking(false);
    startMut.mutate(placeId);
  };
  const endHonbab = () => {
    if (!myCheckIn.data) return;
    if (myCheckIn.data.status === 'SEEKING') return; // 모집중은 상태바의 혼자먹기/그만두기로만
    setEnding(myCheckIn.data); // 밀어서 완료 시트 열기
  };

  // 지도를 드래그해 지도를 움직이면(dragend) 곧바로 '이 위치에서 재검색' 노출(GPS 이동과 무관).
  // mapCenter는 실제 드래그 끝에서만 갱신되고(프로그램적 setCenter는 dragend 미발화), 재검색/내주변 시 null로 정리된다.
  const offerResearch = mapCenter != null && anchor != null;
  const researchHere = () => { if (mapCenter) setAnchor(mapCenter); setMapCenter(null); }; // 지도 중심으로 재검색 + 버튼 숨김
  // '내 주변': 내 GPS로 재검색 + 지도 이동(GPS 없으면 권한 재요청). setMapCenter(null)로 재검색 버튼도 정리.
  // recenter()를 명령형으로 부르는 이유: 드래그만 하고 재검색 안 한 상태에선 anchor가 이미 내 위치라
  // setAnchor(coord)로는 center prop의 lat/lng가 안 바뀌어 HonjeongMap의 setCenter effect가 안 돎 → 지도가 안 돌아온다.
  const nearMe = () => {
    if (source === 'gps') {
      mapRef.current?.recenter();
      setAnchor(coord);
      setMapCenter(null);
    } else {
      requestAgain();
    }
  };

  // 드래그로 펼치는 하단 시트: 핸들/헤더를 위로 끌면 펼침, 아래로 끌면 접힘.
  // ★접힘 높이는 상수가 아니라 **헤더 실측값**이다 — 다 내리면 "지금 N명 혼밥 중"과 '같이 먹기'만
  //   남고 식당 목록은 가려져, 그만큼 지도를 넓게 볼 수 있다. 헤더는 '그 중 N명은 …' 줄이 조건부라
  //   높이가 달라지므로 고정값을 쓰면 잘리거나 빈 공간이 생긴다.
  const sheetH = useRef(new Animated.Value(SHEET_COLLAPSED_FALLBACK)).current;
  const collapsedRef = useRef(SHEET_COLLAPSED_FALLBACK);
  const expandedRef = useRef(false);
  const snapSheet = (expand: boolean) => {
    expandedRef.current = expand;
    Animated.spring(sheetH, {
      toValue: expand ? SHEET_EXPANDED : collapsedRef.current,
      useNativeDriver: false,
      bounciness: 2,
    }).start();
  };
  // 헤더(핸들+제목줄) 실측 → 접힘 높이 갱신. 접혀 있는 상태면 즉시 반영한다.
  const onHeadLayout = (e: LayoutChangeEvent) => {
    const next = Math.round(e.nativeEvent.layout.height) + SHEET_VPADDING;
    if (Math.abs(next - collapsedRef.current) < 1) return;
    collapsedRef.current = next;
    if (!expandedRef.current) sheetH.setValue(next);
  };
  const sheetPan = useRef(
    PanResponder.create({
      // 탭은 통과시키고(버튼 동작), 세로 드래그일 때만 시트를 잡는다.
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_, g) => Math.abs(g.dy) > 4,
      onPanResponderMove: (_, g) => {
        const base = expandedRef.current ? SHEET_EXPANDED : collapsedRef.current;
        sheetH.setValue(Math.min(SHEET_EXPANDED, Math.max(collapsedRef.current, base - g.dy)));
      },
      onPanResponderRelease: (_, g) => {
        if (g.dy < -50) snapSheet(true);
        else if (g.dy > 50) snapSheet(false);
        else snapSheet(expandedRef.current);
      },
    }),
  ).current;

  // 지도 위 떠 있는 버튼들은 시트 높이를 따라 움직인다 — 안 그러면 시트만 내려가고 버튼은 공중에 뜬다.
  // AnimatedNode는 렌더마다 새로 만들면 안 되므로 한 번만 생성한다.
  const zoomBottom = useRef(Animated.add(sheetH, 62)).current;
  const floatBottom = useRef(Animated.add(sheetH, 12)).current;

  return (
    <View style={styles.root}>
      <HonjeongMap
        ref={mapRef}
        center={searchAt}
        // '내 위치' 파란 점은 진짜 GPS일 때만 — 폴백 좌표(연남동 기본 등)를 내 위치처럼 보이게 하지 않는다.
        myLocation={source === 'gps' ? coord : null}
        // 마커 = 하단 리스트(주변 식당)와 같은 집합. 모집중 있으면 숫자 배지, 없으면 작은 점.
        markers={nearbyList.map((p) => ({
          placeId: p.placeId,
          name: p.name,
          latitude: p.latitude,
          longitude: p.longitude,
          activeCount: p.activeCount,
          seekingCount: p.seekingCount,
        }))}
        onMarkerPress={(placeId) => goDetail(placeId)}
        onClusterPress={setClusterIds}
        onCenterChange={setMapCenter}
      />

      {/* 상단 검색 + (위치 안내) + (혼밥 중 상태 카드) */}
      <View style={[styles.topWrap, { top: insets.top + 8 }]}>
        <View style={styles.searchRow}>
          <Pressable style={styles.search} onPress={() => navigation.navigate('PlaceSearch')}>
            <Icon name="search" size={16} color={T2.text} />
            <Text style={styles.searchPlaceholder}>혼밥집 검색</Text>
          </Pressable>
          {/* 알림(종) — 검색창 안에서 우측 상단 독립 버튼으로 이동. 브랜드색 배경 + 흰 종 아이콘. */}
          <BellButton style={styles.bellBtn} iconColor="#fff" />
        </View>

        {permission === 'denied' && (
          <Pressable style={styles.locBanner} onPress={() => Linking.openSettings()}>
            <Text style={styles.locBannerText}>위치를 못 받아 연남동 기준으로 표시 중 · 위치 켜기</Text>
          </Pressable>
        )}

        {honbabOn && myCheckIn.data && (
          <HonbabStatusBar
            mode={checkInMode(myCheckIn.data.status)}
            place={myPlaceName}
            partnerNickname={myCheckIn.data.partnerNickname}
            onEnd={endHonbab}
            onDineAlone={() => dineAloneMut.mutate(myCheckIn.data!.checkInId)}
            onQuit={() => cancelMut.mutate(myCheckIn.data!.checkInId)}
            onOpenChat={() => {
              const cid = myCheckIn.data?.conversationId;
              if (cid) navigation.navigate('ChatRoom', { conversationId: cid });
            }}
            style={{ marginTop: 10 }}
          />
        )}
      </View>

      {/* 줌 +/− */}
      <Animated.View style={[styles.zoom, { bottom: zoomBottom }]}>
        <Pressable style={[styles.zoomBtn, styles.zoomDivider]} onPress={() => mapRef.current?.zoomIn()}>
          <Text style={styles.zoomText}>+</Text>
        </Pressable>
        <Pressable style={styles.zoomBtn} onPress={() => mapRef.current?.zoomOut()}>
          <Text style={styles.zoomText}>−</Text>
        </Pressable>
      </Animated.View>

      {/* 이동 감지 시 지도 하단 가운데 '이 위치에서 재검색' */}
      {offerResearch && (
        <Animated.View style={[styles.researchWrap, { bottom: floatBottom }]} pointerEvents="box-none">
          <Pressable style={styles.researchBtn} onPress={researchHere} disabled={nearby.isFetching}>
            {nearby.isFetching ? (
              <ActivityIndicator size="small" color={T2.brand} />
            ) : (
              <Text style={styles.researchText}>↻ 이 위치에서 재검색</Text>
            )}
          </Pressable>
        </Animated.View>
      )}

      {/* 우하단 '내 주변' — 내 위치로 새로고침(재검색). GPS 없으면 권한 재요청. */}
      <Animated.View style={[styles.nearMeWrap, { bottom: floatBottom }]} pointerEvents="box-none">
        <Pressable style={styles.nearMeBtn} onPress={nearMe} disabled={nearby.isFetching}>
          <Icon name="navigate" size={13} color={T2.brand} />
          <Text style={styles.nearMeText}>내 주변</Text>
        </Pressable>
      </Animated.View>

      {/* 하단 시트 (핸들·헤더를 위로 드래그하면 펼쳐져 전체 리스트가 보임) */}
      <Animated.View style={[styles.sheet, { height: sheetH }]}>
        <View {...sheetPan.panHandlers} onLayout={onHeadLayout}>
          <View style={styles.handle} />
          <View style={[styles.sheetHead, styles.sheetHeadRow]}>
          <View style={{ flex: 1 }}>
            <View style={styles.liveRow}>
              <View style={styles.pulseSm}>
                <View style={styles.pulseHaloSm} />
                <View style={styles.pulseDotSm} />
              </View>
              <Text style={styles.liveTag}>실시간</Text>
            </View>
            <Text style={styles.sheetTitle}>
              지금 <Text style={{ color: T2.brand }}>{honbabTotal ?? '–'}명</Text> 혼밥 중
              {seekingNow > 0 ? (
                <Text style={styles.sheetSubInline}>{'\n'}· 그 중 {seekingNow}명은 같이 먹을 사람 찾는 중</Text>
              ) : null}
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
              <Text style={styles.honbabBtnOnText}>
                {myCheckIn.data?.status === 'SEEKING' ? '모집 중' : myCheckIn.data?.status === 'TOGETHER' ? '같이 먹는 중' : '혼밥 중'}
              </Text>
            </Pressable>
          ) : (
            <Pressable style={styles.honbabBtn} onPress={() => setPicking(true)}>
              <Image source={HONJEONG_ICON} style={styles.honbabIcon} />
              <Text style={styles.honbabBtnText}>같이 먹기</Text>
            </Pressable>
          )}
          </View>
        </View>

        {/* 헤더 아래 본문. ★접었을 때 남는 높이가 0이 되므로 여기서 잘라내야 한다 —
            안 그러면 정렬 칩·목록이 시트 바깥(탭바 위)으로 삐져나온다.
            시트 본체에 overflow:hidden을 주면 iOS에서 상단 그림자까지 함께 잘려 사라진다. */}
        <View style={styles.sheetBody}>
        {/* 정렬 필터 — 거리순 / 리뷰 많은순 / 별점순 */}
        <View style={styles.sortRow}>
          {SORT_OPTIONS.map((s) => {
            const on = sortKey === s.key;
            return (
              <Pressable key={s.key} style={[styles.sortChip, on && styles.sortChipOn]} onPress={() => setSortKey(s.key)}>
                <Text style={[styles.sortChipText, on && styles.sortChipTextOn]}>{s.label}</Text>
              </Pressable>
            );
          })}
        </View>

        <ScrollView style={styles.sheetList} showsVerticalScrollIndicator={false}>
          {nearbySt !== 'ready' ? (
            <StateView
              kind={nearbySt === 'error' ? 'error' : nearbySt === 'empty' ? 'empty' : 'loading'}
              compact
              message={
                nearbySt === 'empty'
                  ? '주변에 등록된 가게가 없어요'
                  : nearbySt === 'error'
                    ? '주변 정보를 불러오지 못했어요'
                    : undefined
              }
              onRetry={nearbySt === 'error' ? () => nearby.refetch() : undefined}
            />
          ) : (
            sortedList.map((r, i) => (
            <Pressable
              key={r.placeId}
              style={[styles.listRow, i === 0 && styles.listRowFirst]}
              onPress={() => goDetail(r.placeId, r.name)}
            >
              <View style={{ flex: 1 }}>
                {/* 이름 + 카테고리(옆에) */}
                <View style={styles.nameRow}>
                  <Text style={styles.listName} numberOfLines={1}>{r.name}</Text>
                  {!!r.category && <Text style={styles.listCategory}>{r.category}</Text>}
                </View>
                {/* 별점 두 개(맛·혼밥) + 리뷰 수 — 리뷰 있을 때만 */}
                {r.reviewCount > 0 && (
                  <View style={styles.ratingRow}>
                    <View style={styles.ratingItem}>
                      <Text style={styles.ratingLabel}>맛</Text>
                      <Text style={styles.listStar}>★ {(r.avgTasteRating ?? 0).toFixed(1)}</Text>
                    </View>
                    <View style={styles.ratingItem}>
                      <Text style={styles.ratingLabel}>혼밥</Text>
                      <Text style={styles.listStar}>★ {(r.avgSoloFriendlyRating ?? 0).toFixed(1)}</Text>
                    </View>
                    <Text style={styles.ratingCount}>리뷰 {r.reviewCount}</Text>
                  </View>
                )}
                {/* 거리(내 위치 기준) · 모집(거리 옆) */}
                <View style={styles.listMetaRow}>
                  <Text style={styles.listMeta}>{distanceLabel(r)}</Text>
                  {r.seekingCount > 0 && (
                    <>
                      <Text style={styles.dot}>·</Text>
                      <Text style={[styles.listTag, { color: T2.brand, fontWeight: '700' }]}>● 모집 {r.seekingCount}</Text>
                    </>
                  )}
                </View>
                {/* 사진 있으면 이름·거리 아래에 가로 스크롤 스트립, 없으면 아무것도 안 그림(기존 모습 유지) */}
                {(r.photoUrls?.length ?? 0) > 0 && (
                  <ScrollView
                    horizontal
                    showsHorizontalScrollIndicator={false}
                    style={styles.listPhotos}
                    contentContainerStyle={styles.listPhotosContent}
                  >
                    {r.photoUrls.map((uri, idx) => (
                      <Image key={`${uri}-${idx}`} source={{ uri }} style={styles.listPhoto} />
                    ))}
                  </ScrollView>
                )}
              </View>
            </Pressable>
            ))
          )}
        </ScrollView>
        </View>
      </Animated.View>

      {/* 식당 선택 시트 — 혼밥 시작 전 어디서 먹는지 선택 */}
      {picking && (
        <>
          <Pressable style={styles.scrim} onPress={() => setPicking(false)} />
          <View style={[styles.pickSheet, { paddingBottom: insets.bottom + 24 }]}>
            <View style={styles.handle} />
            <View style={{ paddingHorizontal: 20, paddingBottom: 4 }}>
              <Text style={styles.pickTitle}>어디서 드실 예정이세요?</Text>
              <Text style={styles.pickSub}>선택한 식당에 ‘모집 중’으로 표시돼요</Text>
            </View>
            <ScrollView style={styles.pickList} showsVerticalScrollIndicator={false}>
              {nearbySt !== 'ready' ? (
                <StateView
                  kind={nearbySt === 'error' ? 'error' : nearbySt === 'empty' ? 'empty' : 'loading'}
                  compact
                  message={
                    nearbySt === 'empty'
                      ? '주변에 가게가 없어요 · 직접 검색해 보세요'
                      : nearbySt === 'error'
                        ? '주변 정보를 불러오지 못했어요'
                        : undefined
                  }
                  onRetry={nearbySt === 'error' ? () => nearby.refetch() : undefined}
                />
              ) : (
                nearbyList.map((p, i) => (
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
                    <Text style={styles.pickMeta}>{[p.category, distanceLabel(p)].filter(Boolean).join(' · ')}</Text>
                  </View>
                  <Icon name="chevronRight" size={18} color={T2.textMute} />
                </Pressable>
                ))
              )}
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

      {/* 묶음 마커 시트 — 같은 좌표(상가 건물)에 겹친 식당 목록에서 하나 선택 → 상세 이동 */}
      {clusterIds && (() => {
        const group = nearbyList.filter((p) => clusterIds.includes(p.placeId));
        return (
          <>
            <Pressable style={styles.scrim} onPress={() => setClusterIds(null)} />
            <View style={[styles.pickSheet, { paddingBottom: insets.bottom + 24 }]}>
              <View style={styles.handle} />
              <View style={{ paddingHorizontal: 20, paddingBottom: 4 }}>
                <Text style={styles.pickTitle}>여기 {group.length}집</Text>
                <Text style={styles.pickSub}>같은 자리(건물)에 있는 식당이에요</Text>
              </View>
              <ScrollView style={styles.pickList} showsVerticalScrollIndicator={false}>
                {group.map((p, i) => (
                  <Pressable
                    key={p.placeId}
                    style={[styles.pickRow, i === 0 && styles.pickRowFirst]}
                    onPress={() => { setClusterIds(null); goDetail(p.placeId, p.name); }}
                  >
                    <View style={styles.pickIcon}>
                      <Text style={styles.pickIconEmoji}>🍽</Text>
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text style={styles.pickName}>{p.name}</Text>
                      <View style={styles.listMetaRow}>
                        <Text style={styles.listMeta}>{[p.category, distanceLabel(p)].filter(Boolean).join(' · ')}</Text>
                        {p.seekingCount > 0 && (
                          <>
                            <Text style={styles.dot}>·</Text>
                            <Text style={[styles.listTag, { color: T2.brand, fontWeight: '700' }]}>● 모집 {p.seekingCount}</Text>
                          </>
                        )}
                      </View>
                    </View>
                    <Icon name="chevronRight" size={18} color={T2.textMute} />
                  </Pressable>
                ))}
              </ScrollView>
            </View>
          </>
        );
      })()}

      {/* 혼밥/같이먹기 종료 — 밀어서 완료 시트 */}
      <EndHonbabSheet
        checkIn={ending}
        onClose={() => setEnding(null)}
        onReportNoShow={(userId, nickname) =>
          navigation.navigate('ReportForm', { targetType: 'USER', targetId: userId, targetNickname: nickname })}
      />
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
  bellBtn: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: T2.brand,
    alignItems: 'center',
    justifyContent: 'center',
    ...shadow,
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
    // bottom은 시트 높이를 따라가야 해서 렌더에서 애니메이션 값(zoomBottom)으로 준다.
    // '내 주변' 알약(+12)보다 50 위에 오도록 +62.
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#fff',
    ...shadow,
  },
  zoomBtn: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  zoomDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  zoomText: { fontSize: 18, color: T2.text },

  // bottom은 렌더에서 floatBottom(시트 높이 + 12)으로 준다.
  nearMeWrap: { position: 'absolute', right: 16, alignItems: 'flex-end' },
  nearMeBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderRadius: 999,
    backgroundColor: '#fff',
    ...shadow,
  },
  nearMeText: { fontSize: 13, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 },

  // bottom은 렌더에서 floatBottom(시트 높이 + 12)으로 준다.
  researchWrap: { position: 'absolute', left: 0, right: 0, alignItems: 'center' },
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
  // 헤더 아래 본문 — 접혔을 때 높이가 0이 되며 내용을 잘라낸다(시트 밖으로 삐져나오지 않게).
  sheetBody: { flex: 1, overflow: 'hidden' },
  sheetList: { flex: 1 },
  sortRow: { flexDirection: 'row', gap: 8, paddingHorizontal: 20, paddingBottom: 12 },
  sortChip: { paddingVertical: 7, paddingHorizontal: 13, borderRadius: 999, backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border },
  sortChipOn: { backgroundColor: T2.brandSoft, borderColor: 'rgba(255,90,31,0.3)' },
  sortChipText: { fontSize: 12.5, fontWeight: '700', color: T2.textMute, letterSpacing: -0.2 },
  sortChipTextOn: { color: T2.brand },
  pickList: { marginTop: 14, maxHeight: 340 },
  sheetHead: { paddingHorizontal: 20, paddingBottom: 12 },
  liveRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  pulseSm: { width: 7, height: 7, alignItems: 'center', justifyContent: 'center' },
  pulseHaloSm: { position: 'absolute', width: 13, height: 13, borderRadius: 7, backgroundColor: T2.brand, opacity: 0.2 },
  pulseDotSm: { width: 7, height: 7, borderRadius: 4, backgroundColor: T2.brand },
  liveTag: { fontSize: 11, fontWeight: '700', color: T2.brand, letterSpacing: 0.5 },
  sheetTitle: { fontSize: 26, fontWeight: '800', color: T2.text, letterSpacing: -0.8, marginTop: 4, lineHeight: 30 },
  sheetSubInline: { fontSize: 13, color: T2.textSub, fontWeight: '600' },
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
  nameRow: { flexDirection: 'row', alignItems: 'baseline', gap: 7 },
  listName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3, flexShrink: 1 },
  listCategory: { fontSize: 12, color: T2.textMute, fontWeight: '600', flexShrink: 0 },
  ratingRow: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 5 },
  ratingItem: { flexDirection: 'row', alignItems: 'center', gap: 3 },
  ratingLabel: { fontSize: 11, color: T2.textMute, fontWeight: '600' },
  ratingCount: { fontSize: 11, color: T2.textMute },
  listMetaRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 3 },
  listMeta: { fontSize: 12, color: T2.textSub },
  listStar: { fontSize: 12, color: '#F5A623', fontWeight: '700' },
  dot: { color: T2.textMute, fontSize: 12 },
  listTag: { fontSize: 12 },
  listPhotos: { marginTop: 10 },
  listPhotosContent: { gap: 6, paddingRight: 4 },
  listPhoto: { width: 104, height: 104, borderRadius: 10, backgroundColor: T2.bg },

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
  honbabIcon: { width: 22, height: 22, resizeMode: 'contain' },
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
