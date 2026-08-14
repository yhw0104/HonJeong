// RestaurantDetail — 식당 상세 (원본: screens/RestaurantDetail.jsx)
// 풀블리드 히어로 + 플로팅 헤더 + 탭(홈/리뷰/사진/메이트/주변) + 하단 고정 CTA.
// 메뉴 탭은 보류(데이터 출처 미정) — TABS에서 임시 숨김. MenuTab 컴포넌트/렌더는 복원 위해 보존.
// 원본의 하단 MinTabBar는 제거(상세는 탭 위로 push되는 풀스크린이라 뒤로가기로 복귀).
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Dimensions, ActivityIndicator, Alert, Image, Share, Linking, Platform } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import * as Clipboard from 'expo-clipboard';
import { ImagePlaceholder, Avatar, Icon, HonbabStatusBar, HONBAB_BAR_H, StateView, PhotoViewer } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { usePlaceDetail, useNearby, usePlaceCheckinSummary, usePlaceMates } from '@/features/place/queries';
import { soloFriendlyLabel } from '@/features/place/soloFriendlyLabel';
import { barHeights, PERIOD_LABEL, PERIOD_RANGE } from '@/features/place/checkinSummary';
import type { PlaceCheckinSummary } from '@/features/place/api';
import { formatDistance, walkingMinutes } from '@/shared/location/distance';
import { useSeekers, useMyCheckIn, useStartCheckIn, useDineAlone, useCancelCheckIn } from '@/features/checkin/queries';
import { EndHonbabSheet } from '@/features/checkin/components/EndHonbabSheet';
import { checkInMode } from '@/features/checkin/statusView';
import type { Seeker, CheckIn } from '@/features/checkin/api';
import { formatElapsed, addressHead } from '@/shared/format';
import type { RootStackScreenProps } from '@/navigation/types';
import { listState, type ListState } from '@/shared/state/listState';
import { usePlaceReviews, usePlaceReviewSummary, useReviewContext, useDeleteReview, usePlacePhotos } from '@/features/review/queries';
import { reviewWriteTarget, reviewEditScreen } from '@/features/review/reviewWriteTarget';
import type { PlaceReview, PlaceReviewSummary } from '@/features/review/api';
import { reviewAuthorUnavailableMessage } from '@/features/review/reviewAuthorCopy';
import { FavoriteSheet } from '@/features/favorites/components/FavoriteSheet';
import { DirectionsSheet } from '@/features/place/components/DirectionsSheet';
import { useFavoriteStatus } from '@/features/favorites/queries';

type Tab = 'home' | 'menu' | 'review' | 'photo' | 'mate' | 'nearby';
const TABS: { key: Tab; label: string }[] = [
  { key: 'home', label: '홈' },
  // { key: 'menu', label: '메뉴' }, // 보류 — 데이터 출처 미정(점주 등록 P2). 복원 시 주석 해제.
  { key: 'review', label: '리뷰' },
  { key: 'photo', label: '사진' },
  { key: 'mate', label: '메이트' },
  { key: 'nearby', label: '주변' },
];

const MENU = [
  { n: '얼큰순두부', d: '소·중·대', p: '9,000', best: true },
  { n: '굴순두부', d: '겨울 한정', p: '10,000', best: false },
  { n: '버섯들깨순두부', d: '담백한 맛', p: '9,500', best: false },
  { n: '순두부 정식', d: '+제육 1인', p: '12,000', best: false },
  { n: '공기밥 추가', d: '', p: '1,000', best: false },
];

// 메이트 탭은 실데이터(usePlaceMates)로 렌더 — 상수 목업 제거
// 주변 탭은 실데이터(useNearby)로 렌더 — 상수 목업 제거

const GRID_W = (Dimensions.get('window').width - 40 - 12) / 3;

