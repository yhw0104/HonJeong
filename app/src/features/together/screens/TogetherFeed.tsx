// TogetherFeed — 같이 먹기 (하단바 신규 탭, 원본: screens/TogetherFeed.jsx)
// 핵심 액션 화면: 받은/보낸 신청 + 지금 같이 먹을 수 있는 사람.
// 하단 탭바는 MainTabs 네비게이터가 렌더하므로 여기서는 그리지 않는다.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, EmojiCircle, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { MainTabScreenProps } from '@/navigation/types';

const RECEIVED = [
  { name: '점심혼밥러', emo: '🍙', place: '큰순두부 연남점', time: '방금', meta: '혼밥 32회 · 같이 먹은 적 2회', msg: '저도 순두부 좋아해요! 같이 조용히 먹어요 :)', mate: true },
  { name: '연남책방지기', emo: '📚', place: '혼밥의자', time: '12분 전', meta: '혼밥 12회 · 첫 매칭', msg: '바테이블 옆자리 어떠세요?', mate: false },
];

const SENT = [
  { name: '조용한미식가', emo: '🍜', place: '옥상국밥', time: '오늘 12:10', state: 'accepted' as const },
  { name: '국밥러버', emo: '🍲', place: '큰순두부 연남점', time: '어제', state: 'pending' as const },
  { name: '면식수행', emo: '🍝', place: '연남 파스타바', time: '2일 전', state: 'declined' as const },
];

const LIVE_NOW = [
  { name: '혼밥부장', emo: '🍱', place: '큰순두부 연남점', dist: '120m', since: '8분째', mood: '대화 환영', mate: true },
  { name: '도시락주의', emo: '🥡', place: '옥상국밥', dist: '480m', since: '3분째', mood: '조용히', mate: false },
  { name: '연남또일이', emo: '🍳', place: '혼밥의자', dist: '650m', since: '15분째', mood: '대화 환영', mate: false },
];

type SentState = 'accepted' | 'pending' | 'declined';
const STATE_MAP: Record<SentState, { label: string; color: string; bg: string; strong: boolean }> = {
  accepted: { label: '수락됨 · 약속 잡기', color: T2.brand, bg: T2.brandSoft, strong: true },
  pending: { label: '응답 대기 중', color: T2.textMute, bg: T2.bg, strong: false },
  declined: { label: '거절됨', color: T2.textMute, bg: T2.bg, strong: false },
};

