// RestaurantDetail — 식당 상세 (원본: screens/RestaurantDetail.jsx)
// 풀블리드 히어로 + 플로팅 헤더 + 6탭(홈/메뉴/리뷰/사진/메이트/주변) + 하단 고정 CTA.
// 원본의 하단 MinTabBar는 제거(상세는 탭 위로 push되는 풀스크린이라 뒤로가기로 복귀).
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Dimensions, ActivityIndicator, Alert } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { ImagePlaceholder, Avatar, Icon, HonbabStatusBar, HONBAB_BAR_H } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { usePlaceDetail } from '@/features/place/queries';
import { useActiveDiners, useMyCheckIn, useStartCheckIn, useEndCheckIn } from '@/features/checkin/queries';
import type { ActiveDiner } from '@/features/checkin/api';
import { formatElapsed } from '@/shared/format';
import type { RootStackScreenProps } from '@/navigation/types';
import { usePlaceReviews, usePlaceReviewSummary, useDeleteReview } from '@/features/review/queries';
import type { PlaceReview, PlaceReviewSummary } from '@/features/review/api';

type Tab = 'home' | 'menu' | 'review' | 'photo' | 'mate' | 'nearby';
const TABS: { key: Tab; label: string }[] = [
  { key: 'home', label: '홈' },
  { key: 'menu', label: '메뉴' },
  { key: 'review', label: '리뷰' },
  { key: 'photo', label: '사진' },
  { key: 'mate', label: '메이트' },
  { key: 'nearby', label: '주변' },
];

const INFO: { k: string; v: string; tel?: boolean; link?: boolean }[] = [
  { k: '전화', v: '02-322-1014', tel: true },
  { k: '영업시간', v: '매일 10:00 – 21:00 · 브레이크 15–17시' },
  { k: '홈페이지', v: 'instagram.com/keun_sundubu', link: true },
];
const FACILITIES = [
  { l: '무료 와이파이', on: true },
  { l: '포장 가능', on: true },
  { l: '예약 가능', on: false },
  { l: '남녀 화장실 구분', on: true },
  { l: '주차', on: false },
  { l: '단체석', on: false },
];
const MENU = [
  { n: '얼큰순두부', d: '소·중·대', p: '9,000', best: true },
  { n: '굴순두부', d: '겨울 한정', p: '10,000', best: false },
  { n: '버섯들깨순두부', d: '담백한 맛', p: '9,500', best: false },
  { n: '순두부 정식', d: '+제육 1인', p: '12,000', best: false },
  { n: '공기밥 추가', d: '', p: '1,000', best: false },
];

// ── 메이트 탭 데이터 ──
const LIVE_MATES = [
  { n: '지현', init: '지', bg: '#171717', tag: '대화 환영', mutual: '같이 3회', here: '바테이블 · 12분째' },
];
const VISITED = [
  { n: '연남또일이', init: '연', bg: '#525252', mood: '조용히', together: 2, visits: 4, score: '4.8', last: '3일 전' },
  { n: '순두부조아', init: '순', bg: '#7C7C7C', mood: '대화 환영', together: 1, visits: 6, score: '4.5', last: '1주 전' },
  { n: '혼밥부장', init: '혼', bg: '#171717', mood: '대화 환영', together: 0, visits: 3, score: '4.6', last: '2주 전' },
];
const SAVED = [
  { init: '미', bg: '#171717' },
  { init: '도', bg: '#525252' },
  { init: '하', bg: '#7C7C7C' },
  { init: '진', bg: '#A3A3A3' },
];

