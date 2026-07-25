// ChatRoom — 매칭 대화방. 대화 목록(ConversationList)에서 진입.
// 말풍선(텍스트/사진) + 입력바 + CLOSED(종료된 대화) 잠금.
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
  Platform,
  Alert,
  Modal,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Screen, StateView, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useMessages, useSendMessage, useMarkRead, useConversations } from '../queries';
import { isClosed } from '../chatFormat';
import type { ChatMessage } from '../types';
import { pickImages, uploadImages } from '@/shared/upload/imageUpload';
import { useBlockUser } from '@/features/safety/queries';

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
  // 우상단 … 메뉴(신고/차단) — MateProfile.tsx와 동일하게 버튼 아래 드롭다운 카드로 표시.
  const [menuOpen, setMenuOpen] = useState(false);
  const insets = useSafeAreaInsets();

  // 진입/새 메시지 수신 시 읽음 처리. markRead.mutate 참조는 매 렌더 안정적이지 않을 수 있어
  // 의존성엔 넣지 않고 id·메시지 개수 변화에만 반응(무한루프 방지).
  useEffect(() => {
    markRead.mutate();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, messages?.length]);

  const onSendText = () => {
    const t = text.trim();
    if (!t || sendMut.isPending) return;
    setText('');
    sendMut.mutate({ type: 'TEXT', text: t });
  };

  const onAttach = async () => {
    if (sendMut.isPending || uploading) return;
    const picked = await pickImages(1);
    if (!picked.length) return;
    setUploading(true);
    try {
      const urls = await uploadImages(picked.map((p) => p.uri));
      if (urls[0]) sendMut.mutate({ type: 'IMAGE', imageUrl: urls[0] });
    } catch {
      Alert.alert('업로드 실패', '사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setUploading(false);
    }
  };

  // partnerUserId는 대화 상대 id — 메시지 발신자가 상대가 아니면 내가 보낸 것(현재 앱엔
  // 로그인 사용자 id를 직접 노출하는 훅이 없어, 1:1 대화에서 상대 기준으로 소유를 판정).
  const partnerUserId = conv?.partnerUserId;

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
  const renderItem = ({ item }: { item: ChatMessage }) => {
    const mine = item.senderUserId !== partnerUserId;
    return (
      <View style={[styles.bubbleRow, mine ? styles.rowMine : styles.rowOther]}>
        <View style={[styles.bubble, mine ? styles.bubbleMine : styles.bubbleOther]}>
          {item.type === 'IMAGE' && item.imageUrl ? (
            <Image source={{ uri: item.imageUrl }} style={styles.image} />
          ) : (
            <Text style={[styles.msgText, mine && styles.msgTextMine]}>{item.text}</Text>
          )}
        </View>
      </View>
    );
  };

  return (
    <Screen bg={T2.bg}>
      <MoreHeader
        title={conv?.partnerNickname ?? '대화'}
        onBack={() => navigation.goBack()}
        right={
          conv ? (
            <Pressable onPress={() => setMenuOpen(true)} hitSlop={10} style={styles.moreBtn}>
              <View style={styles.dotsRow}>
                {[0, 1, 2].map((d) => (
                  <View key={d} style={styles.dot} />
                ))}
              </View>
            </Pressable>
          ) : undefined
        }
      />

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

      {conv && <Text style={styles.sub}>{conv.placeName}</Text>}

      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {isLoading || !conv ? (
          <StateView kind="loading" />
        ) : isError ? (
          <StateView kind="error" onRetry={() => refetch()} />
        ) : (
          <FlatList
            data={messages ?? []}
            keyExtractor={(m) => String(m.id)}
            renderItem={renderItem}
            style={styles.flex}
            contentContainerStyle={styles.list}
          />
        )}

        {closed ? (
          <View style={styles.closedBar}>
            <Text style={styles.closedText}>종료된 대화예요 · 새 메시지는 보낼 수 없어요</Text>
          </View>
        ) : (
          <View style={styles.inputBar}>
            <Pressable
              onPress={onAttach}
              style={[styles.attach, uploading && styles.attachDisabled]}
              disabled={sendMut.isPending || uploading}
              hitSlop={6}
              accessibilityRole="button"
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
              onPress={onSendText}
              style={styles.send}
              disabled={!text.trim() || sendMut.isPending}
              accessibilityRole="button"
            >
              <Text style={styles.sendText}>전송</Text>
            </Pressable>
          </View>
        )}
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  moreBtn: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center' },
  dotsRow: { flexDirection: 'row', gap: 3 },
  dot: { width: 3.5, height: 3.5, borderRadius: 2, backgroundColor: T2.text },
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
  sub: { fontSize: 12, color: T2.textSub, textAlign: 'center', marginBottom: 4 },
  list: { padding: 12 },
  bubbleRow: { marginVertical: 3, flexDirection: 'row' },
  rowMine: { justifyContent: 'flex-end' },
  rowOther: { justifyContent: 'flex-start' },
  bubble: { maxWidth: '76%', borderRadius: 14, paddingHorizontal: 12, paddingVertical: 8 },
  bubbleMine: { backgroundColor: T2.brand },
  bubbleOther: { backgroundColor: T2.surface, borderWidth: 1, borderColor: T2.border },
  msgText: { fontSize: 14, color: T2.text },
  msgTextMine: { color: '#fff' },
  image: { width: 180, height: 180, borderRadius: 10 },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 8,
    padding: 10,
    borderTopWidth: 1,
    borderTopColor: T2.border,
    backgroundColor: T2.surface,
  },
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
    paddingHorizontal: 14,
    height: 36,
    borderRadius: 18,
    backgroundColor: T2.brandSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sendText: { color: T2.brand, fontWeight: '800', fontSize: 13 },
  closedBar: {
    padding: 14,
    borderTopWidth: 1,
    borderTopColor: T2.border,
    alignItems: 'center',
    backgroundColor: T2.surface,
  },
  closedText: { fontSize: 12, color: T2.textMute },
});
