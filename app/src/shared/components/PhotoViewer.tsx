// PhotoViewer — 사진 한 장을 전체화면으로 크게 보는 뷰어.
// 프로필 사진을 탭했을 때 원본을 확인하는 용도(내 프로필·메이트 프로필 공용).
//
// 닫기는 **화면 아무 곳이나 탭**으로 한다. 시트들처럼 아래로 쓸어 닫기도 검토했지만,
// 여기선 화면 전체가 닫기 버튼이라 굳이 제스처를 얹을 이유가 없다 — 오히려 사진 위에
// 제스처를 걸면 나중에 확대/이동을 붙일 때 서로 충돌한다.
//
// uri가 null이면 렌더하지 않는다(= 닫힌 상태). 호출부는 상태 하나만 들고 있으면 된다.
import React from 'react';
import { Modal, View, Text, Image, Pressable, StyleSheet, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

export function PhotoViewer({ uri, onClose }: { uri: string | null; onClose: () => void }) {
  const { width, height } = useWindowDimensions();
  const insets = useSafeAreaInsets();

  return (
    <Modal
      visible={uri != null}
      transparent
      animationType="fade"
      onRequestClose={onClose} // 안드로이드 뒤로가기
      statusBarTranslucent
    >
      {/* 배경 전체가 닫기 영역. 사진 자체를 눌러도 닫힌다(뷰어에서 사진을 누를 다른 이유가 없다). */}
      <Pressable style={styles.backdrop} onPress={onClose} accessibilityRole="button" accessibilityLabel="사진 닫기">
        {uri ? (
          // contain — 세로로 긴 사진도 잘리지 않게. 정사각 프로필이 대부분이라 보통 가로폭에 맞는다.
          <Image source={{ uri }} style={{ width, height }} resizeMode="contain" />
        ) : null}
      </Pressable>
      {/* 탭으로 닫힌다는 걸 모를 수 있으니 X도 함께 둔다. */}
      <Pressable
        style={[styles.close, { top: insets.top + 8 }]}
        onPress={onClose}
        hitSlop={12}
        accessibilityRole="button"
        accessibilityLabel="닫기"
      >
        <View style={styles.closeBg}>
          <Text style={styles.closeX}>×</Text>
        </View>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.92)', alignItems: 'center', justifyContent: 'center' },
  close: { position: 'absolute', right: 14 },
  closeBg: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.16)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeX: { fontSize: 22, color: '#fff', lineHeight: 24 },
});
