// WithdrawAccount — 회원 탈퇴. 더보기 '설정' 최하단에서 진입.
// 지워지는 것과 남는 것을 그대로 적는다 — 리뷰·대화가 '알 수 없음'으로 남는 건 사용자가
// 탈퇴 전에 알아야 하는 사실이고, 알림창 두 번으로 때우면 속이는 게 된다.
import React, { useState } from 'react';
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
];

const KEPT = [
  "작성한 식당 리뷰 — 작성자가 '알 수 없음'으로 바뀝니다",
  "나눈 대화 — 상대방에게는 '알 수 없음'과의 대화로 남습니다",
  '혼밥 횟수 통계 — 누가 남긴 기록인지는 알 수 없게 됩니다',
];

export function WithdrawAccountScreen({ navigation }: RootStackScreenProps<'WithdrawAccount'>) {
  const { signOut } = useAuth();
  const [busy, setBusy] = useState(false);

  const confirm = () => {
    if (busy) return; // 더블탭 방지
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
    setBusy(true);
    try {
      await withdrawAccount();
      await signOut(); // 세션 정리 → 게스트 스택(Welcome)으로 복귀
    } catch (e) {
      setBusy(false);
      Alert.alert('탈퇴 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
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

        <Pressable style={[styles.btn, busy && styles.btnBusy]} onPress={confirm} disabled={busy}>
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
