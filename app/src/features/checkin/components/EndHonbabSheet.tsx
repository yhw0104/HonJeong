// EndHonbabSheet — 혼밥/같이먹기 종료 시트. '밀어서 완료'(ENDED)가 기본, 작게 '취소'(CANCELLED).
// 실수 탭 방지: 끝내기 탭은 이 시트를 열 뿐(무해), 완료는 일부러 슬라이드해야 확정된다.
// checkIn=null이면 아무 것도 렌더하지 않는다(닫히면 언마운트 → 슬라이드 상태 초기화).
import React from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SlideToConfirm } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { CheckIn } from '../api';
import { useEndCheckIn, useCancelCheckIn } from '../queries';

export function EndHonbabSheet({ checkIn, onClose }: { checkIn: CheckIn | null; onClose: () => void }) {
  const insets = useSafeAreaInsets();
  const end = useEndCheckIn();
  const cancel = useCancelCheckIn();
  if (!checkIn) return null;

  const together = checkIn.status === 'TOGETHER';
  const complete = () => { end.mutate(checkIn.checkInId); onClose(); };
  const discard = () => { cancel.mutate(checkIn.checkInId); onClose(); };

  return (
    <>
      <Pressable style={styles.scrim} onPress={onClose} />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + 20 }]}>
        <Pressable style={styles.close} onPress={onClose} hitSlop={8} accessibilityRole="button">
          <Text style={styles.closeX}>×</Text>
        </Pressable>
        <View style={styles.handle} />
        <Text style={styles.title}>{together ? '같이 먹기를 끝낼까요?' : '혼밥을 끝낼까요?'}</Text>
        <Text style={styles.sub}>다 드셨으면 밀어서 완료하세요.</Text>
        <SlideToConfirm label="밀어서 완료" onConfirm={complete} style={styles.slide} />
        <Pressable style={styles.discard} onPress={discard} hitSlop={6} accessibilityRole="button">
          <Text style={styles.discardText}>안 먹었어요(기록 안 함)</Text>
        </Pressable>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  scrim: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 90, backgroundColor: 'rgba(10,10,10,0.4)' },
  sheet: {
    position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 91,
    backgroundColor: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24,
    paddingTop: 12, paddingHorizontal: 20,
    shadowColor: '#000', shadowOffset: { width: 0, height: -8 }, shadowOpacity: 0.18, shadowRadius: 30, elevation: 12,
  },
  close: { position: 'absolute', top: 10, right: 12, width: 34, height: 34, alignItems: 'center', justifyContent: 'center', zIndex: 2 },
  closeX: { fontSize: 24, color: T2.textMute, lineHeight: 26 },
  handle: { width: 36, height: 4, borderRadius: 2, backgroundColor: '#E5E5E5', alignSelf: 'center', marginBottom: 18 },
  title: { fontSize: 20, fontWeight: '800', color: T2.text, letterSpacing: -0.5 },
  sub: { fontSize: 13, color: T2.textMute, marginTop: 6, letterSpacing: -0.3 },
  slide: { marginTop: 20 },
  discard: { alignItems: 'center', paddingVertical: 14, marginTop: 8 },
  discardText: { fontSize: 13.5, fontWeight: '700', color: T2.textMute, letterSpacing: -0.3 },
});
