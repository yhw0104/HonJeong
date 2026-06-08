// Support — 고객센터 · 문의 (원본: screens/Support.jsx)
// 더보기 '고객센터 · 문의'에서 진입. 검색 + 빠른 문의 채널 + FAQ 아코디언 + 운영 정보.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

type Faq = { q: string; a?: string };
const FAQS: Faq[] = [
  {
    q: '같이 먹기 신청은 어떻게 보내나요?',
    a: '식당 상세 화면 하단의 "같이 먹기 신청" 버튼을 누른 뒤, 지금 혼밥 중인 사람을 선택하고 인사 한마디를 보내면 돼요.',
  },
  { q: '혼밥 인증은 어떻게 하나요?', a: '내 혼밥 기록에서 방문한 식당의 "일기 쓰기"를 눌러 사진·기분·별점과 한 줄 기록을 남기면 인증돼요.' },
  { q: '메이트를 차단하면 어떻게 되나요?', a: '서로의 프로필·혼밥 현황이 보이지 않고 같이 먹기 신청도 막혀요. 차단/신고 관리에서 해제할 수 있어요.' },
  { q: '동네(위치)는 어떻게 바꾸나요?', a: '프로필 편집의 "내 동네 · 변경"에서 현재 위치를 다시 설정하면 돼요.' },
  { q: '계정을 삭제하고 싶어요', a: '고객센터 1:1 문의로 요청해 주시면 안내해 드려요. 삭제 시 모든 기록이 사라져요.' },
];

export function SupportScreen({ navigation }: RootStackScreenProps<'Support'>) {
  const [open, setOpen] = useState<number | null>(0);

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="고객센터" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 검색 */}
        <View style={styles.search}>
          <Icon name="search" size={18} color={T2.textMute} />
          <Text style={styles.searchText}>무엇을 도와드릴까요?</Text>
        </View>

        {/* 빠른 문의 채널 */}
        <View style={styles.channels}>
          <Pressable style={[styles.channel, { backgroundColor: T2.text }]}>
            <Icon name="comment" size={24} color="#fff" />
            <Text style={[styles.channelTitle, { color: '#fff' }]}>1:1 문의</Text>
            <Text style={[styles.channelSub, { color: 'rgba(255,255,255,0.6)' }]}>보통 1시간 내 답변</Text>
          </Pressable>
          <Pressable style={[styles.channel, styles.channelOutline]}>
            <Icon name="mail" size={24} color={T2.text} />
            <Text style={[styles.channelTitle, { color: T2.text }]}>이메일 문의</Text>
            <Text style={[styles.channelSub, { color: T2.textMute }]}>help@honjeong.kr</Text>
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

        {/* 운영 정보 */}
        <View style={styles.opBox}>
          <View style={styles.opRow}>
            <Text style={styles.opLabel}>운영 시간</Text>
            <Text style={styles.opValue}>평일 10:00 – 18:00</Text>
          </View>
          <View style={[styles.opRow, { marginTop: 9 }]}>
            <Text style={styles.opLabel}>점심시간</Text>
            <Text style={styles.opValue}>13:00 – 14:00</Text>
          </View>
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 40 },

  search: { flexDirection: 'row', alignItems: 'center', gap: 9, paddingVertical: 13, paddingHorizontal: 15, backgroundColor: '#fff', borderRadius: 12, borderWidth: 1, borderColor: T2.border },
  searchText: { fontSize: 14, color: T2.textMute, letterSpacing: -0.2 },

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

  opBox: { marginTop: 18, padding: 16, backgroundColor: T2.bg, borderRadius: 12, borderWidth: 1, borderColor: T2.border },
  opRow: { flexDirection: 'row', justifyContent: 'space-between' },
  opLabel: { fontSize: 13, color: T2.textMute, letterSpacing: -0.2 },
  opValue: { fontSize: 13, color: T2.text, fontWeight: '600', letterSpacing: -0.2 },
});
