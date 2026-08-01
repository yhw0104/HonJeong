// ConversationList — '대화' 탭. 매칭 성사 후 열리는 대화방 목록.
import React from 'react';
import { View, Text, Pressable, FlatList, StyleSheet, Alert } from 'react-native';
import Swipeable, { type SwipeableMethods } from 'react-native-gesture-handler/ReanimatedSwipeable';
import Animated, { useAnimatedStyle, type SharedValue } from 'react-native-reanimated';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Screen, Avatar, StateView, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackParamList } from '@/navigation/types';
import { useConversations, useDeleteConversation } from '../queries';
import { messagePreview, formatListTime } from '../chatFormat';
import type { ConversationSummary } from '../types';

/** 삭제 버튼의 기본(스냅) 너비. 스와이프를 놓으면 이 크기로 돌아온다. */
const ACTION_WIDTH = 76;

/**
 * 스와이프 삭제 버튼 — 드래그한 만큼 늘어나고, 손을 떼면 기본 너비로 되돌아온다.
 * translation은 오른쪽 액션에서 음수(왼쪽으로 끌수록 작아짐)라 부호를 뒤집어 너비로 쓴다.
 * useAnimatedStyle을 쓰므로 renderRightActions 안에서 인라인으로 못 만들고 별도 컴포넌트여야 한다(훅 규칙).
 */
function DeleteAction({ translation, onPress }: { translation: SharedValue<number>; onPress: () => void }) {
  const animatedStyle = useAnimatedStyle(() => ({
    width: Math.max(ACTION_WIDTH, -translation.value),
  }));

  return (
    <Animated.View style={[styles.deleteAction, animatedStyle]}>
      {/* 아이콘만 있는 버튼이라 스크린리더용 라벨을 명시한다. */}
      <Pressable style={styles.deleteActionPress} onPress={onPress} accessibilityRole="button" accessibilityLabel="대화 삭제">
        <Icon name="trash" size={28} color="#fff" />
      </Pressable>
    </Animated.View>
  );
}

