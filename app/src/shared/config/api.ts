// 백엔드 API 베이스 URL 설정.
//
// 현재 타깃: iOS 시뮬레이터 → Mac의 localhost에 그대로 닿는다.
// 다른 환경에서 띄울 땐 이 값만 바꾸면 된다:
//   - Android 에뮬레이터: 'http://10.0.2.2:8080'
//   - 실제 폰 + Expo Go : 'http://<PC의 LAN IP>:8080' (예: http://192.168.0.5:8080, 같은 와이파이)
export const API_BASE_URL = 'http://localhost:8080';
