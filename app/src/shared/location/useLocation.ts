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

  // watch: 권한이 있으면 이동할 때마다 gps를 갱신. 언마운트·watch 해제 시 구독을 정리한다.
  useEffect(() => {
    if (!watch || permission !== 'granted') return;
    let sub: Location.LocationSubscription | null = null;
    let cancelled = false;
    (async () => {
      try {
        const s = await Location.watchPositionAsync(
          // ★"파란 점이 나를 따라오는" 체감은 이 두 값이 정한다. iOS에서 accuracy는
          //   CLLocationManager.desiredAccuracy로, distanceInterval은 distanceFilter로 그대로 내려간다.
          //   · Highest = kCLLocationAccuracyBest. 앞서 High(오차 ~10m)로도 마커가 거의 안 움직였다 —
          //     걷는 속도의 이동은 10m 정확도로는 잡히지 않는다. BestForNavigation은 여기서 한 단계 더
          //     나아가 추가 센서까지 켜는 내비게이션 전용이라 배터리 소모가 커서 쓰지 않는다.
          //   · distanceInterval 0 = 필터 없음(모든 이동 보고). 5m로 두면 "5m를 움직였다고 판정"되기
          //     전까지 콜백이 아예 오지 않아, 실내·저속에서는 몇 분씩 갱신이 끊긴다. expo도 기본값이 0이다.
          //   timeInterval은 안드로이드 전용이라 iOS에 효과가 없어 넣지 않는다(SDK 56 문서).
          { accuracy: Location.Accuracy.Highest, distanceInterval: 0 },
          (pos) => setGps({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
          // 구독이 시작된 뒤 끊기는 경우(권한 회수 등)는 catch로 안 잡힌다 — 조용히 멈추지 않게 여기서 받는다.
          (err) => console.warn('[useLocation] watch 중단', err),
        );
        if (cancelled) s.remove();
        else sub = s;
      } catch (e) {
        // 워처 시작 실패(시뮬레이터 등) — 1회 취득 값으로 동작.
        console.warn('[useLocation] watch 시작 실패', e);
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
