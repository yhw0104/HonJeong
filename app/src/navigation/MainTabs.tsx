// MainTabs — 하단 탭(홈/같이먹기/즐겨찾기/더보기). 목업의 MinTabBar 디자인을 커스텀 tabBar로 구현.
import React from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';
import { T2 } from '@/shared/theme';
import type { MainTabParamList } from './types';
import { MapHomeScreen } from '@/features/home/screens/MapHome';
import { TogetherFeedScreen } from '@/features/together/screens/TogetherFeed';
import { FavoritesScreen } from '@/features/favorites/screens/Favorites';
import { MoreScreen } from '@/features/profile/screens/More';

const Tab = createBottomTabNavigator<MainTabParamList>();

const LABELS: Record<keyof MainTabParamList, string> = {
  MapHome: '홈',
  TogetherFeed: '같이먹기',
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
  return (
    <View style={[styles.bar, { paddingBottom: Math.max(insets.bottom, 10) }]}>
      {state.routes.map((route, index) => {
        const focused = state.index === index;
        const color = focused ? T2.brand : T2.textMute;
        const onPress = () => {
          const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
          if (!focused && !event.defaultPrevented) {
            navigation.navigate(route.name);
          }
        };
        return (
          <Pressable key={route.key} onPress={onPress} style={styles.item}>
            <TabGlyph name={route.name as keyof MainTabParamList} color={color} />
            <Text style={[styles.label, { color }]}>{LABELS[route.name as keyof MainTabParamList]}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={{ headerShown: false }}
      tabBar={(props) => <MinTabBar {...props} />}
    >
      <Tab.Screen name="MapHome" component={MapHomeScreen} />
      <Tab.Screen name="TogetherFeed" component={TogetherFeedScreen} />
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
});