export function RestaurantDetailScreen({ navigation, route }: RootStackScreenProps<'RestaurantDetail'>) {
  const insets = useSafeAreaInsets();
  const [stab, setStab] = useState<Tab>('home');
  const [copied, setCopied] = useState(false);
  const [addrExpanded, setAddrExpanded] = useState(false);
  const placeId = route.params.placeId;
  const detail = usePlaceDetail(placeId);
  const checkinSummary = usePlaceCheckinSummary(placeId);
  const seekers = useSeekers(placeId);
  const myCheckIn = useMyCheckIn();
  const startMut = useStartCheckIn();
  const [ending, setEnding] = useState<CheckIn | null>(null); // 종료 시트(밀어서 완료) 대상
  // 크게 볼 사진. 프로필·대화창과 같은 뷰어를 쓴다(핀치 확대·아래로 쓸어 닫기 동일).
  const [photoViewer, setPhotoViewer] = useState<string | null>(null);
  const dineAloneMut = useDineAlone();
  const cancelMut = useCancelCheckIn();
  const name = detail.data?.name ?? route.params.name ?? '식당';
  const fullAddr = detail.data?.roadAddress ?? detail.data?.address ?? '주소 정보 없음';
  const detailFailed = detail.isError && !detail.data; // 상세 로딩 실패 → 주소 위장 대신 에러로 대체
  const addrHead = addressHead(fullAddr); // 시·도~구·군까지(기본 표시)
  const addrRest = fullAddr.trim() === addrHead ? '' : fullAddr.trim().slice(addrHead.length).trimStart(); // 나머지(펼침)
  // ACTIVE 또는 TOGETHER — 종료/취소되면 useMyCheckIn이 null을 반환
  const honbabOn = !!myCheckIn.data && myCheckIn.data.placeId === placeId;
  const seekersSt = listState({ isLoading: seekers.isLoading, isError: seekers.isError, count: (seekers.data ?? []).length });
  const reviews = usePlaceReviews(placeId);
  const summary = usePlaceReviewSummary(placeId);
  // 버튼을 누를 때 기다리지 않게 화면 진입 시 미리 받아 둔다(어느 작성 화면을 열지 판단용).
  const reviewCtx = useReviewContext(placeId);

  // 종료 시트가 떠 있는 동안 뒤로가기 스와이프를 끄는 처리는 **EndHonbabSheet 안으로 옮겼다**.
  // 여기서 화면별로 하던 것을 시트가 스스로 하게 바꾼 이유는 그 파일 주석 참조 — 화면마다 막는
  // 방식이라 홈 탭에서 같은 버그가 재발했다(08-08).

  const [favSheet, setFavSheet] = useState(false);
  const [dirOpen, setDirOpen] = useState(false);
  const favStatus = useFavoriteStatus(placeId);
  const saved = favStatus.data?.saved ?? false;

  const delMut = useDeleteReview();
  const confirmDelete = (reviewId: number) =>
    Alert.alert('리뷰 삭제', '이 리뷰를 삭제할까요? 되돌릴 수 없어요.', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: () => delMut.mutate(reviewId) },
    ]);
  // 새 리뷰 — 어느 화면을 열지는 서버가 정한다(연결될 체크인이 있으면 혼밥 화면).
  // 조회에 실패하면 일반 리뷰 화면으로 간다: 혼밥 화면을 잘못 열면 혼밥 별점을 물어봐 놓고
  // 서버가 400으로 거절해 사용자가 저장할 수 없게 된다.
  const writeReview = () => {
    const target = reviewWriteTarget(reviewCtx.data?.linkableCheckInId ?? null);
    if (target.screen === 'DiningLogWrite') {
      navigation.navigate('DiningLogWrite', { placeId, placeName: name, checkInId: target.checkInId });
    } else {
      navigation.navigate('ReviewWrite', { placeId, placeName: name });
    }
  };

  // 수정 — 인증 여부는 서버가 이미 판정했고 바뀌지 않는다(checkIn이 불변).
  const editReview = (r: PlaceReview) => {
    if (reviewEditScreen(r.authenticated) === 'DiningLogWrite') {
      navigation.navigate('DiningLogWrite', {
        placeId,
        placeName: name,
        reviewId: r.reviewId,
        initial: { taste: r.tasteRating, honbab: r.soloFriendlyRating, tags: r.tags, content: r.content ?? '', photos: r.imageUrls },
      });
      return;
    }
    navigation.navigate('ReviewWrite', {
      placeId,
      placeName: name,
      reviewId: r.reviewId,
      initial: {
        taste: r.tasteRating,
        content: r.content ?? '',
        photos: r.imageUrls,
        // 화면엔 안 보이지만 그대로 되돌려 보낸다 — 예전에 쓰인 인증 없는 리뷰에 남아 있는 값을 지키려고.
        keepSolo: { honbab: r.soloFriendlyRating, tags: r.tags },
      },
    });
  };
  const reportReview = (r: PlaceReview) =>
    navigation.navigate('ReportForm', { targetType: 'REVIEW', targetId: r.reviewId, targetNickname: r.user.nickname });
  // 리뷰 작성자 닉네임 탭 → 프로필. 내 리뷰는 내 프로필로 보낸다(MateProfile은 남의 프로필용이라
  // 나 자신에게 '메이트 신청' CTA가 뜬다). 갈 수 없는 작성자(탈퇴·정지)는 이유를 안내한다.
  const openReviewAuthor = (r: PlaceReview) => {
    if (r.user.userId == null) {
      Alert.alert(reviewAuthorUnavailableMessage(r.user.unavailable));
      return;
    }
    if (r.mine) navigation.navigate('MyProfile');
    else navigation.navigate('MateProfile', { userId: r.user.userId });
  };

  const copy = async () => {
    await Clipboard.setStringAsync(fullAddr); // 전체 주소(시·도 포함)를 복사
    setCopied(true);
    setTimeout(() => setCopied(false), 1200);
  };
  const goMealRequest = () => navigation.navigate('MealRequest', { placeId, placeName: name });

  // 공유 — 앱에 공개 URL/딥링크가 없어 가게명+주소 텍스트로 공유. 취소/실패는 조용히 무시.
  const onShare = () => {
    Share.share({ title: name, message: `${name}\n${fullAddr}\n\n🍚 혼정에서 공유` }).catch(() => {});
  };

  // 길찾기 — 외부 지도앱으로 식당명 검색.
  // 네이버: NCP 가이드대로 앱 미설치 시 스토어 설치 페이지로 유도(iOS id311867728 / Android com.nhn.android.nmap).
  // 카카오: 앱 미설치 시 지도 웹으로 폴백.
  const openMap = (provider: 'naver' | 'kakao') => {
    const q = encodeURIComponent(name);
    if (provider === 'naver') {
      const appUrl = `nmap://search?query=${q}&appname=com.anonymous.honjeong`;
      const storeUrl =
        Platform.OS === 'ios'
          ? 'https://apps.apple.com/kr/app/id311867728'
          : 'https://play.google.com/store/apps/details?id=com.nhn.android.nmap';
      Linking.openURL(appUrl).catch(() => Linking.openURL(storeUrl).catch(() => {}));
      return;
    }
    const appUrl = `kakaomap://search?q=${q}`;
    const webUrl = `https://map.kakao.com/link/search/${q}`;
    Linking.openURL(appUrl).catch(() => Linking.openURL(webUrl).catch(() => {}));
  };
  const onDirections = () => setDirOpen(true);
  const goDinerProfile = (userId: number) => navigation.navigate('MateProfile', { userId });
  const toggleHonbab = () => {
    if (honbabOn && myCheckIn.data) {
      if (myCheckIn.data.status !== 'SEEKING') setEnding(myCheckIn.data);
      // SEEKING이면 상태바의 '혼자 먹기/그만두기'로 처리 — CTA 탭은 무시
      return;
    }
    startMut.mutate(placeId);
  };

  return (
    <View style={styles.root}>
      <ScrollView contentContainerStyle={{ paddingBottom: 120 }} showsVerticalScrollIndicator={false}>
        {/* 히어로 */}
        <HeroPhotos placeId={placeId} onPhotoPress={setPhotoViewer} />

        <View style={styles.content}>
          {/* 카테고리 */}
          <View style={{ marginBottom: 10 }}>
            <Text style={styles.eyebrow}>{detail.data?.category ?? '식당'}</Text>
          </View>

          <Text style={styles.title}>{name}</Text>

          {detailFailed ? (
            // 상세 로딩 실패: 주소 자리에 '주소 정보 없음' 위장 대신 에러+재시도로 대체
            <Pressable style={styles.detailErrBanner} onPress={() => detail.refetch()} accessibilityRole="button">
              <Text style={styles.detailErrText}>식당 정보를 불러오지 못했어요 · 다시 시도</Text>
            </Pressable>
          ) : (
            <>
              {/* 주소 + 복사 — 기본은 시·도~구·군, 화살표 누르면 나머지(도로명·번지)를 아래에 펼침 */}
              <View style={styles.addrRow}>
                <Icon name="pin" size={15} color={T2.textMute} />
                <Pressable
                  style={styles.addrTap}
                  onPress={() => setAddrExpanded((v) => !v)}
                  hitSlop={4}
                  disabled={!addrRest}
                >
                  <Text style={styles.addr} numberOfLines={1}>{addrHead}</Text>
                  {addrRest ? (
                    <Icon name={addrExpanded ? 'chevronUp' : 'chevronDown'} size={14} color={T2.textMute} />
                  ) : null}
                </Pressable>
                <Pressable style={styles.copyBtn} onPress={copy}>
                  <Icon name="copy" size={13} color={copied ? T2.brand : T2.textSub} />
                  <Text style={[styles.copyText, { color: copied ? T2.brand : T2.textSub }]}>{copied ? '복사됨' : '복사'}</Text>
                </Pressable>
              </View>
              {addrExpanded && addrRest ? <Text style={styles.addrRest}>{addrRest}</Text> : null}
            </>
          )}

          {/* 평점 */}
          <View style={styles.ratingRow}>
            <View>
              <Text style={styles.ratingNum}>{summary.data?.avgTasteRating?.toFixed(1) ?? '-'}</Text>
              <Text style={styles.ratingLabel}>리뷰 {summary.data?.reviewCount ?? 0}</Text>
            </View>
            <View style={styles.ratingDivider} />
            <View>
              <Text style={[styles.ratingNum, { color: T2.brand }]}>{summary.data?.avgSoloFriendlyRating?.toFixed(1) ?? '-'}</Text>
              <Text style={styles.ratingLabel}>혼밥 친화도</Text>
            </View>
          </View>

          {/* 탭 */}
          <View style={styles.tabs}>
            {TABS.map((t) => {
              const on = stab === t.key;
              return (
                <Pressable key={t.key} style={styles.tab} onPress={() => setStab(t.key)}>
                  <Text style={[styles.tabLabel, { color: on ? T2.text : T2.textMute, fontWeight: on ? '800' : '600' }]}>
                    {t.label}
                  </Text>
                  {on ? <View style={styles.tabUnderline} /> : null}
                </Pressable>
              );
            })}
          </View>

          {stab === 'home' && (
            <HomeTab
              seekers={seekers.data ?? []}
              seekersState={seekersSt}
              onRetrySeekers={() => seekers.refetch()}
              onMeal={goMealRequest}
              onDinerPress={goDinerProfile}
              summary={summary.data}
              phone={detail.data?.phone ?? null}
              onKakao={() => openMap('kakao')}
              summaryStats={checkinSummary.data ?? null}
              summaryLoading={checkinSummary.isLoading}
              summaryError={checkinSummary.isError}
            />
          )}
          {stab === 'menu' && <MenuTab />}
          {stab === 'review' && (
            <ReviewTab
              reviews={reviews.data ?? []}
              isLoading={reviews.isLoading}
              isError={reviews.isError}
              onWrite={writeReview}
              // 아직 어느 화면인지 모르는 동안은 누르지 못하게 한다 — 이 짧은 구간에 누르면
              // 혼밥 후인데도 일반 리뷰 화면이 열려 인증을 놓친다.
              writeDisabled={reviewCtx.isPending}
              onEdit={editReview}
              onDelete={confirmDelete}
              onReport={reportReview}
              onOpenAuthor={openReviewAuthor}
              onPhotoPress={setPhotoViewer}
            />
          )}
          {stab === 'photo' && <PhotoTab placeId={placeId} onPhotoPress={setPhotoViewer} />}
          {stab === 'mate' && (
            <MateTab
              placeId={placeId}
              onMeal={goMealRequest}
              onOpenProfile={goDinerProfile}
              soloRating={summary.data?.avgSoloFriendlyRating ?? null}
              soloReviewCount={summary.data?.soloRatedCount ?? 0}
            />
          )}
          {stab === 'nearby' && (
            <NearbyTab
              placeId={placeId}
              placeName={name}
              center={detail.data ? { lat: detail.data.latitude, lng: detail.data.longitude } : null}
              onOpen={(id, n) => navigation.push('RestaurantDetail', { placeId: id, name: n })}
            />
          )}
        </View>
      </ScrollView>

      {/* 플로팅 헤더 (혼밥 중이면 상태 카드 아래로 내림) */}
      <View style={[styles.floatHeader, { top: insets.top + 8 + (honbabOn ? HONBAB_BAR_H + 10 : 0) }]}>
        <Pressable style={styles.circleBtn} onPress={() => navigation.goBack()} hitSlop={6}>
          <Text style={styles.circleArrow}>←</Text>
        </Pressable>
        <View style={{ flexDirection: 'row', gap: 8 }}>
          <Pressable style={styles.circleBtn} onPress={onShare} hitSlop={4}>
            <Icon name="share" size={15} color={T2.text} />
          </Pressable>
          <Pressable style={styles.circleBtn} onPress={() => setFavSheet(true)} hitSlop={4}>
            <Icon name="heart" size={16} color={saved ? T2.brand : T2.text} />
          </Pressable>
        </View>
      </View>

      {/* 하단 고정 CTA — 길찾기 · 같이 먹기 · 혼밥 시작/중 */}
      <View style={[styles.ctaBar, { paddingBottom: insets.bottom + 12 }]}>
        <Pressable style={styles.ctaNav} onPress={onDirections}>
          <Icon name="navigate" size={20} color={T2.text} />
          <Text style={styles.ctaNavText}>길찾기</Text>
        </Pressable>
        <Pressable style={styles.ctaMate} onPress={goMealRequest}>
          <Icon name="mate" size={17} color={T2.brand} />
          <Text style={styles.ctaMateText}>같이 먹기</Text>
        </Pressable>
        {honbabOn ? (
          <Pressable style={styles.ctaHonbabOn} onPress={toggleHonbab}>
            <View style={styles.ctaPulse}>
              <View style={styles.ctaHalo} />
              <View style={styles.ctaDot} />
            </View>
            <Text style={styles.ctaHonbabOnText}>
              {myCheckIn.data?.status === 'TOGETHER' ? '같이 먹는 중' : myCheckIn.data?.status === 'SEEKING' ? '모집 중' : '혼밥 중'}
            </Text>
          </Pressable>
        ) : (
          <Pressable style={styles.ctaHonbab} onPress={toggleHonbab}>
            <Text style={styles.ctaHonbabText}>같이 먹을 사람 구하기</Text>
          </Pressable>
        )}
      </View>

      {/* 혼밥 중 상태 카드 — 켜져 있으면 최상단에 띄움 */}
      {honbabOn && myCheckIn.data && (
        <View style={[styles.honbabFloat, { top: insets.top + 8 }]}>
          <HonbabStatusBar
            mode={checkInMode(myCheckIn.data.status)}
            place={name}
            partnerNickname={myCheckIn.data.partnerNickname}
            onEnd={() => setEnding(myCheckIn.data!)}
            onDineAlone={() => dineAloneMut.mutate(myCheckIn.data!.checkInId)}
            onQuit={() => cancelMut.mutate(myCheckIn.data!.checkInId)}
            onOpenChat={() => {
              const cid = myCheckIn.data?.conversationId;
              if (cid) navigation.navigate('ChatRoom', { conversationId: cid });
            }}
          />
        </View>
      )}

      <FavoriteSheet placeId={placeId} visible={favSheet} onClose={() => setFavSheet(false)} />

      {/* 혼밥/같이먹기 종료 — 밀어서 완료 시트 */}
      <EndHonbabSheet
        checkIn={ending}
        onClose={() => setEnding(null)}
        onReportNoShow={(userId, nickname) =>
          navigation.navigate('ReportForm', { targetType: 'USER', targetId: userId, targetNickname: nickname })}
      />

      {/* 길찾기 — 네이버/카카오 지도 선택 시트 */}
      <DirectionsSheet
        visible={dirOpen}
        placeName={name}
        onClose={() => setDirOpen(false)}
        onPick={(p) => { setDirOpen(false); openMap(p); }}
      />

      {/* 사진 크게 보기 — 프로필·대화창과 같은 뷰어(핀치 확대·아래로 쓸어 닫기). */}
      <PhotoViewer uri={photoViewer} onClose={() => setPhotoViewer(null)} />
    </View>
  );
}

