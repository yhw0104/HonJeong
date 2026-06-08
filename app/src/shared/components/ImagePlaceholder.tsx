// ImagePlaceholder — 목업의 줄무늬 이미지 플레이스홀더.
// 목업 사용: <ImagePlaceholder w="100%" h={320} radius={0} bg="#EEE9DF" stripe="#E0D9C7" color="#A39B85" tag="대표 사진" />
import React from 'react';
import { View, Text, DimensionValue } from 'react-native';
import Svg, { Path } from 'react-native-svg';

type Props = {
  w?: DimensionValue;
  h?: number;
  radius?: number;
  bg?: string;
  stripe?: string;
  color?: string;
  tag?: string;
};

export function ImagePlaceholder({
  w = '100%',
  h = 160,
  radius = 12,
  bg = '#EEE9DF',
  stripe = '#E0D9C7',
  color = '#A39B85',
  tag,
}: Props) {
  return (
    <View
      style={{
        width: w,
        height: h,
        borderRadius: radius,
        backgroundColor: bg,
        overflow: 'hidden',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {/* 가운데 사진 글리프 */}
      <Svg width={28} height={28} viewBox="0 0 24 24" fill="none">
        <Path
          d="M3 6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6z"
          stroke={color}
          strokeWidth={1.5}
        />
        <Path d="M3 16l5-5 4 4 3-3 6 6" stroke={color} strokeWidth={1.5} strokeLinejoin="round" />
        <Path d="M8.5 9.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z" fill={color} />
      </Svg>
      {/* 하단 스트라이프 힌트 */}
      <View
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          height: 6,
          backgroundColor: stripe,
        }}
      />
      {tag ? (
        <View
          style={{
            position: 'absolute',
            left: 10,
            bottom: 12,
            paddingHorizontal: 8,
            paddingVertical: 4,
            borderRadius: 6,
            backgroundColor: 'rgba(0,0,0,0.45)',
          }}
        >
          <Text style={{ color: '#fff', fontSize: 11, fontWeight: '600' }}>{tag}</Text>
        </View>
      ) : null}
    </View>
  );
}
