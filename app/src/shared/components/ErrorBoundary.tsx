// ErrorBoundary — 앱 전역 렌더 에러 안전망.
// 하위 트리에서 렌더 단계 예외가 나면 흰 화면/크래시 대신 복구 UI(다시 시도)를 보여준다.
// ★한계(React): 이벤트 핸들러·비동기(fetch 등) 에러는 못 잡는다 — 그건 화면별 try/catch·StateView가 처리.
// 원격 크래시 리포팅(Sentry 등)은 배포 단계(mock→real)로 미룬다.
import React from 'react';
import { View } from 'react-native';
import { T2 } from '@/shared/theme';
import { StateView } from './StateView';

type Props = { children: React.ReactNode };
type State = { hasError: boolean };

export class ErrorBoundary extends React.Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    // 개발 중 콘솔에 남긴다. 배포 시 여기서 원격 리포팅으로 교체.
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  // 다시 시도 = 에러 상태 초기화 후 재렌더(소프트 리셋). 결정적 에러면 재발할 수 있음(표준 동작).
  private reset = (): void => this.setState({ hasError: false });

  render(): React.ReactNode {
    if (this.state.hasError) {
      return (
        <View style={{ flex: 1, backgroundColor: T2.bg }}>
          <StateView kind="error" message={'예상치 못한 문제가 생겼어요.\n다시 시도해 주세요.'} onRetry={this.reset} />
        </View>
      );
    }
    return this.props.children;
  }
}
