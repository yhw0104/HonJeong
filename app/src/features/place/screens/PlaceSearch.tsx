// PlaceSearch — 식당 이름 검색 화면. 앱 디자인 톤(웜 오프화이트 배경 + 흰 카드 + 그림자)에 맞춤.
// 상단: 뒤로가기 + 둥근 검색바. 본문: 입력 전 안내 / 결과 카드 리스트 / 빈·실패 상태.
import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, FlatList, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { usePlaceSearch } from '@/features/place/queries';
import type { RootStackScreenProps } from '@/navigation/types';

export function PlaceSearchScreen({ navigation }: RootStackScreenProps<'PlaceSearch'>) {
  const insets = useSafeAreaInsets();
  const [query, setQuery] = useState('');
  const { data, isFetching, isError } = usePlaceSearch(query);
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
        <View style={styles.center}>
          <View style={styles.hintIcon}>
            <Icon name="search" size={30} color={T2.textMute} />
          </View>
          <Text style={styles.hintText}>식당 이름으로 검색해보세요</Text>
        </View>
      ) : isError ? (
        <View style={styles.center}>
          <Text style={styles.msg}>검색에 실패했어요.{'\n'}잠시 후 다시 시도해주세요.</Text>
        </View>
      ) : (
        <FlatList
          data={results}
          keyExtractor={(it) => String(it.placeId)}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            !isFetching ? <Text style={styles.msg}>‘{q}’ 검색 결과가 없어요.</Text> : null
          }
          renderItem={({ item }) => (
            <Pressable
              style={styles.card}
              onPress={() => navigation.navigate('RestaurantDetail', { placeId: item.placeId, name: item.name })}
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
});
