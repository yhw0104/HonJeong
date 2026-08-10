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
  /**
   * 혼밥 인증이 아닌 리뷰 작성/수정(같이먹기 후 · 체크인 없이). 혼밥 별점·태그를 묻지 않는다.
   *
   * checkInId가 없는 게 핵심이다 — 있으면 인증 리뷰이고 그건 DiningLogWrite가 담당한다.
   */
  ReviewWrite: {
    placeId: number;
    placeName: string;
    reviewId?: number;
    initial?: {
      taste: number;
      content: string;
      photos?: string[];
      /**
       * 화면에 띄우지 않고 그대로 되돌려 보낼 값. 2026-08-10 이전에 쓰인 인증 없는 리뷰에는
       * 혼밥 별점·태그가 남아 있는데(과거 데이터 보존), 수정이 받은 값으로 덮어쓰기 때문에
       * 들고 있지 않으면 지워진다.
       */
      keepSolo?: { honbab: number | null; tags: string[] };
    };
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