// ── 주변 탭 데이터 ──
type NearbyFilter = 'honbab' | 'cafe' | 'parking';
const NEARBY_TABS: { key: NearbyFilter; label: string }[] = [
  { key: 'honbab', label: '혼밥 맛집' },
  { key: 'cafe', label: '카페' },
  { key: 'parking', label: '주차' },
];
const NEARBY: Record<NearbyFilter, { n: string; cat: string; walk: string; dist: string; score: string; tag: string }[]> = {
  honbab: [
    { n: '연남 김밥상회', cat: '분식 · 김밥', walk: '도보 2분', dist: '140m', score: '4.7', tag: '1인석' },
    { n: '혼밀라멘 연남', cat: '일식 · 라멘', walk: '도보 4분', dist: '260m', score: '4.6', tag: '바테이블' },
    { n: '오늘의 덮밥', cat: '한식 · 덮밥', walk: '도보 5분', dist: '320m', score: '4.5', tag: '칸막이' },
    { n: '연남 우동집', cat: '일식 · 우동', walk: '도보 6분', dist: '400m', score: '4.3', tag: '1인석' },
  ],
  cafe: [
    { n: '연남 로스터스', cat: '카페 · 디저트', walk: '도보 1분', dist: '90m', score: '4.6', tag: '콘센트' },
    { n: '책읽는 고양이', cat: '북카페', walk: '도보 3분', dist: '210m', score: '4.4', tag: '조용함' },
    { n: '느린오후', cat: '카페', walk: '도보 5분', dist: '330m', score: '4.2', tag: '1인석' },
  ],
  parking: [
    { n: '연남공영주차장', cat: '공영 · 시간당 1,200원', walk: '도보 3분', dist: '200m', score: '여유', tag: '32면' },
    { n: '연남로 노상주차', cat: '노상 · 시간당 1,000원', walk: '도보 2분', dist: '150m', score: '혼잡', tag: '8면' },
  ],
};

const GRID_W = (Dimensions.get('window').width - 40 - 12) / 3;

