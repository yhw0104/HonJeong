// MyReviews — 내가 쓴 리뷰(인증+일반 전체). 더보기 '내가 쓴 리뷰'에서 진입.
import React from 'react';
import { ScrollView } from 'react-native';
import { Screen, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

export function MyReviewsScreen({ navigation }: RootStackScreenProps<'MyReviews'>) {
  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="내가 쓴 리뷰" onBack={() => navigation.goBack()} />
      <ScrollView />
    </Screen>
  );
}
