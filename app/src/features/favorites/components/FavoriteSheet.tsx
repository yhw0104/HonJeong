// FavoriteSheet — 식당상세 하트용 그룹 선택 바텀시트(RN 기본 Modal). 체크 토글로 담기/빼기, 인라인 새 그룹.
import React, { useEffect, useState } from 'react';
import { Modal, View, Text, Pressable, ScrollView, TextInput, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native';
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

  // ★시트가 닫히면 입력 상태를 되돌린다. Modal은 visible=false로도 언마운트되지 않아
  // creating이 true로 남고, 다시 열면 그 TextInput이 포커스를 되찾아 **키보드가 같이 올라왔다**
  // (실기 지적). 여는 쪽이 아니라 닫히는 쪽에서 치워야 다음 열림이 항상 깨끗하다.
  useEffect(() => {
    if (!visible) {
      setCreating(false);
      setNewName('');
    }
  }, [visible]);

  const toggle = (groupId: number, contains: boolean) => {
    if (contains) remove.mutate(groupId);
    else add.mutate(groupId);
  };

  // 바깥을 눌렀을 때. 이름을 입력 중이면 **입력만** 취소한다 — 키보드를 내리려고 빈 곳을 눌렀는데
  // 시트까지 닫혀버리던 문제(실기 지적). 입력 중이 아니면 원래대로 시트를 닫는다.
  const onBackdrop = () => {
    if (creating) {
      setCreating(false);
      setNewName('');
      return; // TextInput이 사라지면서 키보드도 함께 내려간다
    }
    onClose();
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
      {/*
        KeyboardAvoidingView — 키보드가 올라오면 시트를 그 위로 밀어 올린다. 없으면 새 그룹
        이름을 칠 때 입력칸이 키보드에 가려진다(실기 지적). 배경(flex:1)이 줄어들며 시트가 올라간다.
      */}
      <KeyboardAvoidingView style={styles.root} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <Pressable style={styles.backdrop} onPress={onBackdrop} />
        <View style={styles.panel}>
          <View style={styles.handle} />
          <Text style={styles.title}>어디에 저장할까요?</Text>
          {/* 입력 중에는 목록을 줄인다 — 시트가 키보드 위 좁은 공간에 다 들어가야 한다. */}
          <ScrollView style={{ maxHeight: creating ? 200 : 320 }} keyboardDismissMode="on-drag">
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
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, justifyContent: 'flex-end' },
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
