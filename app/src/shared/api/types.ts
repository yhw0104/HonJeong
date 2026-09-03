// 백엔드 ListResponse<T> 엔벨로프(client.ts가 data로 풀어준 형태).
//
// ★page/size/totalElements가 없다. 무한 스크롤을 쓰지 않게 되면서 읽는 곳이 사라졌고,
//   서버는 그 값을 만들려고 655,163행을 세고 있었다(2026-08-24 실측 298ms). 봉투는 content
//   하나로 줄였다 — 필드 이름을 유지해야 이미 배포된 앱이 그대로 동작한다.
export type ListEnvelope<T> = {
  content: T[];
};
