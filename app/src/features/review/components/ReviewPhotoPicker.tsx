// 리뷰 사진 선택·업로드·삭제 블록. 혼밥 리뷰 화면(DiningLogWrite)과 일반 리뷰 화면(ReviewWrite)이 공유한다.
//
// 업로드가 여기서 끝난다 — 부모가 받는 photos는 항상 '업로드 완료된 URL'이라 저장 시 그대로 보내면 된다.
// 업로드 중에는 저장을 막아야 하므로 그 상태만 부모에게 올린다.
import React from 'react';
import { Alert, Image, View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { pickImages, uploadImages, remainingSlots, type PickedAsset } from '@/shared/upload/imageUpload';
import { T2 } from '@/shared/theme';

/** 리뷰 한 건에 붙일 수 있는 사진 수. 서버 ReviewCreateRequest의 @Size(max = 5)와 같아야 한다. */
export const MAX_PHOTOS = 5;

type Props = {
  photos: PickedAsset[];
  onChange: (next: PickedAsset[]) => void;
  /** 업로드 중인지 — 부모가 저장 버튼을 막는 데 쓴다. */
  uploading: boolean;
  onUploadingChange: (v: boolean) => void;
};

export function ReviewPhotoPicker({ photos, onChange, uploading, onUploadingChange }: Props) {
  const onAddPhotos = async () => {
    const picked = await pickImages(remainingSlots(photos.length, MAX_PHOTOS));
    if (picked.length === 0) return;
    // 이미 추가한 사진(assetId 동일)은 제외 — 같은 사진 중복 추가 방지.
    const existingIds = new Set(photos.map((p) => p.assetId).filter((id): id is string => id != null));
    const fresh = picked.filter((a) => !(a.assetId && existingIds.has(a.assetId)));
    if (fresh.length === 0) {
      Alert.alert('이미 추가한 사진이에요', '같은 사진은 다시 추가할 수 없어요.');
      return;
    }
    onUploadingChange(true);
    try {
      const urls = await uploadImages(fresh.map((a) => a.uri));
      const added: PickedAsset[] = urls.map((url, i) => ({ uri: url, assetId: fresh[i]?.assetId ?? null }));
      onChange([...photos, ...added].slice(0, MAX_PHOTOS));
    } catch (e) {
      Alert.alert('사진 업로드 실패', e instanceof Error ? e.message : '다시 시도해주세요.');
    } finally {
      onUploadingChange(false);
    }
  };

  const removePhoto = (idx: number) => onChange(photos.filter((_, i) => i !== idx));

  return (
    <View style={{ marginTop: 28 }}>
      <Text style={styles.label}>사진 ({photos.length}/{MAX_PHOTOS})</Text>

      {photos.length === 0 ? (
        /* 사진 없을 때: 가로 긴 추가 박스 */
        <Pressable onPress={onAddPhotos} disabled={uploading} style={styles.photoAddWide}>
          <Text style={styles.photoAddWideText}>
            {uploading ? '업로드 중…' : '＋  사진을 추가해주세요'}
          </Text>
        </Pressable>
      ) : (
        /* 사진 있을 때: 가로 스크롤 + 오른쪽 하단에 겹친 동그란 ＋ 버튼 */
        <View style={{ marginTop: 12 }}>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={{ marginHorizontal: -2 }}
            contentContainerStyle={{ gap: 10, paddingHorizontal: 2 }}
          >
            {photos.map((p, i) => (
              <View key={`${p.uri}-${i}`} style={styles.photoThumb}>
                <Image source={{ uri: p.uri }} style={styles.photoImg} />
                <Pressable onPress={() => removePhoto(i)} hitSlop={6} style={styles.photoRemove}>
                  <Text style={styles.photoRemoveX}>×</Text>
                </Pressable>
              </View>
            ))}
          </ScrollView>
          {photos.length < MAX_PHOTOS && (
            <Pressable onPress={onAddPhotos} disabled={uploading} hitSlop={6} style={styles.photoAddFab}>
              <Text style={styles.photoAddFabText}>{uploading ? '…' : '＋'}</Text>
            </Pressable>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },
  photoAddWide: { marginTop: 12, height: 88, borderRadius: 14, borderWidth: 1, borderColor: T2.border, borderStyle: 'dashed', alignItems: 'center', justifyContent: 'center', backgroundColor: '#fff' },
  photoAddWideText: { fontSize: 14, fontWeight: '600', color: T2.textMute, letterSpacing: -0.3 },
  photoThumb: { width: 120, height: 120, borderRadius: 14, overflow: 'hidden' },
  photoImg: { width: '100%', height: '100%' },
  photoRemove: { position: 'absolute', top: 5, right: 5, width: 24, height: 24, borderRadius: 12, backgroundColor: 'rgba(0,0,0,0.6)', alignItems: 'center', justifyContent: 'center' },
  photoRemoveX: { color: '#fff', fontSize: 16, lineHeight: 18 },
  photoAddFab: { position: 'absolute', right: 8, bottom: 8, width: 40, height: 40, borderRadius: 20, backgroundColor: T2.brand, borderWidth: 2, borderColor: '#fff', alignItems: 'center', justifyContent: 'center' },
  photoAddFabText: { color: '#fff', fontSize: 22, lineHeight: 24, marginTop: -1 },
});
