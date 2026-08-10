// EndHonbabSheet — 혼밥/같이먹기 종료 시트.
// 혼밥(ACTIVE): '밀어서 완료'(ENDED) + '안 먹었어요'(CANCELLED).
// 같이먹기(TOGETHER): '밀어서 완료'(양쪽 ENDED) + '상대가 안 나왔어요' → 노쇼 서브뷰
//   (그래도 혼밥/다시 모집/안 먹고 감 = leaveMatch, 상대는 서버가 SEEKING 복귀+알림 / '이 사람 신고하기').
//   + '제가 못 가게 됐어요' → 사전 취소 서브뷰(확인 1회 → leaveMatch CANCELLED, 신고 없음).
// checkIn=null이면 렌더 안 함. 닫히면 서브뷰 상태 초기화(다음 열림 대비).
import React, { useEffect, useState } from 'react';
import { View, Text, Pressable, StyleSheet, Animated } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SlideToConfirm } from '@/shared/components';
import { useSheetDismissGesture } from '@/shared/components/useSheetDismissGesture';
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
  const [cantGo, setCantGo] = useState(false); // 같이먹기 '제가 못 가게 됐어요'(사전 취소) 서브뷰
  useEffect(() => { if (!checkIn) { setNoShow(false); setCantGo(false); } }, [checkIn]); // 닫히면 초기화(컴포넌트는 언마운트 안 됨)
  // 아래로 끌어 닫기. 핸들러는 아래 헤더 영역에만 붙인다 — '밀어서 완료'(가로 드래그)를 삼키지 않게.
  const dismiss = useSheetDismissGesture(checkIn != null, onClose);

  // ★시트가 열려 있는 동안 화면 뒤로가기 스와이프를 끈다. 이 시트를 쓰는 화면마다가 아니라
  // **시트 자신이** 끄는 게 핵심이다.
  //
  // 왜: '밀어서 완료'(SlideToConfirm)는 가로 드래그인데 iOS 뒤로가기도 같은 방향 가로 드래그다.
  // 둘이 겹치면 네이티브 제스처가 이겨서, 완료하려고 밀면 이전 화면으로 나가버린다. 특히 썸의
  // 시작 위치가 화면 왼쪽에서 ~24pt(시트 패딩 20 + 트랙 패딩 4)라 엣지 영역과 정확히 겹친다.
  //
  // 08-04에 식당 상세에서 이 문제를 고치면서 "홈 탭은 탭 루트라 뒤로가기 제스처가 없으니 안전"하다고
  // 적어 뒀는데 **그 전제가 틀렸다** — 알림에서 '수락됨'을 눌러 홈으로 온 경로에서 홈에도 뒤로가기가
  // 살아 있었다(실기 확인). 화면마다 막는 방식이라 새 화면에 붙일 때마다 같은 버그가 재발한다.
  //
  // getParent()는 탭 안(MapHome)에서 루트 스택을 집고, 스택 화면(RestaurantDetail)에서는
  // undefined라 자기 자신으로 떨어진다 — 두 경우 모두 뒤로가기를 소유한 내비게이터를 가리킨다.
  const navigation = useNavigation();
  useEffect(() => {
    const owner = (navigation.getParent() ?? navigation) as unknown as {
      setOptions: (options: { gestureEnabled: boolean }) => void;
    };
    owner.setOptions({ gestureEnabled: checkIn == null });
  }, [navigation, checkIn]);
  if (!checkIn) return null;

  const together = checkIn.status === 'TOGETHER';
  // 진행 중엔 재실행 차단(실패 시 시트 유지 + 알림). 성공해야 닫는다.
  const busy = end.isPending || cancel.isPending || leave.isPending;
  const complete = () => { if (busy) return; end.mutate(checkIn.checkInId, { onSuccess: onClose }); };
  const discard = () => { if (busy) return; cancel.mutate(checkIn.checkInId, { onSuccess: onClose }); };
  const leaveTo = (to: LeaveMatchTo) => {
    if (busy) return;
    leave.mutate({ checkInId: checkIn.checkInId, to }, { onSuccess: onClose });
  };
  const report = () => {
    if (checkIn.partnerUserId == null) return;
    onReportNoShow(checkIn.partnerUserId, checkIn.partnerNickname ?? '상대');
    onClose();
  };

  return (
    <>
      {/* 스크림·X도 requestClose로 — 스와이프만 미끄러지고 탭은 툭 사라지면 따로 노는 두 동작이 된다. */}
      <Pressable style={styles.scrim} onPress={dismiss.requestClose} />
      <Animated.View
        onLayout={dismiss.onLayout}
        style={[styles.sheet, { paddingBottom: insets.bottom + 6, transform: [{ translateY: dismiss.translateY }] }]}
      >
        <Pressable style={styles.close} onPress={dismiss.requestClose} hitSlop={8} accessibilityRole="button">
          <Text style={styles.closeX}>×</Text>
        </Pressable>
        {/* 끌어 내리는 영역 — 손잡이와 그 주변 여백. 아래 본문(밀어서 완료)은 건드리지 않는다. */}
        <View {...dismiss.panHandlers} style={styles.grabArea}>
          <View style={styles.handle} />
        </View>

        {together && noShow ? (
          // ── 노쇼 서브뷰: 매칭 깨고 내 상태 선택(상대는 서버가 SEEKING 복귀+알림) ──
          <>
            <Text style={styles.title}>상대가 안 나왔어요</Text>
            <Text style={styles.sub}>이제 어떻게 할까요?</Text>
            <Pressable style={[styles.choice, styles.choicePrimary]} onPress={() => leaveTo('ACTIVE')} disabled={busy} accessibilityRole="button">
              <Text style={[styles.choiceText, { color: T2.brand }]}>그래도 혼밥할게요</Text>
            </Pressable>
            <Pressable style={styles.choice} onPress={() => leaveTo('SEEKING')} disabled={busy} accessibilityRole="button">
              <Text style={styles.choiceText}>다른 사람 기다릴래요</Text>
            </Pressable>
            <Pressable style={styles.choice} onPress={() => leaveTo('CANCELLED')} disabled={busy} accessibilityRole="button">
              <Text style={styles.choiceText}>안 먹고 갈게요</Text>
            </Pressable>
            {checkIn.partnerUserId != null && (
              <Pressable style={styles.discard} onPress={report} hitSlop={6} accessibilityRole="button">
                <Text style={styles.discardText}>이 사람 신고하기</Text>
              </Pressable>
            )}
          </>
        ) : together && cantGo ? (
          // ── 사전 취소 서브뷰: 내가 못 가서 매칭 파기(상대는 서버가 SEEKING 복귀+알림) ──
          <>
            <Text style={styles.title}>못 가게 되셨나요?</Text>
            <Text style={styles.sub}>약속을 취소하면 상대는 다시 모집 상태로 돌아가요.</Text>
            <Pressable style={[styles.choice, styles.choicePrimary]} onPress={() => leaveTo('CANCELLED')} disabled={busy} accessibilityRole="button">
              <Text style={[styles.choiceText, { color: T2.brand }]}>약속 취소하기</Text>
            </Pressable>
            <Pressable style={styles.discard} onPress={() => setCantGo(false)} hitSlop={6} accessibilityRole="button">
              <Text style={styles.discardText}>뒤로</Text>
            </Pressable>
          </>
        ) : (
          <>
            <Text style={styles.title}>{together ? '같이 먹기를 끝낼까요?' : '혼밥을 끝낼까요?'}</Text>
            <Text style={styles.sub}>다 드셨으면 밀어서 완료하세요.</Text>
            <SlideToConfirm label="밀어서 완료" onConfirm={complete} style={styles.slide} />
            {together ? (
              <View style={styles.togetherLinks}>
                <Pressable style={styles.linkBtn} onPress={() => setNoShow(true)} hitSlop={6} accessibilityRole="button">
                  <Text style={styles.linkText}>상대가 안 나왔어요</Text>
                </Pressable>
                <View style={styles.linkDivider} />
                <Pressable style={styles.linkBtn} onPress={() => setCantGo(true)} hitSlop={6} accessibilityRole="button">
                  <Text style={styles.linkText}>제가 못 가게 됐어요</Text>
                </Pressable>
              </View>
            ) : (
              <Pressable style={styles.discard} onPress={discard} disabled={busy} hitSlop={6} accessibilityRole="button">
                <Text style={styles.discardText}>안 먹었어요(기록 안 함)</Text>
              </Pressable>
            )}
          </>
        )}
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
  // 손잡이 자체는 4px이라 잡기 어렵다 — 위아래 여백을 포함한 넓은 영역에 제스처를 건다.
  grabArea: { paddingTop: 2, paddingBottom: 10, marginBottom: 2 },
  handle: { width: 36, height: 4, borderRadius: 2, backgroundColor: '#E5E5E5', alignSelf: 'center' },
  title: { fontSize: 20, fontWeight: '800', color: T2.text, letterSpacing: -0.5 },
  sub: { fontSize: 13, color: T2.textMute, marginTop: 6, letterSpacing: -0.3 },
  slide: { marginTop: 14 },
  choice: { marginTop: 8, paddingVertical: 13, borderRadius: 12, borderWidth: 1.5, borderColor: T2.border, alignItems: 'center' },
  choicePrimary: { marginTop: 12, borderColor: T2.brand, backgroundColor: T2.brandSoft },
  choiceText: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  discard: { alignItems: 'center', paddingVertical: 11, marginTop: 8 },
  discardText: { fontSize: 13.5, fontWeight: '700', color: T2.textMute, letterSpacing: -0.3 },
  togetherLinks: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 12 },
  linkBtn: { paddingVertical: 10, paddingHorizontal: 14 },
  linkText: { fontSize: 13, fontWeight: '600', color: T2.textMute, letterSpacing: -0.2 },
  linkDivider: { width: 1, height: 11, backgroundColor: T2.border },
});
