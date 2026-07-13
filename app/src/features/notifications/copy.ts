// 알림 문구·이동 매핑 — 서버는 type+actorNickname만 주고 문구는 여기서 조립한다(순수 함수, TDD 대상).
import type { NotificationType } from './api';

const MESSAGE: Record<NotificationType, string> = {
  MEAL_REQUEST_RECEIVED: '님이 같이 먹기를 신청했어요',
  MEAL_REQUEST_ACCEPTED: '님이 같이 먹기를 수락했어요',
  MEAL_MATCH_CANCELLED: '님이 같이 먹기 약속을 취소했어요',
  MATE_REQUEST_RECEIVED: '님이 메이트를 신청했어요',
  MATE_REQUEST_ACCEPTED: '님이 메이트를 수락했어요',
};

export function notificationMessage(type: NotificationType, actorNickname: string | null): string {
  return `${actorNickname ?? '누군가'}${MESSAGE[type]}`;
}

/** 알림 탭 시 이동할 화면. 같이먹기 수락은 '같이 먹는 중' 상태가 보이는 홈 지도로. */
export function notificationTarget(type: NotificationType): 'ReceivedRequests' | 'MainTabs' | 'Mates' {
  if (type === 'MEAL_REQUEST_RECEIVED') return 'ReceivedRequests';
  if (type === 'MEAL_REQUEST_ACCEPTED' || type === 'MEAL_MATCH_CANCELLED') return 'MainTabs';
  return 'Mates';
}

/** 알림 아이콘(이모지) — 같이먹기 🍚 / 메이트 🤝. */
export function notificationEmoji(type: NotificationType): string {
  return type.startsWith('MEAL_') ? '🍚' : '🤝';
}
