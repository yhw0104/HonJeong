// firebase.json은 RNFirebase가 **네이티브 빌드 중에** 읽는 파일이라, 잘못되면 tsc·jest는
// 전부 초록불인 채 EAS 빌드가 6분 뒤에 죽는다("Failed to parse firebase.json"). 실제로 겪었다
// (2026-08-08 빌드 12) — 주석을 남기려고 "_comment" 키를 넣은 것이 원인이었다. JSON엔 주석이 없고
// RNFirebase는 모르는 키를 그냥 넘기지 않는다.
//
// 그래서 그 제약을 여기서 즉시 잡는다. 설정의 '사유'는 banner.ts에 적혀 있다.
import firebaseJson from '../../../firebase.json';

describe('firebase.json', () => {
  it('★ react-native에 RNFirebase가 아는 키만 있다 — 주석용 키를 넣으면 빌드가 깨진다', () => {
    // 값이 아니라 '키 목록'을 고정한다. 새 설정을 의도적으로 추가할 땐 이 목록도 함께 늘린다.
    expect(Object.keys(firebaseJson['react-native'])).toEqual(['messaging_ios_foreground_presentation_options']);
  });

  it('포그라운드 표시는 꺼져 있다 — 앱을 보고 있을 때는 인앱 배너(PushBanner)가 담당한다', () => {
    // 여기에 'banner'가 들어가면 OS가 배너를 띄워서, "보고 있는 대화방 메시지는 빼자"는
    // 규칙(shouldShowBanner)이 무력해진다. OS는 어느 화면을 보고 있는지 모른다.
    expect(firebaseJson['react-native'].messaging_ios_foreground_presentation_options).toEqual([]);
  });
});
