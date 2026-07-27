// WithdrawAccount — 회원 탈퇴. 더보기 '설정' 최하단에서 진입.
// 지워지는 것과 남는 것을 그대로 적는다 — 리뷰·대화가 '알 수 없음'으로 남는 건 사용자가
// 탈퇴 전에 알아야 하는 사실이고, 알림창 두 번으로 때우면 속이는 게 된다.
import React, { useEffect, useRef, useState } from 'react';
import { Text, Pressable, ScrollView, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { Screen, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useAuth } from '@/shared/auth/AuthContext';
import { ApiError } from '@/shared/api/client';
import { withdrawAccount } from '../withdrawApi';
import type { RootStackScreenProps } from '@/navigation/types';

const REMOVED = [
  '프로필 정보(닉네임·사진·소개·성별·생년월일)',
  '휴대폰 번호와 카카오 연동',
  '메이트 관계와 주고받은 신청',
  '즐겨찾기와 저장한 식당',
  '알림함과 알림 설정',
  '획득한 뱃지',
  '지금 같이 먹는 중이라면, 그 혼밥과 대화도 상대방 쪽까지 함께 종료됩니다',
];

const KEPT = [
  "작성한 식당 리뷰 — 작성자가 '알 수 없음'으로 바뀝니다",
  "나눈 대화 — 상대방에게는 '알 수 없음'과의 대화로 남습니다",
  '혼밥 횟수 통계 — 누가 남긴 기록인지는 알 수 없게 됩니다',
];

/** 서버가 보낸 진짜 업무 오류(4xx)는 그대로 보여주고, 그 외(5xx·네트워크 등 예기치 못한 실패)는
 *  client.ts가 합성한 "요청 실패 (HTTP 500)" 같은 원문 대신 사람이 읽을 문구로 감춘다. */
function withdrawErrorMessage(e: unknown): string {
  if (e instanceof ApiError && e.status >= 400 && e.status < 500) return e.message;
  return '잠시 후 다시 시도해주세요.';
}

export function WithdrawAccountScreen({ navigation }: RootStackScreenProps<'WithdrawAccount'>) {
  const { signOut } = useAuth();
  const [busy, setBusy] = useState(false);
  // busyRef: confirm()이 연 Alert의 onPress(run)는 그 Alert를 띄운 순간의 클로저를 그대로 들고 있어
  // busy(state)를 직접 읽으면 더블탭으로 큐잉된 두 번째 다이얼로그가 항상 stale한 false를 보게 된다 —
  // 두 다이얼로그가 공유하는 이 ref로만 "이미 처리됨"을 판단할 수 있다(SlideToConfirm의 confirmedRef와 동일한 이유).
  const busyRef = useRef(false);
  // aliveRef: withdrawAccount()가 401로 실패하고 refresh까지 실패하면 onSessionExpired가 이 화면을
  // 먼저 언마운트한다(게스트 스택 전환) — 그 뒤에 catch가 도는 것이므로 언마운트 여부를 확인해야 한다
  // (AuthContext/Welcome의 alive 플래그와 같은 목적이지만, 이펙트 밖 콜백에서 읽어야 해서 ref로 둔다).
  const aliveRef = useRef(true);
  useEffect(() => () => { aliveRef.current = false; }, []);

  const confirm = () => {
    if (busyRef.current) return; // 더블탭 방지
    Alert.alert(
      '정말 탈퇴하시겠어요?',
      '되돌릴 수 없습니다. 같은 번호로 다시 가입해도 이전 기록은 복구되지 않아요.',
      [
        { text: '취소', style: 'cancel' },
        { text: '탈퇴하기', style: 'destructive', onPress: run },
      ],
    );
  };

  const run = async () => {
    if (busyRef.current) return; // 큐잉된 두 번째 다이얼로그가 나중에 같은 run을 다시 불러도 여기서 막힌다
    busyRef.current = true;
    setBusy(true);
    try {
      await withdrawAccount();
      await signOut(); // 세션 정리 → 게스트 스택(Welcome)으로 복귀
    } catch (e) {
      if (!aliveRef.current) return; // 세션 만료로 화면이 이미 사라졌다면 그 위에 아무것도 띄우지 않는다
      busyRef.current = false;
      setBusy(false);
      Alert.alert('탈퇴 실패', withdrawErrorMessage(e));
    }
  };

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="회원 탈퇴" onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.lead}>탈퇴하면 아래 정보가 즉시 삭제되고 되돌릴 수 없어요.</Text>

        <Text style={styles.sectionTitle}>삭제되는 것</Text>
        {REMOVED.map((line) => (
          <Text key={line} style={styles.item}>· {line}</Text>
        ))}

        <Text style={styles.sectionTitle}>남는 것</Text>
        {KEPT.map((line) => (
          <Text key={line} style={styles.item}>· {line}</Text>
        ))}
        <Text style={styles.note}>
          남기고 싶지 않은 리뷰가 있다면 탈퇴 전에 '내가 쓴 리뷰'에서 직접 삭제해 주세요.
        </Text>

        <Pressable
          style={[styles.btn, busy && styles.btnBusy]}
          onPress={confirm}
          disabled={busy}
          accessibilityRole="button"
        >
          {busy ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>탈퇴하기</Text>}
        </Pressable>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 40 },
  lead: { fontSize: 15, color: T2.textSub, lineHeight: 23, letterSpacing: -0.3, marginBottom: 24 },
  sectionTitle: { fontSize: 13, fontWeight: '800', color: T2.text, letterSpacing: -0.2, marginTop: 20, marginBottom: 10 },
  item: { fontSize: 14, color: T2.textSub, lineHeight: 22, letterSpacing: -0.3, marginBottom: 4 },
  note: { fontSize: 13, color: T2.textMute, lineHeight: 20, letterSpacing: -0.3, marginTop: 14 },
  btn: {
    marginTop: 36,
    paddingVertical: 15,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#D64545',
  },
  btnBusy: { opacity: 0.7 },
  btnText: { fontSize: 15, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
});
