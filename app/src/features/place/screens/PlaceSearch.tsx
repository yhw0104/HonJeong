// PlaceSearch — 식당 이름 검색 화면. 앱 디자인 톤(웜 오프화이트 배경 + 흰 카드 + 그림자)에 맞춤.
// 상단: 뒤로가기 + 둥근 검색바. 본문: 입력 전 안내 / 결과 카드 리스트 / 빈·실패 상태.
import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, FlatList, ScrollView, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon, StateView } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { usePlaceSearch, useNearby } from '@/features/place/queries';
import { useDebouncedValue } from '@/shared/useDebouncedValue';
import { MIN_SEARCH_LEN } from '@/shared/search';
import { useRecentSearches } from '@/features/place/recentSearches';
import { nearbyDiningPlaces } from '@/features/place/nearbyDining';
import { SeekingPlacesSection } from '@/features/place/components/SeekingPlacesSection';
import { searchOrigin } from '@/features/place/searchOrigin';
import { useLocation } from '@/shared/location/useLocation';
import { formatDistance } from '@/shared/location/distance';
import { useMyCheckIn } from '@/features/checkin/queries';
import type { RootStackScreenProps } from '@/navigation/types';

export function PlaceSearchScreen({ navigation }: RootStackScreenProps<'PlaceSearch'>) {
  const insets = useSafeAreaInsets();
  const [query, setQuery] = useState('');
  const dq = useDebouncedValue(query, 300); // API 호출은 디바운스된 값으로(입력 표시는 query 그대로)
  const { recent, add, remove, clear } = useRecentSearches();
  const { coord, source } = useLocation();
  // 거리순 정렬의 기준 좌표(기본 좌표는 제외하는 이유는 searchOrigin에 적혀 있다).
  const { data, isFetching, isError, refetch } = usePlaceSearch(dq, searchOrigin(source, coord));
  // 주변 혼밥은 실제 GPS가 있을 때만(내 동네·기본좌표면 '주변'이 아니라 숨김). 폴링 없음.
  const nearbyQuery = useNearby(coord, 1000, source === 'gps', false);
  const myCheckIn = useMyCheckIn(); // 내가 모집중인 식당은 인원에서 나만 뺀다(seekingCount에 내가 포함돼 있어서)
  const nearby =
    source === 'gps'
      ? nearbyDiningPlaces(nearbyQuery.data?.content ?? [], 5, myCheckIn.data?.placeId ?? null)
      : [];
  const q = query.trim();
  const settling = dq.trim() !== q; // 디바운스 정착 전(입력이 아직 API에 반영 안 됨) → 로딩 취급
  const results = data?.content ?? [];

  return (
    <View style={[styles.root, { paddingTop: insets.top + 8 }]}>
      {/* 상단 바: 뒤로 + 검색창 */}
      <View style={styles.topBar}>
        <Pressable style={styles.backBtn} onPress={() => navigation.goBack()} hitSlop={8}>
          <Icon name="chevronLeft" size={22} color={T2.text} />
        </Pressable>
        <View style={styles.searchBox}>
          <Icon name="search" size={16} color={T2.textMute} />
          <TextInput
            style={styles.searchInput}
            placeholder="식당 이름 검색"
            placeholderTextColor={T2.textMute}
            value={query}
            onChangeText={setQuery}
            autoFocus
            returnKeyType="search"
          />
          {q.length > 0 && (
            <Pressable onPress={() => setQuery('')} hitSlop={8}>
              <View style={styles.clearBtn}>
                <Text style={styles.clearX}>✕</Text>
              </View>
            </Pressable>
          )}
        </View>
      </View>

      {/* 본문 — 2글자 미만이면 검색 안 하고 기본 뷰(최근·주변) 유지 */}
      {q.length < MIN_SEARCH_LEN ? (
        recent.length === 0 && nearby.length === 0 ? (
          <View style={styles.center}>
            <View style={styles.hintIcon}>
              <Icon name="search" size={30} color={T2.textMute} />
            </View>
            <Text style={styles.hintText}>식당 이름으로 검색해보세요</Text>
          </View>
        ) : (
          <ScrollView
            contentContainerStyle={styles.emptyScroll}
            keyboardShouldPersistTaps="handled"
            keyboardDismissMode="on-drag"
          >
            {recent.length > 0 && (
              <View style={styles.recentWrap}>
                <View style={styles.recentHeader}>
                  <Text style={styles.recentTitle}>최근 검색어</Text>
                  <Pressable onPress={clear} hitSlop={8}>
                    <Text style={styles.recentClear}>전체 삭제</Text>
                  </Pressable>
                </View>
                <View style={styles.chips}>
                  {recent.map((term) => (
                    <View key={term} style={styles.chip}>
                      <Pressable onPress={() => setQuery(term)} hitSlop={6}>
                        <Text style={styles.chipText}>{term}</Text>
                      </Pressable>
                      <Pressable onPress={() => remove(term)} hitSlop={6} style={styles.chipX}>
                        <Text style={styles.chipXText}>✕</Text>
                      </Pressable>
                    </View>
                  ))}
                </View>
              </View>
            )}
            {/* 같이 먹기 탭과 같은 컴포넌트를 쓴다. 예전에는 여기만 🍽 이모지 아바타에 가운데
                정렬이라 같은 데이터가 두 화면에서 다르게 보였다. 비면 통째로 사라진다
                (emptyText를 주지 않는다) — 여기서는 최근 검색어 아래 곁다리라 그게 낫다. */}
            <View style={styles.nearbyWrap}>
              <SeekingPlacesSection
                places={nearby}
                onPressPlace={(placeId, name) => navigation.navigate('RestaurantDetail', { placeId, name })}
              />
            </View>
          </ScrollView>
        )
      ) : isError ? (
        <StateView kind="error" message="검색에 실패했어요." onRetry={() => refetch()} />
      ) : (
        <FlatList
          data={results}
          keyExtractor={(it) => String(it.placeId)}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="on-drag"
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            isFetching || settling ? (
              <StateView kind="loading" compact />
            ) : (
              <StateView kind="empty" compact message={`‘${q}’ 검색 결과가 없어요.`} />
            )
          }
          renderItem={({ item }) => (
            <Pressable
              style={styles.card}
              onPress={() => {
                add(q); // 결과를 탭한 검색어만 최근 검색어로 기록
                navigation.navigate('RestaurantDetail', { placeId: item.placeId, name: item.name });
              }}
            >
              {/* 같이 먹기 신청 화면·받은 신청 카드와 같은 브랜드 핀 타일. 원래는 🍽 이모지였는데
                  사진이 있는 것처럼 보이게만 하고 아무 정보도 주지 않았다. 식당을 가리키는 표시는
                  앱 전체에서 이 핀 하나로 통일한다(이모지는 기기·OS마다 모양이 달라지기도 한다). */}
              <View style={styles.cardIcon}>
                <Icon name="pin" size={20} color={T2.brand} />
              </View>
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.cardName} numberOfLines={1}>{item.name}</Text>
                <Text style={styles.cardMeta} numberOfLines={1}>
                  {/* 거리를 맨 앞에 둔다 — 거리순으로 정렬돼 있다는 사실이 목록에서 바로 읽혀야 한다.
                      좌표를 안 보낸 검색이면 null이라 자연히 빠진다(0으로 오지 않는다). */}
                  {[
                    item.distanceMeters === null ? null : formatDistance(item.distanceMeters),
                    item.category,
                    item.roadAddress ?? item.address,
                  ].filter(Boolean).join(' · ') || '주소 정보 없음'}
                </Text>
              </View>
              <Icon name="chevronRight" size={18} color={T2.textMute} />
            </Pressable>
          )}
        />
      )}
    </View>
  );
}