export function ConversationListScreen() {
  const nav = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { data, isLoading, isError, refetch } = useConversations();
  const now = new Date(); // 목록 시각 표시 기준(폴링마다 갱신)
  const delMut = useDeleteConversation();
  // 카카오톡처럼 한 번에 한 행만 열려 있게 한다 — 현재 열린 행의 제어 핸들을 담아둔다.
  const openRowRef = React.useRef<SwipeableMethods | null>(null);

  const closeOpenRow = () => {
    openRowRef.current?.close();
    openRowRef.current = null;
  };

  // 실수 삭제 방지 — 기존 MyReviews·Favorites와 같은 확인 팝업 패턴.
  // 취소·삭제 모두 행을 닫는다 — 안 닫으면 빨간 버튼이 열린 채로 남는다.
  const confirmDelete = (item: ConversationSummary, swipeable: SwipeableMethods) => {
    Alert.alert(
      '대화 삭제',
      `${item.partnerNickname}님과의 대화를 삭제할까요?\n내 목록에서만 사라지고 상대에게는 그대로 남아요.`,
      [
        { text: '취소', style: 'cancel', onPress: () => swipeable.close() },
        {
          text: '삭제', style: 'destructive', onPress: () => {
            delMut.mutate(item.conversationId);
            swipeable.close();
          },
        },
      ],
    );
  };

  if (isLoading) {
    return (
      <Screen>
        <Text style={styles.header}>대화</Text>
        <StateView kind="loading" />
      </Screen>
    );
  }
  if (isError) {
    return (
      <Screen>
        <Text style={styles.header}>대화</Text>
        <StateView kind="error" onRetry={() => refetch()} />
      </Screen>
    );
  }

  const list = data ?? [];
  if (list.length === 0) {
    return (
      <Screen>
        <Text style={styles.header}>대화</Text>
        <StateView kind="empty" message={'아직 대화가 없어요\n같이먹기가 성사되면 여기서 대화할 수 있어요'} />
      </Screen>
    );
  }

  const renderItem = ({ item }: { item: ConversationSummary }) => {
    // 이 행의 제어 핸들. ref 콜백이 채우고, 열림/닫힘 훅이 openRowRef와 비교하는 데 쓴다.
    let rowMethods: SwipeableMethods | null = null;

    // 열린 행이 있으면 탭은 '닫기'로만 쓴다(카카오톡 동작) — 그 상태에서 채팅방으로 들어가지 않는다.
    const onRowPress = () => {
      if (openRowRef.current) {
        closeOpenRow();
        return;
      }
      nav.navigate('ChatRoom', { conversationId: item.conversationId });
    };

    const row = (
      <Pressable style={styles.row} onPress={onRowPress}>
        <Avatar uri={item.partnerProfileImageUrl} size={52} />
        <View style={styles.body}>
          <View style={styles.line}>
            <Text style={styles.name} numberOfLines={1}>{item.partnerNickname}</Text>
            <View style={styles.placeChip}>
              <Text style={styles.placeChipText} numberOfLines={1}>{item.placeName}</Text>
            </View>
            {item.status === 'CLOSED' && (
              <View style={styles.closedChip}>
                <Text style={styles.closedChipText}>종료됨</Text>
              </View>
            )}
          </View>
          <Text style={styles.preview} numberOfLines={1}>
            {item.lastMessagePreview ? messagePreview({ type: 'TEXT', text: item.lastMessagePreview }) : ''}
          </Text>
        </View>
        <View style={styles.meta}>
          {/* 메시지가 아직 없으면 매칭 시각을 대신 보여준다 — 정렬 기준(마지막 활동 시각)과 같은 값. */}
          <Text style={styles.time}>{formatListTime(item.lastMessageAt ?? item.createdAt, now)}</Text>
          {item.unreadCount > 0 && (
            <View style={styles.badge}><Text style={styles.badgeText}>{item.unreadCount}</Text></View>
          )}
        </View>
      </Pressable>
    );

    // 진행 중(ACTIVE) 대화는 서버가 삭제를 거절하므로 스와이프 자체를 열지 않는다 —
    // 스와이프한 뒤 실패 알럿을 띄우는 것보다 규칙이 잘 전달된다.
    if (item.status !== 'CLOSED') return row;

    return (
      <Swipeable
        ref={(r) => { rowMethods = r; }}
        // 새 행이 열리기 직전에 이전 행을 닫아 항상 하나만 열려 있게 한다.
        onSwipeableWillOpen={() => {
          if (openRowRef.current && openRowRef.current !== rowMethods) {
            openRowRef.current.close();
          }
          openRowRef.current = rowMethods;
        }}
        // 이 행이 닫히면(스와이프 되돌리기·close() 호출 모두) 추적에서 지운다.
        onSwipeableWillClose={() => {
          if (openRowRef.current === rowMethods) openRowRef.current = null;
        }}
        renderRightActions={(_progress, translation, swipeable) => (
          <DeleteAction translation={translation} onPress={() => confirmDelete(item, swipeable)} />
        )}
        // overshoot를 막으면 스와이프가 버튼 너비에서 잘려 늘어나는 느낌이 안 난다 → 기본값(허용) 사용.
        // friction 1.4로 끌리는 감을 약간 무겁게 해 튕김을 줄인다.
        friction={1.4}
        // 라이브러리 기본 스프링(mass 2·damping 1000·stiffness 700)은 감쇠비가 13을 넘는 과감쇠라
        // 끝에서 질질 끌린다. 감쇠비 ~0.78로 낮춰 탄력을 주되 overshootClamping으로 넘어가진 않게 한다.
        animationOptions={{ mass: 1, damping: 28, stiffness: 320, overshootClamping: true }}
      >
        {row}
      </Swipeable>
    );
  };

  return (
    <Screen>
      <Text style={styles.header}>대화</Text>
      <FlatList
        data={list}
        keyExtractor={(c) => String(c.conversationId)}
        renderItem={renderItem}
        // 목록을 스크롤하면 열려 있던 행을 닫는다(카카오톡 동작).
        onScrollBeginDrag={closeOpenRow}
        ItemSeparatorComponent={() => <View style={styles.divider} />}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { fontSize: 20, fontWeight: '800', color: T2.text, paddingHorizontal: 16, paddingVertical: 12 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16, paddingVertical: 13, backgroundColor: T2.surface },
  body: { flex: 1, gap: 3 },
  line: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  // 이름(가장 큼) > 마지막 대화(중간) > 식당/종료됨 뱃지(가장 작음)
  name: { fontSize: 17, fontWeight: '700', color: T2.text, flexShrink: 1 },
  placeChip: { flexShrink: 1, backgroundColor: T2.bg, borderRadius: 6, paddingHorizontal: 7, paddingVertical: 2 },
  placeChipText: { fontSize: 11, fontWeight: '600', color: T2.textSub, letterSpacing: -0.2 },
  closedChip: { backgroundColor: T2.bg, borderRadius: 6, paddingHorizontal: 7, paddingVertical: 2 },
  closedChipText: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: -0.2 },
  preview: { fontSize: 14, color: T2.textSub },
  divider: { height: 1, backgroundColor: T2.borderStrong },
  meta: { alignItems: 'flex-end', gap: 5, minWidth: 40 },
  time: { fontSize: 11, color: T2.textMute },
  badge: { minWidth: 20, height: 20, borderRadius: 10, backgroundColor: T2.brand, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 6 },
  badgeText: { color: '#fff', fontSize: 11, fontWeight: '800' },
  // 삭제 빨강은 MyReviews의 actionDelete와 같은 값(#d11)을 쓴다 — 앱 전역 관례.
  // 너비는 DeleteAction의 useAnimatedStyle이 드래그에 맞춰 결정한다(여기서 고정하지 않는다).
  deleteAction: { justifyContent: 'center', alignItems: 'center', backgroundColor: '#d11' },
  deleteActionPress: { flex: 1, width: '100%', justifyContent: 'center', alignItems: 'center' },
});
