// HonbabStatusBar — 체크인 상태 카드(모집중/혼밥중/같이먹는중).
// 룩: MapHome의 "지금 연남동에서 혼밥 중" 카운터와 동일한 흰색 카드(둥근 모서리 + 옅은 그림자 + 브랜드 펄스).
// 위치는 호출하는 화면이 정한다(MapHome=검색창 아래 흐름 배치 / RestaurantDetail=상단 절대배치 래퍼).
// style prop으로 마진/배치만 덧붙인다.
import React from 'react';
import { View, Text, Pressable, StyleSheet, type StyleProp, type ViewStyle } from 'react-native';
import { T2 } from '@/shared/theme';
import type { CheckInMode } from '@/features/checkin/statusView';

// 카드 높이. 절대배치하는 화면이 상단 컨트롤을 카드 아래로 내릴 때 참조.
export const HONBAB_BAR_H = 44;

type Props = {
  mode: CheckInMode;           // 'seeking' | 'dining' | 'together'
  place: string;
  partnerNickname?: string | null;
  onEnd: () => void;           // 끝내기(dining/together)
  onDineAlone?: () => void;    // 혼자 먹기 시작(seeking)
  onQuit?: () => void;         // 그만두기(seeking → 취소)
  onOpenChat?: () => void;     // 라벨 탭 → 대화방 진입(together 전용)
  style?: StyleProp<ViewStyle>;
};

export function HonbabStatusBar({ mode, place, partnerNickname, onEnd, onDineAlone, onQuit, onOpenChat, style }: Props) {
  const strong = mode === 'together' ? '같이 먹는 중' : mode === 'seeking' ? '같이 먹을 사람 구하는 중' : '혼밥 중';
  const dim = mode === 'together' && partnerNickname ? `  ·  ${partnerNickname} · ${place}` : `  ·  ${place}`;
  const labelText = (
    <Text style={styles.label} numberOfLines={1}>
      <Text style={styles.labelStrong}>{strong}</Text>
      <Text style={styles.labelDim}>{dim}</Text>
    </Text>
  );
  return (
    <View style={[styles.card, style]}>
      <View style={styles.pulse}>
        <View style={styles.halo} />
        <View style={styles.dot} />
      </View>
      {mode === 'together' ? (
        <Pressable style={styles.labelWrap} onPress={onOpenChat} hitSlop={6}>
          {labelText}
        </Pressable>
      ) : (
        <View style={styles.labelWrap}>{labelText}</View>
      )}
      {mode === 'seeking' ? (
        <View style={{ flexDirection: 'row', gap: 6 }}>
          <Pressable style={styles.quitBtn} onPress={onQuit} hitSlop={6}>
            <Text style={styles.quitText}>그만두기</Text>
          </Pressable>
          <Pressable style={styles.endBtn} onPress={onDineAlone} hitSlop={6}>
            <Text style={styles.endText}>혼자 먹기</Text>
          </Pressable>
        </View>
      ) : (
        <Pressable style={styles.endBtn} onPress={onEnd} hitSlop={6}>
          <Text style={styles.endText}>끝내기</Text>
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    height: HONBAB_BAR_H,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 14,
    backgroundColor: '#fff',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 3,
  },
  labelWrap: { flex: 1, justifyContent: 'center' },
  pulse: { width: 8, height: 8, alignItems: 'center', justifyContent: 'center' },
  halo: { position: 'absolute', width: 16, height: 16, borderRadius: 8, backgroundColor: T2.brand, opacity: 0.18 },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: T2.brand },
  label: { fontSize: 13, letterSpacing: -0.2 },
  labelStrong: { color: T2.text, fontWeight: '800' },
  labelDim: { color: T2.textSub, fontWeight: '600' },
  endBtn: { paddingHorizontal: 11, paddingVertical: 6, borderRadius: 999, backgroundColor: T2.brandSoft },
  endText: { fontSize: 12, fontWeight: '800', color: T2.brand, letterSpacing: -0.2 },
  quitBtn: { paddingHorizontal: 11, paddingVertical: 6, borderRadius: 999, backgroundColor: T2.bg },
  quitText: { fontSize: 12, fontWeight: '700', color: T2.textSub, letterSpacing: -0.2 },
});
