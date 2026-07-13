// EndHonbabSheet — 혼밥/같이먹기 종료 시트.
// 혼밥(ACTIVE): '밀어서 완료'(ENDED) + '안 먹었어요'(CANCELLED).
// 같이먹기(TOGETHER): '밀어서 완료'(양쪽 ENDED) + '상대가 안 나왔어요' → 노쇼 서브뷰
//   (그래도 혼밥/다시 모집/안 먹고 감 = leaveMatch, 상대는 서버가 SEEKING 복귀+알림 / '이 사람 신고하기').
// checkIn=null이면 렌더 안 함. 닫히면 서브뷰 상태 초기화(다음 열림 대비).
import React, { useEffect, useState } from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SlideToConfirm } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { CheckIn, LeaveMatchTo } from '../api';
import { useEndCheckIn, useCancelCheckIn, useLeaveMatch } from '../queries';

export function EndHonbabSheet({ checkIn, onClose, onReportNoShow }: {
  checkIn: CheckIn | null;
  onClose: () => void;
  /** 같이먹기에서 '이 사람 신고하기' 탭 → 부모가 상대 대상 신고 화면으로 이동. */
  onReportNoShow: (partnerUserId: number, partnerNickname: string) => void;
}) {
  const insets = useSafeAreaInsets();
  const end = useEndCheckIn();
  const cancel = useCancelCheckIn();
  const leave = useLeaveMatch();
  const [noShow, setNoShow] = useState(false); // 같이먹기 '상대가 안 나왔어요' 서브뷰
  useEffect(() => { if (!checkIn) setNoShow(false); }, [checkIn]); // 닫히면 초기화(컴포넌트는 언마운트 안 됨)
  if (!checkIn) return null;

  const together = checkIn.status === 'TOGETHER';
  const complete = () => { end.mutate(checkIn.checkInId); onClose(); };
  const discard = () => { cancel.mutate(checkIn.checkInId); onClose(); };
  const leaveTo = (to: LeaveMatchTo) => { leave.mutate({ checkInId: checkIn.checkInId, to }); onClose(); };
  const report = () => {
    if (checkIn.partnerUserId == null) return;
    onReportNoShow(checkIn.partnerUserId, checkIn.partnerNickname ?? '상대');
    onClose();
  };

  return (
    <>
      <Pressable style={styles.scrim} onPress={onClose} />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + 20 }]}>
        <Pressable style={styles.close} onPress={onClose} hitSlop={8} accessibilityRole="button">
          <Text style={styles.closeX}>×</Text>
        </Pressable>
        <View style={styles.handle} />

        {together && noShow ? (
          // ── 노쇼 서브뷰: 매칭 깨고 내 상태 선택(상대는 서버가 SEEKING 복귀+알림) ──
          <>
            <Text style={styles.title}>상대가 안 나왔어요</Text>
            <Text style={styles.sub}>이제 어떻게 할까요?</Text>
            <Pressable style={[styles.choice, styles.choicePrimary]} onPress={() => leaveTo('ACTIVE')} accessibilityRole="button">
              <Text style={[styles.choiceText, { color: T2.brand }]}>그래도 혼밥할게요</Text>
            </Pressable>
            <Pressable style={styles.choice} onPress={() => leaveTo('SEEKING')} accessibilityRole="button">
              <Text style={styles.choiceText}>다른 사람 기다릴래요</Text>
            </Pressable>
            <Pressable style={styles.choice} onPress={() => leaveTo('CANCELLED')} accessibilityRole="button">
              <Text style={styles.choiceText}>안 먹고 갈게요</Text>
            </Pressable>
            {checkIn.partnerUserId != null && (
              <Pressable style={styles.discard} onPress={report} hitSlop={6} accessibilityRole="button">
                <Text style={styles.discardText}>이 사람 신고하기</Text>
              </Pressable>
            )}
          </>
        ) : (
          <>
            <Text style={styles.title}>{together ? '같이 먹기를 끝낼까요?' : '혼밥을 끝낼까요?'}</Text>
            <Text style={styles.sub}>다 드셨으면 밀어서 완료하세요.</Text>
            <SlideToConfirm label="밀어서 완료" onConfirm={complete} style={styles.slide} />
            {together ? (
              <Pressable style={styles.discard} onPress={() => setNoShow(true)} hitSlop={6} accessibilityRole="button">
                <Text style={styles.discardText}>상대가 안 나왔어요</Text>
              </Pressable>
            ) : (
              <Pressable style={styles.discard} onPress={discard} hitSlop={6} accessibilityRole="button">
                <Text style={styles.discardText}>안 먹었어요(기록 안 함)</Text>
              </Pressable>
            )}
          </>
        )}
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
  choice: { marginTop: 10, paddingVertical: 15, borderRadius: 12, borderWidth: 1.5, borderColor: T2.border, alignItems: 'center' },
  choicePrimary: { marginTop: 18, borderColor: T2.brand, backgroundColor: T2.brandSoft },
  choiceText: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  discard: { alignItems: 'center', paddingVertical: 14, marginTop: 8 },
  discardText: { fontSize: 13.5, fontWeight: '700', color: T2.textMute, letterSpacing: -0.3 },
});
