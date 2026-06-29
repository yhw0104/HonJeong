// Favorites — 즐겨찾기 (실데이터). 그룹목록 ↔ 그룹상세를 openGroupId로 토글.
import React, { useState } from 'react';
import { Alert, View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useLocation } from '@/shared/location/useLocation';
import { distanceMeters, formatDistance } from '@/shared/location/distance';
import type { MainTabScreenProps } from '@/navigation/types';
import { useFavoriteGroups, useFavoriteGroupDetail, useDeleteFavoriteGroup, useRemovePlaceFromGroup } from '../queries';
import type { FavoritePlace } from '../api';

export function FavoritesScreen({ navigation }: MainTabScreenProps<'Favorites'>) {
  const [openGroupId, setOpenGroupId] = useState<number | null>(null);
  const groupsQ = useFavoriteGroups();
  const detailQ = useFavoriteGroupDetail(openGroupId);
  const deleteGroup = useDeleteFavoriteGroup();
  const groups = groupsQ.data ?? [];
  const detail = detailQ.data;

  const onDeleteGroup = () => {
    if (!detail || detail.isDefault) return;
    Alert.alert('그룹 삭제', `'${detail.name}' 그룹을 삭제할까요? 담긴 식당은 다른 그룹에 남아요.`, [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: () =>
          deleteGroup.mutate(detail.groupId, { onSuccess: () => setOpenGroupId(null) }),
      },
    ]);
  };

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.h1}>즐겨찾기</Text>
        <View style={styles.segment}>
          <Text style={styles.segLabel}>내 장소</Text>
          <Text style={styles.segCount}>{groups.length}</Text>
          <View style={styles.segUnderline} />
        </View>
      </View>
      <View style={styles.divider} />

      <ScrollView>
        {openGroupId === null ? (
          <View>
            {groups.map((g) => (
              <Pressable key={g.groupId} style={styles.groupRow} onPress={() => setOpenGroupId(g.groupId)}>
                <View style={styles.groupThumb}>
                  <Icon name="star" size={24} color={g.color} />
                </View>
                <View style={{ flex: 1, minWidth: 0 }}>
                  <Text style={styles.groupName}>{g.name}</Text>
                  <View style={styles.metaRow}>
                    <Text style={styles.metaStrong}>{g.placeCount}곳</Text>
                    {g.note ? <Text style={styles.metaDot}>·</Text> : null}
                    {g.note ? (
                      <Text style={styles.metaSub} numberOfLines={1}>
                        {g.note}
                      </Text>
                    ) : null}
                  </View>
                </View>
                <Icon name="chevronRight" size={18} color={T2.textMute} />
              </Pressable>
            ))}
            <Pressable style={styles.newRow} onPress={() => navigation.navigate('NewGroup')}>
              <View style={styles.newPlus}>
                <Text style={styles.newPlusText}>+</Text>
              </View>
              <Text style={styles.newLabel}>새 그룹 만들기</Text>
            </Pressable>
          </View>
        ) : (
          <View>
            <View style={styles.backRow}>
              <Pressable style={styles.backLeft} onPress={() => setOpenGroupId(null)} hitSlop={8}>
                <Icon name="chevronLeft" size={18} color={T2.text} />
                <Text style={styles.backTitle}>{detail?.name ?? ''}</Text>
                <Text style={styles.backCount}>{detail?.places.length ?? 0}</Text>
              </Pressable>
              <View style={styles.backActions}>
                <Pressable
                  hitSlop={8}
                  onPress={() =>
                    detail &&
                    navigation.navigate('NewGroup', {
                      groupId: detail.groupId,
                      initial: { name: detail.name, note: detail.note ?? '', color: detail.color },
                    })
                  }
                >
                  <Icon name="pencil" size={16} color={T2.textSub} />
                </Pressable>
                {detail && !detail.isDefault ? (
                  <Pressable hitSlop={8} onPress={onDeleteGroup}>
                    <Text style={styles.deleteText}>삭제</Text>
                  </Pressable>
                ) : null}
              </View>
            </View>
            {(detail?.places ?? []).length === 0 ? (
              <Text style={styles.empty}>아직 담은 곳이 없어요</Text>
            ) : (
              (detail?.places ?? []).map((p) => (
                <FavoritePlaceRow key={p.placeId} place={p} groupId={openGroupId} />
              ))
            )}
          </View>
        )}
      </ScrollView>
    </Screen>
  );
}

