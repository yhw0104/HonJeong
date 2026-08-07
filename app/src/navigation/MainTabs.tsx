// MainTabs — 하단 탭(홈/같이먹기/즐겨찾기/더보기). 목업의 MinTabBar 디자인을 커스텀 tabBar로 구현.
import React from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';
import { T2 } from '@/shared/theme';
import type { MainTabParamList, RootStackParamList } from './types';
import { MapHomeScreen } from '@/features/home/screens/MapHome';
import { TogetherFeedScreen } from '@/features/together/screens/TogetherFeed';
import { ConversationListScreen } from '@/features/chat/screens/ConversationList';
import { FavoritesScreen } from '@/features/favorites/screens/Favorites';
import { MoreScreen } from '@/features/profile/screens/More';
import { useConversations } from '@/features/chat/queries';
import { totalUnread } from '@/features/chat/chatFormat';
import { hasPushPermission } from '@/shared/push';
import { hasSeenPushPrompt, shouldPromptPush } from '@/shared/push/prompt';

const Tab = createBottomTabNavigator<MainTabParamList>();

const LABELS: Record<keyof MainTabParamList, string> = {
  MapHome: '홈',
  TogetherFeed: '같이먹기',
  Chat: '대화',
  Favorites: '즐겨찾기',
  More: '더보기',
};

function TabGlyph({ name, color }: { name: keyof MainTabParamList; color: string }) {
  if (name === 'MapHome') {
    return (
      <Svg width={22} height={22} viewBox="0 0 24 24" fill="none">
        <Path
          d="M4 11l8-7 8 7M6 9.5V19a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V9.5"
          stroke={color}
          strokeWidth={1.7}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </Svg>
    );
  }
  if (name === 'TogetherFeed') {
    return (
      <Svg width={22} height={22} viewBox="0 0 24 24" fill="none">
        <Path
          d="M9.5 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM4 19v-1.5A3.5 3.5 0 0 1 7.5 14h4a3.5 3.5 0 0 1 3.5 3.5V19"
          stroke={color}
          strokeWidth={1.7}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <Path
          d="M15.5 5.2a3 3 0 0 1 0 5.8M17.4 14.1a3.5 3.5 0 0 1 2.6 3.4V19"
          stroke={color}
          strokeWidth={1.7}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </Svg>
    );
  }
  if (name === 'Chat') {
    return (
      <Svg width={22} height={22} viewBox="0 0 24 24" fill="none">
        <Path d="M5 5h14a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H9l-4 3v-3H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z"
          stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
      </Svg>
    );
  }
  if (name === 'Favorites') {
    return (
      <Svg width={22} height={22} viewBox="0 0 24 24" fill="none">
        <Path
          d="M6 4h12a1 1 0 0 1 1 1v15l-7-4-7 4V5a1 1 0 0 1 1-1z"
          stroke={color}
          strokeWidth={1.7}
          strokeLinejoin="round"
        />
      </Svg>
    );
  }
  return (
    <Svg width={22} height={22} viewBox="0 0 24 24" fill="none">
      <Path d="M4 7h16M4 12h16M4 17h16" stroke={color} strokeWidth={1.7} strokeLinecap="round" />
    </Svg>
  );
}

function MinTabBar({ state, navigation }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();
  const unread = totalUnread(useConversations().data ?? []);
  return (
    <View style={[styles.bar, { paddingBottom: Math.max(insets.bottom, 10) }]}>
      {state.routes.map((route, index) => {
        const focused = state.index === index;
        const color = focused ? T2.brand : T2.textMute;
        const name = route.name as keyof MainTabParamList;
        const onPress = () => {
          const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
          if (!focused && !event.defaultPrevented) {
            navigation.navigate(route.name);
          }
        };
        return (
          <Pressable key={route.key} onPress={onPress} style={styles.item}>
            <View style={styles.glyphWrap}>
              <TabGlyph name={name} color={color} />
              {name === 'Chat' && unread > 0 && (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>{unread > 99 ? '99+' : String(unread)}</Text>
                </View>
              )}
            </View>
            <Text style={[styles.label, { color }]}>{LABELS[name]}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export function MainTabs() {
  // MainTabs는 탭 내비게이터라 useNavigation의 기본 타입으로는 루트 스택 화면으로 못 간다.
  // 루트 스택 타입을 명시해 'PushPermission' 이동을 타입 안전하게 만든다(ConversationList와 같은 방식).
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();

  // 가입/로그인 후 처음 홈에 도착했을 때 한 번만 권한 안내를 띄운다.
  // 온보딩(ProfileSetup)은 signIn()으로 스택 전체가 교체되며 끝나므로
  // 그 안에서는 안내 화면으로 이동할 수 없다 — 그래서 여기서 띄운다.
  React.useEffect(() => {
    let alive = true;
    void (async () => {
      const [seen, granted] = await Promise.all([hasSeenPushPrompt(), hasPushPermission()]);
      if (alive && shouldPromptPush({ seen, granted })) {
        navigation.navigate('PushPermission');
      }
    })();
    return () => {
      alive = false;
    };
  }, [navigation]);

  return (
    <Tab.Navigator
      screenOptions={{ headerShown: false }}
      tabBar={(props) => <MinTabBar {...props} />}
    >
      <Tab.Screen name="MapHome" component={MapHomeScreen} />
      <Tab.Screen name="TogetherFeed" component={TogetherFeedScreen} />
      <Tab.Screen name="Chat" component={ConversationListScreen} />
      <Tab.Screen name="Favorites" component={FavoritesScreen} />
      <Tab.Screen name="More" component={MoreScreen} />
    </Tab.Navigator>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: T2.border,
    paddingTop: 10,
  },
  item: { flex: 1, alignItems: 'center', gap: 3 },
  label: { fontSize: 11, fontWeight: '700', letterSpacing: -0.2 },
  glyphWrap: { width: 22, height: 22, alignItems: 'center', justifyContent: 'center' },
  badge: {
    position: 'absolute',
    top: -3,
    right: -6,
    minWidth: 16,
    height: 16,
    borderRadius: 8,
    paddingHorizontal: 4,
    backgroundColor: T2.brand,
    borderWidth: 1.5,
    borderColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { fontSize: 9, fontWeight: '800', color: '#fff' },
});