export function RestaurantDetailScreen({ navigation, route }: RootStackScreenProps<'RestaurantDetail'>) {
  const insets = useSafeAreaInsets();
  const [stab, setStab] = useState<Tab>('home');
  const [copied, setCopied] = useState(false);
  const placeId = route.params.placeId;
  const detail = usePlaceDetail(placeId);
  const diners = useActiveDiners(placeId);
  const myCheckIn = useMyCheckIn();
  const startMut = useStartCheckIn();
  const endMut = useEndCheckIn();
  const name = detail.data?.name ?? route.params.name ?? '식당';
  const honbabOn = myCheckIn.data?.status === 'ACTIVE' && myCheckIn.data.placeId === placeId;
  const reviews = usePlaceReviews(placeId);
  const summary = usePlaceReviewSummary(placeId);

  const delMut = useDeleteReview();
  const confirmDelete = (reviewId: number) =>
    Alert.alert('리뷰 삭제', '이 리뷰를 삭제할까요? 되돌릴 수 없어요.', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: () => delMut.mutate(reviewId) },
    ]);
  const editReview = (r: PlaceReview) =>
    navigation.navigate('DiningLogWrite', {
      placeId,
      placeName: name,
      reviewId: r.reviewId,
      initial: { taste: r.tasteRating, honbab: r.soloFriendlyRating, tags: r.tags, content: r.content ?? '' },
    });

  const copy = () => {
    setCopied(true);
    setTimeout(() => setCopied(false), 1200);
  };
  const goMealRequest = () => navigation.navigate('MealRequest', { placeId, placeName: name });
  const toggleHonbab = () => {
    if (honbabOn && myCheckIn.data) endMut.mutate(myCheckIn.data.checkInId);
    else startMut.mutate(placeId);
  };

  return (
    <View style={styles.root}>
      <ScrollView contentContainerStyle={{ paddingBottom: 120 }} showsVerticalScrollIndicator={false}>
        {/* 히어로 */}
        <ImagePlaceholder w="100%" h={320} radius={0} tag="대표 사진 · 1/24" />

        <View style={styles.content}>
          {/* 카테고리 + 영업 */}
          <View style={{ marginBottom: 10 }}>
            <Text style={styles.eyebrow}>{detail.data?.category ?? '식당'}</Text>
            {detail.data?.businessStatus ? (
              <View style={styles.openBadge}>
                <Text style={styles.openText}>{detail.data.businessStatus}</Text>
              </View>
            ) : null}
          </View>

          <Text style={styles.title}>{name}</Text>

          {/* 주소 + 복사 */}
          <View style={styles.addrRow}>
            <Icon name="pin" size={15} color={T2.textMute} />
            <Text style={styles.addr}>{detail.data?.roadAddress ?? detail.data?.address ?? '주소 정보 없음'}</Text>
            <Pressable style={styles.copyBtn} onPress={copy}>
              <Icon name="copy" size={13} color={copied ? T2.brand : T2.textSub} />
              <Text style={[styles.copyText, { color: copied ? T2.brand : T2.textSub }]}>{copied ? '복사됨' : '복사'}</Text>
            </Pressable>
          </View>

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

          {stab === 'home' && <HomeTab diners={diners.data ?? []} onMeal={goMealRequest} summary={summary.data} />}
          {stab === 'menu' && <MenuTab />}
          {stab === 'review' && (
            <ReviewTab
              reviews={reviews.data ?? []}
              isLoading={reviews.isLoading}
              isError={reviews.isError}
              onWrite={() => navigation.navigate('DiningLogWrite', { placeId, placeName: name })}
              onEdit={editReview}
              onDelete={confirmDelete}
            />
          )}
          {stab === 'photo' && <PhotoTab />}
          {stab === 'mate' && <MateTab onMeal={goMealRequest} />}
          {stab === 'nearby' && <NearbyTab />}
        </View>
      </ScrollView>

      {/* 플로팅 헤더 (혼밥 중이면 상태 카드 아래로 내림) */}
      <View style={[styles.floatHeader, { top: insets.top + 8 + (honbabOn ? HONBAB_BAR_H + 10 : 0) }]}>
        <Pressable style={styles.circleBtn} onPress={() => navigation.goBack()} hitSlop={6}>
          <Text style={styles.circleArrow}>←</Text>
        </Pressable>
        <View style={{ flexDirection: 'row', gap: 8 }}>
          <View style={styles.circleBtn}>
            <Icon name="share" size={15} color={T2.text} />
          </View>
          <View style={styles.circleBtn}>
            <Icon name="heart" size={16} color={T2.text} />
          </View>
        </View>
      </View>

      {/* 하단 고정 CTA — 길찾기 · 같이 먹기 · 혼밥 시작/중 */}
      <View style={[styles.ctaBar, { paddingBottom: insets.bottom + 12 }]}>
        <Pressable style={styles.ctaNav}>
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
            <Text style={styles.ctaHonbabOnText}>혼밥 중</Text>
          </Pressable>
        ) : (
          <Pressable style={styles.ctaHonbab} onPress={toggleHonbab}>
            <Text style={styles.ctaHonbabText}>혼밥 시작</Text>
          </Pressable>
        )}
      </View>

      {/* 혼밥 중 상태 카드 — 켜져 있으면 최상단에 띄움 */}
      {honbabOn && (
        <View style={[styles.honbabFloat, { top: insets.top + 8 }]}>
          <HonbabStatusBar place={name} onEnd={toggleHonbab} />
        </View>
      )}
    </View>
  );
}

