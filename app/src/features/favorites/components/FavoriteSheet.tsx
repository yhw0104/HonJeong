// FavoriteSheet — 식당상세 하트용 그룹 선택 바텀시트(RN 기본 Modal). 체크 토글로 담기/빼기, 인라인 새 그룹.
import React, { useState } from 'react';
import { Modal, View, Text, Pressable, ScrollView, TextInput, StyleSheet } from 'react-native';
import { Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { DEFAULT_FAVORITE_COLOR } from '../favoriteColors';
import {
  useFavoriteStatus,
  useAddPlaceToGroup,
  useRemovePlaceFromGroup,
  useCreateFavoriteGroup,
} from '../queries';

export function FavoriteSheet({ placeId, visible, onClose }: { placeId: number; visible: boolean; onClose: () => void }) {
  const statusQ = useFavoriteStatus(placeId);
  const add = useAddPlaceToGroup(placeId);
  const remove = useRemovePlaceFromGroup(placeId);
  const createGroup = useCreateFavoriteGroup();
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState('');
  const groups = statusQ.data?.groups ?? [];

  const toggle = (groupId: number, contains: boolean) => {
    if (contains) remove.mutate(groupId);
    else add.mutate(groupId);
  };

  const submitNew = () => {
    const name = newName.trim();
    if (name.length === 0) return;
    createGroup.mutate(
      { name, color: DEFAULT_FAVORITE_COLOR },
      {
        onSuccess: (group) => {
          add.mutate(group.groupId);
          setNewName('');
          setCreating(false);
        },
      },
    );
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} />
      <View style={styles.panel}>
        <View style={styles.handle} />
        <Text style={styles.title}>어디에 저장할까요?</Text>
        <ScrollView style={{ maxHeight: 320 }}>
          {groups.map((g) => (
            <Pressable key={g.groupId} style={styles.row} onPress={() => toggle(g.groupId, g.contains)}>
              <Icon name="star" size={22} color={g.color} />
              <Text style={styles.rowName}>{g.name}</Text>
              <View style={[styles.check, g.contains && styles.checkOn]}>
                {g.contains ? <Text style={styles.checkMark}>✓</Text> : null}
              </View>
            </Pressable>
          ))}
        </ScrollView>

        {creating ? (
          <View style={styles.newRow}>
            <TextInput
              style={styles.newInput}
              value={newName}
              onChangeText={setNewName}
              placeholder="새 그룹 이름"
              placeholderTextColor={T2.textMute}
              maxLength={20}
              autoFocus
              onSubmitEditing={submitNew}
            />
            <Pressable onPress={submitNew} hitSlop={8}>
              <Text style={styles.newAdd}>추가</Text>
            </Pressable>
          </View>
        ) : (
          <Pressable style={styles.newTrigger} onPress={() => setCreating(true)}>
            <Text style={styles.newTriggerText}>＋ 새 그룹 만들기</Text>
          </Pressable>
        )}

        <Pressable style={styles.doneBtn} onPress={onClose}>
          <Text style={styles.doneText}>완료</Text>
        </Pressable>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)' },
  panel: { backgroundColor: T2.bg, borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingHorizontal: 20, paddingTop: 10, paddingBottom: 28 },
  handle: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, backgroundColor: T2.border, marginBottom: 14 },
  title: { fontSize: 18, fontWeight: '800', color: T2.text, letterSpacing: -0.4, marginBottom: 12 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  rowName: { flex: 1, fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  check: { width: 24, height: 24, borderRadius: 12, borderWidth: 1.5, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  checkOn: { backgroundColor: T2.brand, borderColor: T2.brand },
  checkMark: { color: '#fff', fontSize: 14, fontWeight: '800' },
  newRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 12, marginTop: 4 },
  newInput: { flex: 1, fontSize: 15, color: T2.text, borderBottomWidth: 1.5, borderBottomColor: T2.text, paddingVertical: 6 },
  newAdd: { fontSize: 14, fontWeight: '800', color: T2.brand },
  newTrigger: { paddingVertical: 14, marginTop: 4 },
  newTriggerText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  doneBtn: { marginTop: 8, paddingVertical: 15, borderRadius: 14, backgroundColor: T2.brand, alignItems: 'center' },
  doneText: { fontSize: 16, fontWeight: '800', color: '#fff', letterSpacing: -0.3 },
});
