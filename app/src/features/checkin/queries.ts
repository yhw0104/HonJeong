import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type { Coord } from '@/shared/location/pickLocation';
import {
  fetchMyCheckIn, startCheckIn, endCheckIn, fetchStats, fetchMap, fetchActiveDiners,
} from './api';
import { startCheckInWithRecovery } from './recovery';

export function useMyCheckIn() {
  return useQuery({ queryKey: ['checkin', 'me'], queryFn: fetchMyCheckIn });
}
export function useStats() {
  return useQuery({ queryKey: ['checkin', 'stats'], queryFn: fetchStats });
}
export function useMap(coord: Coord, radius = 1000) {
  return useQuery({
    queryKey: ['map', { lat: coord.lat, lng: coord.lng, radius }],
    queryFn: () => fetchMap(coord.lat, coord.lng, radius),
  });
}
export function useActiveDiners(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'diners'],
    queryFn: () => fetchActiveDiners(placeId),
  });
}

// 체크인 시작/종료 후 지도·주변·내체크인·혼밥러목록·통계를 모두 무효화한다(전 화면 자동 갱신).
function invalidateLoop(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['checkin', 'me'] });
  qc.invalidateQueries({ queryKey: ['map'] });
  qc.invalidateQueries({ queryKey: ['nearby'] });
  qc.invalidateQueries({ queryKey: ['place'] });
  qc.invalidateQueries({ queryKey: ['checkin', 'stats'] });
}

export function useStartCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (placeId: number) =>
      startCheckInWithRecovery(placeId, { start: startCheckIn, getMine: fetchMyCheckIn, end: endCheckIn }),
    onSuccess: () => invalidateLoop(qc),
  });
}

export function useEndCheckIn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checkInId: number) => endCheckIn(checkInId),
    onSuccess: () => invalidateLoop(qc),
  });
}
