// DirectionsSheet — 길찾기: 네이버지도/카카오맵 선택 바텀시트.
// EndHonbabSheet와 동일한 톤(스크림 + 아래에서 올라오는 시트 + 우상단 X).
// 각 지도앱을 브랜드색 타일(네이버 그린 / 카카오 옐로) + 핀으로 구분해 보여준다.
import React from 'react';
import { View, Text, Pressable, StyleSheet, Image, Animated, type ImageSourcePropType } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon } from '@/shared/components';
import { useSheetDismissGesture } from '@/shared/components/useSheetDismissGesture';
import { T2 } from '@/shared/theme';

export type MapProvider = 'naver' | 'kakao';

// const NAVER_GREEN = '#03C75A'; // 네이버 브랜드 그린 — 네이버 항목 숨김(공식 로고 확보 시 부활)
const KAKAO_LOGO = require('../../../../assets/map/kakao.png'); // 카카오맵 공식 앱아이콘

export function DirectionsSheet({ visible, placeName, onClose, onPick }: {
  visible: boolean;
  placeName: string;
  onClose: () => void;
  onPick: (provider: MapProvider) => void;
}) {
  const insets = useSafeAreaInsets();
  const dismiss = useSheetDismissGesture(visible, onClose); // 아래로 끌어 닫기
  if (!visible) return null;

  const rows: { key: MapProvider; label: string; logo?: ImageSourcePropType; bg?: string; pin?: string }[] = [
    // 네이버는 공식 로고가 없어 임시로 숨김. 로고 확보 시 NAVER_GREEN과 함께 되살리기.
    // { key: 'naver', label: '네이버지도', bg: NAVER_GREEN, pin: '#fff' },
    { key: 'kakao', label: '카카오맵', logo: KAKAO_LOGO },
  ];

  return (
    <>
      <Pressable style={styles.scrim} onPress={onClose} />
      <Animated.View
        style={[styles.sheet, { paddingBottom: insets.bottom + 14, transform: [{ translateY: dismiss.translateY }] }]}
      >
        <Pressable style={styles.close} onPress={onClose} hitSlop={8} accessibilityRole="button">
          <Text style={styles.closeX}>×</Text>
        </Pressable>
        {/* 끌어 내리는 영역 — 손잡이와 제목까지. 아래 지도앱 목록은 탭이 우선이라 제외한다. */}
        <View {...dismiss.panHandlers} style={styles.grabArea}>
          <View style={styles.handle} />
          <Text style={styles.title}>길찾기</Text>
          <Text style={styles.sub} numberOfLines={1}>'{placeName}'을(를) 어떤 지도로 열까요?</Text>
        </View>

        <View style={styles.list}>
          {rows.map((r) => (
            <Pressable key={r.key} style={styles.row} onPress={() => onPick(r.key)} accessibilityRole="button">
              {r.logo ? (
                <Image source={r.logo} style={styles.tile} resizeMode="cover" />
              ) : (
                <View style={[styles.tile, { backgroundColor: r.bg }]}>
                  <Icon name="pin" size={22} color={r.pin} />
                </View>
              )}
              <Text style={styles.rowLabel}>{r.label}</Text>
              <Icon name="chevronRight" size={16} color={T2.textMute} />
            </Pressable>
          ))}
        </View>
      </Animated.View>
    </>
  );
}

const styles = StyleSheet.create({
  scrim: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 90, backgroundColor: 'rgba(10,10,10,0.4)' },
  sheet: {
    position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 91,
    backgroundColor: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24,
    paddingTop: 10, paddingHorizontal: 20,
    shadowColor: '#000', shadowOffset: { width: 0, height: -8 }, shadowOpacity: 0.18, shadowRadius: 30, elevation: 12,
  },
  close: { position: 'absolute', top: 10, right: 12, width: 34, height: 34, alignItems: 'center', justifyContent: 'center', zIndex: 2 },
  closeX: { fontSize: 24, color: T2.textMute, lineHeight: 26 },
  // 손잡이(4px)만으로는 잡기 어려워, 제목까지 포함한 넓은 영역에 제스처를 건다.
  grabArea: { paddingTop: 2, paddingBottom: 2 },
  handle: { width: 36, height: 4, borderRadius: 2, backgroundColor: '#E5E5E5', alignSelf: 'center', marginBottom: 12 },
  title: { fontSize: 20, fontWeight: '800', color: T2.text, letterSpacing: -0.5 },
  sub: { fontSize: 13, color: T2.textMute, marginTop: 6, letterSpacing: -0.3 },
  list: { marginTop: 16, gap: 10 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 10, paddingHorizontal: 12, borderRadius: 14, borderWidth: 1, borderColor: T2.border, backgroundColor: T2.bg },
  tile: { width: 40, height: 40, borderRadius: 11, alignItems: 'center', justifyContent: 'center' },
  rowLabel: { flex: 1, fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
});
