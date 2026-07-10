// 고객센터 문의 채널 설정.
//
// 문의 창구 = 카카오톡 오픈채팅방. 카톡 앱에서 "오픈채팅 만들기"로 개설하고
// 채팅방 URL(https://open.kakao.com/o/XXXXX)을 아래 KAKAO_OPENCHAT_URL 에 넣는다.
//
// 비어 있으면 '카카오톡 문의' 버튼이 "준비 중" 안내를 띄우고, 이메일 문의가
// 무인프라로 확실히 동작하는 폴백이다(화면이 거짓 약속을 하지 않도록).

/** 카카오톡 오픈채팅방 URL. 비어 있으면 '카카오톡 문의' 버튼이 준비 중 안내를 띄운다. */
export const KAKAO_OPENCHAT_URL = 'https://open.kakao.com/o/scSyknDi';

/** 고객센터 문의 이메일. 탭하면 mailto 로 기본 메일 앱이 열린다. */
export const SUPPORT_EMAIL = 'yhw0104@naver.com';
