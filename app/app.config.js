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

// 푸시 환경. EAS_BUILD_PROFILE은 EAS Build가 자동으로 주입하므로(eas.json의 프로파일 이름),
// 로컬 `expo run:ios`에서는 비어 있어 development가 된다 — 그게 정확히 우리가 원하는 값이다.
// APNs 인증 키는 Sandbox·Production 양쪽으로 발급해 뒀으므로 어느 쪽이든 배달된다.
const APS_ENVIRONMENT =
  process.env.EAS_BUILD_PROFILE === 'production' ? 'production' : 'development';

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
      // Firebase가 APNs로 푸시를 중계하려면 이 파일이 네이티브 프로젝트에 들어가야 한다.
      // 저장소에는 없다(.gitignore) — Firebase 콘솔에서 내려받아 이 경로에 둔다.
      //
      // ★ EAS 빌드에서는 로컬 파일을 쓸 수 없다. EAS Build는 **git이 추적하는 파일만** 업로드하는데
      // 이 파일은 .gitignore에 있어서 빌드 머신에 아예 존재하지 않는다("GoogleService-Info.plist"
      // is missing로 빌드가 죽는다 — 2026-08-07에 실제로 겪었다). 로컬 빌드는 파일이 디스크에
      // 있으니 그냥 되기 때문에 **EAS에서만 드러나는 종류**다.
      // 그래서 EAS에는 file 타입 환경변수로 올려 두고(`eas env:set --type file`), 빌드 머신이
      // 풀어 준 경로를 여기서 읽는다. 로컬(변수 없음)에서는 기존 경로로 폴백한다.
      googleServicesFile: process.env.GOOGLE_SERVICES_INFO_PLIST ?? './GoogleService-Info.plist',
      entitlements: {
        // 원격 푸시 수신 권한. Expo SDK 51+에서는 직접 명시해야 한다.
        // ★ 값이 빌드 종류에 따라 갈려야 한다 — 애플은 푸시 서버를 개발용(sandbox)과
        // 배포용(production) 둘로 나눠 운영하고, 기기 토큰도 그에 따라 다르게 발급된다.
        // 로컬 dev 빌드는 개발용 프로비저닝 프로파일로 서명되므로 development여야 하고
        // (production으로 고정하면 서명이 거부되거나 푸시가 조용히 안 온다),
        // EAS production 빌드는 배포용이라 production이어야 한다.
        'aps-environment': APS_ENVIRONMENT,
      },
      infoPlist: {
        // 앱이 백그라운드일 때도 푸시를 받으려면 필요하다.
        UIBackgroundModes: ['remote-notification'],
      },
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
      // ★권한 문구는 여기가 유일한 출처다.
      //
      // 플러그인을 등록하지 않으면 각 모듈이 **Expo 기본 영문 문구**("Allow $(PRODUCT_NAME) to
      // access your location")를 Info.plist에 넣는다. 한국어 앱인데 시스템 권한 창이 영어로 뜬다.
      //
      // ★안 쓰는 권한은 `false`로 지운다. Expo의 IOSConfig.Permissions가 false를 받으면 해당 키를
      // Info.plist에서 **삭제**한다(applyPermissions의 `delete infoPlist[permission]`). 선언만 해두면
      // 애플이 심사에서 "쓰지도 않는데 왜 요구하느냐"를 묻는다 — 특히 '항상 허용' 위치는 집중 대상이다.
      // 이 앱이 실제로 쓰는 권한은 **앱 사용 중 위치**와 **사진 보관함** 둘뿐이다
      // (useLocation의 requestForegroundPermissionsAsync, imageUpload의 launchImageLibraryAsync).
      [
        'expo-location',
        {
          locationWhenInUsePermission:
            '지금 계신 곳 주변의 식당과 혼밥 중인 이웃을 보여드리기 위해 사용합니다. 앱을 사용하는 동안에만 확인하고, 이동 경로는 저장하지 않습니다.',
          // 백그라운드 위치를 쓰지 않는다 — 워처(watchPositionAsync)도 앱이 떠 있을 때만 돈다.
          locationAlwaysAndWhenInUsePermission: false,
          locationAlwaysPermission: false,
          // 모션 센서는 쓰지 않는다(위치 플러그인이 기본으로 끼워 넣을 뿐이다).
          motionUsagePermission: false,
        },
      ],
      [
        'expo-image-picker',
        {
          photosPermission:
            '리뷰와 프로필에 넣을 사진을 직접 고르실 때만 사진 보관함을 엽니다.',
          // 카메라·마이크는 쓰지 않는다 — 사진은 보관함에서 고르기만 한다(launchCameraAsync 미사용).
          cameraPermission: false,
          microphonePermission: false,
        },
      ],
      // Face ID는 쓰지 않는다 — SecureStore를 토큰 보관에만 쓰고 requireAuthentication을 켜지 않는다.
      ['expo-secure-store', { faceIDPermission: false }],
      [
        '@react-native-kakao/core',
        {
          nativeAppKey: KAKAO_NATIVE_APP_KEY,
          android: { authCodeHandlerActivity: true },
          ios: { handleKakaoOpenUrl: true },
        },
      ],
      '@react-native-firebase/app',
      '@react-native-firebase/messaging',
      // Firebase만 CocoaPods로 받게 해서 앱 전체의 링크 방식을 건드리지 않는다.
      // 이유는 플러그인 파일 주석 참고(요약: SPM은 동적 링크만 지원하는데 이 앱은 정적 링크다).
      './plugins/withRNFirebaseDisableSPM',
    ],
    // EAS 프로젝트 식별자. `eas init`이 발급했지만 **이 파일이 동적 설정(app.config.js)이라
    // CLI가 자동으로 써넣지 못해**("Cannot automatically write to dynamic config") 손으로 넣었다.
    // 지우면 eas build/submit이 어느 프로젝트인지 몰라 실패한다.
    extra: {
      eas: { projectId: 'eca89e34-6fe0-4dda-8748-8661c9f5f79e' },
    },
  },
};
