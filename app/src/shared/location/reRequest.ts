// 앱이 포그라운드로 돌아왔고 아직 GPS를 못 받았으면 위치를 다시 시도한다
// (설정에서 권한을 켜고 돌아온 사용자를 자동으로 반영).
import type { AppStateStatus } from 'react-native';
import type { LocationSource } from './pickLocation';

export function shouldReRequestLocation(source: LocationSource, nextAppState: AppStateStatus): boolean {
  return nextAppState === 'active' && source !== 'gps';
}
