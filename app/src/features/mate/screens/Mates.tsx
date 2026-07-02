// Mates — 메이트 목록 (원본: screens/Mates.jsx)
// 더보기 프로필 '메이트' 스탯에서 진입. 내 메이트 + 받은 메이트 신청.
import React, { useState, useCallback } from 'react';
import { View, Text, Pressable, ScrollView, TextInput, ActivityIndicator, Alert, StyleSheet } from 'react-native';
import { Screen, MoreHeader, EmojiCircle, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import { useFocusEffect } from '@react-navigation/native';
import type { RootStackScreenProps } from '@/navigation/types';
import {
  useMates,
  useSearchUsers,
  useReceivedMateRequests,
  useSentMateRequests,
  useSendMateRequest,
  useAcceptMateRequest,
  useDeclineMateRequest,
} from '@/features/mate/queries';
import { mateErrorMessage } from '@/features/mate/mateCopy';

function emojiFor(nickname: string | null): string {
  return nickname?.[0] ?? '?';
}

function diningStyleLabel(style: 'TALK' | 'QUIET' | null): string | null {
  if (style === 'TALK') return '대화 OK';
  if (style === 'QUIET') return '조용히';
  return null;
}

export function MatesScreen({ navigation }: RootStackScreenProps<'Mates'>) {
  const [query, setQuery] = useState('');

  const mates = useMates();
  const search = useSearchUsers(query);
  const received = useReceivedMateRequests('PENDING');
  const sent = useSentMateRequests();
  const send = useSendMateRequest();
  const accept = useAcceptMateRequest();
  const decline = useDeclineMateRequest();

  useFocusEffect(useCallback(() => { received.refetch(); sent.refetch(); }, [received.refetch, sent.refetch]));

  const searching = query.trim().length > 0;

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="메이트" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 검색 */}
        <View style={styles.search}>
          <Icon name="search" size={18} color={T2.textMute} />
          <TextInput
            style={styles.searchInput}
            placeholder="이름으로 메이트 찾기"
            placeholderTextColor={T2.textMute}
            value={query}
            onChangeText={setQuery}
            autoCorrect={false}
            autoCapitalize="none"
          />
        </View>

        {searching ? (
          /* 검색 결과 */
          <>
            <Text style={styles.label}>검색 결과</Text>
            {search.isLoading ? (
              <ActivityIndicator color={T2.brand} style={{ marginTop: 24 }} />
            ) : search.isError ? (
              <Text style={styles.emptyText}>검색 중 오류가 발생했어요</Text>
            ) : (search.data ?? []).length === 0 ? (
              <Text style={styles.emptyText}>검색 결과가 없어요</Text>
            ) : (
              <View style={{ gap: 10 }}>
                {(search.data ?? []).map((item) => {
                  const label = item.isMate
                    ? '메이트'
                    : item.requestStatus === 'PENDING_SENT'
                    ? '신청함'
                    : item.requestStatus === 'PENDING_RECEIVED'
                    ? '신청 받음'
                    : '+ 메이트 추가';
                  const isRelated = item.isMate || item.requestStatus === 'PENDING_SENT' || item.requestStatus === 'PENDING_RECEIVED';
                  const disabled = isRelated || send.isPending;
                  return (
                    <Pressable
                      key={item.userId}
                      style={styles.card}
                      onPress={() => navigation.navigate('MateProfile', { userId: item.userId })}
                    >
                      <EmojiCircle emoji={emojiFor(item.nickname)} size={48} />
                      <View style={{ flex: 1, minWidth: 0 }}>
                        <Text style={styles.name}>{item.nickname ?? '알 수 없음'}</Text>
                        {item.region ? <Text style={styles.meta}>{item.region}</Text> : null}
                      </View>
                      <Pressable
                        onPress={() => {
                          if (disabled) return;
                          send.mutate(item.userId, {
                            onError: (err) => Alert.alert('오류', mateErrorMessage(err)),
                          });
                        }}
                        style={[
                          styles.addChip,
                          { backgroundColor: disabled ? '#fff' : T2.brand, borderColor: disabled ? T2.border : T2.brand },
                        ]}
                      >
                        <Text style={{ fontSize: 12, fontWeight: '700', color: disabled ? T2.textMute : '#fff', letterSpacing: -0.2 }}>
                          {label}
                        </Text>
                      </Pressable>
                    </Pressable>
                  );
                })}
              </View>
            )}
          </>
        ) : (
          /* 내 메이트 */
          <>
            {mates.isLoading ? (
              <ActivityIndicator color={T2.brand} style={{ marginTop: 48 }} />
            ) : mates.isError ? (
              <Text style={styles.emptyText}>메이트 목록을 불러올 수 없어요</Text>
            ) : (
              <>
                <Text style={styles.label}>내 메이트 {(mates.data ?? []).length}</Text>
                {(mates.data ?? []).length === 0 ? (
                  <Text style={styles.emptyText}>아직 메이트가 없어요</Text>
                ) : (
                  <View style={{ gap: 10 }}>
                    {(mates.data ?? []).map((m) => {
                      const styleTag = diningStyleLabel(m.diningStyle);
                      return (
                        <Pressable
                          key={m.mateUserId}
                          style={styles.card}
                          onPress={() => navigation.navigate('MateProfile', { userId: m.mateUserId })}
                        >
                          <EmojiCircle emoji={emojiFor(m.nickname)} size={48} online={m.isOnline} />
                          <View style={{ flex: 1, minWidth: 0 }}>
                            <View style={styles.nameRow}>
                              <Text style={styles.name}>{m.nickname ?? '알 수 없음'}</Text>
                              {m.mealsTogether > 0 ? (
                                <View style={styles.togetherBadge}>
                                  <Text style={styles.togetherText}>같이 {m.mealsTogether}회</Text>
                                </View>
                              ) : null}
                            </View>
                            {m.isOnline ? (
                              <View style={styles.nowRow}>
                                <View style={styles.nowDot} />
                                <Text style={styles.nowText}>지금 혼밥 중</Text>
                                {m.currentPlaceName ? (
                                  <Text style={styles.nowPlace} numberOfLines={1}>
                                    · {m.currentPlaceName}
                                  </Text>
                                ) : null}
                              </View>
                            ) : (
                              <Text style={styles.meta}>
                                {[m.region, `혼밥 ${m.checkInCount}회`].filter(Boolean).join(' · ')}
                              </Text>
                            )}
                            {styleTag ? (
                              <View style={styles.tagRow}>
                                <View style={styles.tag}>
                                  <Text style={styles.tagText}>{styleTag}</Text>
                                </View>
                              </View>
                            ) : null}
                          </View>
                          <View style={styles.mateChip}>
                            <Text style={{ fontSize: 11, fontWeight: '800', color: T2.textSub }}>✓</Text>
                            <Text style={styles.mateChipText}>메이트</Text>
                          </View>
                        </Pressable>
                      );
                    })}
                  </View>
                )}
              </>
            )}

            {/* 받은 메이트 신청 (검색 중에는 숨김 — 의도적 UX) */}
            {received.isError ? (
              <Text style={[styles.emptyText, { marginTop: 24 }]}>신청을 불러오지 못했어요</Text>
            ) : (received.data ?? []).length > 0 ? (
              <>
                <Text style={[styles.label, { marginTop: 28 }]}>받은 메이트 신청</Text>
                <View style={{ gap: 10 }}>
                  {(received.data ?? []).map((req) => (
                    <View key={req.mateRequestId} style={styles.card}>
                      <EmojiCircle emoji={emojiFor(req.fromUser.nickname)} size={48} />
                      <View style={{ flex: 1, minWidth: 0 }}>
                        <Text style={styles.name}>{req.fromUser.nickname ?? '알 수 없음'}</Text>
                      </View>
                      <View style={{ flexDirection: 'row', gap: 8 }}>
                        <Pressable
                          disabled={accept.isPending}
                          onPress={() =>
                            accept.mutate(req.mateRequestId, {
                              onSuccess: () => Alert.alert('메이트', '메이트가 되었어요'),
                              onError: (err) => Alert.alert('오류', mateErrorMessage(err)),
                            })
                          }
                          style={[
                            styles.addChip,
                            { backgroundColor: T2.brand, borderColor: T2.brand, opacity: accept.isPending ? 0.5 : 1 },
                          ]}
                        >
                          <Text style={{ fontSize: 12, fontWeight: '700', color: '#fff', letterSpacing: -0.2 }}>수락</Text>
                        </Pressable>
                        <Pressable
                          disabled={decline.isPending}
                          onPress={() =>
                            decline.mutate(req.mateRequestId, {
                              onError: (err) => Alert.alert('오류', mateErrorMessage(err)),
                            })
                          }
                          style={[
                            styles.addChip,
                            { backgroundColor: '#fff', borderColor: T2.border, opacity: decline.isPending ? 0.5 : 1 },
                          ]}
                        >
                          <Text style={{ fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: -0.2 }}>거절</Text>
                        </Pressable>
                      </View>
                    </View>
                  ))}
                </View>
              </>
            ) : null}

            {/* 보낸 메이트 신청 (PENDING/DECLINED만, 검색 중에는 숨김) */}
            {(() => {
              const sentVisible = (sent.data ?? []).filter(
                (r) => r.status === 'PENDING' || r.status === 'DECLINED',
              );
              if (sent.isError || sentVisible.length === 0) return null;
              return (
                <>
                  <Text style={[styles.label, { marginTop: 28 }]}>보낸 메이트 신청</Text>
                  <View style={{ gap: 10 }}>
                    {sentVisible.map((req) => {
                      const isPending = req.status === 'PENDING';
                      return (
                        <Pressable
                          key={req.mateRequestId}
                          style={styles.card}
                          onPress={() => navigation.navigate('MateProfile', { userId: req.toUser.userId })}
                        >
                          <EmojiCircle emoji={emojiFor(req.toUser.nickname)} size={48} />
                          <View style={{ flex: 1, minWidth: 0 }}>
                            <Text style={styles.name}>{req.toUser.nickname ?? '알 수 없음'}</Text>
                          </View>
                          <View
                            style={[
                              styles.addChip,
                              { backgroundColor: '#fff', borderColor: T2.border },
                            ]}
                          >
                            <Text style={{ fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: -0.2, opacity: isPending ? 1 : 0.5 }}>
                              {isPending ? '대기 중' : '거절됨'}
                            </Text>
                          </View>
                        </Pressable>
                      );
                    })}
                  </View>
                </>
              );
            })()}
          </>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 40 },
  search: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 9,
    marginBottom: 8,
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
  },
  searchInput: { flex: 1, fontSize: 14, color: T2.text, letterSpacing: -0.2, padding: 0 },
  emptyText: { fontSize: 14, color: T2.textMute, textAlign: 'center', marginTop: 32 },

  label: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginTop: 4, marginBottom: 12 },

  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 13,
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: T2.border,
  },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  name: { flexShrink: 1, fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  togetherBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  togetherText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  meta: { fontSize: 12, color: T2.textMute, marginTop: 4 },

  nowRow: { flexDirection: 'row', alignItems: 'center', gap: 5, marginTop: 5 },
  nowDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: C.open },
  nowText: { fontSize: 12, fontWeight: '700', color: C.open, letterSpacing: -0.2 },
  nowPlace: { flexShrink: 1, fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },

  tagRow: { flexDirection: 'row', gap: 5, marginTop: 8 },
  tag: { backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, paddingHorizontal: 7, paddingVertical: 2, borderRadius: 6 },
  tagText: { fontSize: 11, fontWeight: '600', color: T2.textSub },

  mateChip: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 11,
    paddingVertical: 7,
    borderRadius: 9,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
  },
  mateChipText: { fontSize: 12, fontWeight: '700', color: T2.textSub, letterSpacing: -0.2 },
  addChip: { alignSelf: 'flex-start', paddingHorizontal: 12, paddingVertical: 7, borderRadius: 9, borderWidth: 1 },
});