/* ── 홈 탭 ───────────────────────────────────────── */
function HomeTab({ diners, onMeal, summary }: { diners: ActiveDiner[]; onMeal: () => void; summary: PlaceReviewSummary | undefined }) {
  return (
    <View>
      {/* 혼밥 친화도 카드 */}
      <View style={styles.card}>
        <View style={{ flexDirection: 'row', alignItems: 'flex-end', gap: 12 }}>
          <View>
            <Text style={styles.sectionTitle}>혼밥 친화도</Text>
            <View style={{ flexDirection: 'row', alignItems: 'baseline', gap: 4, marginTop: 7 }}>
              <Text style={styles.scoreBig}>{summary?.avgSoloFriendlyRating?.toFixed(1) ?? '-'}</Text>
              <Text style={styles.scoreOutOf}>/ 5</Text>
            </View>
          </View>
          <View style={{ flex: 1 }} />
          <View style={{ alignItems: 'flex-end' }}>
            <View style={styles.scorePill}>
              <Text style={styles.scorePillText}>혼밥하기 아주 좋아요</Text>
            </View>
            <Text style={styles.scoreNote}>혼밥러 {summary?.reviewCount ?? 0}명 평가</Text>
          </View>
        </View>

        {/* 점수 바 */}
        <View style={{ flexDirection: 'row', gap: 5, marginTop: 16 }}>
          {[0, 1, 2, 3, 4].map((i) => {
            const fill = Math.max(0, Math.min(1, (summary?.avgSoloFriendlyRating ?? 0) - i));
            return (
              <View key={i} style={styles.barTrack}>
                <View style={[styles.barFill, { width: `${fill * 100}%` }]} />
              </View>
            );
          })}
        </View>

        <View style={styles.cardHr} />

        {/* 친화 요소 칩 */}
        {(summary?.topTags ?? []).length > 0 ? (
          <View style={styles.chipWrap}>
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

      {/* 지금 여기서 혼밥 중 (실데이터: 닉네임·경과만) */}
      <View style={styles.mealCard}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
          <View style={styles.liveDot} />
          <Text style={[styles.liveLabel, { flex: 1 }]}>지금 여기서 혼밥 중 · {diners.length}명</Text>
          <Pressable style={styles.mealCardBtn} onPress={onMeal} accessibilityRole="button">
            <Text style={styles.mealCardBtnText}>같이 먹기</Text>
          </Pressable>
        </View>
        {diners.length === 0 ? (
          <Text style={[styles.mealText, { marginTop: 12 }]}>아직 혼밥 중인 사람이 없어요.</Text>
        ) : (
          <View style={{ marginTop: 12, gap: 10 }}>
            {diners.map((d) => (
              <View key={d.checkInId} style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
                <Avatar name={d.nickname[0] ?? '?'} bg="#525252" size={32} />
                <Text style={{ flex: 1, fontSize: 14, fontWeight: '700', color: T2.text }}>{d.nickname}</Text>
                <Text style={{ fontSize: 12, color: T2.textMute }}>{formatElapsed(d.elapsedMinutes)}</Text>
              </View>
            ))}
          </View>
        )}
      </View>

      {/* 정보 */}
      <View style={{ marginTop: 28 }}>
        <Text style={styles.sectionTitle}>정보</Text>
        <View style={{ marginTop: 12 }}>
          {INFO.map((r, i, arr) => (
            <View key={r.k} style={[styles.infoRow, i < arr.length - 1 && styles.infoDivider]}>
              <Text style={styles.infoKey}>{r.k}</Text>
              <Text style={[styles.infoVal, { color: r.link ? T2.brand : T2.text, textDecorationLine: r.link ? 'underline' : 'none' }]}>
                {r.v}
              </Text>
              {r.tel ? (
                <Pressable style={styles.telBtn}>
                  <Icon name="phoneCall" size={13} color={T2.text} />
                  <Text style={styles.telText}>전화</Text>
                </Pressable>
              ) : null}
            </View>
          ))}
        </View>
      </View>

      {/* 편의시설 */}
      <View style={{ marginTop: 24 }}>
        <Text style={styles.sectionTitle}>편의시설</Text>
        <View style={[styles.chipWrap, { marginTop: 12 }]}>
          {FACILITIES.map((f) => (
            <View
              key={f.l}
              style={[
                styles.facilityChip,
                { backgroundColor: f.on ? '#fff' : T2.bg, borderColor: f.on ? T2.borderStrong : T2.border, opacity: f.on ? 1 : 0.55 },
              ]}
            >
              <Text style={{ fontSize: 12, fontWeight: '800', color: f.on ? T2.brand : T2.textMute }}>{f.on ? '✓' : '–'}</Text>
              <Text style={{ fontSize: 13, fontWeight: '600', color: f.on ? T2.text : T2.textMute, letterSpacing: -0.3 }}>{f.l}</Text>
            </View>
          ))}
        </View>
      </View>
    </View>
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
function ReviewTab({ reviews, isLoading, isError, onWrite, onEdit, onDelete }: {
  reviews: PlaceReview[];
  isLoading: boolean; isError: boolean; onWrite: () => void;
  onEdit: (r: PlaceReview) => void; onDelete: (reviewId: number) => void;
}) {
  return (
    <View style={{ marginTop: 4 }}>
      <View style={styles.reviewHead}>
        <View style={{ flex: 1, flexDirection: 'row', alignItems: 'baseline' }}>
          <Text style={styles.reviewHeadTitle}>리뷰</Text>
          <Text style={styles.reviewHeadCount}> {reviews.length}개</Text>
        </View>
        <Pressable style={styles.writeBtn} onPress={onWrite} accessibilityRole="button">
          <Icon name="pencil" size={14} color="#fff" />
          <Text style={styles.writeText}>리뷰 쓰기</Text>
        </Pressable>
      </View>

      {isLoading ? (
        <View style={{ padding: 24, alignItems: 'center' }}><ActivityIndicator color={T2.brand} /></View>
      ) : isError ? (
        <Text style={{ padding: 24, color: T2.textMute }}>리뷰를 불러오지 못했어요.</Text>
      ) : reviews.length === 0 ? (
        <Text style={{ padding: 24, color: T2.textMute }}>아직 리뷰가 없어요. 첫 리뷰를 남겨보세요.</Text>
      ) : (
        reviews.map((r, i, arr) => (
          <View key={r.reviewId} style={[styles.reviewCard, i < arr.length - 1 && styles.reviewDivider]}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
              <Text style={{ fontSize: 14, fontWeight: '700', color: T2.text }}>{r.user.nickname}</Text>
              {r.authenticated ? <Text style={{ fontSize: 11, color: T2.brand }}>✔ 인증</Text> : null}
            </View>
            {r.content ? <Text style={{ marginTop: 6, color: T2.textSub, lineHeight: 20 }}>{r.content}</Text> : null}
            <View style={{ flexDirection: 'row', gap: 6, marginTop: 8 }}>
              <View style={styles.tasteChip}><Text style={styles.tasteText}>맛 ★ {r.tasteRating.toFixed(1)}</Text></View>
              <View style={styles.honbabChip}><Text style={styles.honbabText}>혼밥 ★ {r.soloFriendlyRating.toFixed(1)}</Text></View>
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

/* ── 사진 탭 ─────────────────────────────────────── */
function PhotoTab() {
  return (
    <View style={styles.photoGrid}>
      {Array.from({ length: 9 }).map((_, i) => (
        <ImagePlaceholder key={i} w={GRID_W} h={GRID_W} radius={10} />
      ))}
    </View>
  );
}

/* ── 메이트 탭 ───────────────────────────────────── */
function Section({ title, count, children }: { title: string; count?: number; children: React.ReactNode }) {
  return (
    <View style={{ marginTop: 26 }}>
      <View style={styles.mateSectionHead}>
        <Text style={styles.mateSectionTitle}>{title}</Text>
        {count != null ? <Text style={styles.mateSectionCount}>{count}</Text> : null}
      </View>
      {children}
    </View>
  );
}

function MateTab({ onMeal }: { onMeal: () => void }) {
  return (
    <View style={{ marginTop: 8 }}>
      {/* 요약 배너 — 사회적 신뢰 */}
      <View style={styles.mateSummary}>
        <View style={{ flexDirection: 'row' }}>
          {['민', '도', '하'].map((c, i) => (
            <View key={c} style={{ marginLeft: i ? -10 : 0 }}>
              <Avatar name={c} bg={['#171717', '#525252', '#7C7C7C'][i]} size={36} ring="#fff" />
            </View>
          ))}
        </View>
        <Text style={styles.mateSummaryText}>
          내 메이트 <Text style={{ fontWeight: '800' }}>3명</Text>이 여기 다녀갔어요.{'\n'}
          <Text style={{ color: T2.textSub }}>믿고 혼밥하기 좋은 곳이에요.</Text>
        </Text>
      </View>

      {/* 지금 여기서 혼밥 중 */}
      <Section title="지금 여기서 혼밥 중" count={LIVE_MATES.length}>
        {LIVE_MATES.map((m) => (
          <View key={m.n} style={styles.liveMateCard}>
            <View>
              <Avatar name={m.init} bg={m.bg} size={44} ring={T2.brandSoft} />
              <View style={styles.liveMateDot} />
            </View>
            <View style={{ flex: 1, minWidth: 0 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <Text style={styles.mateName}>{m.n}</Text>
                <View style={styles.mateBadge}>
                  <Text style={styles.mateBadgeText}>메이트</Text>
                </View>
              </View>
              <Text style={styles.mateMeta}>{m.here} · {m.mutual}</Text>
            </View>
            <Pressable style={styles.mateCtaSolid} onPress={onMeal}>
              <Text style={styles.mateCtaSolidText}>같이 먹기</Text>
            </Pressable>
          </View>
        ))}
      </Section>

      {/* 다녀온 메이트 */}
      <Section title="다녀온 메이트" count={VISITED.length}>
        <View style={{ gap: 2 }}>
          {VISITED.map((m, i, arr) => (
            <View key={m.n} style={[styles.mateRow, i < arr.length - 1 && styles.infoDivider]}>
              <Avatar name={m.init} bg={m.bg} size={40} />
              <View style={{ flex: 1, minWidth: 0 }}>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                  <Text style={styles.mateRowName}>{m.n}</Text>
                  <View style={styles.moodChip}>
                    <Text style={styles.moodChipText}>{m.mood}</Text>
                  </View>
                </View>
                <Text style={styles.mateRowMeta}>
                  {m.together > 0 ? <Text style={{ color: T2.brand, fontWeight: '700' }}>같이 {m.together}회 · </Text> : null}
                  방문 {m.visits}회 · 혼밥친화 ★{m.score} · {m.last}
                </Text>
              </View>
              <Pressable
                style={[styles.mateCtaOutline, { borderColor: m.together > 0 ? T2.brand : T2.border }]}
                onPress={onMeal}
              >
                <Text style={[styles.mateCtaOutlineText, { color: m.together > 0 ? T2.brand : T2.textSub }]}>
                  {m.together > 0 ? '같이 먹기' : '메이트 신청'}
                </Text>
              </Pressable>
            </View>
          ))}
        </View>
      </Section>

      {/* 즐겨찾기에 담은 메이트 */}
      <Section title="즐겨찾기에 담은 메이트" count={12}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
          <View style={{ flexDirection: 'row' }}>
            {SAVED.map((s, i) => (
              <View key={s.init} style={{ marginLeft: i ? -10 : 0 }}>
                <Avatar name={s.init} bg={s.bg} size={38} ring={T2.bg} />
              </View>
            ))}
            <View style={styles.savedMore}>
              <Text style={styles.savedMoreText}>+8</Text>
            </View>
          </View>
          <Text style={styles.savedNote}>
            메이트 <Text style={{ color: T2.text, fontWeight: '800' }}>4명</Text>을 포함해{'\n'}12명이 이 식당을 저장했어요
          </Text>
        </View>
      </Section>
    </View>
  );
}

/* ── 주변 탭 ─────────────────────────────────────── */
function NearbyTab() {
  const [filter, setFilter] = useState<NearbyFilter>('honbab');
  const rows = NEARBY[filter];
  return (
    <View style={{ marginTop: 12 }}>
      {/* 안내 */}
      <View style={styles.nearbyNote}>
        <Icon name="pin" size={13} color={T2.textMute} />
        <Text style={styles.nearbyNoteText}>큰순두부 연남점 주변 · 카카오맵 기준</Text>
      </View>

      {/* 필터 칩 */}
      <View style={{ flexDirection: 'row', gap: 8, marginBottom: 4 }}>
        {NEARBY_TABS.map((t) => {
          const on = filter === t.key;
          return (
            <Pressable
              key={t.key}
              onPress={() => setFilter(t.key)}
              style={[styles.filterChip, { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border }]}
            >
              <Text style={[styles.filterChipText, { color: on ? '#fff' : T2.textSub }]}>{t.label}</Text>
            </Pressable>
          );
        })}
      </View>

      {/* 리스트 */}
      <View style={{ marginTop: 6 }}>
        {rows.map((r, i, arr) => (
          <View key={r.n} style={[styles.nearbyRow, i < arr.length - 1 && styles.infoDivider]}>
            <ImagePlaceholder w={56} h={56} radius={12} tag="" />
            <View style={{ flex: 1, minWidth: 0 }}>
              <Text style={styles.nearbyName}>{r.n}</Text>
              <Text style={styles.nearbyCat}>{r.cat}</Text>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 7 }}>
                <Text style={styles.walkText}>{r.walk}</Text>
                <View style={styles.dotSep} />
                <Text style={styles.nearbyDist}>{r.dist}</Text>
                <View style={styles.nearbyTag}>
                  <Text style={styles.nearbyTagText}>{r.tag}</Text>
                </View>
              </View>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              {filter === 'parking' ? (
                <Text style={[styles.parkingStatus, { color: r.score === '여유' ? '#1F8A5B' : T2.brand }]}>{r.score}</Text>
              ) : (
                <>
                  <Text style={styles.nearbyScore}>{r.score}</Text>
                  <Text style={styles.nearbyScoreLabel}>혼밥친화</Text>
                </>
              )}
            </View>
          </View>
        ))}
      </View>

      {/* 지도에서 보기 */}
      <Pressable style={styles.mapBtn}>
        <Icon name="navigate" size={16} color={T2.text} />
        <Text style={styles.mapBtnText}>지도에서 한눈에 보기</Text>
      </Pressable>
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
  openBadge: { alignSelf: 'flex-start', marginTop: 8, paddingHorizontal: 7, paddingVertical: 3, backgroundColor: T2.text, borderRadius: 4 },
  openText: { fontSize: 10, fontWeight: '700', color: '#fff', letterSpacing: 0.2 },
  title: { fontSize: 26, fontWeight: '800', color: T2.text, letterSpacing: -0.8, lineHeight: 30 },

  addrRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 12 },
  addr: { flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.3, lineHeight: 18 },
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

  card: { marginTop: 24, padding: 20, borderRadius: 16, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border },
  sectionTitle: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6 },
  scoreBig: { fontSize: 34, fontWeight: '800', color: T2.text, letterSpacing: -1.5 },
  scoreOutOf: { fontSize: 14, fontWeight: '600', color: T2.textMute },
  scorePill: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 999, backgroundColor: T2.brandSoft },
  scorePillText: { fontSize: 12, fontWeight: '800', color: T2.brand, letterSpacing: -0.3 },
  scoreNote: { fontSize: 11, color: T2.textMute, marginTop: 7, letterSpacing: -0.2 },
  barTrack: { flex: 1, height: 6, borderRadius: 3, backgroundColor: T2.border, overflow: 'hidden' },
  barFill: { height: '100%', backgroundColor: T2.brand, borderRadius: 3 },
  cardHr: { height: 1, backgroundColor: T2.border, marginTop: 18, marginBottom: 16 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  friendlyChip: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 13, paddingVertical: 8, borderRadius: 999, borderWidth: 1 },

  mealCard: { marginTop: 28, padding: 18, borderRadius: 16, backgroundColor: T2.brandSoft, borderWidth: 1, borderColor: 'rgba(255,90,31,0.15)' },
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

  facilityChip: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 12, paddingVertical: 9, borderRadius: 10, borderWidth: 1 },

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

  reviewCard: { paddingBottom: 18, marginBottom: 18 },
  reviewDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },

  photoGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 },

  // 메이트 탭
  mateSummary: { marginTop: 18, padding: 16, borderRadius: 16, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border, flexDirection: 'row', alignItems: 'center', gap: 13 },
  mateSummaryText: { flex: 1, fontSize: 13.5, color: T2.text, lineHeight: 20, fontWeight: '500', letterSpacing: -0.3 },
  mateSectionHead: { flexDirection: 'row', alignItems: 'baseline', gap: 7, marginBottom: 12 },
  mateSectionTitle: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6 },
  mateSectionCount: { fontSize: 12, fontWeight: '700', color: T2.textMute },
  liveMateCard: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 14, borderRadius: 14, backgroundColor: T2.brandSoft, borderWidth: 1, borderColor: 'rgba(255,90,31,0.15)' },
  liveMateDot: { position: 'absolute', right: -1, bottom: -1, width: 13, height: 13, borderRadius: 6.5, backgroundColor: T2.brand, borderWidth: 2.5, borderColor: T2.brandSoft },
  mateName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  mateBadge: { paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5, backgroundColor: '#fff', borderWidth: 1, borderColor: 'rgba(255,90,31,0.25)' },
  mateBadgeText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  mateMeta: { fontSize: 12, color: T2.textSub, marginTop: 3, letterSpacing: -0.2 },
  mateCtaSolid: { alignSelf: 'center', paddingHorizontal: 14, paddingVertical: 9, borderRadius: 10, backgroundColor: T2.brand },
  mateCtaSolidText: { fontSize: 13, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  mateRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 12 },
  mateRowName: { fontSize: 14.5, fontWeight: '700', color: T2.text, letterSpacing: -0.4 },
  moodChip: { paddingHorizontal: 7, paddingVertical: 2, borderRadius: 999, backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border },
  moodChipText: { fontSize: 11, fontWeight: '600', color: T2.textMute },
  mateRowMeta: { fontSize: 12, color: T2.textMute, marginTop: 4, letterSpacing: -0.2 },
  mateCtaOutline: { paddingHorizontal: 12, paddingVertical: 8, borderRadius: 9, backgroundColor: '#fff', borderWidth: 1.5 },
  mateCtaOutlineText: { fontSize: 12.5, fontWeight: '700', letterSpacing: -0.3 },
  savedMore: { marginLeft: -10, width: 38, height: 38, borderRadius: 19, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  savedMoreText: { fontSize: 12, fontWeight: '700', color: T2.textSub },
  savedNote: { flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.3, lineHeight: 18 },

  // 주변 탭
  nearbyNote: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 14 },
  nearbyNoteText: { fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },
  filterChip: { paddingHorizontal: 15, paddingVertical: 8, borderRadius: 999, borderWidth: 1 },
  filterChipText: { fontSize: 13, fontWeight: '700', letterSpacing: -0.3 },
  nearbyRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 14 },
  nearbyName: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.4 },
  nearbyCat: { fontSize: 12, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 },
  walkText: { fontSize: 12, fontWeight: '700', color: T2.text, letterSpacing: -0.2 },
  dotSep: { width: 2, height: 2, borderRadius: 1, backgroundColor: T2.textMute },
  nearbyDist: { fontSize: 12, color: T2.textMute },
  nearbyTag: { marginLeft: 2, paddingHorizontal: 8, paddingVertical: 2, borderRadius: 999, backgroundColor: T2.brandSoft, borderWidth: 1, borderColor: 'rgba(255,90,31,0.18)' },
  nearbyTagText: { fontSize: 11, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
  parkingStatus: { fontSize: 12, fontWeight: '800', letterSpacing: -0.2 },
  nearbyScore: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  nearbyScoreLabel: { fontSize: 10, color: T2.textMute, marginTop: 1 },
  mapBtn: { marginTop: 18, paddingVertical: 14, borderRadius: 12, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.border, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 7 },
  mapBtnText: { fontSize: 13.5, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },

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