export function TogetherFeedScreen(_props: MainTabScreenProps<'TogetherFeed'>) {
  const [tab, setTab] = useState<'received' | 'sent'>('received');

  const SEGMENTS: { key: 'received' | 'sent'; label: string; count: number | null }[] = [
    { key: 'received', label: '받은 신청', count: RECEIVED.length },
    { key: 'sent', label: '보낸 신청', count: null },
  ];

  return (
    <Screen bg={T2.bg} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.h1}>같이 먹기</Text>
        <View style={styles.segRow}>
          {SEGMENTS.map((s) => {
            const on = tab === s.key;
            return (
              <Pressable key={s.key} style={styles.seg} onPress={() => setTab(s.key)}>
                <Text style={[styles.segLabel, { color: on ? T2.text : T2.textMute }]}>{s.label}</Text>
                {s.count != null && (
                  <Text style={[styles.segCount, { color: on ? T2.brand : T2.textMute }]}>{s.count}</Text>
                )}
                {on && <View style={styles.segUnderline} />}
              </Pressable>
            );
          })}
        </View>
      </View>
      <View style={styles.divider} />

      {/* 본문 */}
      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 지금 같이 먹을 수 있어요 — 항상 상단 노출 */}
        <View style={styles.liveHead}>
          <View style={styles.liveHeadLeft}>
            <View style={styles.liveDot} />
            <Text style={styles.liveLabel}>지금 같이 먹을 수 있어요</Text>
          </View>
          <Text style={styles.liveCount}>내 주변 {LIVE_NOW.length}</Text>
        </View>

        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={styles.liveScroll}
          contentContainerStyle={styles.liveScrollContent}
        >
          {LIVE_NOW.map((p, i) => (
            <View key={i} style={styles.liveCard}>
              <View style={styles.liveAvatar}>
                <Text style={styles.liveAvatarEmoji}>{p.emo}</Text>
                <View style={styles.liveAvatarDot} />
              </View>
              <View style={styles.liveNameRow}>
                <Text style={styles.liveName} numberOfLines={1}>{p.name}</Text>
                {p.mate && <Text style={styles.liveMate}>메이트</Text>}
              </View>
              <Text style={styles.livePlace} numberOfLines={1}>{p.place}</Text>
              <Text style={styles.liveMeta}>{p.dist} · {p.since}</Text>
              <Pressable style={styles.liveBtn}>
                <Text style={styles.liveBtnText}>같이 먹기</Text>
              </Pressable>
            </View>
          ))}
        </ScrollView>

        <View style={styles.sectionDivider} />

        {/* 받은 신청 */}
        {tab === 'received' && (
          <View style={{ gap: 12 }}>
            {RECEIVED.map((r, i) => (
              <View key={i} style={styles.recvCard}>
                <View style={styles.recvTop}>
                  <EmojiCircle emoji={r.emo} size={46} />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <View style={styles.recvNameRow}>
                      <Text style={styles.recvName}>{r.name}</Text>
                      {r.mate && (
                        <View style={styles.recvMateBadge}>
                          <Text style={styles.recvMateText}>메이트</Text>
                        </View>
                      )}
                    </View>
                    <Text style={styles.recvMeta}>{r.meta}</Text>
                  </View>
                  <Text style={styles.recvTime}>{r.time}</Text>
                </View>

                <View style={styles.recvPlace}>
                  <Icon name="pin" size={14} color={T2.brand} />
                  <Text style={styles.recvPlaceText}>{r.place}</Text>
                </View>
                <Text style={styles.recvMsg}>“{r.msg}”</Text>

                <View style={styles.recvBtns}>
                  <Pressable style={[styles.recvBtn, styles.recvBtnReject]}>
                    <Text style={styles.recvBtnRejectText}>거절</Text>
                  </Pressable>
                  <Pressable style={[styles.recvBtn, styles.recvBtnAccept]}>
                    <Text style={styles.recvBtnAcceptText}>수락하기</Text>
                  </Pressable>
                </View>
              </View>
            ))}
          </View>
        )}

        {/* 보낸 신청 */}
        {tab === 'sent' && (
          <View>
            {SENT.map((s, i) => {
              const st = STATE_MAP[s.state];
              const declined = s.state === 'declined';
              return (
                <View
                  key={i}
                  style={[styles.sentRow, i < SENT.length - 1 && styles.sentRowBorder]}
                >
                  <EmojiCircle emoji={s.emo} size={44} dimmed={declined} />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Text style={[styles.sentName, { color: declined ? T2.textMute : T2.text }]}>{s.name}</Text>
                    <Text style={styles.sentMeta} numberOfLines={1}>{s.place} · {s.time}</Text>
                  </View>
                  <View
                    style={[
                      styles.sentPill,
                      { backgroundColor: st.bg, borderColor: st.strong ? 'rgba(255,90,31,0.2)' : T2.border },
                    ]}
                  >
                    <Text style={[styles.sentPillText, { color: st.color }]}>{st.label}</Text>
                  </View>
                </View>
              );
            })}
          </View>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  // 헤더
  header: { paddingHorizontal: 20, paddingTop: 12 },
  h1: { fontSize: 28, fontWeight: '800', color: T2.text, letterSpacing: -1 },
  segRow: { flexDirection: 'row', gap: 22, marginTop: 16 },
  seg: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingBottom: 12 },
  segLabel: { fontSize: 16, fontWeight: '800', letterSpacing: -0.3 },
  segCount: { fontSize: 12, fontWeight: '700' },
  segUnderline: { position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, backgroundColor: T2.brand },
  divider: { height: 1, backgroundColor: T2.border },

  scroll: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 32 },

  // 지금 같이 먹을 수 있어요
  liveHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  liveHeadLeft: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  liveDot: { width: 7, height: 7, borderRadius: 3.5, backgroundColor: T2.brand },
  liveLabel: { fontSize: 11, fontWeight: '700', color: T2.text, letterSpacing: 0.6 },
  liveCount: { fontSize: 12, fontWeight: '700', color: T2.textMute },

  liveScroll: { marginHorizontal: -20 },
  liveScrollContent: { paddingHorizontal: 20, paddingBottom: 4, gap: 10 },
  liveCard: {
    width: 156,
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: T2.border,
  },
  liveAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: T2.bg,
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  liveAvatarEmoji: { fontSize: 21 },
  liveAvatarDot: {
    position: 'absolute',
    right: -1,
    bottom: -1,
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: T2.brand,
    borderWidth: 2.5,
    borderColor: '#fff',
  },
  liveNameRow: { flexDirection: 'row', alignItems: 'center', gap: 5, marginTop: 11 },
  liveName: { flexShrink: 1, fontSize: 14, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  liveMate: { fontSize: 9, fontWeight: '800', color: T2.brand },
  livePlace: { fontSize: 11.5, color: T2.textSub, marginTop: 4, letterSpacing: -0.2 },
  liveMeta: { fontSize: 11, color: T2.textMute, marginTop: 2 },
  liveBtn: { marginTop: 11, paddingVertical: 8, borderRadius: 9, backgroundColor: T2.brand, alignItems: 'center' },
  liveBtnText: { fontSize: 12.5, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },

  sectionDivider: { height: 1, backgroundColor: T2.border, marginTop: 22, marginBottom: 18 },

  // 받은 신청
  recvCard: { padding: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  recvTop: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  recvNameRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  recvName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  recvMateBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  recvMateText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  recvMeta: { fontSize: 11, color: T2.textMute, marginTop: 3 },
  recvTime: { fontSize: 11, color: T2.textMute },
  recvPlace: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 7,
    marginTop: 14,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 10,
    backgroundColor: T2.brandSoft,
  },
  recvPlaceText: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
  recvMsg: { fontSize: 13, color: T2.textSub, lineHeight: 21, marginTop: 12, letterSpacing: -0.3 },
  recvBtns: { flexDirection: 'row', gap: 8, marginTop: 16 },
  recvBtn: { paddingVertical: 13, borderRadius: 11, alignItems: 'center' },
  recvBtnReject: { flex: 1, backgroundColor: T2.bg },
  recvBtnRejectText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  recvBtnAccept: { flex: 2, backgroundColor: T2.brand },
  recvBtnAcceptText: { fontSize: 14, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },

  // 보낸 신청
  sentRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 15 },
  sentRowBorder: { borderBottomWidth: 1, borderBottomColor: T2.border },
  sentName: { fontSize: 14.5, fontWeight: '700', letterSpacing: -0.3 },
  sentMeta: { fontSize: 12, color: T2.textMute, marginTop: 3 },
  sentPill: { paddingVertical: 7, paddingHorizontal: 12, borderRadius: 999, borderWidth: 1 },
  sentPillText: { fontSize: 12, fontWeight: '700', letterSpacing: -0.2 },
});
