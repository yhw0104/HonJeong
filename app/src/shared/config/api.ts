// 베이스 URL 우선순위: 빌드 시 주입된 EXPO_PUBLIC_API_BASE_URL(있으면) → 없으면 로컬 개발 기본값.
// 배포/실기기: 프로젝트 루트 .env(또는 EAS env)에 EXPO_PUBLIC_API_BASE_URL 지정.
//   - iOS 시뮬레이터    : 기본값 http://localhost:8080 그대로
//   - Android 에뮬레이터 : EXPO_PUBLIC_API_BASE_URL=http://10.0.2.2:8080
//   - 실제 폰 + Expo Go  : EXPO_PUBLIC_API_BASE_URL=http://<PC LAN IP>:8080 (같은 와이파이)
export const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
