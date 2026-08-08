// 인앱 배너 — 앱을 보고 있을 때 도착한 푸시를 앱 안에서 직접 띄운다.
//
// 왜 OS 배너를 안 쓰는가: iOS는 JS가 개입하기 전에 배너를 띄울지 정한다. 그래서
// "지금 그 대화방을 보고 있으면 띄우지 않는다" 같은 규칙을 표현할 방법이 없다 —
// A가 B와의 대화방을 보고 있는데 B의 메시지 배너가 그 화면 위에 뜬다. 메신저들이 전부
// 그 경우 안 띄우는 이유이기도 하다. 그래서 포그라운드만 우리가 그린다
// (firebase.json은 포그라운드 표시를 끄고, 백그라운드·종료 상태는 그대로 OS 배너다).
//
// 이 파일은 @/shared/push(index.ts)를 import하지 않는다 — index.ts는 @react-native-firebase를
// import 시점에 조회해서 jest에서 즉사한다(prompt.ts·target.ts·installation.ts와 같은 이유).
// 판단 로직을 여기 순수 함수로 두는 것도 그래서다.
import type { IconName } from '@/shared/components';

import type { PushData } from './target';

/** 배너에 그릴 내용. title/body는 서버가 notification 페이로드에 실어 보낸 값이다. */
export type PushNotice = { data: PushData; title: string | null; body: string | null };

/** 화면 이름과 파라미터만 있으면 판단할 수 있다 — 네비게이션 객체 전체를 끌어오지 않는다. */
export type CurrentRoute = { name: string; params?: Record<string, unknown> } | null;

/**
 * 이 푸시를 인앱 배너로 띄울 것인가. (순수)
 *
 * 규칙은 하나다: **지금 보고 있는 대화방의 메시지는 띄우지 않는다.** 이미 그 대화를 보고 있으니
 * 배너는 알려줄 것이 없고, 읽고 있는 화면을 가리기만 한다.
 *
 * 판단이 서지 않으면 띄우는 쪽으로 기운다(현재 화면을 모르거나 대화방 id가 없는 경우).
 * 놓친 알림은 사용자가 영영 모르지만, 잘못 뜬 배너는 잠깐 거슬리고 만다.
 *
 * @param data  푸시 data(서버가 보낸 문자열 맵)
 * @param route 지금 보고 있는 화면. 모르면 null
 * @returns 띄워야 하면 true
 */
export function shouldShowBanner(data: PushData, route: CurrentRoute): boolean {
  if (data.type !== 'CHAT_MESSAGE') return true;
  if (!route || route.name !== 'ChatRoom') return true;
  const viewing = route.params?.conversationId;
  if (viewing == null || data.conversationId == null) return true;
  return String(viewing) !== String(data.conversationId);
}

/**
 * 배너 왼쪽 아이콘. 알림함 아이콘 규칙과 맞춘다 — 같은 사건이 알림함과 배너에서 달라 보이면 안 된다.
 *
 * @param type 푸시 종류
 * @returns 아이콘 이름
 */
export function bannerIcon(type: string): IconName {
  if (type === 'CHAT_MESSAGE') return 'chat';
  if (type === 'BADGE_EARNED') return 'badge';
  return type.startsWith('MEAL_') ? 'rice' : 'friends';
}

// ── 표시 창구 ───────────────────────────────────────────────────────────────
// 푸시 리스너(usePushMessaging)와 배너 UI(PushBanner)는 서로 다른 자리에 산다.
// 리스너는 QueryClientProvider 안쪽의 작은 컴포넌트에 있고, 배너는 화면 위에 그려져야 한다.
// 그 사이를 잇는 가장 얇은 방법으로 모듈 스코프 함수 하나를 둔다(navigationRef와 같은 방식).

type Presenter = (notice: PushNotice) => void;

let presenter: Presenter | null = null;

/** 배너 UI가 마운트되며 자신을 등록한다. 언마운트 시 null로 해제한다. */
export function setBannerPresenter(fn: Presenter | null): void {
  presenter = fn;
}

/** 배너를 띄운다. UI가 아직 없으면 조용히 무시된다(앱 기동 직후 짧은 구간). */
export function presentBanner(notice: PushNotice): void {
  presenter?.(notice);
}
