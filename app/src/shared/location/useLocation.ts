import { useEffect, useMemo, useState } from 'react';
import { AppState } from 'react-native';
import * as Location from 'expo-location';
import { useMyProfile } from '@/features/users/queries';
import { pickLocation, type Coord, type LocationSource } from './pickLocation';
import { shouldReRequestLocation } from './reRequest';

type Permission = 'granted' | 'denied' | 'undetermined';

/**
 * 위치를 한 곳에서 처리: GPS 1회 취득 → 실패/거부 시 저장된 내 동네 → 연남동 기본.
 * 화면은 coord/source/permission만 쓴다. requestAgain으로 권한을 다시 요청한다.
 * watch: true면 이동에 따라 coord를 실시간 갱신한다(5m 간격) — 홈 지도 전용.
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

  // watch: 권한이 있으면 이동(5m)마다 gps를 갱신. 언마운트·watch 해제 시 구독을 정리한다.
  useEffect(() => {
    if (!watch || permission !== 'granted') return;
    let sub: Location.LocationSubscription | null = null;
    let cancelled = false;
    (async () => {
      try {
        const s = await Location.watchPositionAsync(
          // ★두 값이 함께 "파란 점이 나를 따라오는" 체감을 만든다. 이전 값(Balanced·25m)은
          //   정확도가 ~100m라 25m를 걸어도 그 차이를 못 잡아, 실기기에서 마커가 거의 안 움직였다.
          //   High는 오차 ~10m라 짧은 이동도 잡힌다. BestForNavigation은 추가 센서까지 써서
          //   배터리 소모가 큰 내비게이션용이라, 걸어다니며 주변을 보는 이 앱엔 High가 적정선이다.
          //   timeInterval은 안드로이드 전용이라 iOS에 효과가 없어 넣지 않는다(SDK 56 문서).
          { accuracy: Location.Accuracy.High, distanceInterval: 5 },
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

  // 포그라운드 복귀 시, 아직 GPS를 못 받았으면 다시 시도(설정에서 권한 켜고 돌아온 경우 자동 반영).
  useEffect(() => {
    const sub = AppState.addEventListener('change', (next) => {
      if (shouldReRequestLocation(source, next)) load();
    });
    return () => sub.remove();
  }, [source]);

  return { coord, source, permission, requestAgain: load };
}
