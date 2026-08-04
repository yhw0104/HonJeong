// ChatRoom — 매칭 대화방. 대화 목록(ConversationList)에서 진입.
// 카카오톡풍: 연속 메시지는 상대 프로필 1개만·시간·내 메시지 '읽음' 표시. 입력바 + CLOSED(종료) 잠금.
import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Pressable,
  FlatList,
  Image,
  StyleSheet,
  KeyboardAvoidingView,
  Keyboard,
  Platform,
  Alert,
  Modal,
  ActivityIndicator,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';
import { Screen, StateView, Avatar, PhotoViewer } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useMessages, useSendMessage, useMarkRead, useConversations } from '../queries';
import { isClosed, formatTime, readByPartner, truncate } from '../chatFormat';
import type { ChatMessage } from '../types';
import { pickImages, uploadImages } from '@/shared/upload/imageUpload';
import { useBlockUser } from '@/features/safety/queries';

// 헤더 식당명 최대 글자수(초과 시 …). 8자로 확정(2026-07-27, 사용자 확인) —
// 닉네임이 길어도 헤더가 밀리지 않는 폭이 우선.
const PLACE_MAX_CHARS = 8;

export function ChatRoomScreen({ navigation, route }: RootStackScreenProps<'ChatRoom'>) {
  const id = route.params.conversationId;
  const { data: messages, isLoading, isError, refetch } = useMessages(id);
  const conversations = useConversations();
  const conv = conversations.data?.find((c) => c.conversationId === id);
  const closed = conv ? isClosed(conv.status) : false;
  const sendMut = useSendMessage(id);
  const markRead = useMarkRead(id);
  const blockMut = useBlockUser();
  const [text, setText] = useState('');
  const [uploading, setUploading] = useState(false);
  // 전송 대기 중인 첨부 사진(로컬 uri). ＋로 고르면 바로 보내지 않고 여기 담아 두고,
  // 입력창 위 썸네일로 보여준 뒤 전송 버튼을 눌러야 올라간다 — 잘못 고른 사진을 되돌릴 수 있게.
  // 업로드도 이 시점까지 미룬다(취소하면 서버에 고아 파일이 남지 않는다).
  const [pending, setPending] = useState<string | null>(null);
  const [viewer, setViewer] = useState<string | null>(null); // 크게 보는 중인 대화 사진
  // 우상단 … 메뉴(신고/차단) — MateProfile.tsx와 동일하게 버튼 아래 드롭다운 카드로 표시.
  const [menuOpen, setMenuOpen] = useState(false);
  const insets = useSafeAreaInsets();

  // 하단 바(입력/종료 안내)를 홈 인디케이터 영역까지 같은 색으로 덮기 위한 여백.
  // Screen에서 bottom edge를 뺐으므로 그 몫을 여기서 준다 — 안 그러면 바 아래에 크림색 띠가 남는다.
  // 키보드가 올라오면 iOS 키보드가 이미 그 영역을 덮으므로 0으로 되돌린다(안 그러면 빈 틈이 생긴다).
  const [kbUp, setKbUp] = useState(false);
  useEffect(() => {
    const show = Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow';
    const hide = Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide';
    const s1 = Keyboard.addListener(show, () => setKbUp(true));
    const s2 = Keyboard.addListener(hide, () => setKbUp(false));
    return () => { s1.remove(); s2.remove(); };
  }, []);
  const barBottom = kbUp ? 0 : insets.bottom;

  // 진입/새 메시지 수신 시 읽음 처리. markRead.mutate 참조는 매 렌더 안정적이지 않을 수 있어
  // 의존성엔 넣지 않고 id·메시지 개수 변화에만 반응(무한루프 방지).
  useEffect(() => {
    markRead.mutate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, messages?.length]);

  const canSend = (!!text.trim() || !!pending) && !sendMut.isPending && !uploading;

  // 전송 = 첨부 사진(있으면) → 글자(있으면) 순서. 사진 업로드가 실패하면 글자도 보내지 않고
  // 첨부를 그대로 남긴다 — 사진만 사라지고 글자만 올라가는 어긋난 상태를 만들지 않기 위함.
  //
  // ★두 단계 모두 mutateAsync로 결과를 기다린다. mutate(fire-and-forget)를 쓰면 전송 실패가
  //   아무 표시 없이 사라진다 — "가끔 사진이 안 보내진다"의 원인이 이것이었다(업로드는 성공했는데
  //   그 다음 메시지 생성이 실패하면, 화면에는 아무 일도 안 일어난 것처럼 보였다).
  //   실패 시 입력한 글자도 되돌려 놓는다. 다시 치게 만들지 않기 위함.
  const onSend = async () => {
    if (!canSend) return;
    const t = text.trim();
    if (pending) {
      setUploading(true);
      try {
        const [url] = await uploadImages([pending]);
        if (url) await sendMut.mutateAsync({ type: 'IMAGE', imageUrl: url });
        setPending(null);
      } catch (e) {
        // uploadImages는 사람이 읽을 수 있는 메시지를 담아 던진다(용량 초과 등) — 그대로 보여준다.
        Alert.alert('전송 실패', e instanceof Error && e.message ? e.message : '사진을 보내지 못했어요. 잠시 후 다시 시도해주세요.');
        return;
      } finally {
        setUploading(false);
      }
    }
    if (t) {
      setText('');
      try {
        await sendMut.mutateAsync({ type: 'TEXT', text: t });
      } catch {
        setText(t); // 친 글자를 되돌려 준다
        Alert.alert('전송 실패', '메시지를 보내지 못했어요. 잠시 후 다시 시도해주세요.');
      }
    }
  };

  // ＋ — 사진을 고르기만 하고 보내지는 않는다(전송은 onSend에서). 이미 대기 중이면 새로 고르지 않는다.
  const onAttach = async () => {
    if (sendMut.isPending || uploading || pending) return;
    const picked = await pickImages(1);
    if (picked[0]) setPending(picked[0].uri);
  };

  // partnerUserId는 대화 상대 id — 메시지 발신자가 상대가 아니면 내가 보낸 것(현재 앱엔
  // 로그인 사용자 id를 직접 노출하는 훅이 없어, 1:1 대화에서 상대 기준으로 소유를 판정).
  const partnerUserId = conv?.partnerUserId;
  const partnerLastReadAt = conv?.partnerLastReadAt ?? null;
  const msgs = messages ?? [];
  // 내가 보낸 마지막 메시지의 인덱스(그 메시지에만 '읽음'을 붙인다 — 그 이전 내 메시지도 당연히 읽음).
  const lastMineIndex = (() => {
    for (let i = msgs.length - 1; i >= 0; i--) {
      if (msgs[i].senderUserId !== partnerUserId) return i;
    }
    return -1;
  })();

  const openReport = () => {
    setMenuOpen(false);
    if (!conv) return;
    navigation.navigate('ReportForm', {
      targetType: 'USER',
      targetId: conv.partnerUserId,
      targetNickname: conv.partnerNickname,
    });
  };

  const confirmBlock = () => {
    setMenuOpen(false);
    if (!conv) return;
    const nickname = conv.partnerNickname;
    Alert.alert('차단', `${nickname}님을 차단할까요?\n서로의 프로필과 혼밥 현황이 보이지 않게 돼요.`, [
      { text: '취소', style: 'cancel' },
      {
        text: '차단',
        style: 'destructive',
        // 차단하면 백엔드가 매칭을 종료→대화를 CLOSED로 만들므로 목록으로 되돌아간다.
        onPress: () =>
          blockMut.mutate(conv.partnerUserId, {
            onSuccess: () => navigation.goBack(),
            onError: () => Alert.alert('차단 실패', '잠시 후 다시 시도해주세요.'),
          }),
      },
    ]);
  };

  const renderItem = ({ item, index }: { item: ChatMessage; index: number }) => {
    const mine = item.senderUserId !== partnerUserId;
    const prev = msgs[index - 1];
    const next = msgs[index + 1];
    const firstOfRun = !prev || prev.senderUserId !== item.senderUserId;
    const lastOfRun = !next || next.senderUserId !== item.senderUserId;
    const time = lastOfRun ? formatTime(item.createdAt) : '';
    const showRead = mine && index === lastMineIndex && readByPartner(item.createdAt, partnerLastReadAt);

    const bubble = (
      <View style={[styles.bubble, mine ? styles.bubbleMine : styles.bubbleOther]}>
        {item.type === 'IMAGE' && item.imageUrl ? (
          // 탭하면 전체화면 뷰어(확대 가능) — 대화 속 사진은 말풍선 크기로는 알아보기 어렵다.
          <Pressable
            onPress={() => setViewer(item.imageUrl!)}
            accessibilityRole="imagebutton"
            accessibilityLabel="사진 크게 보기"
          >
            <Image source={{ uri: item.imageUrl }} style={styles.image} />
          </Pressable>
        ) : (
          <Text style={[styles.msgText, mine && styles.msgTextMine]}>{item.text}</Text>
        )}
      </View>
    );

    if (mine) {
      return (
        <View style={[styles.bubbleRow, styles.rowMine, { marginTop: firstOfRun ? 8 : 2 }]}>
          <View style={styles.metaMine}>
            {showRead && <Text style={styles.readText}>읽음</Text>}
            {!!time && <Text style={styles.timeText}>{time}</Text>}
          </View>
          {bubble}
        </View>
      );
    }
    return (
      <View style={[styles.bubbleRow, styles.rowOther, { marginTop: firstOfRun ? 8 : 2 }]}>
        {firstOfRun ? (
          <View style={styles.avatarWrap}>
            <Avatar uri={conv?.partnerProfileImageUrl} size={32} />
          </View>
        ) : (
          <View style={styles.avatarSpacer} />
        )}
        {bubble}
        {!!time && <Text style={[styles.timeText, styles.timeOther]}>{time}</Text>}
      </View>
    );
  };

  return (
    // bottom edge는 Screen이 아니라 하단 바가 직접 처리한다 — SafeAreaView가 아래 여백을 잡으면
    // 흰 입력바/종료 안내 아래에 크림색(T2.bg) 띠가 남아 두 톤으로 갈린다.
    <Screen bg={T2.bg} edges={['top', 'left', 'right']}>
      {/* 커스텀 헤더 — 뒤로 / 이름·식당명(중앙, 식당명 길면 …) / … 메뉴 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10} style={styles.headerBack}>
          <Text style={styles.headerArrow}>←</Text>
        </Pressable>
        <View style={styles.headerCenter}>
          <Text style={styles.headerName} numberOfLines={1}>
            {conv?.partnerNickname ?? '대화'}
          </Text>
          {!!conv?.placeName && (
            <Text style={styles.headerPlace} numberOfLines={1}>
              {truncate(conv.placeName, PLACE_MAX_CHARS)}
            </Text>
          )}
        </View>
        <View style={styles.headerRight}>
          {conv ? (
            <Pressable onPress={() => setMenuOpen(true)} hitSlop={10} style={styles.moreBtn}>
              <View style={styles.dotsRow}>
                {[0, 1, 2].map((d) => (
                  <View key={d} style={styles.dot} />
                ))}
              </View>
            </Pressable>
          ) : null}
        </View>
      </View>

      {/* … 드롭다운 메뉴 — 버튼 아래 카드, 바깥 탭으로 닫힘(MateProfile.tsx와 동일 패턴) */}
      <Modal visible={menuOpen} transparent animationType="fade" onRequestClose={() => setMenuOpen(false)}>
        <Pressable style={styles.menuBackdrop} onPress={() => setMenuOpen(false)}>
          <Pressable style={[styles.menuCard, { top: insets.top + 52 }]} onPress={() => {}}>
            <Pressable style={styles.menuItem} onPress={openReport}>
              <Text style={styles.menuItemText}>신고하기</Text>
            </Pressable>
            <View style={styles.menuDivider} />
            <Pressable style={styles.menuItem} onPress={confirmBlock}>
              <Text style={[styles.menuItemText, styles.menuItemDanger]}>차단하기</Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>

      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {isLoading || !conv ? (
          <StateView kind="loading" />
        ) : isError ? (
          <StateView kind="error" onRetry={() => refetch()} />
        ) : (
          <FlatList
            data={msgs}
            keyExtractor={(m) => String(m.id)}
            renderItem={renderItem}
            style={styles.flex}
            contentContainerStyle={styles.list}
          />
        )}

        {closed ? (
          <View style={[styles.closedBar, { paddingBottom: 14 + barBottom }]}>
            <Text style={styles.closedText}>종료된 대화예요 · 새 메시지는 보낼 수 없어요</Text>
          </View>
        ) : (
          <View style={[styles.composer, { paddingBottom: barBottom }]}>
            {/* 전송 대기 사진 — 입력창 바로 위에 붙는다(카카오톡과 같은 자리). ×로 취소. */}
            {pending && (
              <View style={styles.pendingBar}>
                <View style={styles.pendingItem}>
                  <Image source={{ uri: pending }} style={styles.pendingThumb} />
                  {uploading ? (
                    <View style={styles.pendingBusy}>
                      <ActivityIndicator size="small" color="#fff" />
                    </View>
                  ) : (
                    <Pressable
                      style={styles.pendingRemove}
                      onPress={() => setPending(null)}
                      hitSlop={8}
                      accessibilityRole="button"
                      accessibilityLabel="첨부 취소"
                    >
                      <Text style={styles.pendingRemoveX}>×</Text>
                    </Pressable>
                  )}
                </View>
                <Text style={styles.pendingHint}>전송을 누르면 보내져요</Text>
              </View>
            )}
            <View style={styles.inputBar}>
              <Pressable
                onPress={onAttach}
                style={[styles.attach, (uploading || !!pending) && styles.attachDisabled]}
                disabled={sendMut.isPending || uploading || !!pending}
                hitSlop={6}
                accessibilityRole="button"
                accessibilityLabel="사진 첨부"
              >
                <Text style={styles.attachText}>＋</Text>
              </Pressable>
              <TextInput
                style={styles.input}
                value={text}
                onChangeText={setText}
                placeholder="메시지 입력"
                placeholderTextColor={T2.textMute}
                multiline
              />
              <Pressable
                onPress={onSend}
                style={[styles.send, !canSend && styles.sendDisabled]}
                disabled={!canSend}
                accessibilityRole="button"
                accessibilityLabel="전송"
              >
                <Svg width={18} height={18} viewBox="0 0 24 24">
                  {/* 오른쪽을 향하는 종이비행기 — 시각적 균형 위해 오른쪽으로 살짝 이동 */}
                  <Path d="M5.5 5L21.5 12L5.5 19L5.5 13.5L15.5 12L5.5 10.5L5.5 5Z" fill="#fff" />
                </Svg>
              </Pressable>
            </View>
          </View>
        )}
      </KeyboardAvoidingView>

      <PhotoViewer uri={viewer} onClose={() => setViewer(null)} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  // 헤더
  header: { height: 56, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12 },
  headerBack: { width: 32, height: 32, alignItems: 'flex-start', justifyContent: 'center' },
  headerArrow: { fontSize: 22, color: T2.text },
  headerCenter: { flex: 1, flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 6, overflow: 'hidden' },
  headerName: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  headerPlace: { flexShrink: 1, fontSize: 12.5, color: T2.textSub, letterSpacing: -0.2 },
  headerRight: { width: 32, alignItems: 'flex-end' },
  moreBtn: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center' },
  dotsRow: { flexDirection: 'row', gap: 3 },
  dot: { width: 3.5, height: 3.5, borderRadius: 2, backgroundColor: T2.text },
  // 메뉴
  menuBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.12)' },
  menuCard: {
    position: 'absolute',
    right: 12,
    minWidth: 148,
    backgroundColor: '#fff',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: T2.border,
    paddingVertical: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.12,
    shadowRadius: 16,
    elevation: 6,
  },
  menuItem: { paddingVertical: 13, paddingHorizontal: 16 },
  menuItemText: { fontSize: 14, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  menuItemDanger: { color: '#E1493F' },
  menuDivider: { height: 1, backgroundColor: T2.border, marginHorizontal: 8 },
  // 메시지
  list: { paddingHorizontal: 12, paddingVertical: 10 },
  bubbleRow: { flexDirection: 'row', alignItems: 'flex-end' },
  rowMine: { justifyContent: 'flex-end' },
  rowOther: { justifyContent: 'flex-start' },
  avatarWrap: { marginRight: 6, alignSelf: 'flex-start' },
  avatarSpacer: { width: 32, marginRight: 6 },
  bubble: { maxWidth: '70%', borderRadius: 14, paddingHorizontal: 12, paddingVertical: 8 },
  bubbleMine: { backgroundColor: T2.brand },
  bubbleOther: { backgroundColor: T2.surface, borderWidth: 1, borderColor: T2.border },
  msgText: { fontSize: 14, color: T2.text },
  msgTextMine: { color: '#fff' },
  image: { width: 180, height: 180, borderRadius: 10 },
  metaMine: { marginRight: 4, alignItems: 'flex-end', justifyContent: 'flex-end' },
  readText: { fontSize: 10, fontWeight: '700', color: T2.brand, marginBottom: 1 },
  timeText: { fontSize: 10, color: T2.textMute, marginBottom: 2 },
  timeOther: { marginLeft: 6 },
  // 입력바 / 종료
  // composer가 첨부 미리보기 + 입력바를 함께 감싸고 하단 안전영역까지 흰색으로 덮는다.
  // 테두리·배경을 여기로 올렸으므로 inputBar에는 두지 않는다(미리보기가 붙어도 선이 하나만 보이게).
  composer: { borderTopWidth: 1, borderTopColor: T2.border, backgroundColor: T2.surface },
  inputBar: { flexDirection: 'row', alignItems: 'flex-end', gap: 8, padding: 10 },
  pendingBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 10,
    paddingTop: 10,
  },
  // ×가 썸네일 모서리에 걸치도록 위/오른쪽에 여백을 두고, ×는 그 여백 안에 넣는다.
  // 음수 좌표로 부모 밖에 두면 안드로이드에서 잘려 눌리지 않는다.
  pendingItem: { paddingTop: 6, paddingRight: 6 },
  pendingThumb: { width: 56, height: 56, borderRadius: 10, backgroundColor: T2.bg },
  pendingRemove: {
    position: 'absolute',
    top: 0,
    right: 0,
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: 'rgba(20,20,20,0.72)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  pendingRemoveX: { color: '#fff', fontSize: 15, lineHeight: 17, fontWeight: '700' },
  // 업로드 중에는 ×를 감추고 진행 표시로 바꾼다 — 올라가는 중에 취소를 누를 수 없게.
  pendingBusy: {
    position: 'absolute',
    top: 6, // pendingItem의 위 여백만큼 내려 썸네일과 정확히 겹치게
    left: 0,
    width: 56,
    height: 56,
    borderRadius: 10,
    backgroundColor: 'rgba(0,0,0,0.35)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  pendingHint: { fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },
  attach: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: T2.bg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  attachDisabled: { opacity: 0.4 },
  attachText: { fontSize: 20, color: T2.textSub },
  input: {
    flex: 1,
    maxHeight: 100,
    backgroundColor: T2.bg,
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 8,
    fontSize: 14,
    color: T2.text,
  },
  send: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: T2.brand,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendDisabled: { backgroundColor: T2.borderStrong },
  // paddingBottom은 렌더에서 준다(14 + 하단 안전영역) — 흰색이 화면 맨 아래까지 이어지게.
  closedBar: {
    paddingTop: 14,
    paddingHorizontal: 14,
    borderTopWidth: 1,
    borderTopColor: T2.border,
    alignItems: 'center',
    backgroundColor: T2.surface,
  },
  closedText: { fontSize: 12, color: T2.textMute },
});
