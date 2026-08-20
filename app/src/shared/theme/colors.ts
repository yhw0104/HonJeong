// T2 — 혼정 디자인 토큰 (모노톤 + 절제된 오렌지 한 점)
// screens/*.jsx 목업이 전역으로 참조하던 T2 팔레트를 사용처로부터 재구성한 것.
// 색상값은 목업의 hex 단서(브랜드 #FF5A1F, 베이지 보더 #EEE9DF/#E0D9C7, 뮤트 #A39B85)에서 도출.
// 실기기 시각 비교로 미세 튜닝 가능.

export const T2 = {
  bg: '#FAF7F2', // 페이지 배경 (따뜻한 오프화이트)
  surface: '#FFFFFF', // 카드/시트 배경 (목업의 #fff)
  text: '#171717', // 주요 텍스트
  textSub: '#525252', // 보조 텍스트
  textMute: '#A39B85', // 비활성/뮤트 텍스트
  border: '#EEE9DF', // 일반 보더
  borderStrong: '#E0D9C7', // 강조 보더
  brand: '#FF5A1F', // 브랜드 오렌지
  brandSoft: '#FFF4EF', // 브랜드 연한 틴트
  mapBg: '#EDE8DD', // 지도 배경 (베이지)
} as const;

// 일회성/의미 색상 (브랜드 팔레트와 별개)
export const C = {
  open: '#22A65A', // 영업중 그린
  openDark: '#1B8049',
  kakao: '#FEE500',
  kakaoText: '#191600',
  // ★애플 로그인 버튼 색은 브랜드 취향이 아니라 애플 규격이다. HIG는 로고와 문구를
  //   "검정 아니면 흰색"으로만 허용하고 커스텀 색을 금지한다. 여기 값을 바꾸면 심사
  //   지침 위반이 된다(카카오의 #FEE500이 카카오 규격인 것과 같은 성격).
  apple: '#000000',
  appleText: '#FFFFFF',
} as const;

export type ThemeColors = typeof T2;