/* ── 홈 탭 ───────────────────────────────────────── */
function HomeTab({ seekers, seekersState, onRetrySeekers, onMeal, onDinerPress, summary, phone, onKakao, summaryStats, summaryLoading, summaryError }: { seekers: Seeker[]; seekersState: ListState; onRetrySeekers: () => void; onMeal: () => void; onDinerPress: (userId: number) => void; summary: PlaceReviewSummary | undefined; phone: string | null; onKakao: () => void; summaryStats: PlaceCheckinSummary | null; summaryLoading: boolean; summaryError: boolean }) {
  return (
    <View>
      {/* 혼밥 친화도 — 인라인(박스 없음): 점수 숫자 + 라벨 + 5칸 바 + 친화 칩. */}
      <View style={styles.friendlyInline}>
        <View style={styles.friendlyHeadRow}>
          <Text style={styles.friendlyTitle}>혼밥 친화도</Text>
          {/* ★전체 리뷰 수가 아니라 '혼밥 평가 수'다 — 혼밥 별점은 인증 리뷰만 갖기 때문에
              reviewCount를 쓰면 평가하지 않은 사람까지 세어 부풀려진다. */}
          {(summary?.soloRatedCount ?? 0) > 0 ? (
            <Text style={styles.friendlyCount}>혼밥러 {summary?.soloRatedCount}명 평가</Text>
          ) : null}
        </View>
        <View style={styles.friendlyScoreRow}>
          <Text style={styles.friendlyScore}>{summary?.avgSoloFriendlyRating?.toFixed(1) ?? '-'}</Text>
          <Text style={styles.friendlyDot}>·</Text>
          <Text style={styles.friendlyLabel}>{soloFriendlyLabel(summary?.avgSoloFriendlyRating ?? null, summary?.soloRatedCount ?? 0)}</Text>
        </View>

        {/* 점수 5칸 바 */}
        <View style={styles.friendlyBars}>
          {[0, 1, 2, 3, 4].map((i) => {
            const fill = Math.max(0, Math.min(1, (summary?.avgSoloFriendlyRating ?? 0) - i));
            return (
              <View key={i} style={styles.barTrack}>
                <View style={[styles.barFill, { width: `${fill * 100}%` }]} />
              </View>
            );
          })}
        </View>

        {/* 친화 요소 칩 */}
        {(summary?.topTags ?? []).length > 0 ? (
          <View style={[styles.chipWrap, { marginTop: 14 }]}>
            {(summary?.topTags ?? []).map((t) => (
              <View
                key={t.tag}
                style={[
                  styles.friendlyChip,
                  {
                    backgroundColor: T2.brandSoft,
                    borderColor: 'rgba(255,90,31,0.2)',
                  },
                ]}
              >
                <Text style={{ fontSize: 12, fontWeight: '800', color: T2.brand }}>✓</Text>
                <Text style={{ fontSize: 13, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 }}>{t.tag}</Text>
              </View>
            ))}
          </View>
        ) : null}
      </View>

      {/* 지금 여기서 같이 먹을 사람 구하는 중 (실데이터: 닉네임·경과만) */}
      <View style={styles.mealCard}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
          <View style={styles.liveDot} />
          <Text style={[styles.liveLabel, { flex: 1 }]}>지금 여기서 같이 먹을 사람 구하는 중 · {seekers.length}명</Text>
          <Pressable style={styles.mealCardBtn} onPress={onMeal} accessibilityRole="button">
            <Text style={styles.mealCardBtnText}>같이 먹기</Text>
          </Pressable>
        </View>
        {seekersState === 'ready' ? (
          <View style={{ marginTop: 12, gap: 10 }}>
            {seekers.map((d) => (
              <Pressable
                key={d.checkInId}
                accessibilityRole="button"
                onPress={() => onDinerPress(d.userId)}
                style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}
              >
                <Avatar uri={d.profileImageUrl} size={32} />
                <Text style={{ flex: 1, fontSize: 14, fontWeight: '700', color: T2.text }}>{d.nickname}</Text>
                <Text style={{ fontSize: 12, color: T2.textMute }}>{formatElapsed(d.elapsedMinutes)}</Text>
              </Pressable>
            ))}
          </View>
        ) : seekersState === 'loading' ? (
          <StateView kind="loading" compact />
        ) : seekersState === 'error' ? (
          <StateView kind="error" compact message="혼밥러 목록을 불러오지 못했어요" onRetry={onRetrySeekers} />
        ) : (
          <Text style={[styles.mealText, { marginTop: 12 }]}>아직 같이 먹을 사람을 구하는 이가 없어요.</Text>
        )}
      </View>

      {/* 혼밥 기록(사회적 증거) — 누적 혼밥러 + 붐비는 시간대. 같이먹기 카드 아래로 배치. */}
      <SocialProofCard stats={summaryStats} loading={summaryLoading} error={summaryError} />

      {/* 정보 — 전화(있을 때만·실연결) + 영업시간/메뉴는 카카오맵으로 유도(우리 데이터엔 없음) */}
      <View style={styles.sectionGap}>
        <Text style={styles.sectionTitle}>정보</Text>
        <View style={{ marginTop: 12 }}>
          {phone ? (
            <View style={[styles.infoRow, styles.infoDivider]}>
              <Text style={styles.infoKey}>전화</Text>
              <Text style={styles.infoVal}>{phone}</Text>
              <Pressable
                style={styles.telBtn}
                accessibilityRole="button"
                onPress={() =>
                  // 전화 앱이 없는 기기·시뮬레이터에선 openURL이 실패 → 조용히 죽지 않게 번호 복사로 폴백.
                  Linking.openURL(`tel:${phone}`).catch(async () => {
                    await Clipboard.setStringAsync(phone);
                    Alert.alert('전화번호를 복사했어요', phone);
                  })
                }
              >
                <Icon name="phoneCall" size={13} color={T2.text} />
                <Text style={styles.telText}>전화</Text>
              </Pressable>
            </View>
          ) : null}
          <Pressable style={styles.infoRow} onPress={onKakao} accessibilityRole="button">
            <Icon name="pin" size={15} color={T2.textMute} />
            <Text style={[styles.infoVal, { color: T2.brand }]}>영업시간·메뉴는 카카오맵에서 확인</Text>
            <Icon name="chevronRight" size={16} color={T2.textMute} />
          </Pressable>
        </View>
      </View>
    </View>
  );
}

