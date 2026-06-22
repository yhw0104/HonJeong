// 네비게이션 라우트 파라미터 타입 — 화면 간 타입 안전한 이동에 사용.
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import type { CompositeScreenProps } from '@react-navigation/native';

export type RootStackParamList = {
  Welcome: undefined;
  PhoneAuth: undefined;
  VerifyCode: { phone: string };
  ProfileSetup: { onboardingToken: string };
  MainTabs: undefined;
  NewGroup: undefined;
  PlaceSearch: undefined;
  RestaurantDetail: { placeId?: number; name?: string };
  MealRequest: { name?: string } | undefined;
  Mates: undefined;
  MateProfile: { name?: string } | undefined;
  ReceivedRequests: undefined;
  DiningHistory: undefined;
  DiningLogWrite: undefined;
  ChallengeBadges: undefined;
  MyProfile: undefined;
  ProfileEdit: undefined;
  NotificationSettings: undefined;
  Notices: undefined;
  BlockReport: undefined;
  Support: undefined;
};

export type MainTabParamList = {
  MapHome: undefined;
  TogetherFeed: undefined;
  Favorites: undefined;
  More: undefined;
};

export type RootStackScreenProps<T extends keyof RootStackParamList> = NativeStackScreenProps<
  RootStackParamList,
  T
>;

export type MainTabScreenProps<T extends keyof MainTabParamList> = CompositeScreenProps<
  BottomTabScreenProps<MainTabParamList, T>,
  RootStackScreenProps<keyof RootStackParamList>
>;

declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
