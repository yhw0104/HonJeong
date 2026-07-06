import { useEffect, useMemo, useState } from 'react';
import * as Location from 'expo-location';
import { useMyProfile } from '@/features/users/queries';
import { pickLocation, type Coord, type LocationSource } from './pickLocation';

type Permission = 'granted' | 'denied' | 'undetermined';

/**
 * 위치를 한 곳에서 처리: GPS 1회 취득 → 실패/거부 시 저장된 내 동네 → 연남동 기본.
 * 화면은 coord/source/permission만 쓴다. requestAgain으로 권한을 다시 요청한다.
 * watch: true면 이동에 따라 coord를 실시간 갱신한다(25m 간격) — 홈 지도 전용.
 */
export function useLocation(options?: { watch?: boolean }): {
  coord: Coord;
  source: LocationSource;
  permission: Permission;
  requestAgain: () => void;
} {
  const watch = options?.watch ?? false;
  const [gps, setGps] = useState<Coord | null>(null);
  const [permission, setPermission] = useState<Permission>('undetermined');
  const profile = useMyProfile();

  const regionLat = profile.data?.regionLat;
  const regionLng = profile.data?.regionLng;

  const load = async () => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        setPermission('denied');
        setGps(null);
        return;
      }
      setPermission('granted');
      const pos = await Location.getCurrentPositionAsync({});
      setGps({ lat: pos.coords.latitude, lng: pos.coords.longitude });
    } catch {
      // 위치 취득 실패(시뮬레이터·실내 등) → GPS 없음으로 두고 폴백.
      setGps(null);
    }
  };

  useEffect(() => {
    load();
  }, []);

  // watch: 권한이 있으면 이동(25m)마다 gps를 갱신. 언마운트·watch 해제 시 구독을 정리한다.
  useEffect(() => {
    if (!watch || permission !== 'granted') return;
    let sub: Location.LocationSubscription | null = null;
    let cancelled = false;
    (async () => {
      try {
        const s = await Location.watchPositionAsync(
          { accuracy: Location.Accuracy.Balanced, distanceInterval: 25 },
          (pos) => setGps({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        );
        if (cancelled) s.remove();
        else sub = s;
      } catch {
        // 워처 시작 실패(시뮬레이터 등) — 1회 취득 값으로 동작.
      }
    })();
    return () => {
      cancelled = true;
      sub?.remove();
    };
  }, [watch, permission]);

  // coord 참조를 입력이 실제로 바뀔 때만 갱신 — 매 렌더 새 객체면 지도(center)가 불필요하게 반응한다.
  const { coord, source } = useMemo(
    () =>
      pickLocation({
        gps,
        region: regionLat != null && regionLng != null ? { lat: regionLat, lng: regionLng } : null,
      }),
    [gps, regionLat, regionLng],
  );
  return { coord, source, permission, requestAgain: load };
}
