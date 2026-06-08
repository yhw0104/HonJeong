// NewGroup — 새 그룹 만들기 (원본: screens/NewGroup.jsx)
// 즐겨찾기 "새 그룹 만들기"에서 모달로 진입. 이름/이모지/설명/공개 토글.
import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, Toggle } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const EMOJIS = ['🍜', '🍣', '🍲', '🍙', '🍛', '🍔', '☕️', '🍱'];

export function NewGroupScreen({ navigation }: RootStackScreenProps<'NewGroup'>) {
  const [name, setName] = useState('주말 혼밥 코스');
  const [sel, setSel] = useState(0);
  const [desc, setDesc] = useState('');
  const [secret, setSecret] = useState(true);

  return (
    <Screen bg={T2.bg}>
      {/* 상단 바 */}
      <View style={styles.topBar}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.cancel}>취소</Text>
        </Pressable>
        <Text style={styles.topTitle}>새 그룹</Text>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.done}>완료</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {/* 아이콘 + 이름 */}
        <View style={styles.nameRow}>
          <View style={styles.iconPreview}>
            <Text style={{ fontSize: 28 }}>{EMOJIS[sel]}</Text>
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

        {/* 아이콘 선택 */}
        <View style={{ marginTop: 22 }}>
          <Text style={styles.fieldLabel}>아이콘</Text>
          <View style={styles.emojiGrid}>
            {EMOJIS.map((e, i) => {
              const on = i === sel;
              return (
                <Pressable
                  key={e}
                  onPress={() => setSel(i)}
                  style={[
                    styles.emojiCell,
                    { backgroundColor: on ? T2.brandSoft : '#fff', borderColor: on ? T2.brand : T2.border },
                  ]}
                >
                  <Text style={{ fontSize: 20 }}>{e}</Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {/* 설명 */}
        <View style={{ marginTop: 26 }}>
          <Text style={styles.fieldLabel}>
            설명 <Text style={{ fontWeight: '600', color: T2.textMute }}>(선택)</Text>
          </Text>
          <TextInput
            style={styles.descInput}
            value={desc}
            onChangeText={setDesc}
            placeholder="혼밥하기 좋은 주말 코스 모음"
            placeholderTextColor={T2.textMute}
            multiline
            maxLength={60}
          />
        </View>

        {/* 공개 설정 */}
        <View style={styles.secretRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.secretTitle}>나만 보기</Text>
            <Text style={styles.secretSub}>끄면 메이트에게 공개돼요</Text>
          </View>
          <Toggle value={secret} onValueChange={setSecret} />
        </View>

        {/* 만들기 버튼 */}
        <Pressable
          onPress={() => navigation.goBack()}
          style={({ pressed }) => [styles.createBtn, { opacity: pressed ? 0.9 : 1 }]}
        >
          <Text style={styles.createText}>그룹 만들기</Text>
        </Pressable>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  topBar: {
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
  },
  cancel: { fontSize: 15, fontWeight: '600', color: T2.textSub, letterSpacing: -0.3 },
  topTitle: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  done: { fontSize: 15, fontWeight: '800', color: T2.brand, letterSpacing: -0.3 },

  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 32 },

  fieldLabel: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },

  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 14, marginTop: 8 },
  iconPreview: {
    width: 60,
    height: 60,
    borderRadius: 16,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nameInput: { fontSize: 22, fontWeight: '800', color: T2.text, letterSpacing: -0.6, padding: 0, marginTop: 6 },
  nameUnderline: { height: 1.5, backgroundColor: T2.text, marginTop: 8, opacity: 0.85 },

  emojiGrid: { marginTop: 12, flexDirection: 'row', flexWrap: 'wrap', gap: 9 },
  emojiCell: {
    width: 44,
    height: 44,
    borderRadius: 12,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },

  descInput: {
    marginTop: 12,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
    fontSize: 14,
    color: T2.text,
    letterSpacing: -0.2,
    minHeight: 50,
    textAlignVertical: 'top',
  },

  secretRow: {
    marginTop: 22,
    paddingVertical: 16,
    paddingHorizontal: 18,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  secretTitle: { fontSize: 14, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  secretSub: { fontSize: 11, color: T2.textMute, marginTop: 2 },

  createBtn: {
    marginTop: 30,
    paddingVertical: 16,
    borderRadius: 14,
    backgroundColor: T2.brand,
    alignItems: 'center',
    shadowColor: T2.brand,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.28,
    shadowRadius: 18,
    elevation: 4,
  },
  createText: { fontSize: 16, fontWeight: '800', color: '#fff', letterSpacing: -0.3 },
});
