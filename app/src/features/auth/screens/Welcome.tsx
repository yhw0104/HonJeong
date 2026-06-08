// Welcome — 웰컴 / 로그인 진입 (원본: screens/Welcome.jsx)
// absolute 레이아웃을 flex 컬럼(상단 타이포 / 중단 카운터 / 하단 CTA)으로 재배치.
import React from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { Screen, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

export function WelcomeScreen({ navigation }: RootStackScreenProps<'Welcome'>) {
  return (
    <Screen bg={T2.bg}>
      <View style={styles.container}>
        {/* 상단 타이포 블록 */}
        <View style={styles.top}>
          <Text style={styles.eyebrow}>혼밥을 정상화하다</Text>
          <Text style={styles.h1}>
            혼자 밥 먹는 게{'\n'}
            <Text style={{ color: T2.brand }}>쉬워질 때</Text>까지
          </Text>
          <Text style={styles.lead}>
            혼자여도 괜찮은 식당, 그리고 같은 시간 같은 자리의 사람들. 오늘 한 끼, 편하게 누려보세요.
          </Text>
        </View>

        <View style={{ flex: 1 }} />

        {/* 라이브 인디케이터 — 지금 혼밥 중 */}
        <View style={styles.indicator}>
          <View style={styles.indicatorRow}>
            <View style={styles.pulse}>
              <View style={styles.pulseHalo} />
              <View style={styles.pulseDot} />
            </View>
            <Text style={styles.indicatorLabel}>지금 이 순간,</Text>
          </View>
          <View style={styles.countRow}>
            <Text style={styles.countNum}>27</Text>
            <Text style={styles.countUnit}>명의 혼밥러들</Text>
          </View>
        </View>

        {/* 하단 CTA */}
        <View style={styles.cta}>
          {/* 카카오 — 대표 소셜 로그인 (UI 목업: 즉시 입장) */}
          <Pressable
            style={[styles.btn, { backgroundColor: C.kakao }]}
            onPress={() => navigation.navigate('MainTabs')}
          >
            <Icon name="kakao" size={18} color={C.kakaoText} />
            <Text style={[styles.btnText, { color: C.kakaoText }]}>카카오로 계속하기</Text>
          </Pressable>
          {/* Apple */}
          <Pressable
            style={[styles.btn, { backgroundColor: T2.text }]}
            onPress={() => navigation.navigate('MainTabs')}
          >
            <Icon name="apple" size={16} color="#fff" />
            <Text style={[styles.btnText, { color: '#fff' }]}>Apple로 계속하기</Text>
          </Pressable>
          {/* 휴대폰 — 문서상의 인증 흐름 진입점 */}
          <Pressable
            style={[styles.btn, { backgroundColor: T2.brand }]}
            onPress={() => navigation.navigate('PhoneAuth')}
          >
            <Icon name="phone" size={16} color="#fff" />
            <Text style={[styles.btnText, { color: '#fff' }]}>휴대폰 번호로 계속하기</Text>
          </Pressable>
        </View>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, paddingHorizontal: 28 },
  top: { paddingTop: 72 },
  eyebrow: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 1 },
  h1: { fontSize: 40, fontWeight: '800', color: T2.text, letterSpacing: -1.4, marginTop: 14, lineHeight: 46 },
  lead: { fontSize: 15, color: T2.textSub, lineHeight: 24, marginTop: 18, letterSpacing: -0.3, maxWidth: 300 },

  indicator: { marginBottom: 28 },
  indicatorRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  pulse: { width: 10, height: 10, alignItems: 'center', justifyContent: 'center' },
  pulseHalo: {
    position: 'absolute',
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: T2.brand,
    opacity: 0.18,
  },
  pulseDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: T2.brand },
  indicatorLabel: { fontSize: 15, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  countRow: { flexDirection: 'row', alignItems: 'baseline', gap: 6, marginTop: 8, paddingLeft: 18 },
  countNum: { fontSize: 56, fontWeight: '800', color: T2.brand, letterSpacing: -3, lineHeight: 56 },
  countUnit: { fontSize: 21, fontWeight: '800', color: T2.text, letterSpacing: -1 },

  cta: { paddingBottom: 24, gap: 8 },
  btn: {
    paddingVertical: 15,
    borderRadius: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  btnText: { fontSize: 15, fontWeight: '700', letterSpacing: -0.3 },
});
