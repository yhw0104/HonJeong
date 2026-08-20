// SeekingPlacesSection — "지금 모집 중"인 주변 식당 가로 카드 목록.
//
// ★같이 먹기 탭(TogetherFeed)과 검색 첫 화면(PlaceSearch) 두 곳에서 쓴다. 원래는 각자 자기
//   마크업을 들고 있어서 같은 데이터가 서로 다른 모양으로 보였다 — 검색 쪽은 🍽 이모지 아바타에
//   가운데 정렬, 같이 먹기 쪽은 왼쪽 정렬 카드였고, 심지어 거리 포맷 함수도 서로 달랐다
//   (shared/format은 반올림, shared/location/distance는 안 함). 여기 하나로 합쳐 그 갈라짐을 없앤다.
//   실기 지적으로 같이 먹기 쪽 모양을 정본으로 골랐다.
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';
import { formatDistance } from '@/shared/format';
import type { PlaceNearbyItem } from '@/features/place/api';

type Props = {
  /** 이미 nearbyDiningPlaces로 걸러진(가까운 순·본인 제외) 목록. */
  places: PlaceNearbyItem[];
  onPressPlace: (placeId: number, name: string) => void;
  /**
   * 비었을 때 보여줄 문구. 주지 않으면 <b>섹션 자체를 그리지 않는다</b>.
   * 같이 먹기 탭은 이 섹션이 화면의 주인공이라 "없다"는 사실도 말해줘야 하지만,
   * 검색 첫 화면에서는 최근 검색어 아래 곁다리라 비면 조용히 사라지는 편이 낫다.
   */
  emptyText?: string;
  /** 가로 스크롤이 좌우 여백을 뚫고 나가게 할 때의 음수 마진(감싸는 쪽 padding과 맞춘다). */
  bleed?: number;
};

export function SeekingPlacesSection({ places, onPressPlace, emptyText, bleed = 20 }: Props) {
  if (places.length === 0 && emptyText === undefined) return null;

  return (
    <View>
      <View style={styles.head}>
        <View style={styles.headLeft}>
          <View style={styles.dot} />
          <Text style={styles.label}>지금 모집 중</Text>
        </View>
        <Text style={styles.count}>내 주변 {places.length}</Text>
      </View>
      {places.length === 0 ? (
        <Text style={styles.empty}>{emptyText}</Text>
      ) : (
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          // 검색 화면은 키보드가 올라온 채로 이 목록을 탭할 수 있어야 한다(탭 한 번에 이동).
          keyboardShouldPersistTaps="handled"
          style={{ marginHorizontal: -bleed }}
          contentContainerStyle={[styles.scrollContent, { paddingHorizontal: bleed }]}
        >
          {places.map((p) => (
            <Pressable
              key={p.placeId}
              style={styles.card}
              onPress={() => onPressPlace(p.placeId, p.name)}
              accessibilityRole="button"
            >
              <Text style={styles.place} numberOfLines={1}>{p.name}</Text>
              <Text style={styles.meta}>{formatDistance(p.distanceMeters)}</Text>
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{p.seekingCount}명 모집 중</Text>
              </View>
            </Pressable>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  head: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  headLeft: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  dot: { width: 7, height: 7, borderRadius: 3.5, backgroundColor: T2.brand },
  label: { fontSize: 11, fontWeight: '700', color: T2.text, letterSpacing: 0.6 },
  count: { fontSize: 12, fontWeight: '700', color: T2.textMute },
  empty: { fontSize: 13, color: T2.textMute, paddingVertical: 10 },
  scrollContent: { paddingBottom: 4, gap: 10 },
  card: { width: 156, padding: 14, backgroundColor: '#fff', borderRadius: 16, borderWidth: 1, borderColor: T2.border },
  place: { fontSize: 14, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  meta: { fontSize: 11.5, color: T2.textMute, marginTop: 6 },
  badge: {
    alignSelf: 'flex-start', marginTop: 10, paddingHorizontal: 9, paddingVertical: 5,
    borderRadius: 9, backgroundColor: T2.brandSoft,
  },
  badgeText: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 },
});
