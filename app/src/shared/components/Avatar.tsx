// Avatar — 프로필 사진(uri) / 이름 첫 글자 이니셜 / 기본 프로필(👤)을 원형으로 표시.
// 사용: <Avatar uri={profile?.profileImageUrl} size={84} /> 또는 <Avatar name="혼" size={40} />
import React, { useState } from 'react';
import { View, Text, Image } from 'react-native';
import { T2 } from '@/shared/theme';
import { avatarMode } from './avatar-mode';

type Props = {
  name?: string;
  uri?: string | null;
  bg?: string;
  size?: number;
  ring?: string;
  color?: string;
};

export function Avatar({ name, uri, bg = T2.text, size = 40, ring, color = '#fff' }: Props) {
  const [failed, setFailed] = useState(false);
  const mode = avatarMode(failed ? null : uri, name);
  const initial = Array.from(name ?? '')[0] ?? '';
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
  };

  if (mode === 'image') {
    return (
      <View style={wrap}>
        <Image source={{ uri: uri! }} style={{ width: size, height: size }} onError={() => setFailed(true)} />
      </View>
    );
  }

  return (
    <View style={wrap}>
      <Text style={{ color, fontWeight: '800', fontSize: Math.round(size * (mode === 'default' ? 0.5 : 0.42)) }}>
        {mode === 'default' ? '👤' : initial}
      </Text>
    </View>
  );
}
