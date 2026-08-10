// NewGroup — 그룹 생성/수정(모달). groupId가 있으면 수정모드.
import React, { useState } from 'react';
import { Alert, View, Text, TextInput, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { FAVORITE_COLORS, DEFAULT_FAVORITE_COLOR } from '../favoriteColors';
import { buildGroupBody } from '../buildGroupBody';
import { useCreateFavoriteGroup, useUpdateFavoriteGroup } from '../queries';

export function NewGroupScreen({ navigation, route }: RootStackScreenProps<'NewGroup'>) {
  const groupId = route.params?.groupId;
  const initial = route.params?.initial;
  const isEdit = groupId != null;
  const [name, setName] = useState(initial?.name ?? '');
  const [note, setNote] = useState(initial?.note ?? '');
  const [color, setColor] = useState(initial?.color ?? DEFAULT_FAVORITE_COLOR);
  const createMut = useCreateFavoriteGroup();
  const updateMut = useUpdateFavoriteGroup();
  const canSave = name.trim().length > 0 && !createMut.isPending && !updateMut.isPending;

  const onSave = () => {
    if (!canSave) return;
    const body = buildGroupBody({ name, note, color });
    const onSuccess = () => navigation.goBack();
    const onError = () => Alert.alert('저장 실패', '잠시 후 다시 시도해주세요.');
    if (isEdit) {
      updateMut.mutate({ groupId, body }, { onSuccess, onError });
    } else {
      createMut.mutate(body, { onSuccess, onError });
    }
  };

  return (
    <Screen bg={T2.bg}>
      <View style={styles.topBar}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.cancel}>취소</Text>
        </Pressable>
        <Text style={styles.topTitle}>{isEdit ? '그룹 수정' : '새 그룹'}</Text>
        <Pressable onPress={onSave} hitSlop={10} disabled={!canSave}>
          <Text style={[styles.done, !canSave && { opacity: 0.4 }]}>완료</Text>
        </Pressable>
      </View>

      {/* automaticallyAdjustKeyboardInsets — 아래쪽 입력칸이 키보드에 가리지 않게(DiningLogWrite와 같은 이유). */}
      <ScrollView
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        automaticallyAdjustKeyboardInsets
      >
        <View style={styles.nameRow}>
          <View style={[styles.iconPreview, { borderColor: color }]}>
            <Icon name="star" size={30} color={color} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.fieldLabel}>그룹 이름</Text>
            <TextInput
              style={styles.nameInput}
              value={name}
              onChangeText={setName}
              placeholder="그룹 이름"
              placeholderTextColor={T2.textMute}
              maxLength={20}
            />
            <View style={styles.nameUnderline} />
          </View>
        </View>

        <View style={{ marginTop: 22 }}>
          <Text style={styles.fieldLabel}>색상</Text>
          <View style={styles.colorGrid}>
            {FAVORITE_COLORS.map((c) => {
              const on = c === color;
              return (
                <Pressable
                  key={c}
                  onPress={() => setColor(c)}
                  style={[styles.colorCell, { borderColor: on ? T2.text : T2.border }]}
                >
                  <View style={[styles.colorDot, { backgroundColor: c }]} />
                </Pressable>
              );
            })}
          </View>
        </View>

        <View style={{ marginTop: 26 }}>
          <Text style={styles.fieldLabel}>
            설명 <Text style={{ fontWeight: '600', color: T2.textMute }}>(선택)</Text>
          </Text>
          <TextInput
            style={styles.descInput}
            value={note}
            onChangeText={setNote}
            placeholder="혼밥하기 좋은 곳 모음"
            placeholderTextColor={T2.textMute}
            multiline
            maxLength={60}
          />
        </View>

        <Pressable onPress={onSave} disabled={!canSave} style={[styles.createBtn, !canSave && { opacity: 0.5 }]}>
          <Text style={styles.createText}>{isEdit ? '저장' : '그룹 만들기'}</Text>
        </Pressable>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  topBar: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20 },
  cancel: { fontSize: 15, fontWeight: '600', color: T2.textSub, letterSpacing: -0.3 },
  topTitle: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  done: { fontSize: 15, fontWeight: '800', color: T2.brand, letterSpacing: -0.3 },
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 32 },
  fieldLabel: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 14, marginTop: 8 },
  iconPreview: { width: 60, height: 60, borderRadius: 16, backgroundColor: '#fff', borderWidth: 1.5, alignItems: 'center', justifyContent: 'center' },
  nameInput: { fontSize: 22, fontWeight: '800', color: T2.text, letterSpacing: -0.6, padding: 0, marginTop: 6 },
  nameUnderline: { height: 1.5, backgroundColor: T2.text, marginTop: 8, opacity: 0.85 },
  colorGrid: { marginTop: 12, flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  colorCell: { width: 44, height: 44, borderRadius: 12, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  colorDot: { width: 24, height: 24, borderRadius: 12 },
  descInput: { marginTop: 12, paddingVertical: 14, paddingHorizontal: 16, borderRadius: 12, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border, fontSize: 14, color: T2.text, letterSpacing: -0.2, minHeight: 50, textAlignVertical: 'top' },
  createBtn: { marginTop: 30, paddingVertical: 16, borderRadius: 14, backgroundColor: T2.brand, alignItems: 'center' },
  createText: { fontSize: 16, fontWeight: '800', color: '#fff', letterSpacing: -0.3 },
});
