// 같이먹기 신청 받은/보낸 목록 — 같이먹기 탭(TogetherFeed)과 더보기 '같이 먹기 신청'(ReceivedRequests) 공용.
// 두 화면의 디자인을 한 곳에서 관리해 어긋나지 않게 한다. 가로 여백은 감싸는 화면(scroll/wrapper)이 준다.
import React from 'react';
import { View, Text, Pressable, ActivityIndicator, StyleSheet } from 'react-native';
import { EmojiCircle, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { MealRequestListItem } from '@/features/meal/api';
import { mealStatusLabelReceived, mealStatusLabelSent } from '@/features/meal/mealCopy';

export type MealTab = 'received' | 'sent';

/** 받은/보낸 세그먼트 토글 (가로 여백은 감싸는 쪽에서). */
export function MealRequestSegments({ tab, onTab, receivedCount, sentCount }: {
  tab: MealTab;
  onTab: (t: MealTab) => void;
  receivedCount: number;
  sentCount: number;
}) {
  const segs = [
    { key: 'received' as const, label: '받은 신청', count: receivedCount },
    { key: 'sent' as const, label: '보낸 신청', count: sentCount },
  ];
  return (
    <View style={styles.segRow}>
      {segs.map((s) => {
        const on = tab === s.key;
        return (
          <Pressable key={s.key} style={styles.seg} onPress={() => onTab(s.key)} accessibilityRole="button">
            <Text style={[styles.segLabel, { color: on ? T2.text : T2.textMute }]}>{s.label}</Text>
            <Text style={[styles.segCount, { color: on ? T2.brand : T2.textMute }]}>{s.count}</Text>
            {on && <View style={styles.segUnderline} />}
          </Pressable>
        );
      })}
    </View>
  );
}

/** 현재 탭(받은/보낸)의 목록 렌더. 로딩/에러/빈 상태 포함. */
export function MealRequestLists({
  tab,
  receivedList, receivedLoading, receivedError,
  sentList, sentLoading, sentError,
  onAccept, onDecline, acceptPending, declinePending,
  onOpenProfile,
}: {
  tab: MealTab;
  receivedList: MealRequestListItem[];
  receivedLoading: boolean;
  receivedError: boolean;
  sentList: MealRequestListItem[];
  sentLoading: boolean;
  sentError: boolean;
  onAccept: (mealRequestId: number) => void;
  onDecline: (mealRequestId: number) => void;
  acceptPending: boolean;
  declinePending: boolean;
  onOpenProfile: (userId: number) => void;
}) {
  if (tab === 'received') {
    if (receivedLoading) return <ActivityIndicator color={T2.brand} />;
    if (receivedError) return <Text style={styles.emptyInline}>신청을 불러오지 못했어요.</Text>;
    if (receivedList.length === 0) return <Text style={styles.emptyInline}>받은 신청이 없어요.</Text>;
    return (
      <View style={{ gap: 12 }}>
        {receivedList.map((r) => (
          <View key={r.mealRequestId} style={styles.recvCard}>
            <Pressable style={styles.recvTop} onPress={() => onOpenProfile(r.fromUser.userId)} accessibilityRole="button">
              <EmojiCircle emoji={r.fromUser.nickname[0] ?? '?'} size={46} />
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.recvName}>{r.fromUser.nickname}</Text>
                <Text style={styles.recvMeta}>{r.status === 'PENDING' ? '새 신청' : mealStatusLabelReceived(r.status)}</Text>
              </View>
            </Pressable>
            <View style={styles.recvPlace}>
              <Icon name="pin" size={14} color={T2.brand} />
              <Text style={styles.recvPlaceText}>{r.placeName}</Text>
            </View>
            {r.message ? <Text style={styles.recvMsg}>"{r.message}"</Text> : null}
            {r.status === 'PENDING' && (
              <View style={styles.recvBtns}>
                <Pressable style={[styles.recvBtn, styles.recvBtnReject]} disabled={declinePending}
                  onPress={() => onDecline(r.mealRequestId)} accessibilityRole="button">
                  <Text style={styles.recvBtnRejectText}>거절</Text>
                </Pressable>
                <Pressable style={[styles.recvBtn, styles.recvBtnAccept]} disabled={acceptPending}
                  onPress={() => onAccept(r.mealRequestId)} accessibilityRole="button">
                  <Text style={styles.recvBtnAcceptText}>수락하기</Text>
                </Pressable>
              </View>
            )}
          </View>
        ))}
      </View>
    );
  }

  // 보낸 신청 — 상대가 처리한 상태(수락됨/거절됨/만료됨)
  if (sentLoading) return <ActivityIndicator color={T2.brand} />;
  if (sentError) return <Text style={styles.emptyInline}>신청을 불러오지 못했어요.</Text>;
  if (sentList.length === 0) return <Text style={styles.emptyInline}>보낸 신청이 없어요.</Text>;
  return (
    <View>
      {sentList.map((s, i) => {
        const closed = s.status === 'DECLINED' || s.status === 'EXPIRED';
        const accepted = s.status === 'ACCEPTED';
        return (
          <Pressable key={s.mealRequestId} style={[styles.sentRow, i < sentList.length - 1 && styles.sentRowBorder]}
            onPress={() => onOpenProfile(s.toUser.userId)} accessibilityRole="button">
            <EmojiCircle emoji={s.toUser.nickname[0] ?? '?'} size={44} dimmed={closed} />
            <View style={{ flex: 1, minWidth: 0 }}>
              <Text style={[styles.sentName, { color: closed ? T2.textMute : T2.text }]}>{s.toUser.nickname}</Text>
              <Text style={styles.sentMeta} numberOfLines={1}>{s.placeName}</Text>
            </View>
            <View style={[styles.sentPill, { backgroundColor: accepted ? T2.brandSoft : T2.bg, borderColor: accepted ? 'rgba(255,90,31,0.2)' : T2.border }]}>
              <Text style={[styles.sentPillText, { color: accepted ? T2.brand : T2.textMute }]}>{mealStatusLabelSent(s.status)}</Text>
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  segRow: { flexDirection: 'row', gap: 22, marginTop: 16 },
  seg: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingBottom: 12 },
  segLabel: { fontSize: 16, fontWeight: '800', letterSpacing: -0.3 },
  segCount: { fontSize: 12, fontWeight: '700' },
  segUnderline: { position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, backgroundColor: T2.brand },
  emptyInline: { fontSize: 13, color: T2.textMute, paddingVertical: 10 },
  recvCard: { padding: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  recvTop: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  recvName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  recvMeta: { fontSize: 11, color: T2.textMute, marginTop: 3 },
  recvPlace: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 14, paddingVertical: 8, paddingHorizontal: 12, borderRadius: 10, backgroundColor: T2.brandSoft, alignSelf: 'flex-start' },
  recvPlaceText: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
  recvMsg: { fontSize: 13, color: T2.textSub, lineHeight: 21, marginTop: 12, letterSpacing: -0.3 },
  recvBtns: { flexDirection: 'row', gap: 8, marginTop: 16 },
  recvBtn: { paddingVertical: 13, borderRadius: 11, alignItems: 'center' },
  recvBtnReject: { flex: 1, backgroundColor: T2.bg },
  recvBtnRejectText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  recvBtnAccept: { flex: 2, backgroundColor: T2.brand },
  recvBtnAcceptText: { fontSize: 14, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  sentRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 15 },
  sentRowBorder: { borderBottomWidth: 1, borderBottomColor: T2.border },
  sentName: { fontSize: 14.5, fontWeight: '700', letterSpacing: -0.3 },
  sentMeta: { fontSize: 12, color: T2.textMute, marginTop: 3 },
  sentPill: { paddingVertical: 7, paddingHorizontal: 12, borderRadius: 999, borderWidth: 1 },
  sentPillText: { fontSize: 12, fontWeight: '700', letterSpacing: -0.2 },
});