function FavoritePlaceRow({ place, groupId }: { place: FavoritePlace; groupId: number }) {
  const { coord } = useLocation();
  const remove = useRemovePlaceFromGroup(place.placeId);
  const dist =
    coord != null
      ? formatDistance(distanceMeters({ lat: coord.lat, lng: coord.lng }, { lat: place.latitude, lng: place.longitude }))
      : null;
  return (
    <View style={styles.placeRow}>
      <View style={styles.placeThumb}>
        <Text style={{ fontSize: 20 }}>🍽</Text>
      </View>
      <View style={{ flex: 1, minWidth: 0 }}>
        <View style={styles.placeNameRow}>
          <Text style={styles.placeName} numberOfLines={1}>
            {place.name}
          </Text>
          {place.visited ? (
            <View style={styles.visitedBadge}>
              <Text style={styles.visitedText}>다녀옴</Text>
            </View>
          ) : null}
        </View>
        <View style={styles.metaRow}>
          {place.category ? <Text style={styles.metaSub}>{place.category}</Text> : null}
          {place.category && dist ? <Text style={styles.metaDot}>·</Text> : null}
          {dist ? <Text style={styles.metaStrong}>{dist}</Text> : null}
        </View>
        {place.address ? (
          <Text style={styles.placeAddr} numberOfLines={1}>
            {place.address}
          </Text>
        ) : null}
      </View>
      <Pressable hitSlop={8} onPress={() => remove.mutate(groupId)} style={styles.removeBtn}>
        <Text style={styles.removeX}>×</Text>
      </Pressable>
    </View>
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

  groupRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14, paddingHorizontal: 20, borderBottomWidth: 1, borderBottomColor: T2.border },
  groupThumb: { width: 52, height: 52, borderRadius: 12, backgroundColor: T2.mapBg, borderWidth: 1, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  groupName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 4 },
  metaStrong: { fontSize: 12, fontWeight: '700', color: T2.text },
  metaDot: { fontSize: 12, color: T2.textMute },
  metaSub: { flexShrink: 1, fontSize: 12, color: T2.textSub },

  newRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 18, paddingHorizontal: 20 },
  newPlus: { width: 40, height: 40, borderRadius: 10, borderWidth: 1.5, borderStyle: 'dashed', borderColor: T2.borderStrong, alignItems: 'center', justifyContent: 'center' },
  newPlusText: { color: T2.textSub, fontSize: 22, fontWeight: '300' },
  newLabel: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },

  backRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: 12, paddingHorizontal: 20, borderBottomWidth: 1, borderBottomColor: T2.border },
  backLeft: { flexDirection: 'row', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 },
  backTitle: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  backCount: { fontSize: 12, fontWeight: '700', color: T2.brand, marginLeft: 2 },
  backActions: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  deleteText: { fontSize: 13, fontWeight: '700', color: T2.textMute },

  empty: { textAlign: 'center', color: T2.textMute, fontSize: 13, paddingVertical: 40 },

  placeRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14, paddingHorizontal: 20, borderBottomWidth: 1, borderBottomColor: T2.border },
  placeThumb: { width: 52, height: 52, borderRadius: 12, backgroundColor: T2.mapBg, borderWidth: 1, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  placeNameRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  placeName: { flexShrink: 1, fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  visitedBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  visitedText: { fontSize: 10, fontWeight: '700', color: T2.textSub },
  placeAddr: { fontSize: 12, color: T2.textMute, marginTop: 4, letterSpacing: -0.2 },
  removeBtn: { width: 28, height: 28, alignItems: 'center', justifyContent: 'center' },
  removeX: { fontSize: 20, color: T2.textMute },
});
