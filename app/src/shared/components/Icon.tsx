// Icon — 목업의 inline <svg>들을 react-native-svg로 이식한 아이콘 세트.
// 목업에서 단순 글리프(←, ★, ✓, ›, ● 등)로 그리던 것은 <Text>로 두고,
// 실제 path 기반 아이콘만 여기서 제공한다.
import React from 'react';
import Svg, { Path, Circle, Rect, Line } from 'react-native-svg';
import { T2, C } from '@/shared/theme';

export type IconName =
  | 'search'
  | 'navigate'
  | 'phone'
  | 'kakao'
  | 'apple'
  | 'info'
  | 'chevronDown'
  | 'chevronRight'
  | 'chevronLeft'
  | 'heart'
  | 'pin'
  | 'book'
  | 'badge'
  | 'mate'
  | 'shield'
  | 'bell'
  | 'note'
  | 'help'
  | 'share'
  | 'comment'
  | 'bookmark'
  | 'pencil'
  | 'copy'
  | 'phoneCall'
  | 'camera'
  | 'mail'
  | 'pushpin'
  | 'chevronUp'
  | 'star'
  | 'rice'
  | 'friends'
  | 'chat'
  | 'trash';

type Props = {
  name: IconName;
  size?: number;
  color?: string;
};

export function Icon({ name, size = 18, color = T2.text }: Props) {
  switch (name) {
    case 'search':
      return (
        <Svg width={size} height={size} viewBox="0 0 16 16" fill="none">
          <Circle cx={7} cy={7} r={5} stroke={color} strokeWidth={1.5} />
          <Path d="M11 11l3 3" stroke={color} strokeWidth={1.5} strokeLinecap="round" />
        </Svg>
      );
    case 'navigate':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M12 2L2 12l10 10 10-10L12 2z"
            fill="none"
            stroke={color}
            strokeWidth={1.6}
            strokeLinejoin="round"
          />
          <Path
            d="M9 13v-2.5a1.5 1.5 0 0 1 1.5-1.5H14m0 0l-1.8-1.8M14 9l-1.8 1.8"
            stroke={color}
            strokeWidth={1.6}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </Svg>
      );
    case 'phone':
      return (
        <Svg width={size} height={size} viewBox="0 0 16 16" fill="none">
          <Rect x={4} y={1} width={8} height={14} rx={2} stroke={color} strokeWidth={1.5} />
          <Line x1={6.8} y1={12.6} x2={9.2} y2={12.6} stroke={color} strokeWidth={1.5} strokeLinecap="round" />
        </Svg>
      );
    case 'kakao':
      return (
        <Svg width={size} height={size} viewBox="0 0 18 18" fill="none">
          <Path
            d="M9 1.6C4.8 1.6 1.4 4.3 1.4 7.6c0 2.1 1.4 4 3.6 5.1-.16.57-.58 2.06-.66 2.38-.1.4.14.4.3.29.13-.09 2.05-1.39 2.88-1.96.48.07.97.1 1.48.1 4.2 0 7.6-2.7 7.6-6C16.6 4.3 13.2 1.6 9 1.6z"
            fill={color}
          />
        </Svg>
      );
    case 'apple':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
          <Path d="M17.05 12.04c-.03-2.6 2.12-3.85 2.22-3.91-1.21-1.77-3.09-2.01-3.76-2.04-1.6-.16-3.12.94-3.93.94-.81 0-2.06-.92-3.39-.89-1.74.03-3.35 1.01-4.25 2.57-1.81 3.15-.46 7.81 1.3 10.37.86 1.25 1.89 2.66 3.23 2.61 1.3-.05 1.79-.84 3.36-.84 1.57 0 2.01.84 3.39.81 1.4-.02 2.29-1.28 3.15-2.54.99-1.46 1.4-2.87 1.42-2.94-.03-.01-2.72-1.04-2.75-4.13zM14.6 4.56c.72-.87 1.2-2.08 1.07-3.28-1.03.04-2.28.69-3.02 1.56-.66.77-1.24 2-1.08 3.18 1.15.09 2.32-.59 3.03-1.46z" />
        </Svg>
      );
    case 'info':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Circle cx={12} cy={12} r={9} stroke={color} strokeWidth={1.6} />
          <Path d="M12 8v5" stroke={color} strokeWidth={1.8} strokeLinecap="round" />
          <Circle cx={12} cy={16} r={0.6} fill={color} stroke={color} />
        </Svg>
      );
    case 'chevronDown':
      return (
        <Svg width={size} height={size} viewBox="0 0 11 11" fill="none">
          <Path d="M2 4l3.5 3.5L9 4" stroke={color} strokeWidth={1.6} strokeLinecap="round" />
        </Svg>
      );
    case 'chevronRight':
      return (
        <Svg width={size} height={size} viewBox="0 0 11 11" fill="none">
          <Path d="M4 2l3.5 3.5L4 9" stroke={color} strokeWidth={1.6} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'heart':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M12 20s-7-4.5-7-9.5A4.5 4.5 0 0 1 12 7a4.5 4.5 0 0 1 7 3.5C19 15.5 12 20 12 20z"
            fill={color === T2.text ? 'none' : color}
            stroke={color}
            strokeWidth={1.6}
            strokeLinejoin="round"
          />
        </Svg>
      );
    case 'pin':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M12 22s7-7.2 7-12a7 7 0 1 0-14 0c0 4.8 7 12 7 12z"
            fill={color}
            stroke={color}
            strokeWidth={1.4}
            strokeLinejoin="round"
          />
          <Circle cx={12} cy={10} r={2.5} fill="#fff" />
        </Svg>
      );
    case 'chevronLeft':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M15 6l-6 6 6 6" stroke={color} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'book':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M4 5a2 2 0 0 1 2-2h11v16H6a2 2 0 0 0-2 2V5z" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Path d="M4 19a2 2 0 0 1 2-2h11" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'badge':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Circle cx={12} cy={9} r={5.5} stroke={color} strokeWidth={1.7} />
          <Path d="M8.5 13.5L7 21l5-2.5L17 21l-1.5-7.5" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'mate':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M4 18a5 5 0 0 1 10 0M9 9a3 3 0 1 0 0-.01M17 13a3 3 0 1 0-2-5.2M20 18a4 4 0 0 0-5-3.8"
            stroke={color}
            strokeWidth={1.7}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </Svg>
      );
    case 'shield':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6l7-3z" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'bell':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6zM10 20a2 2 0 0 0 4 0" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'note':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Rect x={5} y={4} width={14} height={16} rx={2} stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
          <Path d="M9 9h6M9 13h6M9 17h3" stroke={color} strokeWidth={1.7} strokeLinecap="round" />
        </Svg>
      );
    case 'help':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Circle cx={12} cy={12} r={9} stroke={color} strokeWidth={1.7} />
          <Path d="M9.5 9.5a2.5 2.5 0 0 1 4 1.8c0 1.5-2 2-2 3.2" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Circle cx={11.5} cy={17} r={0.6} fill={color} />
        </Svg>
      );
    case 'share':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
        </Svg>
      );
    case 'comment':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M21 11.5a8.4 8.4 0 01-9 8.4 9 9 0 01-3.5-.6L3 21l1.4-4.2A8.4 8.4 0 1121 11.5z"
            stroke={color}
            strokeWidth={1.7}
            strokeLinejoin="round"
          />
        </Svg>
      );
    case 'bookmark':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M6 3h12a1 1 0 011 1v17l-7-4-7 4V4a1 1 0 011-1z" stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
        </Svg>
      );
    case 'pencil':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M4 20h4l10-10-4-4L4 16v4z" stroke={color} strokeWidth={1.8} strokeLinejoin="round" />
          <Path d="M13.5 6.5l4 4" stroke={color} strokeWidth={1.8} strokeLinecap="round" />
        </Svg>
      );
    case 'copy':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Rect x={8} y={8} width={11} height={13} rx={2} stroke={color} strokeWidth={1.8} />
          <Path d="M16 5H7a2 2 0 0 0-2 2v10" stroke={color} strokeWidth={1.8} strokeLinecap="round" />
        </Svg>
      );
    case 'phoneCall':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M6.5 4h3l1.5 4-2 1.5a11 11 0 0 0 5 5l1.5-2 4 1.5v3a1.5 1.5 0 0 1-1.6 1.5C12 23 5 16 5 6.6A1.5 1.5 0 0 1 6.5 4z"
            stroke={color}
            strokeWidth={1.7}
            strokeLinejoin="round"
          />
        </Svg>
      );
    case 'camera':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M4 8a2 2 0 0 1 2-2h2l1.5-2h5L18 6a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8z"
            stroke={color}
            strokeWidth={1.7}
            strokeLinejoin="round"
          />
          <Circle cx={12} cy={12.5} r={3} stroke={color} strokeWidth={1.7} />
        </Svg>
      );
    case 'mail':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Rect x={3} y={5} width={18} height={14} rx={2} stroke={color} strokeWidth={1.7} />
          <Path d="M4 7l8 6 8-6" stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
        </Svg>
      );
    case 'pushpin':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M9 3h6l-1 6 4 4v2H6v-2l4-4-1-6z" stroke={color} strokeWidth={1.8} strokeLinejoin="round" />
          <Path d="M12 15v6" stroke={color} strokeWidth={1.8} strokeLinecap="round" />
        </Svg>
      );
    case 'chevronUp':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M6 15l6-6 6 6" stroke={color} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'star':
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path
            d="M12 3.5l2.6 5.27 5.82.85-4.21 4.1.99 5.78L12 16.77l-5.2 2.73.99-5.78-4.21-4.1 5.82-.85L12 3.5z"
            fill={color}
            stroke={color}
            strokeWidth={1.2}
            strokeLinejoin="round"
          />
        </Svg>
      );
    case 'rice':
      // 밥공기(라인) — 알림 '같이먹기'.
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M2.5 11H21.5" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Path d="M4.5 11a7.5 7.5 0 0 0 15 0" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Path d="M8 11a4 3 0 0 1 8 0" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'friends':
      // 두 사람(대칭) — 알림 '메이트'.
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Circle cx={8.5} cy={9} r={2.6} stroke={color} strokeWidth={1.7} />
          <Circle cx={15.5} cy={9} r={2.6} stroke={color} strokeWidth={1.7} />
          <Path d="M4 18a4.5 4.5 0 0 1 9 0" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Path d="M11 18a4.5 4.5 0 0 1 9 0" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'chat':
      // 말풍선 두 개(대화) — 프로필 '도란도란 대화하며'.
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M3 5.8a1.8 1.8 0 0 1 1.8-1.8h8.4a1.8 1.8 0 0 1 1.8 1.8v4a1.8 1.8 0 0 1-1.8 1.8h-4.5l-3 2.6v-2.6a1.8 1.8 0 0 1-2.7-1.8z" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Path d="M17 9.2h2.2a1.8 1.8 0 0 1 1.8 1.8v3.4a1.8 1.8 0 0 1-1.8 1.8h-0.3v2.2l-2.6-2.2h-2.5a1.8 1.8 0 0 1-1.8-1.8" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
        </Svg>
      );
    case 'trash':
      // 휴지통 — 대화 목록 스와이프 삭제 액션.
      return (
        <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <Path d="M4 7h16" stroke={color} strokeWidth={1.7} strokeLinecap="round" />
          <Path d="M9.5 7V5.2a1.2 1.2 0 0 1 1.2-1.2h2.6a1.2 1.2 0 0 1 1.2 1.2V7" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Path d="M6.5 7l.8 12a2 2 0 0 0 2 1.9h5.4a2 2 0 0 0 2-1.9l.8-12" stroke={color} strokeWidth={1.7} strokeLinecap="round" strokeLinejoin="round" />
          <Path d="M10.3 11v6M13.7 11v6" stroke={color} strokeWidth={1.7} strokeLinecap="round" />
        </Svg>
      );
    default:
      return null;
  }
}