/* ── 사회적 증거 카드 — 누적 혼밥러 + 붐비는 시간대(팝타임 미니 바) ── */
const SOCIAL_BAR_MAX = 40; // 미니 바 최대 높이(px) — barHeights()의 0~1 정규화 값을 여기 곱해 실제 픽셀로 환산

function SocialProofCard({ stats, loading, error }: { stats: PlaceCheckinSummary | null; loading: boolean; error: boolean }) {
  return (
    <View style={styles.socialCard}>
      <View style={styles.socialEyebrowRow}>
        <Icon name="rice" size={13} color={T2.textMute} />
        <Text style={styles.sectionTitle}>혼밥 기록</Text>
      </View>
      {loading ? (
        <StateView kind="loading" compact />
      ) : error ? (
        // 조회 실패를 '0명'으로 위장하지 않는다(정직한 상태 표시).
        <Text style={styles.socialEmptyText}>혼밥 기록을 불러오지 못했어요</Text>
      ) : stats && stats.totalDiners > 0 ? (
        <SocialProofStats stats={stats} />
      ) : (
        <Text style={styles.socialEmptyText}>아직 첫 혼밥러를 기다려요 🍚</Text>
      )}
    </View>
  );
}

function SocialProofStats({ stats }: { stats: PlaceCheckinSummary }) {
  const heights = barHeights(stats.periods); // 각 시간대 카운트를 최댓값 기준 0~1로 정규화
  return (
    <>
      <Text style={styles.socialCaption}>
        여기서 지금까지 <Text style={styles.socialCaptionNum}>{stats.totalDiners}명</Text>이 혼밥했어요
      </Text>

      {stats.peakPeriodKey != null ? (
        <>
          <Text style={styles.socialPeakLine}>
            주로 <Text style={styles.socialPeakName}>{PERIOD_LABEL[stats.peakPeriodKey]}</Text>에 붐벼요
          </Text>
          <View style={styles.barsRow}>
            {stats.periods.map((p, i) => {
              const isPeak = p.key === stats.peakPeriodKey;
              // 실데이터가 0인 시간대는 얇은 기준선으로, 있는 시간대는 최소 6px로 바닥에서 눈에 띄게(값 왜곡 없이 가독성만 확보)
              const h = Math.max(heights[i] * SOCIAL_BAR_MAX, p.count > 0 ? 6 : 3);
              return (
                <View key={p.key} style={styles.barCol}>
                  <Text style={[styles.barCount, isPeak && styles.barCountPeak]}>{p.count}</Text>
                  <View style={styles.barColTrack}>
                    <View style={[styles.periodBar, { height: h, backgroundColor: isPeak ? T2.brand : T2.border }]} />
                  </View>
                  <Text style={[styles.periodLabel, isPeak && styles.periodLabelPeak]}>{PERIOD_LABEL[p.key]}</Text>
                  <Text style={styles.periodRange}>({PERIOD_RANGE[p.key]})</Text>
                </View>
              );
            })}
          </View>
        </>
      ) : null}
    </>
  );
}

