import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert } from 'react-native';
import type { Coord } from '@/shared/location/pickLocation';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import {
  fetchMyCheckIn, startCheckIn, endCheckIn, cancelCheckIn, dineAlone, fetchStats, fetchMap, fetchSeekers,
  leaveMatch, type LeaveMatchTo,
} from './api';
import { startCheckInWithRecovery } from './recovery';
import { startErrorCopy } from './startErrorCopy';

// 내 체크인도 남의 행동으로 바뀐다 — 상대가 내 신청을 수락하면 서버가 기존 체크인을 취소(SEEKING)
// 또는 종료(ACTIVE)하고 새 TOGETHER를 insert한다(MealRequestService.accept). checkInId까지 바뀌므로
// 갱신이 없으면 상태바가 죽은 id로 종료/그만두기를 호출한다 → 라이브 폴링.
export function useMyCheckIn() {
  return useQuery({ queryKey: ['checkin', 'me'], queryFn: fetchMyCheckIn, refetchInterval: LIVE_REFETCH_MS });
}
// 아래 3개는 다른 사용자의 실시간 혼밥 현황 → refetchInterval로 주기 폴링한다.
export function useStats() {
  return useQuery({ queryKey: ['checkin', 'stats'], queryFn: fetchStats, refetchInterval: LIVE_REFETCH_MS });
}
export function useMap(coord: Coord, radius = 1000) {
  return useQuery({
    queryKey: ['map', { lat: coord.lat, lng: coord.lng, radius }],
    queryFn: () => fetchMap(coord.lat, coord.lng, radius),
    refetchInterval: LIVE_REFETCH_MS,
  });
}
export function useSeekers(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'seekers'],
    queryFn: () => fetchSeekers(placeId),
    refetchInterval: LIVE_REFETCH_MS,
  });
}

// 체크인 시작/종료/취소 후 지도·주변·내체크인·혼밥러목록·통계를 모두 무효화한다(전 화면 자동 갱신).
// 같이먹기 수락도 체크인을 전이시키므로 meal 쪽에서 재사용한다.
export function invalidateCheckInLoop(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['checkin', 'me'] });
  qc.invalidateQueries({ queryKey: ['map'] });
  qc.invalidateQueries({ queryKey: ['nearby'] });
  qc.invalidateQueries({ queryKey: ['place'] });
  qc.invalidateQueries({ queryKey: ['checkin', 'stats'] });
  qc.invalidateQueries({ queryKey: ['meal'] }); // 받은/보낸 같이먹기 신청(매칭·정리 반영)
}

export function useStartCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (placeId: number) =>
      startCheckInWithRecovery(placeId, { start: startCheckIn, getMine: fetchMyCheckIn, end: endCheckIn }),
    onSuccess: () => invalidateCheckInLoop(qc),
    onError: (e) => {
      // 진짜 충돌(이미 모집/혼밥 중)과 네트워크 실패 등을 구분해 안내(오인 문구 방지).
      const { title, message } = startErrorCopy(e);
      Alert.alert(title, message);
    },
  });
}

export function useEndCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checkInId: number) => endCheckIn(checkInId),
    onSuccess: () => invalidateCheckInLoop(qc),
    onError: () => Alert.alert('앗', '종료하지 못했어요. 잠시 후 다시 시도해 주세요.'),
  });
}

export function useCancelCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checkInId: number) => cancelCheckIn(checkInId),
    onSuccess: () => invalidateCheckInLoop(qc),
    onError: () => Alert.alert('앗', '취소하지 못했어요. 잠시 후 다시 시도해 주세요.'),
  });
}

export function useDineAlone() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checkInId: number) => dineAlone(checkInId),
    onSuccess: () => invalidateCheckInLoop(qc),
    onError: () => Alert.alert('앗', '종료하지 못했어요. 잠시 후 다시 시도해 주세요.'),
  });
}

// 같이먹기 매칭 깨기(노쇼/취소 처리) — 내 상태를 to로, 상대는 서버가 SEEKING 복귀+알림.
export function useLeaveMatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ checkInId, to }: { checkInId: number; to: LeaveMatchTo }) => leaveMatch(checkInId, to),
    onSuccess: () => invalidateCheckInLoop(qc),
    onError: () => Alert.alert('앗', '처리하지 못했어요. 잠시 후 다시 시도해 주세요.'),
  });
}
