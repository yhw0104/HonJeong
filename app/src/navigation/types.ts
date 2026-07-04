// 네비게이션 라우트 파라미터 타입 — 화면 간 타입 안전한 이동에 사용.
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import type { CompositeScreenProps, NavigatorScreenParams } from '@react-navigation/native';

export type RootStackParamList = {
  Welcome: undefined;
  PhoneAuth: undefined;
  VerifyCode: { phone: string };
  ProfileSetup: { onboardingToken: string };
  MainTabs: NavigatorScreenParams<MainTabParamList> | undefined;
  NewGroup: { groupId?: number; initial?: { name: string; note: string; color: string } } | undefined;
  PlaceSearch: undefined;
  RestaurantDetail: { placeId: number; name?: string };
  MealRequest: { placeId: number; placeName: string };
  Mates: undefined;
  MateProfile: { userId: number };
  ReceivedRequests: undefined;
  DiningHistory: undefined;
  DiningLogWrite: {
    placeId: number;
    placeName: string;
    checkInId?: number;
    reviewId?: number;
    initial?: { taste: number; honbab: number; tags: string[]; content: string; photos?: string[] };
  };
  ChallengeBadges: undefined;
  MyProfile: undefined;
  ProfileEdit: undefined;
  NotificationSettings: undefined;
  Notifications: undefined;
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
