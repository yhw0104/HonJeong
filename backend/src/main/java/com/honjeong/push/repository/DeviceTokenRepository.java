package com.honjeong.push.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.push.domain.DeviceToken;

/**
 * 기기 토큰 조회·삭제.
 *
 * <p>사용처: DeviceTokenService(등록·삭제), PushDispatcher(발송 대상), AccountWithdrawalService(탈퇴).
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /**
     * 토큰 문자열로 찾는다(해제 시 소유자 확인).
     *
     * @param token FCM 등록 토큰
     * @return 있으면 해당 행, 없으면 empty
     */
    Optional<DeviceToken> findByToken(String token);

    /**
     * 등록 UPSERT — 처음 보는 토큰이면 넣고, 이미 있으면 주인·플랫폼·사용 시각을 갱신한다.
     *
     * <p><b>왜 네이티브 ON CONFLICT인가.</b> 앱 시작 시 {@code registerPushToken}과
     * {@code onTokenRefresh}가 거의 동시에 뜬다. "조회해서 없으면 INSERT"로 쓰면 둘 다 empty를
     * 보고 INSERT해 한쪽이 UNIQUE 위반으로 500이 난다(앱이 삼켜 사용자 영향은 없지만 로그가 더러워진다).
     *
     * <p>그렇다고 {@code DataIntegrityViolationException}을 잡아 재조회하면 <b>더 나쁘다</b> —
     * Postgres는 트랜잭션 안에서 유니크 위반이 나면 트랜잭션 전체를 abort시켜 이후 문장이 25P02로
     * 실패한다(07-23 뱃지 도메인에서 겪은 사고). 그래서 {@code UserBadgeRepository.insertIfAbsent}와
     * 같은 방식으로 DB에 원자적으로 맡긴다.
     *
     * <p>{@code platform}도 갱신한다 — 토큰은 기기에 붙는 값이라 같은 토큰이 다른 플랫폼으로 올 일은
     * 없지만, 안 쓰면 안드로이드가 붙었을 때 최초 등록 값이 영원히 남는다.
     *
     * <p>{@code last_registered_at}도 함께 갱신한다 — 이 UPSERT가 그 칸을 갱신하는 <b>유일한</b>
     * 경로다. 앱이 시작할 때마다 재등록하므로 살아 있는 기기는 계속 신선하게 유지된다.
     *
     * <p>{@code installationId}도 갱신한다 — 구버전 앱이 남긴 NULL 행을 새 앱의 등록이 메운다.
     *
     * @param userId         토큰의 새 주인
     * @param token          FCM 등록 토큰
     * @param platform       기기 플랫폼 이름(IOS·ANDROID — CHECK 제약 대상이라 enum 이름 그대로)
     * @param now            등록·갱신 시각
     * @param installationId 앱 설치 식별자(구버전 앱은 null)
     * @return 영향받은 행 수(항상 1)
     */
    @Modifying
    @Query(value = "INSERT INTO device_tokens "
            + "(user_id, token, platform, last_used_at, last_registered_at, installation_id, created_at, updated_at) "
            + "VALUES (:userId, :token, :platform, :now, :now, :installationId, :now, :now) "
            + "ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, platform = EXCLUDED.platform, "
            + "last_used_at = EXCLUDED.last_used_at, last_registered_at = EXCLUDED.last_registered_at, "
            + "installation_id = EXCLUDED.installation_id, updated_at = EXCLUDED.updated_at",
            nativeQuery = true)
    int upsert(@Param("userId") Long userId, @Param("token") String token,
            @Param("platform") String platform, @Param("now") LocalDateTime now,
            @Param("installationId") String installationId);

    /**
     * 한 사용자의 모든 기기 토큰(발송 대상).
     *
     * @param userId 사용자 ID
     * @return 토큰 목록(없으면 빈 리스트)
     */
    List<DeviceToken> findAllByUser_Id(Long userId);

    /**
     * 한 사용자의 <b>아직 신선한</b> 기기 토큰(발송 대상).
     *
     * <p>{@code lastRegisteredAt}이 임계값보다 <b>뒤</b>인 행만 준다. 앱은 시작할 때마다 재등록하므로
     * 살아 있는 기기는 계속 신선하고, 로그아웃 뒤 폐기에 실패해 주인 없이 남은 토큰만 늙는다.
     * Firebase가 "보내기 전에 staleness window 안인지 확인하라"고 권고하는 자리이기도 하다.
     *
     * <p>경계는 등호를 뺀다 — {@link #deleteAllByLastRegisteredAtBefore}와 짝이다.
     *
     * <p>{@link #findAllByUser_Id}는 남겨 둔다 — 테스트가 "전부 지워졌나"를 확인하는 데 쓰고 있고,
     * 그것을 이 메서드로 바꾸면 단언의 의미가 "window 안에 없나"로 달라진다.
     *
     * @param userId    사용자 ID
     * @param threshold 이 시각보다 뒤에 등록된 것만 발송 대상
     * @return 신선한 토큰 목록(없으면 빈 리스트)
     */
    List<DeviceToken> findAllByUser_IdAndLastRegisteredAtAfter(Long userId, LocalDateTime threshold);

    /**
     * 한 사용자의 기기 토큰을 전부 지운다(탈퇴).
     *
     * @param userId 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조.
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.user.id = :userId")
    int deleteAllByUser_Id(@Param("userId") Long userId);

    /**
     * 토큰 하나를 지운다(로그아웃).
     *
     * @param token 해제할 FCM 등록 토큰
     * @return 삭제된 행 수(없었으면 0)
     */
    // clearAutomatically 금지 — 위와 같은 이유.
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token")
    int deleteByToken(@Param("token") String token);

    /**
     * 죽은 토큰 하나를 <b>그 주인의 것일 때만</b> 지운다(발송 결과 기록).
     *
     * <p><b>왜 주인까지 보는가.</b> 발송은 조회 → HTTP → 기록 세 구간으로 나뉘어 있고, 그 사이에
     * 다른 요청이 {@link #upsert}로 같은 토큰의 주인을 바꿔 놓을 수 있다(기기가 남에게 넘어가
     * 새 사용자가 로그인한 경우 — token이 UNIQUE라 같은 행의 user_id가 갱신된다). 토큰 문자열만
     * 보고 지우면 <b>방금 등록한 새 주인의 행</b>이 낡은 발송 결과 때문에 사라진다. 창이 좁고
     * 앱이 다음 시작 때 재등록하므로 치명적이진 않지만, 주인을 조건에 넣으면 아예 없앨 수 있다.
     *
     * @param token  삭제할 FCM 등록 토큰
     * @param userId 발송 시점에 이 토큰의 주인이던 사용자
     * @return 삭제된 행 수(주인이 바뀌었거나 이미 없었으면 0)
     */
    // clearAutomatically 금지 — 위와 같은 이유.
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token AND d.user.id = :userId")
    int deleteByTokenAndUserId(@Param("token") String token, @Param("userId") Long userId);

    /**
     * staleness window를 벗어난 토큰을 전부 지운다(청소 스케줄러).
     *
     * <p>기준이 {@code lastUsedAt}이 아니라 {@code lastRegisteredAt}인 것이 핵심이다 —
     * {@code lastUsedAt}은 발송 시도마다 갱신되므로, 정작 지우려는 고아 토큰(지금도 계속
     * 발송되고 있는 토큰)이 영원히 신선해 보인다({@code PushDeliveryRecorder.recordResult} Javadoc).
     *
     * <p>경계는 등호를 뺀다 — {@link #findAllByUser_IdAndLastRegisteredAtAfter}와 짝이다.
     *
     * @param threshold 이 시각보다 앞에 등록된 행을 지운다
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — 위와 같은 이유.
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.lastRegisteredAt < :threshold")
    int deleteAllByLastRegisteredAtBefore(@Param("threshold") LocalDateTime threshold);

    /**
     * 같은 기기의 <b>다른</b> 토큰을 지운다(등록 시 정리).
     *
     * <p>토큰은 갱신될 때마다 값이 바뀌므로 한 기기에 여러 행이 쌓일 수 있다. 정상 경로에서는
     * 로그아웃이 정리하지만, FCM 폐기가 실패하면 기기에 값이 없어 다시는 지목할 수 없는 행이 남는다
     * — 그 폰을 넘겨받은 사람의 잠금화면에 이전 사용자의 알림이 계속 뜬다. 설치 ID는 토큰이 바뀌어도
     * 그대로라 "같은 기기의 옛 토큰"을 지목할 수 있고, <b>주인이 누구든</b> 정리된다(그게 요점이다 —
     * {@code unregister}는 주인을 확인하므로 기기를 넘겨받은 새 사용자는 옛 행을 못 지운다).
     *
     * <p>{@code installationId}가 NULL인 행은 어떤 값과도 같지 않으므로 매칭되지 않는다 —
     * 설치 ID를 보내지 않는 구버전 앱의 행은 안전하다.
     *
     * @param installationId 앱 설치 식별자
     * @param token          지금 등록하는 토큰(이것만 남긴다)
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — 위와 같은 이유.
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.installationId = :installationId AND d.token <> :token")
    int deleteByInstallationIdAndTokenNot(@Param("installationId") String installationId,
            @Param("token") String token);
}
