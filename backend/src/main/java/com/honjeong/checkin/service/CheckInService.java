package com.honjeong.checkin.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.badge.service.BadgeService;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.chat.service.ConversationService;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.dto.CheckInUserResponse;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.dto.PlaceCheckinSummaryResponse;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.config.HonjeongCheckInProperties;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.service.NotificationService;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: 혼밥 체크인 비즈니스 로직 — 시작(단일 활성 제약)·종료·취소·내 체크인 조회·사회적 증거 통계·지도 마커·모집중(SEEKING) 목록·TTL 자동 만료
 * 2. 사용 Controller: CheckInController, PlaceCheckInController (그 외 CheckInExpiryScheduler·MealRequestService에서도 사용)
 *
 * <p>[기존 주석] 체크인 도메인 서비스. 단일 활성 제약(같은 장소 멱등 / 다른 장소 409)·종료·내 체크인·통계·지도·모집중 목록·TTL 만료를 담당한다.
 *
 * <p>모든 시각은 주입된 {@link Clock}의 instant를 Asia/Seoul로 환산해 KST로 통일한다 — 통계 "오늘" 경계와 저장
 * 시각의 기준을 일치시켜 경계 어긋남을 막는다(전역 Clock 빈의 zone과 무관).
 */
@Service
public class CheckInService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 반경 상한(m). 이보다 큰 radius 요청은 이 값으로 줄인다(방어적 클램프).
    static final int MAX_RADIUS = 10_000;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private final CheckInRepository checkInRepository;
    private final PlaceService placeService;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final MealRequestRepository mealRequestRepository;
    private final NotificationService notificationService;
    private final Clock clock;
    private final HonjeongCheckInProperties props;
    private final BadgeService badgeService;
    private final ConversationService conversationService;

    public CheckInService(CheckInRepository checkInRepository, PlaceService placeService,
            UserRepository userRepository, BlockRepository blockRepository,
            MealRequestRepository mealRequestRepository, NotificationService notificationService,
            Clock clock, HonjeongCheckInProperties props, BadgeService badgeService,
            ConversationService conversationService) {
        this.checkInRepository = checkInRepository;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.blockRepository = blockRepository;
        this.mealRequestRepository = mealRequestRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.props = props;
        this.badgeService = badgeService;
        this.conversationService = conversationService;
    }

    /**
     * 기능: 혼밥 체크인(모집중)을 시작한다(같은 장소 멱등 반환, 다른 장소/이미 활동 중이면 409, 경쟁 시 인덱스 위반을 409로 변환)
     * Request: userId — 체크인하는 회원 ID, request — CheckInRequest(placeId)
     * Response: CheckInResponse — 새로 만들었거나 멱등 반환한 기존 체크인(SEEKING)
     *
     * <p>[기존 주석] 혼밥 체크인을 시작한다. placeId로 장소를 조회한 뒤, 기존 활성(SEEKING/ACTIVE/TOGETHER)이 있으면
     * 같은 장소로 아직 SEEKING인 경우만 멱등 반환하고, 그 외(다른 장소이거나 이미 ACTIVE/TOGETHER로 진행 중)는 409.
     * 경쟁으로 단일활성 인덱스가 위반되면 {@link DataIntegrityViolationException}을 409로 변환한다.
     *
     * @param userId  체크인하는 회원 id
     * @param request 선택한 장소 id를 담은 요청
     * @return 체크인 응답(새로 만들었거나 멱등 반환한 기존 체크인)
     */
    @Transactional
    public CheckInResponse createCheckIn(Long userId, CheckInRequest request) {
        Place place = placeService.getById(request.placeId());

        Optional<CheckIn> current = checkInRepository.findByUser_IdAndStatusIn(
                userId, List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE, CheckInStatus.TOGETHER));
        if (current.isPresent()) {
            CheckIn existing = current.get();
            if (existing.getStatus() == CheckInStatus.SEEKING
                    && existing.getPlace().getId().equals(place.getId())) {
                return CheckInResponse.from(existing);                        // 같은 장소 모집 → 멱등
            }
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_ACTIVE);    // 이미 활동 중 → 409
        }

        try {
            User userRef = userRepository.getReferenceById(userId);
            CheckIn saved = checkInRepository.save(CheckIn.startSeeking(userRef, place, now()));
            return CheckInResponse.from(saved);
        } catch (DataIntegrityViolationException e) {                         // 경쟁(인덱스 위반)
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_ACTIVE);
        }
    }

    /**
     * 기능: 체크인을 종료(ENDED)한다 — TOGETHER면 같은 매칭의 파트너 체크인도 함께 종료
     * Request: userId — 요청 회원 ID, checkInId — 종료할 체크인 ID
     * Response: CheckInResponse — 종료된(또는 이미 종료된) 요청자 본인의 체크인
     *
     * <p>[기존 주석] 체크인을 종료한다. 없으면 404, 본인 것이 아니면 403, 이미 ENDED면 멱등 반환한다.
     * TOGETHER면 같은 매칭(mealRequestId)의 파트너 체크인도 함께 ENDED 처리한다(같이먹기는 한쪽만 끝낼 수 없음).
     *
     * @param userId    요청 회원 id
     * @param checkInId 종료할 체크인 id
     * @return 종료된(또는 이미 종료된) 요청자 본인의 체크인 응답
     */
    @Transactional
    public CheckInResponse endCheckIn(Long userId, Long checkInId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));
        if (!checkIn.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        LocalDateTime now = now();
        if (checkIn.getStatus() == CheckInStatus.TOGETHER && checkIn.getMealRequestId() != null) {
            // 같은 매칭의 양쪽(나+파트너)을 함께 종료
            checkInRepository.findTogetherByMealRequestId(checkIn.getMealRequestId())
                    .forEach(c -> c.end(now));
            conversationService.close(checkIn.getMealRequestId()); // 매칭 종료 → 대화 닫기
        } else {
            checkIn.end(now);
            badgeService.checkAndAward(userId, true); // 솔로 완료 → 혼밥 뱃지 지급 체크
        }
        return CheckInResponse.from(checkIn);
    }

    /**
     * 기능: 체크인을 취소(CANCELLED)한다 — 짧은 혼밥/모집 포기 오집계 방지용, 소유자의 SEEKING·ACTIVE만 가능
     * Request: userId — 요청 회원 ID, checkInId — 취소할 체크인 ID
     * Response: CheckInResponse — 취소된 체크인
     *
     * <p>[기존 주석] 체크인을 취소(CANCELLED)한다. 짧은 혼밥/모집 포기 오집계 방지용 — 소유자의 SEEKING·ACTIVE만 취소 가능하다.
     *
     * @param userId    요청 회원 id
     * @param checkInId 취소할 체크인 id
     * @return 취소된 체크인 응답
     */
    @Transactional
    public CheckInResponse cancelCheckIn(Long userId, Long checkInId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));
        if (!checkIn.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (checkIn.getStatus() != CheckInStatus.SEEKING
                && checkIn.getStatus() != CheckInStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CHECKIN_NOT_ACTIVE);
        }
        LocalDateTime now = now();
        checkIn.cancel(now);
        // 모집을 접었으므로(그만두기) 이 체크인에 걸린 대기 신청은 수락 불가한 좀비 — 만료 처리
        mealRequestRepository.expirePendingByToCheckIn(checkInId, now);
        return CheckInResponse.from(checkIn);
    }

    /**
     * 기능: 모집중(SEEKING)을 혼밥중(ACTIVE)으로 전이한다 — 매칭 실패/포기 후 혼자 먹기 시작
     * Request: userId — 요청 회원 ID, checkInId — 전이할 체크인 ID
     * Response: CheckInResponse — 전이된 체크인 응답
     *
     * <p>[기존 주석] 모집중(SEEKING)을 혼밥중(ACTIVE)으로 전이한다 — 매칭 실패/포기 후 혼자 먹기 시작.
     * 없으면 404, 본인 것이 아니면 403, SEEKING이 아니면 409(CHECKIN_NOT_SEEKING).
     *
     * @param userId    요청 회원 id
     * @param checkInId 전이할 체크인 id
     * @return 전이된 체크인 응답
     */
    @Transactional
    public CheckInResponse dineAlone(Long userId, Long checkInId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));
        if (!checkIn.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (checkIn.getStatus() != CheckInStatus.SEEKING) {
            throw new BusinessException(ErrorCode.CHECKIN_NOT_SEEKING);
        }
        LocalDateTime now = now();
        checkIn.dineAlone(now);
        // 혼자 먹기로 전환하면 모집이 끝나므로, 이 체크인에 걸린 대기 신청을 만료 처리(SEEKING만 수락 가능하므로 좀비 방지)
        mealRequestRepository.expirePendingByToCheckIn(checkInId, now);
        return CheckInResponse.from(checkIn);
    }

    /**
     * 기능: 내 현재 체크인(SEEKING/ACTIVE/TOGETHER)을 조회한다 — TOGETHER면 파트너 닉네임 포함
     * Request: userId — 회원 ID
     * Response: CheckInResponse — 현재 진행 중 체크인(없으면 null)
     *
     * <p>[기존 주석] 내 현재 체크인(SEEKING/ACTIVE/TOGETHER)을 반환한다. 없으면 null.
     * TOGETHER면 파트너 닉네임을 함께 채워 앱이 "같이 먹는 중"을 렌더할 수 있게 한다.
     * SEEKING을 빠뜨리면 체크인 직후(createCheckIn이 만드는 상태) /me가 null을 반환해
     * "이미 활성"이라는 createCheckIn의 409 판단과 모순되고, 앱이 재시작 후 checkInId를
     * 복구할 수 없게 되므로 단일 활성 제약(SEEKING/ACTIVE/TOGETHER)과 항상 같은 상태 집합을 조회해야 한다.
     *
     * @param userId 회원 id
     * @return 현재 체크인 응답 또는 null
     */
    @Transactional(readOnly = true)
    public CheckInResponse getMyCurrentCheckIn(Long userId) {
        Optional<CheckIn> current = checkInRepository.findByUser_IdAndStatusIn(
                userId, List.of(CheckInStatus.SEEKING, CheckInStatus.ACTIVE, CheckInStatus.TOGETHER));
        if (current.isEmpty()) {
            return null;
        }
        CheckIn c = current.get();
        if (c.getStatus() != CheckInStatus.TOGETHER) {
            return CheckInResponse.from(c);
        }
        CheckIn partner = checkInRepository.findTogetherByMealRequestId(c.getMealRequestId()).stream()
                .filter(x -> !x.getUser().getId().equals(userId))
                .findFirst()
                .orElse(null);
        Long partnerUserId = partner != null ? partner.getUser().getId() : null;
        // DisplayNames로 감쌀 필요 없음: 탈퇴 시 AccountWithdrawalService.endOngoing이 TOGETHER 체크인
        // 양쪽을 즉시 end()하므로(한쪽만 끝내면 상대가 "같이 먹는 중"에 갇히기 때문), 여기 도달하는 시점
        // (c.getStatus()==TOGETHER)의 partner는 항상 비탈퇴 상태다. endOngoing이 TOGETHER 정리를
        // 건너뛰도록(정리 범위 축소) 바뀌면 이 가정이 깨지고 partnerNickname이 null로 새어나간다.
        String partnerNickname = partner != null ? partner.getUser().getNickname() : null;
        Long conversationId = conversationService.findIdByMealRequestId(c.getMealRequestId());
        return CheckInResponse.from(c, partnerUserId, partnerNickname, conversationId);
    }

    /**
     * 기능: 같이먹기(TOGETHER) 매칭을 깨고 내 체크인을 지정 상태로 전이 — 상대는 항상 SEEKING 복귀 + 알림 + 대화 닫기.
     * 내가 혼밥 계속(ACTIVE)/다시 모집(SEEKING)/취소(CANCELLED) 중 선택하고, 상대(노쇼자/취소자)는 SEEKING으로
     * 복귀시킨 뒤 MEAL_MATCH_CANCELLED 알림을 발행한다. (상대 식별은 leaveMatch 전에 mealRequestId로 먼저 조회)
     * mealRequestId는 mine.leaveMatch가 null로 지우므로 그 전에 캡처해 매칭 해체에 연동된 대화를 CLOSED로 닫는다
     * (노쇼/취소 경로도 endCheckIn·TTL 만료·차단 정리와 동일하게 "매칭 해체 → 대화 닫기" 불변식을 지켜야 한다).
     *
     * @param userId    요청 사용자(체크인 소유자여야 함)
     * @param checkInId 내 TOGETHER 체크인 id
     * @param to        전이할 내 상태(ACTIVE/SEEKING/CANCELLED만 허용)
     * @return 전이된 내 체크인 응답
     * @throws BusinessException INVALID_INPUT(to 부적절)/CHECKIN_NOT_FOUND(404)/FORBIDDEN(403)/CHECKIN_NOT_TOGETHER(409)
     */
    @Transactional
    public CheckInResponse leaveMatch(Long userId, Long checkInId, String toStr) {
        CheckInStatus to;
        try {
            to = CheckInStatus.valueOf(toStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "to는 ACTIVE/SEEKING/CANCELLED만 가능합니다.");
        }
        if (to != CheckInStatus.ACTIVE && to != CheckInStatus.SEEKING && to != CheckInStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "to는 ACTIVE/SEEKING/CANCELLED만 가능합니다.");
        }
        CheckIn mine = checkInRepository.findById(checkInId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));
        if (!mine.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (mine.getStatus() != CheckInStatus.TOGETHER) {
            throw new BusinessException(ErrorCode.CHECKIN_NOT_TOGETHER);
        }
        LocalDateTime now = now();
        // mine.leaveMatch로 mealRequestId가 지워지기 전에 먼저 캡처 — 상대 조회뿐 아니라 대화 닫기에도 필요.
        Long mrId = mine.getMealRequestId();
        // 상대(같은 매칭)는 SEEKING으로 복귀 + 알림
        checkInRepository.findTogetherByMealRequestId(mrId).stream()
                .filter(x -> !x.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(partner -> {
                    partner.leaveMatch(CheckInStatus.SEEKING, now);
                    notificationService.publish(partner.getUser().getId(), NotificationType.MEAL_MATCH_CANCELLED, userId);
                });
        mine.leaveMatch(to, now);
        if (mrId != null) {
            conversationService.close(mrId); // 매칭 해체(노쇼/취소) → 대화 닫기(읽기전용) — 방치 시 estranged 유저가 계속 메시지 가능
        }
        return CheckInResponse.from(mine);
    }

    /**
     * 기능: 사회적 증거 통계(오늘 혼밥한 사람 수·현재 혼밥 중 수·현재 모집중 수)를 집계한다
     * Request: 없음
     * Response: CheckInStatsResponse — todayCount(오늘 distinct 사용자), activeCount(현재 ACTIVE),
     * seekingCount(현재 SEEKING)
     *
     * <p>[기존 주석] 사회적 증거 통계를 반환한다. "오늘"은 Asia/Seoul 자정 기준이다.
     *
     * @return todayCount(오늘 distinct 사용자)·activeCount(현재 ACTIVE)·seekingCount(현재 SEEKING)
     */
    @Transactional(readOnly = true)
    public CheckInStatsResponse getStats() {
        LocalDateTime todayStart = LocalDate.ofInstant(clock.instant(), KST).atStartOfDay();
        long today = checkInRepository.countDistinctUsersStartedSince(todayStart);
        long active = checkInRepository.countByStatus(CheckInStatus.ACTIVE);
        long seeking = checkInRepository.countByStatus(CheckInStatus.SEEKING);
        return new CheckInStatsResponse(today, active, seeking);
    }

    /**
     * 기능: 반경 내 식당별 현재 혼밥러 수 마커를 조회한다(바운딩박스 후보 → Haversine 원형 보정·거리순 정렬)
     * Request: lat — 중심 위도(필수), lng — 중심 경도(필수), radius — 반경(m, 1~10,000 클램프)
     * Response: List&lt;MapMarkerResponse&gt; — 거리순 정렬된 마커 목록(반경 밖 제외)
     *
     * <p>[기존 주석] 반경 내 식당별 현재 혼밥러 수 마커. 바운딩박스로 후보를 좁힌 뒤 Haversine로 원형 보정·거리순 정렬한다.
     *
     * @param lat    중심 위도(필수)
     * @param lng    중심 경도(필수)
     * @param radius 반경(m, 1~{@link #MAX_RADIUS}로 클램프)
     * @return 거리순 정렬된 마커 목록(반경 밖 제외)
     * @throws BusinessException lat/lng가 없으면 {@link ErrorCode#INVALID_INPUT}
     */
    @Transactional(readOnly = true)
    public List<MapMarkerResponse> getMap(Double lat, Double lng, int radius) {
        if (lat == null || lng == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "lat/lng는 필수입니다.");
        }
        int r = Math.min(Math.max(radius, 1), MAX_RADIUS);
        double dLat = r / METERS_PER_DEGREE_LAT;
        double dLng = r / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return checkInRepository.countActiveByPlaceWithinBounds(lat - dLat, lat + dLat, lng - dLng, lng + dLng)
                .stream()
                .filter(m -> haversine(lat, lng, m.latitude(), m.longitude()) <= r)
                .sorted(Comparator.comparingDouble(m -> haversine(lat, lng, m.latitude(), m.longitude())))
                .toList();
    }

    /** 기능: 두 좌표 간 거리(m)를 Haversine 공식으로 계산한다. */
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * 기능: 식당의 현재 모집중(SEEKING) 목록을 조회한다(같이먹기 신청 대상, 차단 관계 상호 은닉, 경과분 계산 포함)
     * Request: viewerId — 조회하는 회원 ID(차단 필터 기준), placeId — 식당 ID
     * Response: List&lt;CheckInUserResponse&gt; — 현재 SEEKING 목록(startedAt 오름차순)
     *
     * <p>[기존 주석] 식당의 현재 모집중(SEEKING) 목록을 반환한다. 경과분은 now−startedAt. 프라이버시상 닉네임·시작시각·경과만 노출한다.
     * 차단 관계(양방향) 유저는 목록에서 상호 은닉한다(FR-108).
     *
     * @param viewerId 조회하는 회원 id(차단 필터 기준)
     * @param placeId  식당 id
     * @return 현재 SEEKING 목록(startedAt 오름차순, 차단 상대 제외)
     */
    @Transactional(readOnly = true)
    public List<CheckInUserResponse> getSeekers(Long viewerId, Long placeId) {
        List<Long> excluded = blockRepository.findExclusionIds(viewerId);
        LocalDateTime now = now();
        return checkInRepository.findSeekingWithUserByPlace(placeId, excluded).stream()
                .map(c -> new CheckInUserResponse(
                        c.getId(),
                        c.getUser().getId(),
                        // DisplayNames로 감쌀 필요 없음: 탈퇴 시 AccountWithdrawalService.endOngoing이 SEEKING
                        // 체크인을 cancel()로 정리하므로(end()는 SEEKING을 무시해 모집중 목록·지도 집계에 계속
                        // 잡히기 때문), 여기 조회되는 SEEKING 체크인의 사용자는 항상 비탈퇴 상태다. endOngoing이
                        // SEEKING 정리를 빼도록(정리 범위 축소) 바뀌면 이 가정이 깨지고 닉네임이 null로 새어나간다.
                        c.getUser().getNickname(),
                        c.getStartedAt(),
                        Duration.between(c.getStartedAt(), now).toMinutes()))
                .toList();
    }

    /**
     * 기능: 식당 상세 사회적 증거(누적 혼밥 세션 수 + 붐비는 시간대)를 집계한다
     * Request: placeId — 식당 ID
     * Response: PlaceCheckinSummaryResponse — totalDiners(누적 혼밥 세션 수, 중복 포함=시간대 바 합), periods(시간대별 세션 수), peakPeriodKey(붐비는 시간대, 없으면 null)
     *
     * <p>[의도] 식당 상세 사회적 증거: 누적 혼밥 세션 수 + 붐비는 시간대. 없는 place면 PLACE_NOT_FOUND.
     * totalDiners는 distinct 사람이 아니라 세션 수(같은 사람 반복 방문도 각각) — 시간대 바 합과 항상 일치하도록 같은 목록에서 파생.
     *
     * @param placeId 식당 id
     * @return 누적 혼밥 세션 수(중복 포함) + 시간대별 집계 + 피크 시간대
     * @throws BusinessException 존재하지 않는 place면 {@link ErrorCode#PLACE_NOT_FOUND}
     */
    @Transactional(readOnly = true)
    public PlaceCheckinSummaryResponse getCheckinSummary(Long placeId) {
        placeService.getById(placeId); // 존재 검증(없으면 PLACE_NOT_FOUND)
        // totalDiners = 같은 세션 목록에서 파생한 전체 세션 수(중복 포함) → "N명"과 시간대 바 합이 구조적으로 항상 일치
        CheckinTimeBuckets.Summary s = CheckinTimeBuckets.summarize(
                checkInRepository.findDinerStartedAtByPlace(placeId));
        return new PlaceCheckinSummaryResponse(s.total(), s.periods(), s.peakKey());
    }

    /**
     * 기능: TTL을 초과한 방치 체크인(ACTIVE·TOGETHER·SEEKING)을 일괄 정리한다(스케줄러 호출용)
     * Request: 없음
     * Response: int — 정리된 체크인 수(ACTIVE + TOGETHER + SEEKING)
     *
     * <p>[기존 주석] 방치된 ACTIVE 체크인(ttlHours 초과, startedAt 기준)과 방치된 TOGETHER 체크인(togetherTtlHours 초과,
     * matchedAt 기준)을 각각 일괄 ENDED 처리하고, 방치된 SEEKING 체크인(seekingTtlHours 초과, startedAt 기준)은
     * 안 먹은 모집이므로 CANCELLED로 처리한 뒤 합산 건수를 반환한다.
     *
     * @return 정리된 체크인 수(ACTIVE + TOGETHER + SEEKING)
     */
    @Transactional
    public int expireStaleCheckIns() {
        LocalDateTime now = now();
        int endedActive = checkInRepository.endActiveStartedBefore(now.minusHours(props.ttlHours()), now);
        // TOGETHER TTL 만료는 벌크 UPDATE라 엔티티를 로딩하지 않는다 — 대화를 닫으려면 대상 mealRequestId를
        // bulk end 전에 먼저 수집해둬야 한다(bulk end 후엔 matchedAt 조건으로 다시 찾을 수 없다).
        LocalDateTime togetherThreshold = now.minusHours(props.togetherTtlHours());
        List<Long> expiringMrIds = checkInRepository.findMealRequestIdsOfTogetherMatchedBefore(togetherThreshold);
        int endedTogether = checkInRepository.endTogetherMatchedBefore(togetherThreshold, now);
        expiringMrIds.forEach(conversationService::close);
        int cancelledSeeking = checkInRepository.cancelSeekingStartedBefore(now.minusHours(props.seekingTtlHours()), now);
        // 방금 SEEKING을 벗어난(만료된) 체크인에 걸려 있던 대기 신청까지 catch-all로 만료
        mealRequestRepository.expirePendingForEndedTargets(now);
        return endedActive + endedTogether + cancelledSeeking;
    }

    /** 기능: 현재 시각을 KST LocalDateTime으로 반환한다(Clock instant를 Asia/Seoul로 환산). */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
