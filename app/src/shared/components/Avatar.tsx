// Avatar — 프로필 사진(uri)을 원형으로 표시하고, 사진이 없으면 앱 아이콘을 보여준다.
// 사용: <Avatar uri={profile?.profileImageUrl} size={84} />
//
// 폴백을 앱 아이콘 하나로 통일한 이유: 이전에는 name을 넘긴 화면은 닉네임 첫 글자, 안 넘긴 화면은 👤가
// 나와서 같은 사람이 화면마다 다르게 보였다(2026-07-30 사용자 지적).
import React, { useState } from 'react';
import { View, Image } from 'react-native';
import { T2, C } from '@/shared/theme';
import { avatarMode } from './avatar-mode';

// 앱 아이콘 원본(assets/icon.png, 1024²)은 여백이 넓어 그대로 넣으면 냄비가 점처럼 작아진다 →
// 확대해 넣고 원으로 잘라낸다. 아래 두 값은 원본 픽셀을 실측한 것이다(그림 bbox y 276..671).
const APP_ICON = require('../../../assets/icon.png');
const GLYPH_HEIGHT = 0.386;   // 캔버스 높이 대비 그림(하트+냄비) 높이
const GLYPH_CENTER_Y = 0.462; // 그림의 세로 중심 — 하트가 위에 있어 캔버스 중앙보다 3.8% 위다
const TARGET_HEIGHT = 0.54;   // 원 지름 대비 그림 높이(아바타에서 적당한 크기)
const FALLBACK_SCALE = TARGET_HEIGHT / GLYPH_HEIGHT;                 // ≈1.4배로 그린다
const FALLBACK_SHIFT_Y = (0.5 - GLYPH_CENTER_Y) * FALLBACK_SCALE;    // 그만큼 내려 실제 중앙에 맞춘다

type Props = {
  uri?: string | null;
  bg?: string;
  size?: number;
  ring?: string;
  /** '혼밥 중' 초록 점(메이트 목록 등) — 구 EmojiCircle에서 넘어온 표시. */
  online?: boolean;
  /** 종료된 신청처럼 흐리게 보여야 할 때. */
  dimmed?: boolean;
};

export function Avatar({ uri, bg = T2.border, size = 40, ring, online = false, dimmed = false }: Props) {
  const [failed, setFailed] = useState(false);
  const mode = avatarMode(failed ? null : uri);
  // 폴백(앱 아이콘)에는 흰 카드 위에서 타일로 읽히도록 얇은 보더를 준다. 사진에는 주지 않는다(기존 모양 유지).
  const outline = mode === 'fallback' && !ring ? { borderWidth: 1, borderColor: T2.border } : {};
  const wrap = {
    width: size,
    height: size,
    borderRadius: size / 2,
    backgroundColor: bg,
    alignItems: 'center' as const,
    justifyContent: 'center' as const,
    borderWidth: ring ? 2.5 : 0,
    borderColor: ring,
    overflow: 'hidden' as const,
    ...outline,
  };
  const drawn = Math.round(size * FALLBACK_SCALE);
  const dot = Math.round(size * 0.31);

  return (
    <View style={{ opacity: dimmed ? 0.7 : 1 }}>
      <View style={wrap}>
        {mode === 'image' ? (
          <Image source={{ uri: uri! }} style={{ width: size, height: size }} onError={() => setFailed(true)} />
        ) : (
          <Image
            source={APP_ICON}
            style={{ width: drawn, height: drawn, transform: [{ translateY: size * FALLBACK_SHIFT_Y }] }}
            resizeMode="contain"
          />
        )}
      </View>
      {online ? (
        <View
          style={{
            position: 'absolute',
            right: -1,
            bottom: -1,
            width: dot,
            height: dot,
            borderRadius: dot / 2,
            backgroundColor: C.open,
            borderWidth: 2.5,
            borderColor: '#fff',
          }}
        />
      ) : null}
    </View>
  );
}
