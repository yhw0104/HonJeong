// ProfileSetup — 프로필 완성 03/03 (원본: screens/ProfileSetup.jsx)
// 커스텀 폼 컨트롤을 Pressable 토글 + TextInput으로 구현. 음식은 최대 3개 다중선택.
import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet, Alert, Modal } from 'react-native';
import { Screen, StepProgress, CTAButton, FieldLabel, Icon, Avatar } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { apiGet, apiPost, ApiError } from '@/shared/api/client';
import { pickImages, uploadImages } from '@/shared/upload/imageUpload';
import { useAuth } from '@/shared/auth/AuthContext';
import { NICKNAME_MAX, NICK_HINT, canSubmitNickname, precheckNickname, type NickStatus } from '@/features/auth/nickname';
import { type Birth, daysInMonth, isAtLeast14, formatBirth, toIsoDate, clampDay } from '@/features/auth/birthdate';
import type { RootStackScreenProps } from '@/navigation/types';

const FOODS = ['한식', '일식', '양식', '중식', '면 요리', '매운맛', '디저트'];
const STYLES_OPT = [
  { key: 'talk', label: '도란도란 대화하며', sub: '가볍게 이야기 나누는 게 좋아요' },
  { key: 'quiet', label: '조용히 각자', sub: '편하게, 말 없이 먹어도 좋아요' },
];

// 생년월일 피커 범위: 최소 90세~최대 만14세(연 기준). 실제 만14 판정은 제출 시 isAtLeast14로 한다.
const NOW_Y = new Date().getFullYear();
const MIN_YEAR = NOW_Y - 90;
const MAX_YEAR = NOW_Y - 14;
const YEARS = Array.from({ length: MAX_YEAR - MIN_YEAR + 1 }, (_, i) => MAX_YEAR - i); // 최신 연도 먼저
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1);

type Term = { key: string; label: string; req: boolean; detail?: boolean };
// 약관 키를 백엔드 /auth/terms 필드명과 동일하게 맞춘다(그대로 전송). detail=true면 '보기'로 전문을 연다.
const TERMS: Term[] = [
  { key: 'age', label: '만 14세 이상입니다', req: true },
  { key: 'service', label: '서비스 이용약관', req: true, detail: true },
  { key: 'privacy', label: '개인정보 처리방침', req: true, detail: true },
  { key: 'location', label: '위치정보 이용 동의', req: true, detail: true },
  { key: 'marketing', label: '마케팅 알림 수신', req: false, detail: true },
];

