// PlaceSearch — 식당 이름 검색 화면. 검색 결과 탭 → 식당 상세(placeId 전달).
import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, FlatList, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { T2 } from '@/shared/theme';
import { usePlaceSearch } from '@/features/place/queries';
import type { RootStackScreenProps } from '@/navigation/types';

export function PlaceSearchScreen({ navigation }: RootStackScreenProps<'PlaceSearch'>) {
  const insets = useSafeAreaInsets();
  const [query, setQuery] = useState('');
  const { data, isFetching, isError } = usePlaceSearch(query);

  return (
    <View style={[styles.root, { paddingTop: insets.top + 8 }]}>
      <TextInput
        style={styles.input}
        placeholder="식당 이름 검색"
        value={query}
        onChangeText={setQuery}
        autoFocus
        returnKeyType="search"
      />
      {isError && <Text style={styles.msg}>검색에 실패했어요. 잠시 후 다시 시도해주세요.</Text>}
      {!isError && query.trim().length > 0 && !isFetching && (data?.content.length ?? 0) === 0 && (
        <Text style={styles.msg}>검색 결과가 없어요.</Text>
      )}
      <FlatList
        data={data?.content ?? []}
        keyExtractor={(it) => String(it.placeId)}
        renderItem={({ item }) => (
          <Pressable
            style={styles.row}
            onPress={() => navigation.navigate('RestaurantDetail', { placeId: item.placeId, name: item.name })}
          >
            <Text style={styles.name}>{item.name}</Text>
            <Text style={styles.meta}>{[item.category, item.roadAddress].filter(Boolean).join(' · ')}</Text>
          </Pressable>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff', paddingHorizontal: 16 },
  input: {
    height: 48, borderRadius: 12, backgroundColor: T2.bg,
    borderWidth: 1, borderColor: T2.border, paddingHorizontal: 14, fontSize: 15,
  },
  msg: { color: T2.textMute, fontSize: 13, marginTop: 16, textAlign: 'center' },
  row: { paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: T2.border },
  name: { fontSize: 15, fontWeight: '700', color: T2.text },
  meta: { fontSize: 12, color: T2.textSub, marginTop: 3 },
});
