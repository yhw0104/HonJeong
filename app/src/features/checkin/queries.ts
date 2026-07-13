import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert } from 'react-native';
import type { Coord } from '@/shared/location/pickLocation';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import {
  fetchMyCheckIn, startCheckIn, endCheckIn, cancelCheckIn, dineAlone, fetchStats, fetchMap, fetchSeekers,
  leaveMatch, type LeaveMatchTo,
} from './api';
import { startCheckInWithRecovery } from './recovery';

export function useMyCheckIn() {
  return useQuery({ queryKey: ['checkin', 'me'], queryFn: fetchMyCheckIn });
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
function invalidateLoop(qc: ReturnType<typeof useQueryClient>) {
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
    onSuccess: () => invalidateLoop(qc),
    onError: () => Alert.alert('잠깐요', '이미 다른 곳에서 모집/혼밥 중이에요. 먼저 끝내고 다시 시도해 주세요.'),
  });
}

export function useEndCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checkInId: number) => endCheckIn(checkInId),
    onSuccess: () => invalidateLoop(qc),
  });
}

export function useCancelCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checkInId: number) => cancelCheckIn(checkInId),
    onSuccess: () => invalidateLoop(qc),
  });
}

export function useDineAlone() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checkInId: number) => dineAlone(checkInId),
    onSuccess: () => invalidateLoop(qc),
  });
}

// 같이먹기 매칭 깨기(노쇼/취소 처리) — 내 상태를 to로, 상대는 서버가 SEEKING 복귀+알림.
export function useLeaveMatch() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ checkInId, to }: { checkInId: number; to: LeaveMatchTo }) => leaveMatch(checkInId, to),
    onSuccess: () => invalidateLoop(qc),
  });
}