/* ── 메뉴 탭 ─────────────────────────────────────── */
function MenuTab() {
  return (
    <View style={{ marginTop: 4 }}>
      {MENU.map((m, i, arr) => (
        <View key={m.n} style={[styles.menuRow, i < arr.length - 1 && styles.infoDivider]}>
          <View style={{ flex: 1, minWidth: 0 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <Text style={styles.menuName}>{m.n}</Text>
              {m.best ? (
                <View style={styles.bestBadge}>
                  <Text style={styles.bestText}>대표</Text>
                </View>
              ) : null}
            </View>
            {m.d ? <Text style={styles.menuDesc}>{m.d}</Text> : null}
          </View>
          <Text style={styles.menuPrice}>{m.p}원</Text>
        </View>
      ))}
    </View>
  );
}

/* ── 리뷰 탭 ─────────────────────────────────────── */
function ReviewTab({ reviews, isLoading, isError, onWrite, writeDisabled, onEdit, onDelete, onReport, onOpenAuthor, onPhotoPress }: {
  reviews: PlaceReview[];
  isLoading: boolean; isError: boolean; onWrite: () => void;
  /** 어느 작성 화면을 열지 아직 모르는 동안 true — 잘못된 화면이 열리지 않게 잠시 막는다. */
  writeDisabled: boolean;
  onEdit: (r: PlaceReview) => void; onDelete: (reviewId: number) => void; onReport: (r: PlaceReview) => void;
  onOpenAuthor: (r: PlaceReview) => void;
  /** 리뷰 사진을 눌렀을 때 — 전체화면 뷰어로 넘긴다. */
  onPhotoPress: (uri: string) => void;
}) {
  return (
    <View style={{ marginTop: 4 }}>
      <View style={styles.reviewHead}>
        <View style={{ flex: 1, flexDirection: 'row', alignItems: 'baseline' }}>
          <Text style={styles.reviewHeadTitle}>리뷰</Text>
          <Text style={styles.reviewHeadCount}> {reviews.length}개</Text>
        </View>
        <Pressable
          style={[styles.writeBtn, writeDisabled && { opacity: 0.5 }]}
          onPress={onWrite}
          disabled={writeDisabled}
          accessibilityRole="button"
        >
          <Icon name="pencil" size={14} color="#fff" />
          <Text style={styles.writeText}>리뷰 쓰기</Text>
        </Pressable>
      </View>

      {isLoading ? (
        <View style={{ padding: 24, alignItems: 'center' }}><ActivityIndicator color={T2.brand} /></View>
      ) : isError ? (
        <Text style={styles.reviewNotice}>리뷰를 불러오지 못했어요.</Text>
      ) : reviews.length === 0 ? (
        <Text style={styles.reviewNotice}>아직 리뷰가 없어요.{'\n'}첫 리뷰를 남겨보세요.</Text>
      ) : (
        reviews.map((r, i, arr) => (
          <View key={r.reviewId} style={[styles.reviewCard, i < arr.length - 1 && styles.reviewDivider]}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
              {/* 닉네임 탭 → 작성자 프로필. 갈 수 없는 작성자(탈퇴·정지)는 이동 대신 이유를 안내한다. */}
              <Pressable hitSlop={6} onPress={() => onOpenAuthor(r)} accessibilityRole="button">
                <Text style={styles.reviewAuthor}>{r.user.nickname}</Text>
              </Pressable>
              {r.authenticated ? <Text style={{ fontSize: 11, color: T2.brand }}>✔ 혼밥</Text> : null}
              {!r.mine && (
                <Pressable hitSlop={8} style={{ marginLeft: 'auto' }} onPress={() => onReport(r)}>
                  <Text style={styles.reviewReport}>신고</Text>
                </Pressable>
              )}
            </View>
            {r.content ? <Text style={{ marginTop: 6, color: T2.textSub, lineHeight: 20 }}>{r.content}</Text> : null}
            {(r.imageUrls?.length ?? 0) > 0 && (
              <ScrollView
                horizontal
                showsHorizontalScrollIndicator={false}
                style={{ marginTop: 10, marginHorizontal: -2 }}
                contentContainerStyle={{ gap: 8, paddingHorizontal: 2 }}
              >
                {r.imageUrls!.map((uri, idx) => (
                  <Pressable
                    key={`${uri}-${idx}`}
                    onPress={() => onPhotoPress(uri)}
                    accessibilityRole="button"
                    accessibilityLabel="사진 크게 보기"
                  >
                    <Image source={{ uri }} style={{ width: 220, height: 220, borderRadius: 12 }} />
                  </Pressable>
                ))}
              </ScrollView>
            )}
            <View style={{ flexDirection: 'row', gap: 6, marginTop: 8 }}>
              <View style={styles.tasteChip}><Text style={styles.tasteText}>맛 ★ {r.tasteRating.toFixed(1)}</Text></View>
              {/* 혼밥 별점은 혼밥 인증 리뷰만 갖는다 — 없으면 칩 자체를 안 그린다. */}
              {r.soloFriendlyRating != null && (
                <View style={styles.honbabChip}><Text style={styles.honbabText}>혼밥 ★ {r.soloFriendlyRating.toFixed(1)}</Text></View>
              )}
            </View>
            {r.tags.length > 0 ? (
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
                {r.tags.map((t) => (<View key={t} style={styles.tagChip}><Text style={styles.tagChipText}>{t}</Text></View>))}
              </View>
            ) : null}
            {r.mine ? (
              <View style={{ flexDirection: 'row', gap: 16, marginTop: 10 }}>
                <Pressable hitSlop={6} onPress={() => onEdit(r)}>
                  <Text style={{ fontSize: 12, fontWeight: '700', color: T2.textSub }}>수정</Text>
                </Pressable>
                <Pressable hitSlop={6} onPress={() => onDelete(r.reviewId)}>
                  <Text style={{ fontSize: 12, fontWeight: '700', color: '#d11' }}>삭제</Text>
                </Pressable>
              </View>
            ) : null}
          </View>
        ))
      )}
    </View>
  );
}

/* ── 히어로 대표사진 (리뷰 사진 캐러셀) ──────────────── */
function HeroPhotos({ placeId, onPhotoPress }: { placeId: number; onPhotoPress: (uri: string) => void }) {
  const photos = usePlacePhotos(placeId);
  const items = photos.data ?? [];
  const [idx, setIdx] = useState(0);
  const W = Dimensions.get('window').width;
  if (items.length === 0) {
    return <ImagePlaceholder w="100%" h={320} radius={0} tag="대표 사진" />;
  }
  return (
    <View>
      <ScrollView
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onMomentumScrollEnd={(e) => setIdx(Math.round(e.nativeEvent.contentOffset.x / W))}
      >
        {items.map((p, i) => (
          <Pressable
            key={`${p.photoUrl}-${i}`}
            onPress={() => onPhotoPress(p.photoUrl)}
            accessibilityRole="button"
            accessibilityLabel="사진 크게 보기"
          >
            <Image source={{ uri: p.photoUrl }} style={{ width: W, height: 320 }} />
          </Pressable>
        ))}
      </ScrollView>
      {items.length > 1 ? (
        <View style={styles.heroCounter}>
          <Text style={styles.heroCounterText}>{idx + 1} / {items.length}</Text>
        </View>
      ) : null}
    </View>
  );
}

/* ── 사진 탭 ─────────────────────────────────────── */
function PhotoTab({ placeId, onPhotoPress }: { placeId: number; onPhotoPress: (uri: string) => void }) {
  const photos = usePlacePhotos(placeId);
  if (photos.isLoading) return <Text style={styles.tabEmpty}>불러오는 중…</Text>;
  const items = photos.data ?? [];
  if (items.length === 0) return <Text style={styles.tabEmpty}>아직 사진이 없어요</Text>;
  return (
    <View style={styles.photoGrid}>
      {items.map((p, i) => (
        <Pressable
          key={`${p.photoUrl}-${i}`}
          onPress={() => onPhotoPress(p.photoUrl)}
          accessibilityRole="button"
          accessibilityLabel="사진 크게 보기"
        >
          <Image source={{ uri: p.photoUrl }} style={{ width: GRID_W, height: GRID_W, borderRadius: 10 }} />
        </Pressable>
      ))}
    </View>
  );
}

/* ── 메이트 탭 ───────────────────────────────────── */
function MateTab({ placeId, onMeal, onOpenProfile, soloRating, soloReviewCount }: {
  placeId: number;
  onMeal: () => void;
  onOpenProfile: (userId: number) => void;
  soloRating: number | null;
  soloReviewCount: number;
}) {
  const q = usePlaceMates(placeId);
  const st = listState({ isLoading: q.isLoading, isError: q.isError, count: (q.data?.mates ?? []).length });
  if (st === 'loading') return <View style={{ marginTop: 20 }}><StateView kind="loading" /></View>;
  if (st === 'error') return <View style={{ marginTop: 20 }}><StateView kind="error" message="메이트 정보를 불러오지 못했어요" onRetry={() => q.refetch()} /></View>;

  const { visitedCount, mates, savedCount, savedMates } = q.data!;
  // '같이 먹기' 버튼을 다는 섹션이라, 신청을 받을 수 있는 상태(모집중)인 메이트만 띄운다.
  const seekingMates = mates.filter((m) => m.seekingNow);
  // 저장 아바타 스택 — 저장한 메이트 최대 4명 + 나머지("+N"). N = 전체 저장자 - 보여준 메이트 수.
  const savedShown = savedMates.slice(0, 4);
  const savedOverflow = savedCount - savedShown.length;
  // 요약 배너 아바타 스택 — 다녀간 메이트만(최대 3). 아직 안 다녀갔으면 스택 없이 평가 문구만.
  const stackMates = mates.filter((m) => m.lastVisitedAt != null || m.visitCount > 0).slice(0, 3);

  return (
    <View style={{ marginTop: 8 }}>
      {/* 요약 배너 — 사회적 신뢰(아바타 스택 + 문구). 목업 정합. */}
      <View style={styles.mateSummaryCard}>
        {stackMates.length > 0 ? (
          <View style={styles.mateAvatarStack}>
            {stackMates.map((m, i) => (
              <View key={m.userId} style={{ marginLeft: i ? -10 : 0 }}>
                <Avatar uri={m.profileImageUrl} size={36} ring="#fff" />
              </View>
            ))}
          </View>
        ) : null}
        <View style={{ flex: 1, minWidth: 0 }}>
          {visitedCount > 0 ? (
            <>
              <Text style={styles.mateSummaryLine}>
                내 메이트 <Text style={{ fontWeight: '800', color: T2.brand }}>{visitedCount}명</Text>이 여기 다녀갔어요
              </Text>
              <Text style={styles.mateSummarySub}>믿고 혼밥하기 좋은 곳이에요</Text>
            </>
          ) : soloRating != null ? (
            // 다녀간 메이트가 없으면 혼밥 친화도 평가로 대체(하드코딩 대신 실데이터).
            <>
              <Text style={styles.mateSummaryLine}>
                혼밥 친화도 <Text style={{ fontWeight: '800', color: T2.brand }}>★{soloRating.toFixed(1)}</Text> · {soloFriendlyLabel(soloRating, soloReviewCount)}
              </Text>
              <Text style={styles.mateSummarySub}>혼밥러 {soloReviewCount}명이 평가했어요</Text>
            </>
          ) : (
            <>
              <Text style={styles.mateSummaryLine}>아직 다녀간 메이트가 없어요</Text>
              <Text style={styles.mateSummarySub}>같이 먹기를 통해 새로운 메이트를 만들어 봐요</Text>
            </>
          )}
        </View>
      </View>

      {/* 지금 여기서 모집 중 — 섹션 라벨 + 라이브 카드(각 메이트 한 박스) */}
      {seekingMates.length > 0 ? (
        <View style={styles.mateSection}>
          <View style={styles.mateSectionHead}>
            <Text style={styles.mateSectionTitle}>지금 여기서 모집 중</Text>
            <Text style={styles.mateSectionCount}>{seekingMates.length}</Text>
          </View>
          <View style={{ gap: 10 }}>
            {seekingMates.map((m) => (
              <View key={m.userId} style={styles.mateLiveRow}>
                <Pressable
                  onPress={() => onOpenProfile(m.userId)}
                  accessibilityRole="button"
                  style={{ flexDirection: 'row', alignItems: 'center', gap: 12, flex: 1, minWidth: 0 }}
                >
                  <View style={styles.liveAvatarWrap}>
                    <Avatar uri={m.profileImageUrl} size={44} ring={T2.brandSoft} />
                    <View style={styles.liveAvatarDot} />
                  </View>
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                      <Text style={styles.mateRowName} numberOfLines={1}>{m.nickname}</Text>
                      <View style={styles.mateBadge}><Text style={styles.mateBadgeText}>메이트</Text></View>
                    </View>
                    {m.togetherCount > 0 ? (
                      <Text style={styles.mateLiveMeta} numberOfLines={1}>같이 {m.togetherCount}회 먹었어요</Text>
                    ) : null}
                  </View>
                </Pressable>
                <Pressable style={styles.mateCtaSolid} onPress={onMeal}>
                  <Text style={styles.mateCtaSolidText}>같이 먹기</Text>
                </Pressable>
              </View>
            ))}
          </View>
        </View>
      ) : null}

      {/* 즐겨찾기에 담은 메이트 — 섹션 라벨 + 아바타 스택 + 저장 수(내 메이트 포함). 목업 정합. */}
      {savedCount > 0 ? (
        <View style={styles.mateSection}>
          <View style={styles.mateSectionHead}>
            <Text style={styles.mateSectionTitle}>즐겨찾기에 담은 메이트</Text>
            <Text style={styles.mateSectionCount}>{savedCount}</Text>
          </View>
          {savedShown.length > 0 ? (
            <View style={styles.mateSavedRow}>
              <View style={styles.mateAvatarStack}>
                {savedShown.map((s, i) => (
                  <View key={s.userId} style={{ marginLeft: i ? -10 : 0 }}>
                    <Avatar uri={s.profileImageUrl} size={38} ring={T2.bg} />
                  </View>
                ))}
                {savedOverflow > 0 ? (
                  <View style={styles.mateSavedMore}>
                    <Text style={styles.mateSavedMoreText}>+{savedOverflow}</Text>
                  </View>
                ) : null}
              </View>
              <Text style={styles.mateSavedText}>
                <Text style={{ fontWeight: '800', color: T2.text }}>메이트 {savedMates.length}명</Text>을 포함해{'\n'}
                {savedCount}명이 이 식당을 저장했어요
              </Text>
            </View>
          ) : (
            // 저장한 메이트 아바타가 없으면(비메이트 저장자만) 텍스트만
            <Text style={styles.mateSavedText}>
              <Text style={{ fontWeight: '800', color: T2.text }}>{savedCount}명</Text>이 이 식당을 저장했어요
            </Text>
          )}
        </View>
      ) : null}
    </View>
  );
}

/* ── 주변 탭 ─────────────────────────────────────── */
type NearbySort = 'distance' | 'solo';
const NEARBY_SORTS: { key: NearbySort; label: string }[] = [
  { key: 'distance', label: '가까운 순' },
  { key: 'solo', label: '혼밥 좋은 순' },
];

function NearbyTab({
  placeId,
  placeName,
  center,
  onOpen,
}: {
  placeId: number;
  placeName: string;
  center: { lat: number; lng: number } | null;
  onOpen: (placeId: number, name: string) => void;
}) {
  const [sort, setSort] = useState<NearbySort>('distance');
  // 중심 = 이 식당 좌표. 좌표가 아직 없으면(detail 로딩) 호출하지 않는다.
  const q = useNearby(center ?? { lat: 0, lng: 0 }, 1000, center != null);
  const rows = (q.data?.content ?? []).filter((r) => r.placeId !== placeId); // 자기 자신 제외
  // 정렬 — 가까운 순(거리 asc) / 혼밥 좋은 순(친화도 desc, 평가 없으면 뒤로·거리로 동점 처리).
  const sorted = [...rows].sort((a, b) => {
    if (sort === 'solo') {
      const ra = a.avgSoloFriendlyRating;
      const rb = b.avgSoloFriendlyRating;
      if (ra == null && rb == null) return a.distanceMeters - b.distanceMeters;
      if (ra == null) return 1;
      if (rb == null) return -1;
      if (rb !== ra) return rb - ra;
      return a.distanceMeters - b.distanceMeters;
    }
    return a.distanceMeters - b.distanceMeters;
  });
  const showList = center != null && !q.isLoading && !q.isError && rows.length > 0;

  return (
    <View style={{ marginTop: 12 }}>
      <View style={styles.nearbyNote}>
        <Icon name="pin" size={13} color={T2.textMute} />
        <Text style={styles.nearbyNoteText} numberOfLines={1}>{placeName} 주변 · 1km 이내</Text>
      </View>

      {/* 정렬 칩 — 목록이 있을 때만 */}
      {showList ? (
        <View style={styles.nearbySortRow}>
          {NEARBY_SORTS.map((s) => {
            const on = sort === s.key;
            return (
              <Pressable
                key={s.key}
                onPress={() => setSort(s.key)}
                accessibilityRole="button"
                accessibilityState={{ selected: on }}
                style={[styles.nearbySortChip, { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border }]}
              >
                <Text style={[styles.nearbySortText, { color: on ? '#fff' : T2.textSub }]}>{s.label}</Text>
              </Pressable>
            );
          })}
        </View>
      ) : null}

      {center == null || q.isLoading ? (
        <View style={styles.nearbyStateBox}>
          <ActivityIndicator color={T2.brand} />
        </View>
      ) : q.isError ? (
        <Text style={styles.nearbyStateText}>주변 정보를 불러오지 못했어요.</Text>
      ) : sorted.length === 0 ? (
        <Text style={styles.nearbyStateText}>주변에 등록된 식당이 없어요.</Text>
      ) : (
        <View style={{ marginTop: 6 }}>
          {sorted.map((r, i, arr) => (
            <Pressable
              key={r.placeId}
              onPress={() => onOpen(r.placeId, r.name)}
              style={[styles.nearbyRow, i < arr.length - 1 && styles.infoDivider]}
            >
              {/* 대표 사진(리뷰 사진 최신) — 없으면 placeholder */}
              {r.photoUrls?.length ? (
                <Image source={{ uri: r.photoUrls[0] }} style={styles.nearbyThumb} />
              ) : (
                <ImagePlaceholder w={56} h={56} radius={12} tag="" />
              )}
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.nearbyName} numberOfLines={1}>{r.name}</Text>
                {r.category ? (
                  <Text style={styles.nearbyCat} numberOfLines={1}>{r.category}</Text>
                ) : null}
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 7 }}>
                  <Text style={styles.walkText}>도보 {walkingMinutes(r.distanceMeters)}분</Text>
                  <View style={styles.dotSep} />
                  <Text style={styles.nearbyDist}>{formatDistance(r.distanceMeters)}</Text>
                  {/* 지금 모집 중이면 라이브 뱃지(거리 뒤 인라인) */}
                  {r.seekingCount > 0 ? (
                    <View style={styles.nearbyActive}>
                      <View style={styles.nearbyActiveDot} />
                      <Text style={styles.nearbyActiveText}>모집 {r.seekingCount}</Text>
                    </View>
                  ) : null}
                </View>
              </View>
              {/* 혼밥 친화도 — 평가 있을 때만(없으면 정직하게 생략) */}
              {r.avgSoloFriendlyRating != null ? (
                <View style={styles.nearbyScore}>
                  <Text style={styles.nearbyScoreNum}>★ {r.avgSoloFriendlyRating.toFixed(1)}</Text>
                  <Text style={styles.nearbyScoreLabel}>혼밥친화</Text>
                </View>
              ) : null}
            </Pressable>
          ))}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: T2.bg },
  content: { paddingHorizontal: 20, paddingTop: 28 },

  floatHeader: { position: 'absolute', left: 16, right: 16, flexDirection: 'row', justifyContent: 'space-between' },
  honbabFloat: { position: 'absolute', left: 16, right: 16, zIndex: 50 },
  circleBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.95)',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 6,
    elevation: 3,
  },
  circleArrow: { fontSize: 18, color: T2.text },

  eyebrow: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6 },
  title: { fontSize: 26, fontWeight: '800', color: T2.text, letterSpacing: -0.8, lineHeight: 30 },

  addrRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 12 },
  addrTap: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 4 },
  addr: { flexShrink: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.3, lineHeight: 18 },
  addrRest: { marginLeft: 25, marginTop: 3, fontSize: 13, color: T2.text, letterSpacing: -0.3, lineHeight: 18 },
  copyBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: T2.border,
    backgroundColor: '#fff',
  },
  copyText: { fontSize: 12, fontWeight: '700', letterSpacing: -0.2 },

  ratingRow: { flexDirection: 'row', alignItems: 'baseline', gap: 14, marginTop: 14, paddingBottom: 18, borderBottomWidth: 1, borderBottomColor: T2.border },
  ratingNum: { fontSize: 24, fontWeight: '800', letterSpacing: -0.8, color: T2.text },
  ratingLabel: { fontSize: 11, color: T2.textMute, marginTop: 2 },
  ratingDivider: { width: 1, height: 32, backgroundColor: T2.border },

  tabs: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 4, borderBottomWidth: 1, borderBottomColor: T2.border },
  tab: { flex: 1, paddingVertical: 12, alignItems: 'center' },
  tabLabel: { fontSize: 15, letterSpacing: -0.3, textAlign: 'center' },
  tabUnderline: { position: 'absolute', left: 0, right: 0, bottom: -1, height: 2, backgroundColor: T2.brand },

  // 홈 탭 상단 리듬 — 14(짝을 이루는 통계 카드끼리) / 24(성격이 다른 섹션 사이) 두 값만 사용
  card: { marginTop: 24, padding: 20, borderRadius: 16, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border },
  sectionTitle: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6 },
  sectionGap: { marginTop: 24 },
  scoreBig: { fontSize: 34, fontWeight: '800', color: T2.text, letterSpacing: -1.5 },
  scoreOutOf: { fontSize: 14, fontWeight: '600', color: T2.textMute },
  scorePill: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 999, backgroundColor: T2.brandSoft },
  scorePillText: { fontSize: 12, fontWeight: '800', color: T2.brand, letterSpacing: -0.3 },
  scoreNote: { fontSize: 11, color: T2.textMute, marginTop: 7, letterSpacing: -0.2 },
  barTrack: { flex: 1, height: 6, borderRadius: 3, backgroundColor: T2.border, overflow: 'hidden' },
  barFill: { height: '100%', backgroundColor: T2.brand, borderRadius: 3 },
  cardHr: { height: 1, backgroundColor: T2.border, marginTop: 18, marginBottom: 16 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  friendlyInline: { marginTop: 20 },
  friendlyHeadRow: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between' },
  friendlyTitle: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  friendlyScoreRow: { flexDirection: 'row', alignItems: 'baseline', gap: 7, marginTop: 8 },
  friendlyScore: { fontSize: 20, fontWeight: '800', color: T2.text, letterSpacing: -0.5 },
  friendlyDot: { fontSize: 14, color: T2.textMute },
  friendlyLabel: { fontSize: 15, fontWeight: '800', color: T2.brand, letterSpacing: -0.3 },
  friendlyBars: { flexDirection: 'row', gap: 5, marginTop: 12 },
  friendlyCount: { fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },
  friendlyChip: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 13, paddingVertical: 8, borderRadius: 999, borderWidth: 1 },

  // 사회적 증거 카드 (홈 탭 최상단 — 누적 혼밥러 + 붐비는 시간대)
  socialCard: { marginTop: 24, paddingTop: 22, borderTopWidth: 1, borderTopColor: T2.border }, // 박스 아닌 구분선 인라인 섹션
  socialEyebrowRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  socialEmptyText: { marginTop: 12, fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3, lineHeight: 21 },
  socialCaption: { marginTop: 10, fontSize: 15, color: T2.text, letterSpacing: -0.3 },
  socialCaptionNum: { color: T2.brand, fontWeight: '800' },
  socialPeakLine: { marginTop: 14, fontSize: 13, color: T2.textSub, letterSpacing: -0.2 },
  socialPeakName: { color: T2.text, fontWeight: '800' },
  barsRow: { flexDirection: 'row', marginTop: 12 },
  barCol: { flex: 1, alignItems: 'center' },
  barCount: { marginBottom: 4, fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: -0.2 },
  barCountPeak: { color: T2.brand },
  barColTrack: { height: SOCIAL_BAR_MAX, justifyContent: 'flex-end' },
  periodBar: { width: 26, borderTopLeftRadius: 4, borderTopRightRadius: 4 },
  periodLabel: { marginTop: 8, fontSize: 11, fontWeight: '600', color: T2.textMute, letterSpacing: -0.2 },
  periodLabelPeak: { color: T2.text, fontWeight: '800' },
  periodRange: { marginTop: 2, fontSize: 9, color: T2.textMute, letterSpacing: -0.2 },

  detailErrBanner: { marginTop: 12, paddingVertical: 12, paddingHorizontal: 14, borderRadius: 10, backgroundColor: T2.brandSoft },
  detailErrText: { fontSize: 13, fontWeight: '700', color: T2.brand, letterSpacing: -0.2, textAlign: 'center' },

  mealCard: { marginTop: 24, padding: 18, borderRadius: 16, backgroundColor: T2.brandSoft, borderWidth: 1, borderColor: 'rgba(255,90,31,0.15)' },
  liveDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: T2.brand },
  liveLabel: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: 0.4 },
  mealText: { flex: 1, fontSize: 13, color: T2.text, lineHeight: 18, fontWeight: '500' },
  mealCardBtn: { paddingHorizontal: 12, paddingVertical: 7, borderRadius: 9, backgroundColor: T2.brand },
  mealCardBtnText: { fontSize: 12.5, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  infoRow: { flexDirection: 'row', alignItems: 'center', gap: 16, paddingVertical: 12 },
  infoDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  infoKey: { width: 56, fontSize: 13, fontWeight: '700', color: T2.textMute, letterSpacing: -0.2 },
  infoVal: { flex: 1, fontSize: 14, lineHeight: 21, letterSpacing: -0.3 },
  telBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 11, paddingVertical: 6, borderRadius: 8, borderWidth: 1, borderColor: T2.border },
  telText: { fontSize: 12, fontWeight: '700', color: T2.text, letterSpacing: -0.2 },

  menuRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14 },
  menuName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  bestBadge: { backgroundColor: T2.brand, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  bestText: { fontSize: 10, fontWeight: '700', color: '#fff' },
  menuDesc: { fontSize: 12, color: T2.textMute, marginTop: 4 },
  menuPrice: { fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.2 },

  reviewHead: { flexDirection: 'row', alignItems: 'center', paddingTop: 4, paddingBottom: 16 },
  reviewHeadTitle: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  reviewHeadCount: { fontSize: 13, color: T2.textMute },
  writeBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 14, paddingVertical: 9, borderRadius: 10, backgroundColor: T2.brand },
  writeText: { fontSize: 13, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },

  // 리뷰 탭의 안내 문구(없음·실패 공용). 기존에는 padding만 있어 왼쪽에 붙어 보였다 —
  // 목록이 비어 화면이 넓게 남는 자리라 가운데 정렬이 맞다.
  reviewNotice: { paddingVertical: 40, paddingHorizontal: 24, color: T2.textMute, textAlign: 'center', lineHeight: 22 },
  reviewCard: { paddingBottom: 22, marginBottom: 22 },
  reviewDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  reviewReport: { fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },
  // 작성자 이름은 눌러도(프로필 이동) 안 눌러도(탈퇴자) 같은 모양 — 탈퇴자만 회색으로 뜨면 낙인이 된다.
  reviewAuthor: { fontSize: 14, fontWeight: '700', color: T2.text },
  heroCounter: { position: 'absolute', right: 12, bottom: 12, backgroundColor: 'rgba(0,0,0,0.55)', borderRadius: 12, paddingHorizontal: 10, paddingVertical: 4 },
  heroCounterText: { color: '#fff', fontSize: 12, fontWeight: '700' },

  photoGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 },
  tabEmpty: { textAlign: 'center', color: T2.textMute, fontSize: 13, paddingVertical: 32 },

  // 메이트 탭
  // 요약 배너
  mateSummaryCard: { marginTop: 18, padding: 16, borderRadius: 16, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border, flexDirection: 'row', alignItems: 'center', gap: 13 },
  mateAvatarStack: { flexDirection: 'row' },
  mateSummaryLine: { fontSize: 14, fontWeight: '600', color: T2.text, letterSpacing: -0.3, lineHeight: 20 },
  mateSummarySub: { marginTop: 2, fontSize: 13, color: T2.textSub, letterSpacing: -0.3, lineHeight: 18 },
  // 섹션 라벨(지금 모집중 / 즐겨찾기)
  mateSection: { marginTop: 26 },
  mateSectionHead: { flexDirection: 'row', alignItems: 'baseline', gap: 7, marginBottom: 12 },
  mateSectionTitle: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6 },
  mateSectionCount: { fontSize: 12, fontWeight: '700', color: T2.textMute },
  // 라이브 카드(각 메이트 한 박스)
  mateLiveRow: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14, borderRadius: 14, backgroundColor: T2.brandSoft, borderWidth: 1, borderColor: 'rgba(255,90,31,0.15)' },
  liveAvatarWrap: { position: 'relative' },
  liveAvatarDot: { position: 'absolute', right: -1, bottom: -1, width: 13, height: 13, borderRadius: 6.5, backgroundColor: T2.brand, borderWidth: 2.5, borderColor: T2.brandSoft },
  mateBadge: { backgroundColor: '#fff', borderWidth: 1, borderColor: 'rgba(255,90,31,0.25)', borderRadius: 5, paddingHorizontal: 6, paddingVertical: 2 },
  mateBadgeText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  mateLiveMeta: { fontSize: 12, color: T2.textSub, marginTop: 3, letterSpacing: -0.2 },
  mateCtaSolid: { alignSelf: 'center', paddingHorizontal: 14, paddingVertical: 9, borderRadius: 10, backgroundColor: T2.brand },
  mateCtaSolidText: { fontSize: 13, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  mateRowName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  // 즐겨찾기 사회 증거
  mateSavedRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingHorizontal: 2 },
  mateSavedMore: { marginLeft: -10, width: 38, height: 38, borderRadius: 19, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  mateSavedMoreText: { fontSize: 12, fontWeight: '700', color: T2.textSub },
  mateSavedText: { flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.3, lineHeight: 18 },

  // 주변 탭
  nearbyNote: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 14 },
  nearbyNoteText: { flexShrink: 1, fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },
  nearbySortRow: { flexDirection: 'row', gap: 8, marginBottom: 4 },
  nearbySortChip: { paddingHorizontal: 15, paddingVertical: 8, borderRadius: 999, borderWidth: 1 },
  nearbySortText: { fontSize: 13, fontWeight: '700', letterSpacing: -0.3 },
  nearbyThumb: { width: 56, height: 56, borderRadius: 12, backgroundColor: T2.border },
  nearbyScore: { alignItems: 'flex-end', flexShrink: 0 },
  nearbyScoreNum: { fontSize: 16, fontWeight: '800', color: T2.brand, letterSpacing: -0.4 },
  nearbyScoreLabel: { fontSize: 10, color: T2.textMute, marginTop: 1 },
  nearbyRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14 },
  nearbyName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.4 },
  nearbyCat: { fontSize: 12, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 },
  walkText: { fontSize: 12, fontWeight: '700', color: T2.text, letterSpacing: -0.2 },
  dotSep: { width: 2, height: 2, borderRadius: 1, backgroundColor: T2.textMute },
  nearbyDist: { fontSize: 12, color: T2.textMute },
  nearbyStateBox: { paddingVertical: 40, alignItems: 'center', justifyContent: 'center' },
  nearbyStateText: { paddingVertical: 36, textAlign: 'center', fontSize: 13, color: T2.textMute, letterSpacing: -0.2 },
  nearbyActive: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 9, paddingVertical: 5, borderRadius: 999, backgroundColor: T2.brandSoft },
  nearbyActiveDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: T2.brand },
  nearbyActiveText: { fontSize: 12, fontWeight: '800', color: T2.brand, letterSpacing: -0.2 },

  // 리뷰탭 칩
  tasteChip: { paddingHorizontal: 9, paddingVertical: 5, borderRadius: 8, backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border },
  tasteText: { fontSize: 12, fontWeight: '800', color: T2.text },
  honbabChip: { paddingHorizontal: 9, paddingVertical: 5, borderRadius: 8, backgroundColor: T2.brandSoft, borderWidth: 1, borderColor: 'rgba(255,90,31,0.18)' },
  honbabText: { fontSize: 12, fontWeight: '800', color: T2.brand },
  tagChip: { paddingHorizontal: 9, paddingVertical: 4, borderRadius: 999, backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border },
  tagChipText: { fontSize: 11, fontWeight: '600', color: T2.textMute },

  ctaBar: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: 'row',
    gap: 10,
    paddingHorizontal: 16,
    paddingTop: 12,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: T2.border,
  },
  ctaNav: { width: 56, borderRadius: 12, backgroundColor: T2.bg, alignItems: 'center', justifyContent: 'center', gap: 3 },
  ctaNavText: { fontSize: 10, fontWeight: '700', color: T2.textSub, letterSpacing: -0.2 },
  ctaMate: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: T2.brand,
  },
  ctaMateText: { fontSize: 14, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 },
  ctaHonbab: { flex: 1, paddingVertical: 16, borderRadius: 12, backgroundColor: T2.brand, alignItems: 'center', justifyContent: 'center' },
  ctaHonbabText: { fontSize: 14, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  ctaHonbabOn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 7,
    paddingVertical: 16,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1.5,
    borderColor: T2.brand,
  },
  ctaHonbabOnText: { fontSize: 14, fontWeight: '800', color: T2.brand, letterSpacing: -0.3 },
  ctaPulse: { width: 8, height: 8, alignItems: 'center', justifyContent: 'center' },
  ctaHalo: { position: 'absolute', width: 14, height: 14, borderRadius: 7, backgroundColor: T2.brand, opacity: 0.25 },
  ctaDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: T2.brand },
});
