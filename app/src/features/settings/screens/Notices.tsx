// Notices — 공지사항 (실데이터). 카테고리 칩 필터 + 리스트(핀/NEW·제목·날짜·펼침 본문).
// 등록/수정은 운영자가 DB 직접 INSERT — 앱은 조회 전용.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useNotices } from '../queries';
import { noticeCategoryLabel, isNewNotice } from '../noticeCopy';
import { formatDotDate } from '@/features/safety/reportCopy';

const CATEGORIES = ['전체', '업데이트', '이벤트', '안내'];
const tagColor = (label: string) => (label === '업데이트' ? T2.brand : label === '이벤트' ? C.openDark : T2.textSub);

export function NoticesScreen({ navigation }: RootStackScreenProps<'Notices'>) {
  const [cat, setCat] = useState('전체');
  const [openId, setOpenId] = useState<number | null>(null);
  const noticesQ = useNotices();
  const notices = noticesQ.data ?? [];
  const now = new Date();

  const list = cat === '전체' ? notices : notices.filter((n) => noticeCategoryLabel(n.category) === cat);

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
        {noticesQ.isError ? <Text style={styles.stateText}>잠시 후 다시 시도해주세요</Text> : null}
        {noticesQ.isSuccess && notices.length === 0 ? (
          <Text style={styles.stateText}>등록된 공지가 없어요</Text>
        ) : null}
        {list.length > 0 ? (
          <View style={styles.block}>
            {list.map((n, i) => {
              const label = noticeCategoryLabel(n.category);
              const expandable = !!n.body;
              const isOpen = expandable && openId === n.id;
              return (
                <Pressable
                  key={n.id}
                  onPress={() => expandable && setOpenId(isOpen ? null : n.id)}
                  style={[styles.item, i < list.length - 1 && styles.itemDivider]}
                >
                  <View style={styles.tagRow}>
                    {n.pinned ? <Icon name="pushpin" size={13} color={T2.brand} /> : null}
                    <Text style={[styles.tag, { color: tagColor(label) }]}>{label}</Text>
                    {isNewNotice(n.publishedAt, now) ? (
                      <View style={styles.newBadge}>
                        <Text style={styles.newText}>NEW</Text>
                      </View>
                    ) : null}
                  </View>
                  <View style={styles.titleRow}>
                    <Text style={styles.title}>{n.title}</Text>
                    <Icon name={expandable ? (isOpen ? 'chevronUp' : 'chevronDown') : 'chevronRight'} size={16} color={T2.textMute} />
                  </View>
                  <Text style={styles.date}>{formatDotDate(n.publishedAt)}</Text>
                  {isOpen && n.body ? (
                    <View style={styles.body}>
                      <Text style={styles.bodyText}>{n.body}</Text>
                    </View>
                  ) : null}
                </Pressable>
              );
            })}
          </View>
        ) : null}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  chipRow: { flexDirection: 'row', gap: 8, paddingHorizontal: 20, paddingBottom: 12 },
  chip: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 999, borderWidth: 1 },

  stateText: { textAlign: 'center', color: T2.textMute, fontSize: 13, paddingVertical: 40, letterSpacing: -0.2 },

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
