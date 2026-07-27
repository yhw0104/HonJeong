// TermsList — 약관 및 정책 목록. 더보기 '설정'에서 진입해 각 문서를 TermsView로 연다.
// 가입 후에도 약관·개인정보처리방침을 다시 볼 수 있어야 해서 만든 화면
// (이전에는 온보딩(ProfileSetup)에서만 열람 가능했다).
import React from 'react';
import { Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { termsListItems } from '../withdrawApi';
import type { RootStackScreenProps } from '@/navigation/types';

export function TermsListScreen({ navigation }: RootStackScreenProps<'TermsList'>) {
  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="약관 및 정책" onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll}>
        {termsListItems().map((item) => (
          <Pressable
            key={item.key}
            style={styles.row}
            onPress={() => navigation.navigate('TermsView', { termKey: item.key })}
            accessibilityRole="button"
          >
            <Text style={styles.rowLabel}>{item.title}</Text>
            <Icon name="chevronRight" size={16} color={T2.textMute} />
          </Pressable>
        ))}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 32 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 18,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  rowLabel: { fontSize: 15, color: T2.text, letterSpacing: -0.3 },
});
