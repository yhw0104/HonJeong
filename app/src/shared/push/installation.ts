// 이 기기(앱 설치)를 가리키는 불투명 식별자. 서버가 "같은 기기의 옛 토큰"을 지목하는 데 쓴다.
//
// 왜 필요한가: 로그아웃은 ①서버 삭제 ②FCM 폐기 두 단계로 토큰을 끊는데, 둘 다 실패하면 그 토큰은
// FCM에 살아 있고 서버 DB에도 남아 있는데 기기에는 없다 — 다시는 그 값을 지목할 수 없다. 그 폰을
// 넘겨받은 사람의 잠금화면에 이전 사용자의 알림이 계속 뜬다. 설치 ID는 토큰이 갱신돼도 그대로라
// 그 고아 행을 지목할 수 있고, 다음에 누가 로그인하든 등록 시점에 정리된다.
//
// 이 파일은 @/shared/push(index.ts)를 import하지 않는다 — index.ts는 @react-native-firebase를
// import 시점에 조회해서 jest에서 즉사한다(prompt.ts·target.ts와 같은 이유).
import * as SecureStore from 'expo-secure-store';

const INSTALLATION_KEY = 'push.installationId';

/** 서버 컬럼이 VARCHAR(64)다. 아래 생성 규칙은 이보다 훨씬 짧지만 상한을 명시해 둔다. */
export const MAX_INSTALLATION_ID_LENGTH = 64;

/**
 * 새 설치 ID를 만든다. (순수 — 난수원과 시각을 주입받는다)
 *
 * ★ **추측 가능하면 안 된다.** 남의 설치 ID를 아는 사람이 그것을 자기 등록에 실어 보내면
 * 서버가 그 기기의 토큰 행을 지운다 — 상대의 알림을 끊을 수 있다. 그래서 시각만으로 만들지 않고
 * 난수 조각을 여러 개 이어 붙인다(조각 6개 × 약 52비트 중 하위 일부 → 충분히 넓다).
 *
 * 시각을 섞는 이유는 추측을 막기 위해서가 아니라, 난수원이 어떤 이유로든 같은 값을 내는
 * 두 기기가 있어도 갈라지게 하기 위해서다.
 *
 * @param random 0 이상 1 미만 난수를 주는 함수(런타임에서는 Math.random)
 * @param now    현재 시각(ms)
 * @returns 소문자 영숫자로만 이루어진 식별자
 */
export function newInstallationId(random: () => number, now: number): string {
  const chunk = () => Math.floor(random() * 0xffffffff).toString(36);
  return [now.toString(36), chunk(), chunk(), chunk(), chunk(), chunk(), chunk()].join('');
}

// 앱이 도는 동안은 한 번만 읽는다 — 등록은 앱 시작·로그인·토큰 갱신에서 뜨는데
// 매번 SecureStore를 때릴 이유가 없다.
let cached: string | null = null;

/**
 * 이 설치의 ID를 돌려준다. 없으면 만들어 저장한다.
 *
 * SecureStore에 두는 이유는 비밀이라서가 아니라 **앱을 껐다 켜도 같은 값이어야** 하기 때문이다
 * (세션 토큰이 이미 여기 있어 새 의존성이 늘지 않는다). iOS 키체인은 앱을 지워도 값이 남는데,
 * 여기서는 오히려 그게 맞다 — 재설치해도 같은 물리 기기이므로 이전 설치가 남긴 고아 토큰이 정리된다.
 *
 * 저장에 실패해도 예외를 던지지 않는다. 설치 ID는 있으면 좋은 값이지 없으면 등록이 막혀야 하는
 * 값이 아니다 — 없으면 서버는 구버전 앱과 똑같이 취급하고, 정리는 60일 staleness 청소가 맡는다.
 *
 * @returns 설치 ID. 저장소를 못 쓰면 null
 */
export async function getInstallationId(): Promise<string | null> {
  if (cached) return cached;
  try {
    const saved = await SecureStore.getItemAsync(INSTALLATION_KEY);
    if (saved) {
      cached = saved;
      return saved;
    }
    const created = newInstallationId(Math.random, Date.now());
    await SecureStore.setItemAsync(INSTALLATION_KEY, created);
    cached = created;
    return created;
  } catch {
    // 저장소를 못 쓰는 상황(기기 잠금 등). 등록은 설치 ID 없이 계속 진행한다.
    return null;
  }
}

/** 테스트 전용 — 모듈 캐시를 비운다. 운영 코드에서 부르지 말 것. */
export function resetInstallationIdCacheForTest(): void {
  cached = null;
}
