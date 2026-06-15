package com.honjeong.checkin.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.repository.CheckInRepository;
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

    private final CheckInRepository checkInRepository;
    private final PlaceService placeService;
    private final UserRepository userRepository;
    private final Clock clock;

    public CheckInService(CheckInRepository checkInRepository, PlaceService placeService,
            UserRepository userRepository, Clock clock) {
        this.checkInRepository = checkInRepository;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * 혼밥 체크인을 시작한다. 가게를 캐시 upsert한 뒤, 기존 ACTIVE가 있으면 같은 장소는 멱등 반환, 다른 장소는 409.
     * 경쟁으로 단일활성 인덱스가 위반되면 {@link DataIntegrityViolationException}을 409로 변환한다.
     *
     * @param userId  체크인하는 회원 id
     * @param request 선택한 가게 정보
     * @return 체크인 응답(새로 만들었거나 멱등 반환한 기존 체크인)
     */
    @Transactional
    public CheckInResponse createCheckIn(Long userId, CheckInRequest request) {
        Place place = placeService.findOrCreateByExternalId(request.toUpsertCommand());

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
     *
     * @param userId    요청 회원 id
     * @param checkInId 종료할 체크인 id
     * @return 종료된(또는 이미 종료된) 체크인 응답
     */
    @Transactional
    public CheckInResponse endCheckIn(Long userId, Long checkInId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHECKIN_NOT_FOUND));
        if (!checkIn.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        checkIn.end(now());
        return CheckInResponse.from(checkIn);
    }

    /**
     * 내 현재 ACTIVE 체크인을 반환한다. 없으면 null(컨트롤러가 data:null로 응답).
     *
     * @param userId 회원 id
     * @return 현재 ACTIVE 체크인 또는 null
     */
    @Transactional(readOnly = true)
    public CheckInResponse getMyActiveCheckIn(Long userId) {
        return checkInRepository.findByUser_IdAndStatus(userId, CheckInStatus.ACTIVE)
                .map(CheckInResponse::from)
                .orElse(null);
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

    /** 현재 시각을 KST LocalDateTime으로 반환한다(Clock instant를 Asia/Seoul로 환산). */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
