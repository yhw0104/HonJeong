// 카카오맵 설정.
//
// 발급 절차 (developers.kakao.com):
//   1) 내 애플리케이션 → 애플리케이션 추가하기 (앱 이름 "혼정")
//   2) 앱 키 → "JavaScript 키"를 아래 KAKAO_JS_KEY 에 붙여넣기
//   3) 제품 설정 → 카카오맵 → "사용" ON  (안 켜면 지도 안 뜸)
//   4) 앱 설정 → 플랫폼 → Web 플랫폼 등록 → 사이트 도메인에 KAKAO_MAP_BASE_URL 값을 추가
//
// JS 키는 도메인 제한이 걸린 클라이언트 키라 앱 번들에 노출돼도 무방하다.
// 나중에 키를 git에서 빼고 싶으면 app.json 의 expo.extra 로 옮기고 expo-constants 로 읽으면 된다.

/** 카카오 JavaScript 앱 키. 비어 있으면 HonjeongMap이 안내 화면을 보여준다. */
export const KAKAO_JS_KEY = '4d2807f7727448cf3f24a4e5ec5a8e16';

/**
 * WebView 문서가 로드되는 가상 도메인(origin). 카카오 JS SDK는 등록된 도메인에서만 동작하므로,
 * 이 값을 카카오 콘솔의 Web 플랫폼 "사이트 도메인"에 그대로 등록해야 지도가 뜬다.
 */
export const KAKAO_MAP_BASE_URL = 'https://localhost';

/** 지도 기본 중심(연남동 부근)과 확대 레벨. */
export const DEFAULT_MAP_CENTER = { lat: 37.5571, lng: 126.9255 } as const;
export const DEFAULT_MAP_LEVEL = 4;
