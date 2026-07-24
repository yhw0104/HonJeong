// BadgeMedal — 뱃지 시안(screens/뱃지 시안.html)의 메달 디자인을 react-native-svg로 이식.
// 그라데이션 링 + 라인 아이콘(lucide, 손으로 이식) + 티어 숫자 + 잠금(회색) 상태.
// RN은 conic-gradient 미지원 → 선형 그라데이션 광택으로 근사.
import React, { useId } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import Svg, { Circle, Path, Line, Defs, LinearGradient, Stop } from 'react-native-svg';

export type BadgeIconKey =
  | 'sprout' | 'utensils' | 'trophy' | 'camera' | 'bookOpen'
  | 'handshake' | 'userPlus' | 'users' | 'bookmark' | 'house';
export type BadgeTier = 'brand' | 'gold' | 'mint';

// 티어별 색(시안: 오렌지=시작 / 골드=꾸준함 / 민트=연결).
const TIER: Record<BadgeTier, { light: string; main: string; deep: string; disc: string }> = {
  brand: { light: '#FED7AA', main: '#F97316', deep: '#C2410C', disc: '#FFF8F2' },
  gold: { light: '#F7E3AE', main: '#E0A324', deep: '#B57F12', disc: '#FFFDF4' },
  mint: { light: '#C6EFDF', main: '#10B981', deep: '#047857', disc: '#F4FFFB' },
};
// 미획득(잠금) — 회색.
const LOCK = { light: '#EAE1D3', main: '#D3C6B2', deep: '#B7A88E', disc: '#F3ECE0' };

/** 각 뱃지 아이콘의 lucide path(24x24 viewBox, stroke). 원본에서 이식. */
function IconPaths({ name, color }: { name: BadgeIconKey; color: string }) {
  const sp = { stroke: color, strokeWidth: 2, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, fill: 'none' };
  switch (name) {
    case 'sprout':
      return (
        <>
          <Path d="M14 9.536V7a4 4 0 0 1 4-4h1.5a.5.5 0 0 1 .5.5V5a4 4 0 0 1-4 4 4 4 0 0 0-4 4c0 2 1 3 1 5a5 5 0 0 1-1 3" {...sp} />
          <Path d="M4 9a5 5 0 0 1 8 4 5 5 0 0 1-8-4" {...sp} />
          <Path d="M5 21h14" {...sp} />
        </>
      );
    case 'utensils':
      return (
        <>
          <Path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2" {...sp} />
          <Path d="M7 2v20" {...sp} />
          <Path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7" {...sp} />
        </>
      );
    case 'trophy':
      return (
        <>
          <Path d="M10 14.66v1.626a2 2 0 0 1-.976 1.696A5 5 0 0 0 7 21.978" {...sp} />
          <Path d="M14 14.66v1.626a2 2 0 0 0 .976 1.696A5 5 0 0 1 17 21.978" {...sp} />
          <Path d="M18 9h1.5a1 1 0 0 0 0-5H18" {...sp} />
          <Path d="M4 22h16" {...sp} />
          <Path d="M6 9a6 6 0 0 0 12 0V3a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1z" {...sp} />
          <Path d="M6 9H4.5a1 1 0 0 1 0-5H6" {...sp} />
        </>
      );
    case 'camera':
      return (
        <>
          <Path d="M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z" {...sp} />
          <Circle cx={12} cy={13} r={3} stroke={color} strokeWidth={2} fill="none" />
        </>
      );
    case 'bookOpen':
      return (
        <>
          <Path d="M12 5v16" {...sp} />
          <Path d="M20.001 19A2 2 0 0 0 22 17V5a2 2 0 0 0-1.999-2L16 3.002A5 5 0 0 0 12 5a5 5 0 0 0-4-2H4a2 2 0 0 0-2 2v12a2 2 0 0 0 1.999 2H8a5 5 0 0 1 4 2 5 5 0 0 1 4-2z" {...sp} />
        </>
      );
    case 'handshake':
      return (
        <>
          <Path d="m11 17 2 2a1 1 0 1 0 3-3" {...sp} />
          <Path d="m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4" {...sp} />
          <Path d="m21 3 1 11h-2" {...sp} />
          <Path d="M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3" {...sp} />
          <Path d="M3 4h8" {...sp} />
        </>
      );
    case 'userPlus':
      return (
        <>
          <Path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" {...sp} />
          <Circle cx={9} cy={7} r={4} stroke={color} strokeWidth={2} fill="none" />
          <Line x1={19} x2={19} y1={8} y2={14} stroke={color} strokeWidth={2} strokeLinecap="round" />
          <Line x1={22} x2={16} y1={11} y2={11} stroke={color} strokeWidth={2} strokeLinecap="round" />
        </>
      );
    case 'users':
      return (
        <>
          <Path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" {...sp} />
          <Path d="M16 3.128a4 4 0 0 1 0 7.744" {...sp} />
          <Path d="M22 21v-2a4 4 0 0 0-3-3.87" {...sp} />
          <Circle cx={9} cy={7} r={4} stroke={color} strokeWidth={2} fill="none" />
        </>
      );
    case 'bookmark':
      return <Path d="M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z" {...sp} />;
    case 'house':
      return (
        <>
          <Path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" {...sp} />
          <Path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" {...sp} />
        </>
      );
  }
}

export function BadgeMedal({ icon, tier, tierNum, earned, size }: {
  icon: BadgeIconKey;
  tier: BadgeTier;
  tierNum?: number;
  earned: boolean;
  size: number;
}) {
  const c = earned ? TIER[tier] : LOCK;
  const gid = useId(); // 그라데이션 id는 인스턴스마다 고유해야 함(중복 방지)
  const half = size / 2;
  const discR = size * 0.356; // 74/104 지름 → 반지름
  const iconSize = size * 0.34;

  return (
    <View style={{ width: size, height: size, alignItems: 'center', justifyContent: 'center' }}>
      <Svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <Defs>
          <LinearGradient id={gid} x1="0%" y1="0%" x2="100%" y2="100%">
            <Stop offset="0%" stopColor={c.light} />
            <Stop offset="55%" stopColor={c.main} />
            <Stop offset="100%" stopColor={c.deep} />
          </LinearGradient>
        </Defs>
        <Circle cx={half} cy={half} r={half - size * 0.01} fill={`url(#${gid})`} />
        <Circle cx={half} cy={half} r={discR} fill={c.disc} stroke="#fff" strokeWidth={size * 0.02} />
      </Svg>

      {/* 아이콘(디스크 중앙) */}
      <View style={{ position: 'absolute', width: iconSize, height: iconSize }}>
        <Svg width={iconSize} height={iconSize} viewBox="0 0 24 24" fill="none">
          <IconPaths name={icon} color={c.deep} />
        </Svg>
      </View>

      {/* 티어 숫자(획득한 마일스톤 뱃지만) */}
      {earned && tierNum ? (
        <View
          style={[
            styles.tier,
            { minWidth: size * 0.24, height: size * 0.24, borderRadius: size * 0.12, right: size * 0.02, bottom: size * 0.02 },
          ]}
        >
          <Text style={[styles.tierText, { color: c.deep, fontSize: size * 0.11 }]}>{tierNum}</Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  tier: {
    position: 'absolute',
    paddingHorizontal: 4,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.14,
    shadowRadius: 3,
    elevation: 2,
  },
  tierText: { fontWeight: '800', letterSpacing: -0.3 },
});
