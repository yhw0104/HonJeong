import { useEffect, useState } from 'react';
import * as Location from 'expo-location';
import { useMyProfile } from '@/features/users/queries';
import { pickLocation, type Coord, type LocationSource } from './pickLocation';

type Permission = 'granted' | 'denied' | 'undetermined';

/**
 * 위치를 한 곳에서 처리: GPS 1회 취득 → 실패/거부 시 저장된 내 동네 → 연남동 기본.
 * 화면은 coord/source/permission만 쓴다. requestAgain으로 권한을 다시 요청한다.
 */
export function useLocation(): {
  coord: Coord;
  source: LocationSource;
  permission: Permission;
  requestAgain: () => void;
} {
  const [gps, setGps] = useState<Coord | null>(null);
  const [permission, setPermission] = useState<Permission>('undetermined');
  const profile = useMyProfile();

  const region: Coord | null =
    profile.data?.regionLat != null && profile.data?.regionLng != null
      ? { lat: profile.data.regionLat, lng: profile.data.regionLng }
      : null;

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

  const { coord, source } = pickLocation({ gps, region });
  return { coord, source, permission, requestAgain: load };
}
