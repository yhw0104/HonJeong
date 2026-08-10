// 네비게이션 라우트 파라미터 타입 — 화면 간 타입 안전한 이동에 사용.
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import type { CompositeScreenProps, NavigatorScreenParams } from '@react-navigation/native';

export type RootStackParamList = {
  Welcome: undefined;
  PhoneAuth: undefined;
  VerifyCode: { phone: string };
  ProfileSetup: { onboardingToken: string };
  PushPermission: undefined;
  TermsView: { termKey: string };
  MainTabs: NavigatorScreenParams<MainTabParamList> | undefined;
  NewGroup: { groupId?: number; initial?: { name: string; note: string; color: string } } | undefined;
  PlaceSearch: undefined;
  RestaurantDetail: { placeId: number; name?: string };
  MealRequest: { placeId: number; placeName: string };
  ChatRoom: { conversationId: number };
  Mates: undefined;
  MateProfile: { userId: number };
  ReceivedRequests: undefined;
  DiningHistory: undefined;
  MyReviews: undefined;
  DiningLogWrite: {
    placeId: number;
    placeName: string;
    checkInId?: number;
    reviewId?: number;
    /** honbab은 혼밥 인증 리뷰만 값을 갖는다 — 인증 아닌 리뷰를 수정할 때는 null이 온다. */
    initial?: { taste: number; honbab: number | null; tags: string[]; content: string; photos?: string[] };
  };
  ChallengeBadges: undefined;
  MyProfile: undefined;
  ProfileEdit: undefined;
  NotificationSettings: undefined;
  Notifications: undefined;
  Notices: undefined;
  BlockReport: undefined;
  Support: undefined;
  ReportForm: { targetType: 'USER' | 'REVIEW'; targetId: number; targetNickname: string };
  TermsList: undefined;
  WithdrawAccount: undefined;
};

export type MainTabParamList = {
  MapHome: undefined;
  TogetherFeed: undefined;
  Chat: undefined;
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
