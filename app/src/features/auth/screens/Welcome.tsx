// Welcome — 웰컴 / 로그인 진입 (원본: screens/Welcome.jsx)
// absolute 레이아웃을 flex 컬럼(상단 타이포 / 중단 카운터 / 하단 CTA)으로 재배치.
import React, { useState, useEffect, useRef } from 'react';
import { View, Text, Pressable, StyleSheet, Alert, Platform } from 'react-native';
// expo-apple-authentication을 여기서 직접 import하지 않는다 — 버튼을 직접 그리게 되면서
// 이 파일에 남은 애플 의존은 appleLogin.ts(로그인 호출·지원 여부)를 거치는 것뿐이다.
import { Screen, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import { apiGet, apiPost, ApiError } from '@/shared/api/client';
import { useAuth } from '@/shared/auth/AuthContext';
import { loginWithKakao } from '@/features/auth/kakaoLogin';
import { loginWithApple, isAppleLoginAvailable } from '@/features/auth/appleLogin';
import { oauthNext, type OAuthResponse } from '@/features/auth/oauthResult';
import type { RootStackScreenProps } from '@/navigation/types';

export function WelcomeScreen({ navigation }: RootStackScreenProps<'Welcome'>) {
  // 모집중·혼밥중 카운트(사회적 증거). 비로그인 공개 통계라 로그인 전에도 호출 가능. 실패/로딩 시 '–'.
  const [counts, setCounts] = useState<{ seekingCount: number; activeCount: number } | null>(null);
  const { signIn } = useAuth();
  const [kakaoBusy, setKakaoBusy] = useState(false);
  // 더블탭 가드를 상태가 아니라 ref로 둔다. 상태는 리렌더가 돌아야 갱신돼, 같은 프레임에 두 번 누르면
  // 두 호출 모두 false를 읽고 통과한다. 카카오 버튼은 그래도 disabled prop이라는 뒷받침이 있지만,
  // 애플 네이티브 버튼에는 그런 prop 자체가 없어(onPress·buttonType·buttonStyle·cornerRadius·style이
  // 전부다) 이 가드가 유일한 방어선이다. ref는 동기적으로 읽고 써서 창이 아예 열리지 않는다.
  const appleBusy = useRef(false);
  // 애플 로그인 지원 여부. 비동기로만 알 수 있어 '아직 모름'이 별도의 상태로 필요하다 —
  // 모르는 동안 자리(48pt)를 비워 두면 나중에 버튼이 끼어들 때 위쪽이 통째로 밀려 올라간다(아래 참고).
  // ★안드로이드는 물어볼 일이 없으므로 처음부터 'unavailable'로 시작한다. 여기서 'unknown'으로
  //   두면 effect가 즉시 return해 영영 갱신되지 않아, 버튼이 뜰 리 없는 플랫폼에 빈 48pt가 남는다.
  const [appleAvailability, setAppleAvailability] = useState<'unknown' | 'available' | 'unavailable'>(
    Platform.OS === 'ios' ? 'unknown' : 'unavailable',
  );

  useEffect(() => {
    if (Platform.OS !== 'ios') return; // 초기값이 이미 'unavailable'이라 그대로 둔다
    let alive = true;
    isAppleLoginAvailable().then((ok) => {
      if (alive) setAppleAvailability(ok ? 'available' : 'unavailable');
    });
    return () => { alive = false; };
  }, []);

  useEffect(() => {
    let alive = true;
    apiGet<{ todayCount: number; activeCount: number; seekingCount: number }>('/check-ins/stats')
      .then((stats) => { if (alive) setCounts({ seekingCount: stats.seekingCount, activeCount: stats.activeCount }); })
      .catch(() => { /* 연결 실패 시 폴백('–') 유지 */ });
    return () => { alive = false; };
  }, []);

  // 카카오 로그인 → 서버 검증 → 신규면 프로필 설정, 기존이면 바로 입장.
  // 휴대폰 인증(VerifyCode.tsx)과 같은 분기 구조를 유지한다.
  const onKakao = async () => {
    if (kakaoBusy) return; // 더블탭 방지
    setKakaoBusy(true);
    try {
      const idToken = await loginWithKakao();
      if (idToken === null) return; // 사용자가 취소 — 알림 없이 조용히 복귀
      // token:null — 공개 엔드포인트(로그인 전)라 세션 토큰을 붙이지 않는다. 안 붙이면 세션 요청으로
      // 분류돼 401 시 refresh를 시도하다 실패해 세션 만료 처리(캐시 초기화)가 헛돌기 때문.
      const result = await apiPost<OAuthResponse>('/auth/oauth/kakao', { idToken }, { token: null });
      const next = oauthNext(result);
      if (next.kind === 'onboarding') {
        navigation.navigate('ProfileSetup', { onboardingToken: next.onboardingToken });
      } else if (next.kind === 'login') {
        await signIn(next.tokens);
      } else {
        Alert.alert('로그인 오류', '예상치 못한 응답입니다. 다시 시도해주세요.');
      }
    } catch (e) {
      // 사용자에게는 지금처럼 친절한 문구만 보여주되, 실제 원인(ID 토큰 미발급/SDK 오류/네트워크 등)은
      // 콘솔에 남겨 매번 시뮬레이터 시스템 로그를 뒤지지 않고도 원인을 바로 확인할 수 있게 한다.
      console.warn('[kakao] 로그인 실패', e);
      Alert.alert('로그인 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setKakaoBusy(false);
    }
  };

  // 애플 로그인 → 서버 검증 → 신규면 프로필 설정, 기존이면 바로 입장. onKakao와 같은 구조다.
  const onApple = async () => {
    if (appleBusy.current) return; // 더블탭 방지 — ref라 같은 프레임의 두 번째 탭도 막힌다
    appleBusy.current = true;
    try {
      const credential = await loginWithApple();
      if (credential === null) return; // 사용자가 취소 — 알림 없이 조용히 복귀
      // token:null — 카카오와 같은 이유(공개 엔드포인트라 세션 토큰을 붙이지 않는다).
      const result = await apiPost<OAuthResponse>(
        '/auth/oauth/apple',
        { idToken: credential.identityToken, authorizationCode: credential.authorizationCode },
        { token: null },
      );
      const next = oauthNext(result);
      if (next.kind === 'onboarding') {
        navigation.navigate('ProfileSetup', { onboardingToken: next.onboardingToken });
      } else if (next.kind === 'login') {
        await signIn(next.tokens);
      } else {
        Alert.alert('로그인 오류', '예상치 못한 응답입니다. 다시 시도해주세요.');
      }
    } catch (e) {
      console.warn('[apple] 로그인 실패', e);
      Alert.alert('로그인 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
    } finally {
      appleBusy.current = false;
    }
  };

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
            <Text style={styles.countNum}>{counts ? counts.seekingCount + counts.activeCount : '–'}</Text>
            <Text style={styles.countUnit}>명 혼밥 중</Text>
          </View>
          {(counts?.seekingCount ?? 0) > 0 ? (
            <Text style={styles.countSub}>· 그 중 {counts?.seekingCount}명은 같이 먹을 사람 찾는 중</Text>
          ) : null}
        </View>

        {/* 하단 CTA */}
        <View style={styles.cta}>
          {/* 애플 — 카카오보다 위에 둔다. HIG가 Sign in with Apple을 다른 로그인 수단보다
              덜 눈에 띄게 배치하지 말라고 요구한다. iOS 13+에서만 뜬다.
              ★애플 공식 컴포넌트(AppleAuthenticationButton)를 쓰지 않고 직접 그린다.
              그 컴포넌트는 글자 크기를 버튼 높이의 43%로 스스로 정하고(prop이 없다 —
              buttonStyle·buttonType·cornerRadius·onPress·style이 전부다), 카카오 규격은
              레이블을 높이의 1/3 이하로 요구한다. 두 비율이 겹치지 않아서, 공식 컴포넌트를
              쓰는 한 같은 높이에서 두 버튼의 글자 크기는 절대 같아질 수 없다(실측: 높이 48에서
              애플 20.6pt / 카카오 15pt). 그래서 카카오 버튼과 같은 styles.btn·styles.btnText로
              그려 타이포를 하나로 맞춘다.
              애플이 강제하는 나머지는 그대로 지킨다 — 공식 로고 모양(Icon 'apple'), 승인된 문구
              ("Apple로 계속하기"는 공식 컴포넌트가 CONTINUE에 쓰던 한국어 문구 그대로),
              검정 배경·흰 로고/글자(C.apple·C.appleText), 모서리 12(0~높이/2 허용), 높이 48
              (최소 30 이상), 버튼 둘레 여백(좌우 28·위 28·아래 8 모두 높이/10=4.8 초과). */}
          {appleAvailability === 'available' ? (
            <Pressable
              style={[styles.btn, { backgroundColor: C.apple }]}
              onPress={onApple}
              accessibilityRole="button"
              accessibilityLabel="Apple로 계속하기"
            >
              <Icon name="apple" size={18} color={C.appleText} />
              <Text style={[styles.btnText, { color: C.appleText }]}>Apple로 계속하기</Text>
            </Pressable>
          ) : appleAvailability === 'unknown' ? (
            // 지원 여부를 묻는 동안 자리만 잡아 둔다(아무것도 그리지 않는 빈 View).
            // cta가 flex 컬럼 맨 아래에 있어서, 뒤늦게 버튼이 끼어들면 위쪽 카운터 블록이
            // 56pt(버튼 48 + gap 8)만큼 솟구친다 — 앱을 새로 켤 때마다 첫 화면이 덜컥이고,
            // 하필 그 화면이 심사자가 앱에서 처음 보는 장면이다. 같은 크기를 미리 비워 두면
            // 버튼이 그 자리에 그대로 들어와 아무것도 움직이지 않는다.
            // 미리 진짜 버튼을 그려 두지 않는 이유: isAppleLoginAvailable()이 false로 답하면
            // 버튼이 떴다가 사라지는 게 되고, "resolves true일 때만 노출" 조건도 깨진다.
            <View style={styles.appleBtn} />
          ) : null}
          {/* 카카오 — 대표 소셜 로그인 */}
          <Pressable
            style={[styles.btn, { backgroundColor: C.kakao }]}
            onPress={onKakao}
            disabled={kakaoBusy}
          >
            <Icon name="kakao" size={18} color={C.kakaoText} />
            <Text style={[styles.btnText, { color: C.kakaoText }]}>카카오로 계속하기</Text>
          </Pressable>
          {/* 휴대폰 — TestFlight 배포 동안 숨김.
              mock SMS는 인증번호가 "000000" 고정이라, 공개된 서버에 이 경로를 열어두면
              전화번호만 알면 타인 계정으로 로그인된다. 실 SMS 게이트웨이를 붙이는
              슬라이스에서 되살릴 것. PhoneAuth·VerifyCode 화면과 라우트는 그대로 둔다. */}
          {/* <Pressable
            style={[styles.btn, { backgroundColor: T2.brand }]}
            onPress={() => navigation.navigate('PhoneAuth')}
          >
            <Icon name="phone" size={16} color="#fff" />
            <Text style={[styles.btnText, { color: '#fff' }]}>휴대폰 번호로 계속하기</Text>
          </Pressable> */}
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
  countSub: { fontSize: 15, fontWeight: '600', color: T2.textSub, letterSpacing: -0.3, marginTop: 6, paddingLeft: 18 },

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
  // 애플 버튼이 뜰 자리를 지원 여부 확인 동안 비워 두는 자리표시자 전용 스타일.
  // 48 = 실제 버튼(styles.btn)의 높이: paddingVertical 15×2 + 내용 한 줄 18. 내용 높이를 정하는 건
  // 텍스트가 아니라 아이콘이다 — Icon이 <Svg height={18}>로 딱 18을 차지하고, 15pt 한 줄 텍스트는
  // 그보다 낮아서(≈17.9) 행 높이에 영향을 주지 않는다. 그래서 폰트 메트릭에 기대지 않고 확정된다.
  // ★이 값이 styles.btn의 실제 높이와 어긋나면 버튼이 들어올 때 화면이 그 차이만큼 튄다 —
  //   자리표시자를 두는 목적 자체가 사라진다. btn의 padding이나 아이콘 크기를 바꾸면 같이 고칠 것.
  appleBtn: { height: 48, width: '100%' },
});