export function ProfileSetupScreen({ navigation, route }: RootStackScreenProps<'ProfileSetup'>) {
  const { onboardingToken } = route.params;
  const { signIn } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [nickname, setNickname] = useState('');
  // 빈 값으로 시작한다 — 예시 문장을 초기값으로 넣으면 placeholder처럼 보이지만 실제로는 '값'이라,
  // 직접 쓰려면 먼저 지워야 하고 그냥 두면 남의 문장이 내 소개로 저장된다(목업에서 넘어온 흔적).
  const [intro, setIntro] = useState('');
  const [gender, setGender] = useState<'female' | 'male'>('female');
  const [foods, setFoods] = useState<string[]>([]);
  const [style, setStyle] = useState('talk');
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [birth, setBirth] = useState<Birth | null>(null);
  const [birthPickerOpen, setBirthPickerOpen] = useState(false);
  const [draft, setDraft] = useState<Birth>({ y: MAX_YEAR - 6, m: 1, d: 1 }); // 피커 임시값(확인 시 반영)
  const [terms, setTerms] = useState<Record<string, boolean>>({
    age: false,
    service: false,
    privacy: false,
    location: false,
    marketing: false,
  });

  const allChecked = TERMS.every((t) => terms[t.key]);
  const requiredOk = TERMS.filter((t) => t.req).every((t) => terms[t.key]);
  const toggleAllTerms = () => setTerms(Object.fromEntries(TERMS.map((t) => [t.key, !allChecked])));
  const toggleTerm = (k: string) => setTerms((p) => ({ ...p, [k]: !p[k] }));

  // 닉네임 실시간 중복 확인. 입력이 멈추면(디바운스 400ms) nickname-check를 호출한다.
  // 2자 미만은 확인하지 않고(idle), 온보딩 토큰으로 호출한다(아직 정식 로그인 전).
  const [nickStatus, setNickStatus] = useState<NickStatus>('idle');

  useEffect(() => {
    const pre = precheckNickname(nickname);
    if (pre.action === 'set') {
      setNickStatus(pre.status); // 2자 미만=idle, 자음/모음 낱자만=invalid → 서버 확인 없음
      return;
    }
    const trimmed = nickname.trim();
    setNickStatus('checking');
    const handle = setTimeout(async () => {
      try {
        const res = await apiGet<{ nickname: string; available: boolean }>(
          `/users/nickname-check?nickname=${encodeURIComponent(trimmed)}`,
          { token: onboardingToken },
        );
        setNickStatus(res.available ? 'available' : 'taken');
      } catch {
        setNickStatus('error');
      }
    }, 400);
    return () => clearTimeout(handle); // 다음 입력이 오면 이전 예약을 취소(디바운스)
  }, [nickname, onboardingToken]);

  const toggleFood = (f: string) => {
    setFoods((prev) => {
      if (prev.includes(f)) return prev.filter((x) => x !== f);
      if (prev.length >= 3) return prev;
      return [...prev, f];
    });
  };

  // 프로필 사진(선택): 갤러리 1장 선택 → POST /api/files(아직 로그인 전이라 온보딩 토큰) 업로드 → 미리보기.
  const onPickPhoto = async () => {
    if (uploadingPhoto) return;
    const picked = await pickImages(1);
    if (picked.length === 0) return;
    setUploadingPhoto(true);
    try {
      const [url] = await uploadImages([picked[0].uri], onboardingToken);
      setImageUrl(url);
    } catch {
      Alert.alert('업로드 실패', '사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
    } finally {
      setUploadingPhoto(false);
    }
  };

  // 약관 제출 → 프로필 제출(가입 확정 + 정식 토큰) → 로그인(네비게이터가 메인으로 전환).
  const onComplete = async () => {
    if (!requiredOk) {
      Alert.alert('약관 동의 필요', '필수 약관에 모두 동의해주세요.');
      return;
    }
    // 생년월일 미선택은 CTA 비활성으로 이미 막힌다 — !birth는 타입 좁히기 겸 백스톱이라 별도 안내를 두지 않는다.
    if (!birth || !isAtLeast14(birth, new Date())) {
      Alert.alert('생년월일 확인', '만 14세 이상만 가입할 수 있어요.');
      return;
    }
    setSubmitting(true);
    try {
      await apiPost(
        '/auth/terms',
        { age: terms.age, service: terms.service, privacy: terms.privacy, location: terms.location, marketing: terms.marketing },
        { token: onboardingToken },
      );
      const tokens = await apiPost<{ accessToken: string; refreshToken: string }>(
        '/auth/complete',
        {
          nickname,
          gender: gender === 'female' ? 'FEMALE' : 'MALE',
          diningStyle: style === 'talk' ? 'TALK' : 'QUIET',
          introduction: intro,
          // region은 보내지 않는다 — '내 동네' 기능 제거 결정(2026-07-04). 서버 필드는 선택이라 생략 가능.
          favoriteFoods: foods,
          profileImageUrl: imageUrl ?? undefined,
          birthDate: toIsoDate(birth),
        },
        { token: onboardingToken },
      );
      await signIn(tokens);
    } catch (e) {
      Alert.alert('가입 완료 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Screen bg={T2.bg}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.backArrow}>←</Text>
        </Pressable>
      </View>

      {/* automaticallyAdjustKeyboardInsets — 아래쪽 입력칸이 키보드에 가리지 않게(DiningLogWrite와 같은 이유). */}
      <ScrollView
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        automaticallyAdjustKeyboardInsets
      >
        <StepProgress step={3} />

        <Text style={styles.h1}>프로필을{'\n'}완성해주세요</Text>
        <Text style={styles.lead}>
          <Text style={{ color: T2.text, fontWeight: '700' }}>같이 먹기</Text>를 신청하거나 받을 때, 상대에게 보여지는 정보예요.
        </Text>

        {/* 프로필 사진 (선택) */}
        <Pressable style={styles.photoRow} onPress={onPickPhoto} disabled={uploadingPhoto}>
          <View>
            <Avatar uri={imageUrl} size={72} bg={T2.border} />
            {/* 프로필 편집(ProfileEdit) 화면의 사진 배지와 동일 — 이모지 대신 카메라 아이콘, 배지 크기도 같게. */}
            <View style={styles.cameraBadge}>
              <Icon name="camera" size={15} color="#fff" />
            </View>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.photoTitle}>
              {uploadingPhoto ? '업로드 중…' : imageUrl ? '프로필 사진 변경' : '프로필 사진 추가'}
            </Text>
            <Text style={styles.photoSub}>상대에게 보여지는 정보예요.{'\n'}얼굴 사진이면 더 좋아요.</Text>
          </View>
        </Pressable>

        {/* 닉네임 */}
        <View style={{ marginTop: 32 }}>
          <FieldLabel>닉네임</FieldLabel>
          <View style={styles.nickRow}>
            <TextInput
              style={styles.nickInput}
              value={nickname}
              onChangeText={setNickname}
              maxLength={NICKNAME_MAX}
              placeholder="닉네임"
              placeholderTextColor={T2.textMute}
            />
            {nickStatus !== 'idle' && (
              <Text style={[styles.nickStatusText, { color: NICK_HINT[nickStatus].color }]}>
                {NICK_HINT[nickStatus].text}
              </Text>
            )}
          </View>
          <Text style={styles.hint}>한글·영문 2–10자 · 언제든 변경 가능</Text>
        </View>

        {/* 성별 · 연령대 */}
        <View style={styles.twoCol}>
          <View style={{ flex: 1 }}>
            <FieldLabel>성별</FieldLabel>
            <View style={{ flexDirection: 'row', gap: 6 }}>
              {[
                { k: 'female', l: '여성' },
                { k: 'male', l: '남성' },
              ].map((g) => {
                const on = gender === g.k;
                return (
                  <Pressable
                    key={g.k}
                    onPress={() => setGender(g.k as 'female' | 'male')}
                    style={[
                      styles.segment,
                      { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border },
                    ]}
                  >
                    <Text style={{ fontSize: 14, fontWeight: '700', color: on ? '#fff' : T2.textMute }}>{g.l}</Text>
                  </Pressable>
                );
              })}
            </View>
          </View>
          <View style={{ flex: 1 }}>
            <FieldLabel>생년월일</FieldLabel>
            <Pressable
              style={styles.dropdown}
              onPress={() => { setDraft(birth ?? { y: MAX_YEAR - 6, m: 1, d: 1 }); setBirthPickerOpen(true); }}
            >
              <Text style={{ fontSize: 14, fontWeight: '700', color: birth ? T2.text : T2.textMute, letterSpacing: -0.3 }}>
                {birth ? formatBirth(birth) : '선택'}
              </Text>
              <Icon name="chevronDown" size={11} color={T2.textMute} />
            </Pressable>
            {/* 필수인데 버튼만 비활성이면 이유를 알 수 없다 — 닉네임 힌트와 같은 자리에 이유를 적는다. */}
            <Text style={styles.hint}>필수 · 만 14세 이상</Text>
          </View>
        </View>

        {/* 한 줄 소개 */}
        <View style={{ marginTop: 28 }}>
          <FieldLabel>한 줄 소개</FieldLabel>
          <TextInput
            style={styles.introInput}
            value={intro}
            onChangeText={setIntro}
            multiline
            maxLength={60}
            placeholder="나를 한 줄로 소개해보세요"
            placeholderTextColor={T2.textMute}
          />
        </View>

        {/* 좋아하는 음식 */}
        <View style={{ marginTop: 28 }}>
          <FieldLabel>
            좋아하는 음식 <Text style={{ color: T2.textMute, fontWeight: '400' }}>· 최대 3개</Text>
          </FieldLabel>
          <View style={styles.chips}>
            {FOODS.map((f) => {
              const on = foods.includes(f);
              return (
                <Pressable
                  key={f}
                  onPress={() => toggleFood(f)}
                  style={[
                    styles.chip,
                    { backgroundColor: on ? T2.brand : '#fff', borderColor: on ? T2.brand : T2.border },
                  ]}
                >
                  <Text style={{ fontSize: 13, fontWeight: '600', color: on ? '#fff' : T2.text }}>{f}</Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {/* 같이 먹을 때 */}
        <View style={{ marginTop: 28 }}>
          <FieldLabel>같이 먹을 때</FieldLabel>
          {STYLES_OPT.map((r) => {
            const on = style === r.key;
            return (
              <Pressable
                key={r.key}
                onPress={() => setStyle(r.key)}
                style={[
                  styles.radioRow,
                  { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border },
                ]}
              >
                <View style={[styles.radio, { borderColor: on ? T2.brand : T2.borderStrong, backgroundColor: on ? T2.brand : 'transparent' }]}>
                  {on ? <View style={styles.radioInner} /> : null}
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={{ fontSize: 15, fontWeight: '700', color: on ? '#fff' : T2.text, letterSpacing: -0.3 }}>{r.label}</Text>
                  <Text style={{ fontSize: 12, color: on ? 'rgba(255,255,255,0.6)' : T2.textMute, marginTop: 1 }}>{r.sub}</Text>
                </View>
              </Pressable>
            );
          })}
        </View>

        {/* 인증 배지 */}
        <View style={styles.verifyBadge}>
          <View style={styles.verifyIcon}>
            <Text style={{ color: '#fff', fontSize: 15, fontWeight: '800' }}>✓</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={{ fontSize: 13, fontWeight: '700', color: T2.text, letterSpacing: -0.3 }}>휴대폰 인증 완료</Text>
            <Text style={{ fontSize: 12, color: T2.textSub, marginTop: 1, letterSpacing: -0.2 }}>상대에게 '인증된 사용자'로 표시돼요</Text>
          </View>
        </View>

        {/* 약관 동의 — 신규 가입 시에만(이 화면은 신규 회원만 도달) */}
        <View style={styles.termsWrap}>
          <Pressable style={styles.allRow} onPress={toggleAllTerms}>
            <View style={[styles.allCheck, { backgroundColor: allChecked ? T2.text : '#fff', borderColor: allChecked ? T2.text : T2.borderStrong }]}>
              <Text style={styles.checkMark}>✓</Text>
            </View>
            <Text style={styles.allText}>약관에 모두 동의</Text>
          </Pressable>
          {TERMS.map((t) => {
            const on = terms[t.key];
            return (
              <Pressable key={t.key} style={styles.termRow} onPress={() => toggleTerm(t.key)}>
                <View
                  style={[
                    styles.termCheck,
                    { backgroundColor: on ? T2.brand : 'transparent', borderWidth: on ? 0 : 1.5, borderColor: T2.borderStrong },
                  ]}
                >
                  {on ? <Text style={styles.termMark}>✓</Text> : null}
                </View>
                <Text style={styles.termLabel}>
                  <Text style={[styles.termTag, { color: t.req ? T2.text : T2.textMute }]}>{t.req ? '필수 ' : '선택 '}</Text>
                  {t.label}
                </Text>
                {t.detail ? (
                  <Pressable hitSlop={8} onPress={() => navigation.navigate('TermsView', { termKey: t.key })} accessibilityRole="button">
                    <Text style={styles.termView}>보기</Text>
                  </Pressable>
                ) : null}
              </Pressable>
            );
          })}
        </View>
      </ScrollView>

      {/* CTA */}
      <View style={styles.ctaWrap}>
        <CTAButton
          label="시작하기"
          // 닉네임은 중복확인 통과(available)까지 필요 — 1글자(idle)로 확인을 우회하는 구멍 차단.
          // 생년월일(birth)도 필수 — 미선택이면 버튼 자체를 막고, onComplete에서 한 번 더 확인한다.
          disabled={submitting || !requiredOk || !birth || !canSubmitNickname(nickname, nickStatus)}
          onPress={onComplete}
        />
      </View>

      {/* 생년월일 선택 바텀시트 — 연/월/일 3열 스크롤 */}
      <Modal visible={birthPickerOpen} transparent animationType="fade" onRequestClose={() => setBirthPickerOpen(false)}>
        <Pressable style={styles.ageBackdrop} onPress={() => setBirthPickerOpen(false)}>
          <Pressable style={styles.ageSheet} onPress={() => {}}>
            <Text style={styles.ageSheetTitle}>생년월일 선택</Text>
            <View style={styles.birthCols}>
              <BirthColumn data={YEARS} suffix="년" value={draft.y} onSelect={(y) => setDraft((p) => clampDay({ ...p, y }))} />
              <BirthColumn data={MONTHS} suffix="월" value={draft.m} onSelect={(m) => setDraft((p) => clampDay({ ...p, m }))} />
              <BirthColumn
                data={Array.from({ length: daysInMonth(draft.y, draft.m) }, (_, i) => i + 1)}
                suffix="일"
                value={draft.d}
                onSelect={(d) => setDraft((p) => ({ ...p, d }))}
              />
            </View>
            <Pressable style={styles.birthConfirm} onPress={() => { setBirth(draft); setBirthPickerOpen(false); }}>
              <Text style={styles.birthConfirmText}>확인</Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>
    </Screen>
  );
}

function BirthColumn({ data, suffix, value, onSelect }: {
  data: number[]; suffix: string; value: number; onSelect: (n: number) => void;
}) {
  return (
    <ScrollView style={styles.birthCol} showsVerticalScrollIndicator={false}>
      {data.map((n) => {
        const on = n === value;
        return (
          <Pressable key={n} style={styles.birthItem} onPress={() => onSelect(n)}>
            <Text style={[styles.birthItemText, on && { color: T2.brand, fontWeight: '800' }]}>{n}{suffix}</Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  header: { height: 56, justifyContent: 'center', paddingHorizontal: 20 },
  backArrow: { fontSize: 22, color: T2.text },
  scroll: { paddingHorizontal: 28, paddingBottom: 24 },

  h1: { fontSize: 30, fontWeight: '800', color: T2.text, letterSpacing: -1, lineHeight: 35 },
  lead: { fontSize: 14, color: T2.textSub, marginTop: 12, lineHeight: 21, letterSpacing: -0.3 },

  photoRow: { marginTop: 28, flexDirection: 'row', alignItems: 'center', gap: 16 },
  // ProfileEdit.cameraBadge와 같은 값(30·r15·보더3) — 두 화면의 사진 배지가 달라 보이지 않게 맞춘다.
  cameraBadge: {
    position: 'absolute',
    right: -2,
    bottom: -2,
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: T2.brand,
    borderWidth: 3,
    borderColor: T2.bg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  photoTitle: { fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  photoSub: { fontSize: 12, color: T2.textMute, marginTop: 2, lineHeight: 17 },

  nickRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingBottom: 12, borderBottomWidth: 2, borderBottomColor: T2.text },
  nickInput: { flex: 1, fontSize: 20, fontWeight: '700', color: T2.text, letterSpacing: -0.4, padding: 0 },
  nickStatusText: { fontSize: 12, fontWeight: '700' },
  hint: { marginTop: 8, fontSize: 11, color: T2.textMute },

  twoCol: { marginTop: 28, flexDirection: 'row', gap: 20 },
  segment: { flex: 1, paddingVertical: 11, borderRadius: 10, borderWidth: 1, alignItems: 'center' },
  dropdown: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 11,
    paddingHorizontal: 14,
    borderRadius: 10,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
  },

  ageBackdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  ageSheet: { backgroundColor: '#fff', borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingHorizontal: 20, paddingTop: 18, paddingBottom: 28 },
  ageSheetTitle: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3, marginBottom: 8 },

  birthCols: { flexDirection: 'row', gap: 8, height: 200, marginTop: 8 },
  birthCol: { flex: 1, backgroundColor: T2.bg, borderRadius: 12 },
  birthItem: { paddingVertical: 12, alignItems: 'center' },
  birthItemText: { fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  birthConfirm: { marginTop: 14, paddingVertical: 15, borderRadius: 12, backgroundColor: T2.brand, alignItems: 'center' },
  birthConfirmText: { fontSize: 15, fontWeight: '800', color: '#fff', letterSpacing: -0.3 },

  introInput: {
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
    fontSize: 14,
    color: T2.text,
    lineHeight: 21,
    letterSpacing: -0.3,
    minHeight: 60,
    textAlignVertical: 'top',
  },

  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { paddingVertical: 9, paddingHorizontal: 14, borderRadius: 999, borderWidth: 1 },

  radioRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14, paddingHorizontal: 16, borderRadius: 12, marginBottom: 8, borderWidth: 1 },
  radio: { width: 18, height: 18, borderRadius: 9, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  radioInner: { width: 6, height: 6, borderRadius: 3, backgroundColor: T2.text },

  verifyBadge: {
    marginTop: 16,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: 'rgba(255,90,31,0.06)',
    borderWidth: 1,
    borderColor: 'rgba(255,90,31,0.15)',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  verifyIcon: { width: 32, height: 32, borderRadius: 16, backgroundColor: T2.brand, alignItems: 'center', justifyContent: 'center' },

  ctaWrap: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 24, backgroundColor: T2.bg },

  termsWrap: { marginTop: 28 },
  allRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingBottom: 14, borderBottomWidth: 1, borderBottomColor: T2.border },
  allCheck: { width: 20, height: 20, borderRadius: 10, borderWidth: 1.5, alignItems: 'center', justifyContent: 'center' },
  checkMark: { color: '#fff', fontSize: 11, fontWeight: '800' },
  allText: { flex: 1, fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  termRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 11 },
  termCheck: { width: 16, height: 16, borderRadius: 8, alignItems: 'center', justifyContent: 'center' },
  termMark: { color: '#fff', fontSize: 9, fontWeight: '800' },
  termLabel: { flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.2 },
  termTag: { fontWeight: '700' },
  termView: { fontSize: 12, color: T2.textMute, fontWeight: '600', textDecorationLine: 'underline', letterSpacing: -0.2 },
});
