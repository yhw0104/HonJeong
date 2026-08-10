// 리뷰 본문 입력 블록(라벨 + 둥근 상자 + 글자 수). 혼밥 리뷰 화면과 일반 리뷰 화면이 공유한다.
//
// 두 화면이 같은 모양이어야 하므로 한 곳에 둔다 — 실기 테스트에서 지적된 키보드 처리도
// 여기 들어 있다(아래 useEffect 참조).
import React, { useEffect, useState } from 'react';
import { Keyboard, View, Text, TextInput, ScrollView, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';

/**
 * 본문 최대 길이.
 *
 * ★서버 `ReviewCreateRequest`·`ReviewUpdateRequest`의 `@Size(max = 1000)`와 같아야 한다 —
 * 넘겨 보내면 400으로 튕긴다. DB `reviews.content`는 TEXT라 제한이 없어서, 실질 상한은 이 검증값이다.
 */
export const MAX_CONTENT = 1000;

type Props = {
  value: string;
  onChangeText: (v: string) => void;
  /**
   * 이 블록을 담고 있는 ScrollView. 포커스 시 끝까지 스크롤하는 데 쓴다.
   * 본문 상자가 화면의 **마지막 내용**이라는 전제가 깔려 있다.
   */
  scrollRef: React.RefObject<ScrollView | null>;
};

export function ReviewBodyInput({ value, onChangeText, scrollRef }: Props) {
  const [focused, setFocused] = useState(false);

  // 본문에 커서를 두면 상자 **전체**가 키보드 위로 오게 끝까지 스크롤한다.
  //
  // automaticallyAdjustKeyboardInsets만으로는 모자란다: 여러 줄 입력에서 RN이 기준으로 삼는 건
  // 상자가 아니라 '커서가 놓인 줄'이라(RCTTextInputComponentView가 selectionRect를 넘긴다),
  // 빈 상자에 처음 커서를 두면 맨 윗줄만 올라오고 상자 아랫부분은 키보드에 덮인 채 남는다.
  //
  // keyboardDidShow를 듣는 이유: 포커스 시점엔 아직 키보드 여백이 안 붙어서 끝까지 못 간다.
  // 이미 키보드가 올라와 있는 상태에서 옮겨온 경우(이벤트가 안 옴)를 위해 즉시 한 번도 부른다.
  useEffect(() => {
    if (!focused) return;
    const toEnd = () => scrollRef.current?.scrollToEnd({ animated: true });
    const shown = Keyboard.addListener('keyboardDidShow', toEnd);
    toEnd();
    return () => shown.remove();
  }, [focused, scrollRef]);

  return (
    <View style={{ marginTop: 28 }}>
      <Text style={styles.label}>이 곳은 어땠나요</Text>
      <View style={[styles.box, focused && styles.boxOn]}>
        <TextInput
          style={styles.input}
          value={value}
          onChangeText={onChangeText}
          multiline
          maxLength={MAX_CONTENT}
          // 라벨이 이미 묻고 있으니 placeholder는 짧게 받는다 — 둘 다 길면 같은 말을 두 번 한다.
          placeholder="자유롭게 적어주세요"
          placeholderTextColor={T2.textMute}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
        />
      </View>
      {/* 남은 분량을 눈으로 알 수 있게. maxLength가 막아 주지만 '왜 안 써지지'가 되지 않도록 보여준다. */}
      <Text style={[styles.counter, value.length >= MAX_CONTENT && styles.counterFull]}>
        {value.length} / {MAX_CONTENT}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  label: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5 },
  box: {
    marginTop: 12,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: T2.border,
    backgroundColor: '#fff',
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  boxOn: { borderColor: T2.brand },
  input: {
    fontSize: 15,
    color: T2.text,
    lineHeight: 24,
    letterSpacing: -0.3,
    textAlignVertical: 'top',
    minHeight: 132,
    padding: 0,
  },
  // tabular-nums — 자릿수가 바뀔 때 글자 폭이 흔들리지 않게.
  counter: { marginTop: 8, alignSelf: 'flex-end', fontSize: 12, color: T2.textMute, fontVariant: ['tabular-nums'] },
  counterFull: { color: T2.brand, fontWeight: '700' },
});
