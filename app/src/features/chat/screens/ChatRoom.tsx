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
} from 'react-native';
import { Screen, StateView, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useMessages, useSendMessage, useMarkRead, useConversations } from '../queries';
import { isClosed } from '../chatFormat';
import type { ChatMessage } from '../types';
import { pickImages, uploadImages } from '@/shared/upload/imageUpload';

export function ChatRoomScreen({ navigation, route }: RootStackScreenProps<'ChatRoom'>) {
  const id = route.params.conversationId;
  const { data: messages, isLoading, isError, refetch } = useMessages(id);
  const conversations = useConversations();
  const conv = conversations.data?.find((c) => c.conversationId === id);
  const closed = conv ? isClosed(conv.status) : false;
  const sendMut = useSendMessage(id);
  const markRead = useMarkRead(id);
  const [text, setText] = useState('');

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
    if (sendMut.isPending) return;
    const picked = await pickImages(1);
    if (!picked.length) return;
    const urls = await uploadImages(picked.map((p) => p.uri));
    if (urls[0]) sendMut.mutate({ type: 'IMAGE', imageUrl: urls[0] });
  };

  // partnerUserId는 대화 상대 id — 메시지 발신자가 상대가 아니면 내가 보낸 것(현재 앱엔
  // 로그인 사용자 id를 직접 노출하는 훅이 없어, 1:1 대화에서 상대 기준으로 소유를 판정).
  const partnerUserId = conv?.partnerUserId;
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
      <MoreHeader title={conv?.partnerNickname ?? '대화'} onBack={() => navigation.goBack()} />
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
              style={styles.attach}
              disabled={sendMut.isPending}
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
