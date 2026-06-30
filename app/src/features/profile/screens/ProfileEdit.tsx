// ProfileEdit — 프로필 편집 (원본: screens/ProfileEdit.jsx)
// MyProfile '편집'에서 모달로 진입. 닉네임/소개/동네/음식/성향 편집.
import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet, Alert } from 'react-native';
import { Screen, FieldLabel, Avatar, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useMyProfile, useUpdateMyProfile } from '@/features/users/queries';
import { pickImages, uploadImages } from '@/shared/upload/imageUpload';
import type { RootStackScreenProps } from '@/navigation/types';

const FOODS = ['한식', '일식', '양식', '중식', '면 요리', '매운맛', '디저트'];
const STYLES_OPT = [
  { key: 'talk', label: '도란도란 대화하며', sub: '가볍게 이야기 나누는 게 좋아요' },
  { key: 'quiet', label: '조용히 각자', sub: '편하게, 말 없이 먹어도 좋아요' },
];

export function ProfileEditScreen({ navigation }: RootStackScreenProps<'ProfileEdit'>) {
  const { data: profile } = useMyProfile();
  const update = useUpdateMyProfile();

  const [nickname, setNickname] = useState('');
  const [bio, setBio] = useState('');
  const [foods, setFoods] = useState<string[]>([]);
  const [style, setStyle] = useState('talk');
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  // 서버 프로필이 도착하면 폼 초깃값을 채운다(최초 동기화).
  useEffect(() => {
    if (!profile) return;
    setNickname(profile.nickname ?? '');
    setBio(profile.introduction ?? '');
    setFoods(profile.favoriteFoods ?? []);
    setStyle(profile.diningStyle === 'QUIET' ? 'quiet' : 'talk');
    setImageUrl(profile.profileImageUrl ?? null);
  }, [profile]);

  // 사진 변경: 갤러리에서 1장 선택 → POST /api/files 업로드 → 미리보기 url 교체(저장 시 PATCH로 전송).
  const onChangePhoto = async () => {
    if (uploading) return;
    const picked = await pickImages(1);
    if (picked.length === 0) return;
    setUploading(true);
    try {
      const [url] = await uploadImages([picked[0].uri]);
      setImageUrl(url);
    } catch {
      Alert.alert('업로드 실패', '사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setUploading(false);
    }
  };

  const toggleFood = (f: string) => {
    setFoods((prev) => {
      if (prev.includes(f)) return prev.filter((x) => x !== f);
      if (prev.length >= 3) return prev;
      return [...prev, f];
    });
  };

  const onSave = () => {
    update.mutate(
      {
        nickname: nickname.trim(),
        introduction: bio,
        diningStyle: style === 'quiet' ? 'QUIET' : 'TALK',
        favoriteFoods: foods,
        profileImageUrl: imageUrl ?? undefined,
      },
      {
        onSuccess: () => navigation.goBack(),
        onError: () => Alert.alert('저장 실패', '잠시 후 다시 시도해주세요.'),
      },
    );
  };

  return (
    <Screen bg={T2.bg}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.cancel}>취소</Text>
        </Pressable>
        <Text style={styles.title}>프로필 편집</Text>
        <Pressable onPress={onSave} disabled={update.isPending} hitSlop={10}>
          <Text style={[styles.save, update.isPending && { opacity: 0.4 }]}>저장</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        {/* 사진 변경 */}
        <Pressable style={styles.photoBlock} onPress={onChangePhoto} disabled={uploading}>
          <View>
            <Avatar uri={imageUrl} bg={T2.bg} size={84} />
            <View style={styles.cameraBadge}>
              <Icon name="camera" size={15} color="#fff" />
            </View>
          </View>
          <Text style={styles.photoChange}>{uploading ? '업로드 중…' : '사진 변경'}</Text>
        </Pressable>

        {/* 닉네임 */}
        <View style={{ marginTop: 20 }}>
          <FieldLabel>닉네임</FieldLabel>
          <View style={styles.fieldBox}>
            <TextInput
              style={styles.fieldInput}
              value={nickname}
              onChangeText={(t) => setNickname(t.slice(0, 12))}
              maxLength={12}
              placeholder="닉네임"
              placeholderTextColor={T2.textMute}
            />
            <Text style={styles.counter}>{nickname.length} / 12</Text>
          </View>
        </View>

        {/* 한 줄 소개 */}
        <View style={{ marginTop: 24 }}>
          <FieldLabel>한 줄 소개</FieldLabel>
          <TextInput
            style={styles.bioInput}
            value={bio}
            onChangeText={setBio}
            multiline
            maxLength={80}
            placeholder="나를 한 줄로 소개해보세요"
            placeholderTextColor={T2.textMute}
          />
        </View>

        {/* 내 동네 */}
        <View style={{ marginTop: 24 }}>
          <FieldLabel>내 동네</FieldLabel>
          <View style={[styles.fieldBox, { gap: 8 }]}>
            <Icon name="pin" size={16} color={T2.textMute} />
            <Text style={styles.hoodText}>연남동</Text>
            <Text style={styles.changeText}>변경</Text>
          </View>
        </View>

        {/* 좋아하는 음식 */}
        <View style={{ marginTop: 24 }}>
          <FieldLabel>좋아하는 음식 · 최대 3개</FieldLabel>
          <View style={styles.chips}>
            {FOODS.map((f) => {
              const on = foods.includes(f);
              return (
                <Pressable
                  key={f}
                  onPress={() => toggleFood(f)}
                  style={[styles.chip, { backgroundColor: on ? T2.brand : '#fff', borderColor: on ? T2.brand : T2.border }]}
                >
                  <Text style={{ fontSize: 13, fontWeight: '600', color: on ? '#fff' : T2.text }}>{f}</Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {/* 같이 먹을 때 */}
        <View style={{ marginTop: 24 }}>
          <FieldLabel>같이 먹을 때</FieldLabel>
          {STYLES_OPT.map((r) => {
            const on = style === r.key;
            return (
              <Pressable
                key={r.key}
                onPress={() => setStyle(r.key)}
                style={[styles.radioRow, { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border }]}
              >
                <View style={[styles.radio, { borderColor: on ? T2.brand : T2.borderStrong, backgroundColor: on ? T2.brand : 'transparent' }]}>
                  {on ? <View style={styles.radioInner} /> : null}
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={{ fontSize: 15, fontWeight: '700', color: on ? '#fff' : T2.text, letterSpacing: -0.3 }}>{r.label}</Text>
                  <Text style={{ fontSize: 12, color: on ? 'rgba(255,255,255,0.6)' : T2.textMute, marginTop: 1 }}>{r.sub}</Text>
                </View>
              </Pressable>
            );
          })}
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20 },
  cancel: { fontSize: 14, fontWeight: '600', color: T2.textSub, letterSpacing: -0.2 },
  title: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  save: { fontSize: 14, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },

  scroll: { paddingHorizontal: 20, paddingTop: 12, paddingBottom: 40 },

  photoBlock: { alignItems: 'center', paddingBottom: 8 },
  cameraBadge: {
    position: 'absolute',
    right: -2,
    bottom: -2,
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: T2.brand,
    borderWidth: 3,
    borderColor: T2.bg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  photoChange: { fontSize: 13, fontWeight: '700', color: T2.brand, marginTop: 12, letterSpacing: -0.2 },

  fieldBox: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    height: 52,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: T2.border,
  },
  fieldInput: { flex: 1, fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3, padding: 0 },
  counter: { fontSize: 12, color: T2.textMute },
  hoodText: { flex: 1, fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  changeText: { fontSize: 13, fontWeight: '700', color: T2.brand },

  bioInput: {
    padding: 16,
    minHeight: 76,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: T2.border,
    fontSize: 14,
    color: T2.text,
    lineHeight: 22,
    letterSpacing: -0.3,
    textAlignVertical: 'top',
  },

  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { paddingVertical: 9, paddingHorizontal: 14, borderRadius: 999, borderWidth: 1 },

  radioRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14, paddingHorizontal: 16, borderRadius: 12, marginBottom: 8, borderWidth: 1 },
  radio: { width: 18, height: 18, borderRadius: 9, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  radioInner: { width: 6, height: 6, borderRadius: 3, backgroundColor: T2.text },
});
