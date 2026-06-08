// Notices — 공지사항 (원본: screens/Notices.jsx)
// 더보기 '공지사항'에서 진입. 카테고리 칩 필터 + 리스트(핀/NEW·제목·날짜·펼침 본문).
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

type Notice = { tag: string; title: string; date: string; isNew: boolean; pinned?: boolean; body?: string };

const ITEMS: Notice[] = [
  {
    tag: '업데이트',
    title: '같이 먹기 신청에 인사 한마디 기능이 추가됐어요',
    date: '2026.06.02',
    isNew: true,
    pinned: true,
    body: '이제 같이 먹기 신청을 보낼 때 짧은 인사를 함께 보낼 수 있어요. 처음 만나는 메이트에게 부담 없이 분위기를 전해보세요.',
  },
  { tag: '안내', title: '6월 정기 점검 안내 (6/8 새벽 2시~4시)', date: '2026.05.30', isNew: true },
  { tag: '이벤트', title: '첫 혼밥 인증하면 뱃지 2배 지급', date: '2026.05.24', isNew: false },
  { tag: '안내', title: '커뮤니티 이용규칙 개정 안내', date: '2026.05.18', isNew: false },
  { tag: '업데이트', title: '즐겨찾기 그룹 공개 설정 기능 출시', date: '2026.05.10', isNew: false },
];

const CATEGORIES = ['전체', '업데이트', '이벤트', '안내'];
const tagColor = (t: string) => (t === '업데이트' ? T2.brand : t === '이벤트' ? C.openDark : T2.textSub);

export function NoticesScreen({ navigation }: RootStackScreenProps<'Notices'>) {
  const [cat, setCat] = useState('전체');
  const [open, setOpen] = useState<number | null>(0);

  const list = cat === '전체' ? ITEMS : ITEMS.filter((n) => n.tag === cat);

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="공지사항" onBack={() => navigation.goBack()} />

      {/* 카테고리 칩 */}
      <View style={styles.chipRow}>
        {CATEGORIES.map((c) => {
          const on = cat === c;
          return (
            <Pressable
              key={c}
              onPress={() => setCat(c)}
              style={[styles.chip, { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border }]}
            >
              <Text style={{ fontSize: 13, fontWeight: '700', color: on ? '#fff' : T2.textSub, letterSpacing: -0.2 }}>{c}</Text>
            </Pressable>
          );
        })}
      </View>

      <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
        <View style={styles.block}>
          {list.map((n, i) => {
            const idx = ITEMS.indexOf(n);
            const expandable = !!n.body;
            const isOpen = expandable && open === idx;
            return (
              <Pressable
                key={n.title}
                onPress={() => expandable && setOpen(isOpen ? null : idx)}
                style={[styles.item, i < list.length - 1 && styles.itemDivider]}
              >
                <View style={styles.tagRow}>
                  {n.pinned ? <Icon name="pushpin" size={13} color={T2.brand} /> : null}
                  <Text style={[styles.tag, { color: tagColor(n.tag) }]}>{n.tag}</Text>
                  {n.isNew ? (
                    <View style={styles.newBadge}>
                      <Text style={styles.newText}>NEW</Text>
                    </View>
                  ) : null}
                </View>
                <View style={styles.titleRow}>
                  <Text style={styles.title}>{n.title}</Text>
                  <Icon name={expandable ? (isOpen ? 'chevronUp' : 'chevronDown') : 'chevronRight'} size={16} color={T2.textMute} />
                </View>
                <Text style={styles.date}>{n.date}</Text>
                {isOpen && n.body ? (
                  <View style={styles.body}>
                    <Text style={styles.bodyText}>{n.body}</Text>
                  </View>
                ) : null}
              </Pressable>
            );
          })}
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  chipRow: { flexDirection: 'row', gap: 8, paddingHorizontal: 20, paddingBottom: 12 },
  chip: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 999, borderWidth: 1 },

  block: { backgroundColor: '#fff', borderTopWidth: 1, borderBottomWidth: 1, borderColor: T2.border },
  item: { paddingVertical: 16, paddingHorizontal: 20 },
  itemDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  tagRow: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  tag: { fontSize: 11, fontWeight: '800', letterSpacing: -0.2 },
  newBadge: { backgroundColor: T2.brand, paddingHorizontal: 5, paddingVertical: 2, borderRadius: 4 },
  newText: { fontSize: 9, fontWeight: '800', color: '#fff', letterSpacing: 0.3 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 7 },
  title: { flex: 1, fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3, lineHeight: 21 },
  date: { fontSize: 12, color: T2.textMute, marginTop: 6, letterSpacing: -0.2 },
  body: { marginTop: 12, padding: 14, backgroundColor: T2.bg, borderRadius: 12 },
  bodyText: { fontSize: 13, color: T2.textSub, lineHeight: 21, letterSpacing: -0.3 },
});
