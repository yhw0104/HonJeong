// Expo 앱 설정. app.json에서 이전했다 — 카카오 네이티브 앱 키를 .env에서 읽어야 해서
// 정적 JSON이 아닌 동적 설정이 필요하다(Expo CLI가 .env를 자동 로드한다).
const KAKAO_NATIVE_APP_KEY = process.env.EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY;

if (!KAKAO_NATIVE_APP_KEY) {
  // 빌드 시점에 조용히 잘못 설정되는 것을 막는다(카카오 로그인이 런타임에야 깨지는 상황 방지).
  throw new Error(
    'EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY가 없습니다. app/.env에 카카오 네이티브 앱 키를 넣어주세요.',
  );
}

module.exports = {
  expo: {
    name: '혼정',
    slug: 'honjeong',
    description: '혼밥을 정상화하다',
    version: '1.0.0',
    orientation: 'portrait',
    icon: './assets/icon.png',
    userInterfaceStyle: 'light',
    ios: {
      supportsTablet: true,
      bundleIdentifier: 'com.honjeong.app',
    },
    android: {
      package: 'com.honjeong.app',
      adaptiveIcon: {
        backgroundColor: '#F7EEDD',
        foregroundImage: './assets/android-icon-foreground.png',
      },
      predictiveBackGestureEnabled: false,
    },
    web: {
      favicon: './assets/favicon.png',
    },
    plugins: [
      'expo-secure-store',
      [
        '@react-native-kakao/core',
        {
          nativeAppKey: KAKAO_NATIVE_APP_KEY,
          android: { authCodeHandlerActivity: true },
          ios: { handleKakaoOpenUrl: true },
        },
      ],
    ],
  },
};
