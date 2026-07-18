// PlaceSearch — 식당 이름 검색 화면. 앱 디자인 톤(웜 오프화이트 배경 + 흰 카드 + 그림자)에 맞춤.
// 상단: 뒤로가기 + 둥근 검색바. 본문: 입력 전 안내 / 결과 카드 리스트 / 빈·실패 상태.
import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, FlatList, ScrollView, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon, StateView } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { usePlaceSearch, useNearby } from '@/features/place/queries';
import { useRecentSearches } from '@/features/place/recentSearches';
import { nearbyDiningPlaces } from '@/features/place/nearbyDining';
import { useLocation } from '@/shared/location/useLocation';
import { formatDistance } from '@/shared/location/distance';
import { useMyCheckIn } from '@/features/checkin/queries';
import type { RootStackScreenProps } from '@/navigation/types';

export function PlaceSearchScreen({ navigation }: RootStackScreenProps<'PlaceSearch'>) {
  const insets = useSafeAreaInsets();
  const [query, setQuery] = useState('');
  const { data, isFetching, isError, refetch } = usePlaceSearch(query);
  const { recent, add, remove, clear } = useRecentSearches();
  const { coord, source } = useLocation();
  // 주변 혼밥은 실제 GPS가 있을 때만(내 동네·기본좌표면 '주변'이 아니라 숨김). 폴링 없음.
  const nearbyQuery = useNearby(coord, 1000, source === 'gps', false);
  const myCheckIn = useMyCheckIn(); // 내가 모집중인 식당은 인원에서 나만 뺀다(seekingCount에 내가 포함돼 있어서)
  const nearby =
    source === 'gps'
      ? nearbyDiningPlaces(nearbyQuery.data?.content ?? [], 5, myCheckIn.data?.placeId ?? null)
      : [];
  const q = query.trim();
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

      {/* 본문 */}
      {q.length === 0 ? (
        recent.length === 0 && nearby.length === 0 ? (
          <View style={styles.center}>
            <View style={styles.hintIcon}>
              <Icon name="search" size={30} color={T2.textMute} />
            </View>
            <Text style={styles.hintText}>식당 이름으로 검색해보세요</Text>
          </View>
        ) : (
          <ScrollView contentContainerStyle={styles.emptyScroll} keyboardShouldPersistTaps="handled">
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
            {nearby.length > 0 && (
              <View style={styles.nearbyWrap}>
                <Text style={styles.nearbyTitle}>지금 주변에서 같이 먹을 사람 구하는 중</Text>
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  keyboardShouldPersistTaps="handled"
                  contentContainerStyle={styles.nearbyScroll}
                >
                  {nearby.map((p) => (
                    <Pressable
                      key={p.placeId}
                      style={styles.nearbyCard}
                      onPress={() => navigation.navigate('RestaurantDetail', { placeId: p.placeId, name: p.name })}
                    >
                      <View style={styles.nearbyAvatar}>
                        <Text style={styles.nearbyEmoji}>🍽</Text>
                      </View>
                      <Text style={styles.nearbyCardName} numberOfLines={1}>{p.name}</Text>
                      <Text style={styles.nearbyCardMeta} numberOfLines={1}>
                        {[p.category, formatDistance(p.distanceMeters)].filter(Boolean).join(' · ')}
                      </Text>
                      <View style={styles.countBadge}>
                        <Text style={styles.countText}>모집 {p.seekingCount}</Text>
                      </View>
                    </Pressable>
                  ))}
                </ScrollView>
              </View>
            )}
          </ScrollView>
        )
      ) : isError ? (
        <StateView kind="error" message="검색에 실패했어요." onRetry={() => refetch()} />
      ) : (
        <FlatList
          data={results}
          keyExtractor={(it) => String(it.placeId)}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            isFetching ? (
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
              <View style={styles.cardIcon}>
                <Text style={styles.cardEmoji}>🍽</Text>
              </View>
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.cardName} numberOfLines={1}>{item.name}</Text>
                <Text style={styles.cardMeta} numberOfLines={1}>
                  {[item.category, item.roadAddress ?? item.address].filter(Boolean).join(' · ') || '주소 정보 없음'}
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
  cardIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: T2.bg,
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardEmoji: { fontSize: 18 },
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

  nearbyWrap: { paddingTop: 24 },
  nearbyTitle: { fontSize: 13, fontWeight: '700', color: T2.text, letterSpacing: -0.3, marginBottom: 12, paddingHorizontal: 20 },
  nearbyScroll: { paddingHorizontal: 20, gap: 12 },
  nearbyCard: {
    width: 150,
    alignItems: 'center',
    backgroundColor: T2.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: T2.border,
    paddingVertical: 18,
    paddingHorizontal: 14,
    ...shadow,
  },
  nearbyAvatar: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: T2.bg,
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 10,
  },
  nearbyEmoji: { fontSize: 24 },
  nearbyCardName: { fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3, textAlign: 'center' },
  nearbyCardMeta: { fontSize: 11, color: T2.textSub, marginTop: 3, textAlign: 'center', letterSpacing: -0.2 },
  countBadge: { backgroundColor: T2.brand, borderRadius: 10, paddingHorizontal: 10, paddingVertical: 5, marginTop: 12 },
  countText: { fontSize: 11, fontWeight: '800', color: '#fff', letterSpacing: -0.2 },
});
