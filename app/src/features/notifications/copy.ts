// 알림 문구·이동 매핑 — 서버는 type+actorNickname만 주고 문구는 여기서 조립한다(순수 함수, TDD 대상).
//
// ★ 알려진 중복: 같은 사건의 문구가 두 곳에 산다. 여기(알림함)와
// backend/src/main/java/com/honjeong/push/service/PushMessages.java(푸시 배너)다.
// 배너는 앱이 아니라 OS가 그리므로 서버가 완성된 문장을 내려보내야 한다 — 제거할 수 없는 중복이다.
// 그래서 문구를 고칠 때는 두 파일을 같은 커밋에서 고친다. 한쪽만 고치면 알림함과 배너가 다른 말을 한다.
import type { IconName } from '@/shared/components';
import type { NotificationType } from './api';

const MESSAGE: Record<NotificationType, string> = {
  MEAL_REQUEST_RECEIVED: '님이 같이 먹기를 신청했어요',
  MEAL_REQUEST_ACCEPTED: '님이 같이 먹기를 수락했어요',
  MEAL_MATCH_CANCELLED: '님이 같이 먹기 약속을 취소했어요',
  MATE_REQUEST_RECEIVED: '님이 메이트를 신청했어요',
  MATE_REQUEST_ACCEPTED: '님이 메이트를 수락했어요',
  BADGE_EARNED: '새 뱃지를 획득했어요 🎉',
};

export function notificationMessage(type: NotificationType, actorNickname: string | null): string {
  if (type === 'BADGE_EARNED') return MESSAGE[type]; // actor 없는 자기 획득 — 닉네임 접두 없음
  return `${actorNickname ?? '누군가'}${MESSAGE[type]}`;
}

/** 알림 탭 시 이동할 화면. 같이먹기 수락은 '같이 먹는 중' 상태가 보이는 홈 지도로. */
export function notificationTarget(
  type: NotificationType,
): 'ReceivedRequests' | 'MainTabs' | 'Mates' | 'ChallengeBadges' {
  if (type === 'MEAL_REQUEST_RECEIVED') return 'ReceivedRequests';
  if (type === 'MEAL_REQUEST_ACCEPTED' || type === 'MEAL_MATCH_CANCELLED') return 'MainTabs';
  if (type === 'BADGE_EARNED') return 'ChallengeBadges';
  return 'Mates';
}

/** 알림 아이콘 — 같이먹기 밥(rice) / 메이트 친구(friends) / 뱃지 획득(badge). */
export function notificationIcon(type: NotificationType): IconName {
  if (type === 'BADGE_EARNED') return 'badge';
  return type.startsWith('MEAL_') ? 'rice' : 'friends';
}
