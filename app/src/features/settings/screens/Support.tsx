// Support — 고객센터 · 문의 (원본: screens/Support.jsx)
// 더보기 '고객센터 · 문의'에서 진입. 빠른 문의 채널(카카오톡/이메일) + FAQ 아코디언.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Linking, Alert } from 'react-native';
import * as Clipboard from 'expo-clipboard';
import { Screen, MoreHeader, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { KAKAO_OPENCHAT_URL, SUPPORT_EMAIL } from '@/shared/config/support';

// ★답변은 실제 동작과 화면 문구에 맞춰 적는다. 화면 이름·버튼 이름을 바꾸면 여기도 같이 고칠 것 —
//   고객센터가 없는 버튼을 안내하면 문의가 늘지 줄지 않는다.
type Faq = { q: string; a?: string };
const FAQS: Faq[] = [
  {
    q: '같이 먹을 사람은 어떻게 찾나요?',
    a: '두 가지 길이 있어요. ① 식당 상세 화면 아래 "같이 먹을 사람 구하기"를 누르면 내가 모집 중이 되고, 그 식당에 있는 다른 분이 신청할 수 있어요. ② 같이 먹기 탭이나 검색 첫 화면의 "지금 모집 중"에서 이미 모집 중인 식당을 골라 신청할 수도 있어요. 신청할 땐 인사 한마디를 함께 보낼 수 있어요.',
  },
  {
    q: '혼밥 기록은 어떻게 남기나요?',
    a: '"내 혼밥 기록"에서 방문한 식당의 "일기 쓰기"를 누르거나, 식당 상세에서 "리뷰 쓰기"로도 남길 수 있어요. 사진·기분·별점과 한 줄 기록을 적으면 돼요.',
  },
  {
    q: '별점이 왜 두 개인가요?',
    a: '"맛"과 "혼밥하기 좋은지"는 다른 이야기라서요. 맛있어도 4인석뿐이라 혼자 앉기 불편한 곳이 있고, 그 반대도 있어요. 혼밥 별점은 혼자 온 사람 입장에서만 매겨 주세요.',
  },
  {
    q: '메이트를 차단하면 어떻게 되나요?',
    a: '서로의 프로필이 보이지 않고, 같이 먹기·메이트 신청과 채팅 알림도 모두 막혀요. 더보기 > 차단/신고 관리에서 해제할 수 있어요.',
  },
  {
    q: '계정을 삭제하고 싶어요',
    a: "더보기 > 설정 > '회원 탈퇴'에서 직접 삭제할 수 있어요. 삭제하면 되돌릴 수 없고, 작성한 리뷰와 대화는 '알 수 없음'으로 남아요.",
  },
];

export function SupportScreen({ navigation }: RootStackScreenProps<'Support'>) {
  const [open, setOpen] = useState<number | null>(0);

  // 링크를 열되, 열 핸들러가 없으면(예: iOS 시뮬레이터엔 메일 앱이 없음) 주소/링크를
  // 클립보드에 복사하고 안내한다 — 버튼이 오류 대신 늘 쓸모있게.
  const openOrCopy = async (url: string, copyValue: string, title: string, guide: string) => {
    try {
      await Linking.openURL(url);
    } catch {
      await Clipboard.setStringAsync(copyValue);
      Alert.alert(title, guide, [{ text: '확인' }]);
    }
  };

  const onKakao = () => {
    if (KAKAO_OPENCHAT_URL) {
      openOrCopy(KAKAO_OPENCHAT_URL, KAKAO_OPENCHAT_URL, '카카오톡 문의', `카카오톡을 열 수 없어 링크를 복사했어요.\n${KAKAO_OPENCHAT_URL}`);
    } else {
      Alert.alert('카카오톡 문의', '오픈채팅을 준비 중이에요. 그 사이 이메일로 문의해 주시면 빠르게 답변드릴게요.', [{ text: '확인' }]);
    }
  };
  const onEmail = () =>
    openOrCopy(`mailto:${SUPPORT_EMAIL}`, SUPPORT_EMAIL, '이메일 문의', `메일 앱을 열 수 없어 이메일 주소를 복사했어요.\n${SUPPORT_EMAIL} 로 보내주세요.`);

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="고객센터" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 빠른 문의 채널 */}
        <View style={styles.channels}>
          <Pressable style={[styles.channel, { backgroundColor: T2.text }]} onPress={onKakao}>
            <Icon name="kakao" size={24} color="#fff" />
            <Text style={[styles.channelTitle, { color: '#fff' }]}>카카오톡 문의</Text>
            <Text style={[styles.channelSub, { color: 'rgba(255,255,255,0.6)' }]}>보통 1시간 내 답변</Text>
          </Pressable>
          <Pressable style={[styles.channel, styles.channelOutline]} onPress={onEmail}>
            <Icon name="mail" size={24} color={T2.text} />
            <Text style={[styles.channelTitle, { color: T2.text }]}>이메일 문의</Text>
            <Text style={[styles.channelSub, { color: T2.textMute }]}>{SUPPORT_EMAIL}</Text>
          </Pressable>
        </View>

        {/* FAQ */}
        <Text style={styles.faqLabel}>자주 묻는 질문</Text>
        <View style={styles.faqBlock}>
          {FAQS.map((f, i) => {
            const isOpen = open === i;
            return (
              <View key={f.q} style={[i < FAQS.length - 1 && styles.faqDivider]}>
                <Pressable style={styles.faqQ} onPress={() => setOpen(isOpen ? null : i)}>
                  <Text style={styles.qMark}>Q</Text>
                  <Text style={styles.qText}>{f.q}</Text>
                  <Icon name={isOpen ? 'chevronUp' : 'chevronDown'} size={16} color={T2.textMute} />
                </Pressable>
                {isOpen && f.a ? (
                  <View style={styles.faqA}>
                    <Text style={styles.aMark}>A</Text>
                    <Text style={styles.aText}>{f.a}</Text>
                  </View>
                ) : null}
              </View>
            );
          })}
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 40 },

  channels: { flexDirection: 'row', gap: 10, marginTop: 14 },
  channel: { flex: 1, paddingVertical: 18, paddingHorizontal: 14, borderRadius: 16 },
  channelOutline: { backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border },
  channelTitle: { fontSize: 15, fontWeight: '800', marginTop: 12, letterSpacing: -0.3 },
  channelSub: { fontSize: 11, marginTop: 3 },

  faqLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginTop: 28, marginBottom: 10 },
  faqBlock: { backgroundColor: '#fff', borderRadius: 14, borderWidth: 1, borderColor: T2.border, overflow: 'hidden' },
  faqDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  faqQ: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 15, paddingHorizontal: 16 },
  qMark: { fontSize: 15, fontWeight: '700', color: T2.brand },
  qText: { flex: 1, fontSize: 14, fontWeight: '600', color: T2.text, letterSpacing: -0.3, lineHeight: 20 },
  faqA: { flexDirection: 'row', gap: 10, paddingHorizontal: 16, paddingBottom: 16 },
  aMark: { fontSize: 15, fontWeight: '700', color: T2.textMute },
  aText: { flex: 1, fontSize: 13, color: T2.textSub, lineHeight: 21, letterSpacing: -0.3 },
});
