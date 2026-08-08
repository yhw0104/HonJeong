// PushBanner — 앱을 보고 있을 때 도착한 알림을 위에서 내려오는 카드로 보여준다.
//
// 왜 우리가 그리는가 · 언제 안 띄우는가는 banner.ts 주석 참조(OS 배너로는 "지금 그 대화방을
// 보고 있으면 빼자"를 표현할 수 없다).
//
// 조작:
//   · 탭        해당 화면으로 이동하고 닫는다(배너를 눌렀다 = 보러 가겠다).
//   · 위로 스와이프  닫는다. 아래로는 안 움직인다(늘어져 보이지 않게).
//   · 그냥 두면  4초 뒤 자동으로 닫힌다.
//
// 이 파일은 @react-native-firebase를 import하지 않는다 — 표시 창구(setBannerPresenter)로만
// 리스너와 이어진다. 그래서 격리 계층은 index.ts 하나로 유지된다.
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Animated, PanResponder, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';

import { bannerIcon, setBannerPresenter, type PushNotice } from './banner';

/** 자동으로 닫히기까지(ms). 읽기엔 넉넉하고, 화면을 오래 가리지는 않는 길이. */
const AUTO_DISMISS_MS = 4000;
/** 위로 이만큼 끌면 닫는다(px). */
const SWIPE_DISMISS = 24;
/** 닫힐 때 위로 사라지는 거리(px). 배너 높이보다 넉넉히 잡아 완전히 감춘다. */
const HIDDEN_Y = -200;

export function PushBanner({ onOpen }: { onOpen: (notice: PushNotice) => void }) {
  const insets = useSafeAreaInsets();
  const [notice, setNotice] = useState<PushNotice | null>(null);
  const y = useRef(new Animated.Value(HIDDEN_Y)).current;
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // PanResponder는 한 번만 만들어 재사용하므로 최신 값을 ref로 본다
  // (매 렌더 새로 만들면 드래그 도중 핸들러가 갈아끼워져 제스처가 끊긴다).
  const noticeRef = useRef<PushNotice | null>(null);
  noticeRef.current = notice;

  const clearTimer = () => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
  };

  const hide = useCallback(() => {
    clearTimer();
    Animated.timing(y, { toValue: HIDDEN_Y, duration: 180, useNativeDriver: true }).start(() => {
      setNotice(null);
    });
  }, [y]);

  const openRef = useRef(onOpen);
  openRef.current = onOpen;
  const open = useCallback(() => {
    const current = noticeRef.current;
    hide();
    if (current) openRef.current(current);
  }, [hide]);

  // 표시 창구에 자신을 등록한다. 리스너(usePushMessaging)가 이걸 통해 배너를 띄운다.
  useEffect(() => {
    setBannerPresenter((next) => {
      clearTimer();
      setNotice(next);
      y.setValue(HIDDEN_Y);
      Animated.spring(y, { toValue: 0, useNativeDriver: true, bounciness: 6, speed: 14 }).start();
      timer.current = setTimeout(() => {
        Animated.timing(y, { toValue: HIDDEN_Y, duration: 180, useNativeDriver: true }).start(() => setNotice(null));
      }, AUTO_DISMISS_MS);
    });
    return () => {
      setBannerPresenter(null);
      clearTimer();
    };
  }, [y]);

  const pan = useRef(
    PanResponder.create({
      // 탭은 통과시킨다 — 눌러서 이동하는 게 주 동작이다.
      onStartShouldSetPanResponder: () => false,
      // 위로 끄는 세로 드래그만 잡는다.
      onMoveShouldSetPanResponder: (_, g) => g.dy < -4 && Math.abs(g.dy) > Math.abs(g.dx),
      onPanResponderGrant: () => {
        // 손을 대면 자동 닫기를 멈춘다 — 읽는 중에 사라지면 안 된다.
        if (timer.current) clearTimeout(timer.current);
        timer.current = null;
      },
      onPanResponderMove: (_, g) => {
        if (g.dy < 0) y.setValue(g.dy);
      },
      onPanResponderRelease: (_, g) => {
        if (g.dy < -SWIPE_DISMISS) {
          Animated.timing(y, { toValue: HIDDEN_Y, duration: 140, useNativeDriver: true }).start(() => setNotice(null));
          return;
        }
        Animated.spring(y, { toValue: 0, useNativeDriver: true, bounciness: 6 }).start();
      },
      onPanResponderTerminate: () => {
        Animated.spring(y, { toValue: 0, useNativeDriver: true, bounciness: 6 }).start();
      },
    }),
  ).current;

  if (!notice) return null;

  return (
    <Animated.View
      style={[styles.wrap, { top: insets.top + 6, transform: [{ translateY: y }] }]}
      {...pan.panHandlers}
      pointerEvents="box-none"
    >
      <Pressable style={styles.card} onPress={open} accessibilityRole="button">
        <View style={styles.iconWrap}>
          <Icon name={bannerIcon(notice.data.type)} size={17} color={T2.brand} />
        </View>
        <View style={{ flex: 1, minWidth: 0 }}>
          {notice.title ? (
            <Text style={styles.title} numberOfLines={1}>
              {notice.title}
            </Text>
          ) : null}
          {notice.body ? (
            <Text style={styles.body} numberOfLines={2}>
              {notice.body}
            </Text>
          ) : null}
        </View>
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  // 화면 맨 위에 떠 있는 층. 좌우 여백은 시트·카드와 같은 16이다.
  wrap: { position: 'absolute', left: 16, right: 16, zIndex: 999 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: T2.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: T2.border,
    paddingVertical: 13,
    paddingHorizontal: 14,
    // 지도·목록 위에 떠야 해서 그림자를 시트만큼 준다.
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.16,
    shadowRadius: 18,
    elevation: 10,
  },
  iconWrap: {
    width: 34,
    height: 34,
    borderRadius: 12,
    backgroundColor: T2.brandSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: { fontSize: 14, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  body: { fontSize: 13, color: T2.textSub, letterSpacing: -0.2, marginTop: 2 },
});