const shadow = {
  shadowColor: '#000',
  shadowOffset: { width: 0, height: 2 },
  shadowOpacity: 0.06,
  shadowRadius: 10,
  elevation: 2,
};

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: T2.bg },

  topBar: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingBottom: 12 },
  backBtn: { width: 36, height: 36, alignItems: 'center', justifyContent: 'center' },
  searchBox: {
    flex: 1,
    height: 46,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 9,
    backgroundColor: T2.surface,
    borderRadius: 14,
    paddingHorizontal: 14,
    ...shadow,
  },
  searchInput: { flex: 1, fontSize: 15, color: T2.text, padding: 0, letterSpacing: -0.3 },
  clearBtn: {
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: T2.borderStrong,
    alignItems: 'center',
    justifyContent: 'center',
  },
  clearX: { fontSize: 10, color: '#fff', fontWeight: '700', lineHeight: 12 },

  listContent: { paddingHorizontal: 16, paddingTop: 4, paddingBottom: 24 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: T2.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: T2.border,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginBottom: 8,
  },
  // 같이 먹기 신청 화면의 placeThumb와 같은 모양(brandSoft 타일 + 핀). 크기만 44로 둔다 —
  // 이 카드가 목록이라 46이면 행이 그만큼 두꺼워진다.
  cardIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: T2.brandSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  cardMeta: { fontSize: 12, color: T2.textSub, marginTop: 3, letterSpacing: -0.2 },

  center: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 32, paddingBottom: 80 },
  hintIcon: { marginBottom: 14 },
  hintText: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  msg: { fontSize: 14, color: T2.textSub, textAlign: 'center', lineHeight: 21, marginTop: 24 },

  emptyScroll: { paddingBottom: 24 },

  recentWrap: { paddingHorizontal: 20, paddingTop: 8 },
  recentHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  recentTitle: { fontSize: 13, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  recentClear: { fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: T2.surface,
    borderWidth: 1,
    borderColor: T2.border,
    borderRadius: 18,
    paddingLeft: 14,
    paddingRight: 10,
    paddingVertical: 8,
  },
  chipText: { fontSize: 13, color: T2.text, letterSpacing: -0.2 },
  chipX: { width: 16, height: 16, alignItems: 'center', justifyContent: 'center' },
  chipXText: { fontSize: 10, color: T2.textMute, fontWeight: '700', lineHeight: 12 },

  // ★paddingHorizontal 20이 필요하다 — 감싸는 emptyScroll에는 좌우 여백이 없고,
  //   SeekingPlacesSection이 bleed(기본 20)만큼 음수 마진으로 카드를 여백 밖까지 흘려보내기
  //   때문이다. 여기가 0이면 헤더가 화면 왼쪽 끝에 붙고 카드는 화면 밖으로 밀린다.
  //   같이 먹기 탭도 같은 구조다(그쪽은 바깥 scroll이 paddingHorizontal 20을 갖고 있다).
  nearbyWrap: { paddingHorizontal: 20, paddingTop: 24 },
});
