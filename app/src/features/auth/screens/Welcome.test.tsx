// 웰컴 화면의 소셜 로그인 버튼 두 개가 같은 타이포로 그려지는지 고정한다.
//
// ★실기에서 "두 버튼 글자 크기가 다르다"는 지적을 받아 고친 것이라, 그 지적이 되살아나지
//   않게 여기서 못 박는다. 원인은 애플 공식 컴포넌트(AppleAuthenticationButton)였다 —
//   글자 크기를 버튼 높이의 43%로 스스로 정하고 그걸 바꿀 prop이 없어서, 카카오 규격
//   (레이블 ≤ 높이의 1/3)과 절대 만나지 못했다(실측 20.6pt vs 15pt).
//   그래서 애플 버튼을 직접 그리는 쪽으로 바꿨고, 이 테스트는 그 결정이 조용히 되돌아가는
//   것을 막는다. 공식 컴포넌트로 돌아가면 여기서 애플 쪽 <Text>를 못 찾아 실패한다.
import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';
import { Text, StyleSheet } from 'react-native';

import { WelcomeScreen } from './Welcome';

// Welcome은 '@/shared/components' 배럴에서 Screen·Icon 둘만 쓰는데, 그 배럴이 HonjeongMap →
// react-native-webview와 PhotoViewer → react-native-reanimated까지 함께 끌고 온다. 둘 다
// 네이티브 모듈이라 jest에서 로드하는 순간 죽는다(이 화면과는 아무 상관이 없다).
// 배럴째 대역으로 바꿔 그 사슬을 끊는다 — 이 테스트가 보는 건 <Text>의 스타일뿐이라
// Screen·Icon의 실제 구현이 필요 없다.
// ★앞으로 화면을 렌더하는 테스트를 쓸 때마다 같은 벽에 부딪힌다. 근본 해법은 배럴을
//   쪼개는 것이지만, 그건 이 수정의 범위 밖이다.
jest.mock('@/shared/components', () => {
  const React = require('react');
  const { View } = require('react-native');
  return {
    Screen: ({ children }: { children: React.ReactNode }) => React.createElement(View, null, children),
    Icon: () => null,
  };
});

jest.mock('@/features/auth/appleLogin', () => ({
  isAppleLoginAvailable: jest.fn().mockResolvedValue(true),
  loginWithApple: jest.fn(),
}));
jest.mock('@/features/auth/kakaoLogin', () => ({ loginWithKakao: jest.fn() }));
jest.mock('@/shared/auth/AuthContext', () => ({ useAuth: () => ({ signIn: jest.fn() }) }));
jest.mock('@/shared/api/client', () => ({
  // 통계는 화면 상단 카운터용이라 이 테스트와 무관하다. 미해결 프라미스를 남기면
  // 테스트가 끝난 뒤 setState가 돌아 경고가 나므로 즉시 값을 준다.
  apiGet: jest.fn().mockResolvedValue({ todayCount: 0, activeCount: 0, seekingCount: 0 }),
  apiPost: jest.fn(),
  ApiError: class ApiError extends Error {},
}));

/** 화면을 렌더하고 지원여부·통계 프라미스가 정착할 때까지 기다린다. */
async function renderWelcome() {
  const navigation = { navigate: jest.fn(), goBack: jest.fn() };
  let tree!: TestRenderer.ReactTestRenderer;
  await act(async () => {
    tree = TestRenderer.create(<WelcomeScreen navigation={navigation as never} route={{} as never} />);
  });
  return tree;
}

/**
 * children에서 문자열 잎만 모아 잇는다.
 * ★JSON.stringify로 훑으면 안 된다 — React 엘리먼트에는 _owner 순환 참조가 있어 즉시 터진다.
 */
function textOf(children: unknown): string {
  if (typeof children === 'string' || typeof children === 'number') return String(children);
  if (Array.isArray(children)) return children.map(textOf).join('');
  return '';
}

/** 문구로 버튼 레이블 <Text>를 찾아 최종 스타일을 평탄화해 돌려준다. */
function labelStyle(tree: TestRenderer.ReactTestRenderer, text: string) {
  const node = tree.root
    .findAllByType(Text)
    .find((n) => textOf(n.props.children).includes(text));
  if (!node) throw new Error(`"${text}" 레이블을 찾지 못했다`);
  return StyleSheet.flatten(node.props.style) as { fontSize?: number; fontWeight?: string; letterSpacing?: number };
}

describe('WelcomeScreen 소셜 로그인 버튼', () => {
  it('★애플과 카카오 버튼의 글자 크기·굵기·자간이 모두 같다', async () => {
    const tree = await renderWelcome();

    const apple = labelStyle(tree, 'Apple로 계속하기');
    const kakao = labelStyle(tree, '카카오로 계속하기');

    // 셋을 다 보는 이유: fontSize만 맞춰도 굵기나 자간이 다르면 실기에서는 여전히
    // "글자가 다르다"로 보인다. 지적의 실체는 크기 하나가 아니라 '같아 보이는가'였다.
    expect(apple.fontSize).toBe(kakao.fontSize);
    expect(apple.fontWeight).toBe(kakao.fontWeight);
    expect(apple.letterSpacing).toBe(kakao.letterSpacing);
    // 값이 둘 다 undefined면 위 세 단언이 전부 통과해 버린다 — 실제로 크기가 지정돼
    // 있다는 것까지 확인해야 이 테스트가 무언가를 지킨다.
    expect(apple.fontSize).toBe(15);
  });

  it('애플 버튼 색은 애플 규격(검정 배경·흰 글자)이다 — 커스텀 색은 심사 지침 위반이다', async () => {
    const tree = await renderWelcome();
    expect(labelStyle(tree, 'Apple로 계속하기').color).toBe('#FFFFFF');
  });
});
