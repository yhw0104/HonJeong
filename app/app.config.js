// Expo 앱 설정. app.json에서 이전했다 — 카카오 네이티브 앱 키를 .env에서 읽어야 해서
// 정적 JSON이 아닌 동적 설정이 필요하다(Expo CLI가 .env를 자동 로드한다).
const KAKAO_NATIVE_APP_KEY = process.env.EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY;

if (!KAKAO_NATIVE_APP_KEY) {
  // 빌드 시점에 조용히 잘못 설정되는 것을 막는다(카카오 로그인이 런타임에야 깨지는 상황 방지).
  throw new Error(
    'EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY가 없습니다. app/.env에 카카오 네이티브 앱 키를 넣어주세요.',
  );
}

// EXPO_PUBLIC_API_BASE_URL은 로컬 개발에서는 비워둬도 된다 — app/src/shared/config/api.ts가
// http://localhost:8080으로 폴백하고, 시뮬레이터에서는 그게 맞는 값이다. 하지만 EAS production
// 빌드(EAS_BUILD_PROFILE은 EAS Build가 자동으로 주입한다 — eas.json의 프로파일 이름)에서
// 이 값을 빠뜨리면 그 로컬용 폴백이 그대로 실려서, "빌드는 성공하지만 실기기에서 localhost를
// 호출하는" 앱이 나온다 — eas build → eas submit → TestFlight 처리까지 다 끝난 뒤에야
// 드러나는 실패라 진단 비용이 크다. 그래서 이 경우에만 카카오 키와 같은 방식으로 빌드 시점에 막는다.
if (!process.env.EXPO_PUBLIC_API_BASE_URL && process.env.EAS_BUILD_PROFILE === 'production') {
  throw new Error(
    'EXPO_PUBLIC_API_BASE_URL이 없습니다. EAS production 빌드에는 배포 서버 주소가 필요합니다 — ' +
      'app/eas.json 등록 안내대로 eas env:set --name EXPO_PUBLIC_API_BASE_URL --value https://<도메인> ' +
      '--environment production --visibility plaintext 를 실행해주세요.',
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
      config: {
        // 표준 HTTPS/TLS 외의 자체 암호화를 쓰지 않으므로 수출 규제 예외 대상이 아니라고 고정
        // 응답한다. 없으면 TestFlight에 올릴 때마다 App Store Connect의 "Export Compliance"
        // 질문에 손으로 답해야 하고, 답하기 전까지 빌드가 "Missing Compliance" 상태로 걸려
        // 테스터가 설치할 수 없다 — 배포 런북 단계 3 참고.
        usesNonExemptEncryption: false,
      },
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
