// RootNavigator — 인증 상태에 따라 온보딩(guest) 또는 메인(authed) 화면 그룹을 보여주는 루트 스택.
// 로그인/로그아웃으로 status가 바뀌면 React Navigation이 자동으로 해당 그룹의 첫 화면으로 전환한다.
import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { RootStackParamList } from './types';
import { useAuth } from '@/shared/auth/AuthContext';
import { WelcomeScreen } from '@/features/auth/screens/Welcome';
import { PhoneAuthScreen } from '@/features/auth/screens/PhoneAuth';
import { VerifyCodeScreen } from '@/features/auth/screens/VerifyCode';
import { ProfileSetupScreen } from '@/features/auth/screens/ProfileSetup';
import { TermsViewScreen } from '@/features/auth/screens/TermsView';
import { NewGroupScreen } from '@/features/favorites/screens/NewGroup';
import { RestaurantDetailScreen } from '@/features/place/screens/RestaurantDetail';
import { PlaceSearchScreen } from '@/features/place/screens/PlaceSearch';
import { MealRequestScreen } from '@/features/mate/screens/MealRequest';
import { MatesScreen } from '@/features/mate/screens/Mates';
import { MateProfileScreen } from '@/features/mate/screens/MateProfile';
import { ReceivedRequestsScreen } from '@/features/mate/screens/ReceivedRequests';
import { DiningHistoryScreen } from '@/features/record/screens/DiningHistory';
import { MyReviewsScreen } from '@/features/review/screens/MyReviews';
import { DiningLogWriteScreen } from '@/features/record/screens/DiningLogWrite';
import { ChallengeBadgesScreen } from '@/features/record/screens/ChallengeBadges';
import { MyProfileScreen } from '@/features/profile/screens/MyProfile';
import { ProfileEditScreen } from '@/features/profile/screens/ProfileEdit';
import { NotificationSettingsScreen } from '@/features/settings/screens/NotificationSettings';
import { NotificationsScreen } from '@/features/notifications/screens/Notifications';
import { NoticesScreen } from '@/features/settings/screens/Notices';
import { BlockReportScreen } from '@/features/settings/screens/BlockReport';
import { SupportScreen } from '@/features/settings/screens/Support';
import { ReportFormScreen } from '@/features/safety/screens/ReportForm';
import { MainTabs } from './MainTabs';

const Stack = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  const { status } = useAuth();

  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      {status === 'guest' ? (
        // 미로그인 — 온보딩 흐름
        <>
          <Stack.Screen name="Welcome" component={WelcomeScreen} />
          <Stack.Screen name="PhoneAuth" component={PhoneAuthScreen} />
          <Stack.Screen name="VerifyCode" component={VerifyCodeScreen} />
          <Stack.Screen name="ProfileSetup" component={ProfileSetupScreen} />
          <Stack.Screen name="TermsView" component={TermsViewScreen} />
        </>
      ) : (
        // 로그인됨 — 메인 탭 + 세부 화면들
        <>
          <Stack.Screen name="MainTabs" component={MainTabs} />
          <Stack.Screen name="NewGroup" component={NewGroupScreen} options={{ presentation: 'modal' }} />
          <Stack.Screen name="RestaurantDetail" component={RestaurantDetailScreen} />
          <Stack.Screen name="PlaceSearch" component={PlaceSearchScreen} />
          <Stack.Screen name="MealRequest" component={MealRequestScreen} options={{ presentation: 'modal' }} />
          <Stack.Screen name="Mates" component={MatesScreen} />
          <Stack.Screen name="MateProfile" component={MateProfileScreen} />
          <Stack.Screen name="ReceivedRequests" component={ReceivedRequestsScreen} />
          <Stack.Screen name="DiningHistory" component={DiningHistoryScreen} />
          <Stack.Screen name="MyReviews" component={MyReviewsScreen} />
          <Stack.Screen name="ChallengeBadges" component={ChallengeBadgesScreen} />
          <Stack.Screen name="MyProfile" component={MyProfileScreen} />
          <Stack.Screen name="DiningLogWrite" component={DiningLogWriteScreen} options={{ presentation: 'modal' }} />
          <Stack.Screen name="ProfileEdit" component={ProfileEditScreen} options={{ presentation: 'modal' }} />
          <Stack.Screen name="NotificationSettings" component={NotificationSettingsScreen} />
          <Stack.Screen name="Notifications" component={NotificationsScreen} />
          <Stack.Screen name="Notices" component={NoticesScreen} />
          <Stack.Screen name="BlockReport" component={BlockReportScreen} />
          <Stack.Screen name="ReportForm" component={ReportFormScreen} options={{ presentation: 'modal' }} />
          <Stack.Screen name="Support" component={SupportScreen} />
        </>
      )}
    </Stack.Navigator>
  );
}
