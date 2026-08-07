// 푸시 → 이동 화면 매핑(순수 함수).
//
// 이 파일은 shared/push/index.ts를 import하지 않는다 — index.ts는 @react-native-firebase를
// import 시점에 조회해서 jest에서 즉사한다(prompt.ts와 같은 이유).
import type { NotificationType } from '@/features/notifications/api';
import { notificationTarget } from '@/features/notifications/copy';
import type { RootStackParamList } from '@/navigation/types';

/** 서버가 push data에 담아 보내는 값. FCM data는 항상 문자열이다(FcmPushSender.putData). */
export type PushData = { type: string; conversationId?: string };

export type PushTarget = { screen: keyof RootStackParamList; params?: Record<string, unknown> };

// 알림함에 쌓이는 6종. 서버 NotificationType과 이름이 1:1로 같다(PushType.from 참조).
// CHAT_MESSAGE는 여기 없다 — 알림함에 저장되지 않는 푸시 전용 종류다.
const IN_APP_TYPES: NotificationType[] = [
  'MEAL_REQUEST_RECEIVED',
  'MEAL_REQUEST_ACCEPTED',
  'MEAL_MATCH_CANCELLED',
  'MATE_REQUEST_RECEIVED',
  'MATE_REQUEST_ACCEPTED',
  'BADGE_EARNED',
];

/**
 * 푸시를 눌렀을 때 갈 화면.
 *
 * 알림함 6종은 notificationTarget에 위임한다 — 알림함에서 누르나 배너에서 누르나
 * 같은 화면에 도착해야 하는데, 매핑을 두 벌로 두면 반드시 갈린다.
 * 채팅만 여기서 처리한다(대화방 id가 필요해 반환 형태가 다르다).
 *
 * 모르는 종류면 null을 돌려 아무 데도 가지 않는다 — 백엔드를 먼저 배포하므로
 * 구버전 앱이 새 종류를 받는 상황이 상시 존재한다.
 */
export function pushTarget(data: PushData): PushTarget | null {
  if (data.type === 'CHAT_MESSAGE') {
    const id = Number(data.conversationId);
    // id가 없거나 깨졌으면 대화 목록으로 보낸다 — 방을 못 찾아 빈 화면을 띄우느니
    // 목록에서 직접 고르게 하는 편이 낫다.
    return Number.isFinite(id) && id > 0
      ? { screen: 'ChatRoom', params: { conversationId: id } }
      : { screen: 'MainTabs', params: { screen: 'Chat' } };
  }
  if (!IN_APP_TYPES.includes(data.type as NotificationType)) return null;
  return { screen: notificationTarget(data.type as NotificationType) };
}
