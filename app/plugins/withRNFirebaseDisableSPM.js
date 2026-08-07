const { withDangerousMod } = require('expo/config-plugins');
const fs = require('fs');
const path = require('path');

const FLAG = '$RNFirebaseDisableSPM = true';

// SPM을 끄면 Firebase가 CocoaPods로 들어오는데, Swift로 작성된 Firebase 팟(FirebaseCoreInternal 등)이
// 정적 라이브러리로 빌드되려면 의존 팟이 모듈맵을 제공해야 한다. 아래 셋은 C/ObjC로 작성돼 모듈맵이
// 없으므로 개별적으로 켜 준다. 전역 `use_modular_headers!`를 쓰지 않는 이유는 그것이 앱의 모든
// 팟(카카오·svg·webview 등)의 헤더 처리 방식을 함께 바꾸기 때문이다 — 필요한 만큼만 건드린다.
const MODULAR_HEADER_PODS = ['GoogleUtilities', 'nanopb', 'GoogleDataTransport'];
const ANCHOR = 'config = use_native_modules!(config_command)';

/**
 * Podfile 맨 위에 `$RNFirebaseDisableSPM = true`를 넣는다.
 *
 * 왜 필요한가: react-native-firebase 26은 Firebase iOS SDK를 SPM(Swift Package Manager)으로
 * 가져오는데, Firebase의 Swift Package는 동적 라이브러리만 제공한다. 반면 이 앱은 CocoaPods
 * 기본값인 정적 링크로 돌고 있어서 `pod install`이 거부한다.
 *
 * 해결책은 둘인데 영향 범위가 크게 다르다:
 *   1. use_frameworks! :linkage => :dynamic  → 앱의 *모든* 네이티브 모듈의 링크 방식이 바뀐다
 *      (카카오 SDK·reanimated·gesture-handler·svg·webview). 2026-08-01에 reanimated 도입이
 *      dyld 심볼 오류로 앱을 즉사시킨 전례가 있어 피한다.
 *   2. $RNFirebaseDisableSPM = true → Firebase만 기존 CocoaPods 경로로 받는다. 나머지는 무변경.
 * 2번을 택했다.
 *
 * 이 플래그는 Podfile에만 넣을 수 있고(환경변수나 플러그인 옵션이 없다 —
 * node_modules/@react-native-firebase/app/firebase_spm.rb 참고), `ios/`는 git에 없어
 * prebuild 때마다 새로 생성되므로 config plugin으로 매번 다시 넣어야 한다.
 */
module.exports = function withRNFirebaseDisableSPM(config) {
  return withDangerousMod(config, [
    'ios',
    (cfg) => {
      const podfile = path.join(cfg.modRequest.platformProjectRoot, 'Podfile');
      let contents = fs.readFileSync(podfile, 'utf8');

      // target 블록보다 먼저 선언돼야 하므로 맨 앞에 넣는다. 이미 있으면 건드리지 않는다.
      if (!contents.includes(FLAG)) {
        contents = `${FLAG}\n\n${contents}`;
      }

      // 모듈맵 지정은 target 블록 *안*이어야 한다. 오토링킹 직후에 끼워 넣는다.
      if (!contents.includes(':modular_headers => true')) {
        if (!contents.includes(ANCHOR)) {
          throw new Error(
            `[withRNFirebaseDisableSPM] Podfile에서 기준점을 찾지 못했습니다: "${ANCHOR}". ` +
              'Expo 템플릿이 바뀐 것이므로 이 플러그인을 갱신해야 합니다.',
          );
        }
        const lines = MODULAR_HEADER_PODS.map(
          (name) => `  pod '${name}', :modular_headers => true`,
        ).join('\n');
        contents = contents.replace(ANCHOR, `${ANCHOR}\n\n${lines}`);
      }

      fs.writeFileSync(podfile, contents);
      return cfg;
    },
  ]);
};
