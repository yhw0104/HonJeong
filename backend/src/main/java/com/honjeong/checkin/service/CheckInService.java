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

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.dto.CheckInUserResponse;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.config.HonjeongCheckInProperties;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 체크인 도메인 서비스. 단일 활성 제약(같은 장소 멱등 / 다른 장소 409)·종료·내 체크인·통계·지도·혼밥러 목록·TTL 만료를 담당한다.
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
    private final Clock clock;
    private final HonjeongCheckInProperties props;

    public CheckInService(CheckInRepository checkInRepository, PlaceService placeService,
            UserRepository userRepository, Clock clock, HonjeongCheckInProperties props) {
        this.checkInRepository = checkInRepository;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.clock = clock;
        this.props = props;
    }

    /**
     * 혼밥 체크인을 시작한다. placeId로 장소를 조회한 뒤, 기존 ACTIVE가 있으면 같은 장소는 멱등 반환, 다른 장소는 409.
     * 경쟁으로 단일활성 인덱스가 위반되면 {@link DataIntegrityViolationException}을 409로 변환한다.
     *
     * @param userId  체크인하는 회원 id
     * @param request 선택한 장소 id를 담은 요청
     * @return 체크인 응답(새로 만들었거나 멱등 반환한 기존 체크인)
     */
    @Transactional
    public CheckInResponse createCheckIn(Long userId, CheckInRequest request) {
        Place place = placeService.getById(request.placeId());

        Optional<CheckIn> active = checkInRepository.findByUser_IdAndStatus(userId, CheckInStatus.ACTIVE);
        if (active.isPresent()) {
            CheckIn existing = active.get();
            if (existing.getPlace().getId().equals(place.getId())) {
                return CheckInResponse.from(existing);                       // 같은 장소 → 멱등
            }
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_ACTIVE);   // 다른 장소 → 409
        }

        try {
            User userRef = userRepository.getReferenceById(userId);
            CheckIn saved = checkInRepository.save(CheckIn.start(userRef, place, now()));
            return CheckInResponse.from(saved);
        } catch (DataIntegrityViolationException e) {                        // 경쟁 상황(인덱스 위반)
            throw new BusinessException(ErrorCode.CHECKIN_ALREADY_ACTIVE);
        }
    }

    /**
     * 체크인을 종료한다. 없으면 404, 본인 것이 아니면 403, 이미 ENDED면 멱등 반환한다.
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
        } else {
            checkIn.end(now);
        }
        return CheckInResponse.from(checkIn);
    }

    /**
     * 체크인을 취소(CANCELLED)한다. 짧은 혼밥 오집계 방지용 — 소유자의 ACTIVE만 취소 가능하다.
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
        if (checkIn.getStatus() != CheckInStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CHECKIN_NOT_ACTIVE);
        }
        checkIn.cancel(now());
        return CheckInResponse.from(checkIn);
    }

    /**
     * 내 현재 체크인(ACTIVE 또는 TOGETHER)을 반환한다. 없으면 null.
     * TOGETHER면 파트너 닉네임을 함께 채워 앱이 "같이 먹는 중"을 렌더할 수 있게 한다.
     *
     * @param userId 회원 id
     * @return 현재 체크인 응답 또는 null
     */
    @Transactional(readOnly = true)
    public CheckInResponse getMyCurrentCheckIn(Long userId) {
        Optional<CheckIn> current = checkInRepository.findByUser_IdAndStatusIn(
                userId, List.of(CheckInStatus.ACTIVE, CheckInStatus.TOGETHER));
        if (current.isEmpty()) {
            return null;
        }
        CheckIn c = current.get();
        if (c.getStatus() != CheckInStatus.TOGETHER) {
            return CheckInResponse.from(c);
        }
        String partnerNickname = checkInRepository.findTogetherByMealRequestId(c.getMealRequestId()).stream()
                .filter(x -> !x.getUser().getId().equals(userId))
                .findFirst()
                .map(x -> x.getUser().getNickname())
                .orElse(null);
        return CheckInResponse.from(c, partnerNickname);
    }

    /**
     * 사회적 증거 통계를 반환한다. "오늘"은 Asia/Seoul 자정 기준이다.
     *
     * @return todayCount(오늘 distinct 사용자)·activeCount(현재 ACTIVE)
     */
    @Transactional(readOnly = true)
    public CheckInStatsResponse getStats() {
        LocalDateTime todayStart = LocalDate.ofInstant(clock.instant(), KST).atStartOfDay();
        long today = checkInRepository.countDistinctUsersStartedSince(todayStart);
        long active = checkInRepository.countByStatus(CheckInStatus.ACTIVE);
        return new CheckInStatsResponse(today, active);
    }

    /**
     * 반경 내 식당별 현재 혼밥러 수 마커. 바운딩박스로 후보를 좁힌 뒤 Haversine로 원형 보정·거리순 정렬한다.
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

    /** 두 좌표 간 거리(m)를 Haversine 공식으로 계산한다. */
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * 식당의 현재 혼밥러 목록을 반환한다. 경과분은 now−startedAt. 프라이버시상 닉네임·시작시각·경과만 노출한다.
     *
     * @param placeId 식당 id
     * @return 현재 ACTIVE 혼밥러 목록(startedAt 오름차순)
     */
    @Transactional(readOnly = true)
    public List<CheckInUserResponse> getActiveDiners(Long placeId) {
        LocalDateTime now = now();
        return checkInRepository.findActiveWithUserByPlace(placeId).stream()
                .map(c -> new CheckInUserResponse(
                        c.getId(),
                        c.getUser().getId(),
                        c.getUser().getNickname(),
                        c.getStartedAt(),
                        Duration.between(c.getStartedAt(), now).toMinutes()))
                .toList();
    }

    /**
     * 방치된 ACTIVE 체크인(ttlHours 초과, startedAt 기준)과 방치된 TOGETHER 체크인(togetherTtlHours 초과,
     * matchedAt 기준)을 각각 일괄 ENDED 처리하고 합산 만료 건수를 반환한다.
     *
     * @return 만료된 체크인 수(ACTIVE + TOGETHER)
     */
    @Transactional
    public int expireStaleCheckIns() {
        LocalDateTime now = now();
        int endedActive = checkInRepository.endActiveStartedBefore(now.minusHours(props.ttlHours()), now);
        int endedTogether = checkInRepository.endTogetherMatchedBefore(now.minusHours(props.togetherTtlHours()), now);
        return endedActive + endedTogether;
    }

    /** 현재 시각을 KST LocalDateTime으로 반환한다(Clock instant를 Asia/Seoul로 환산). */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
