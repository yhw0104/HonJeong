// TermsView — 약관 전문 보기. 온보딩 약관동의(ProfileSetup)에서 각 항목 '보기'로 진입.
import React from 'react';
import { ScrollView, Text, StyleSheet } from 'react-native';
import { Screen, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { TERMS_CONTENT } from '@/features/auth/termsContent';

export function TermsViewScreen({ navigation, route }: RootStackScreenProps<'TermsView'>) {
  const doc = TERMS_CONTENT[route.params.termKey];
  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title={doc?.title ?? '약관'} onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll}>
        <Text style={styles.body}>{doc?.body ?? '내용을 준비 중이에요.'}</Text>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { padding: 20, paddingBottom: 48 },
  body: { fontSize: 13, lineHeight: 22, color: T2.textSub, letterSpacing: -0.2 },
});
