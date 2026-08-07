// 푸시 → 무효화할 React Query 키(순수 함수).
//
// 이 파일도 shared/push/index.ts를 import하지 않는다(target.ts·prompt.ts와 같은 이유).
//
// 실시간 전략 §5: 푸시를 알림 표시로만 쓰지 말고 "새로고침 신호"로도 쓴다.
// 이러면 WebSocket 없이도 상태바 실시간성이 해결된다.
//
// 폴링은 그대로 둔다 — 푸시는 배달 보장이 없으므로 유일한 갱신 수단이 되면 안 된다(§8).

/**
 * 이 종류의 푸시를 받았을 때 버릴 캐시 키 목록.
 *
 * 키는 prefix로 매칭된다 — ['checkin']은 ['checkin','me']와 ['checkin','stats']를 함께 버린다.
 */
export function pushInvalidationKeys(type: string): string[][] {
  // 채팅은 예외다 — PushType.CHAT_MESSAGE는 notifications 테이블에 저장되지 않으므로
  // (알림함에 안 쌓기로 한 결정) 알림함을 다시 불러와도 새 행이 없다. 헛된 요청만 는다.
  if (type === 'CHAT_MESSAGE') return [['chat']];

  const keys: string[][] = [['notifications']];
  switch (type) {
    case 'MEAL_REQUEST_RECEIVED':
      keys.push(['meal']);
      break;
    case 'MEAL_REQUEST_ACCEPTED':
    case 'MEAL_MATCH_CANCELLED':
      // 상대 행동으로 내 체크인이 통째로 바뀐다(checkInId까지) — 07-31 사고 참조.
      // checkin/queries.ts의 invalidateCheckInLoop와 같은 범위 + 대화방 생성 반영(['chat']).
      keys.push(['meal'], ['checkin'], ['map'], ['nearby'], ['place'], ['chat']);
      break;
    case 'MATE_REQUEST_RECEIVED':
    case 'MATE_REQUEST_ACCEPTED':
      keys.push(['mate']);
      break;
    case 'BADGE_EARNED':
      // 실제 쿼리 키는 ['users','me','badges']다(record/queries.ts의 useBadges).
      // ['badges']로 두면 아무것도 매칭되지 않아 조용히 무효화가 안 된다.
      keys.push(['users', 'me', 'badges']);
      break;
    default:
      break; // 모르는 종류 — 알림함만 갱신한다
  }
  return keys;
}
