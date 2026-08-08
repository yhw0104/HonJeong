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
     * @param userId   토큰의 새 주인
     * @param token    FCM 등록 토큰
     * @param platform 기기 플랫폼 이름(IOS·ANDROID — CHECK 제약 대상이라 enum 이름 그대로)
     * @param now      등록·갱신 시각
     * @return 영향받은 행 수(항상 1)
     */
    @Modifying
    @Query(value = "INSERT INTO device_tokens "
            + "(user_id, token, platform, last_used_at, last_registered_at, created_at, updated_at) "
            + "VALUES (:userId, :token, :platform, :now, :now, :now, :now) "
            + "ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, platform = EXCLUDED.platform, "
            + "last_used_at = EXCLUDED.last_used_at, last_registered_at = EXCLUDED.last_registered_at, "
            + "updated_at = EXCLUDED.updated_at",
            nativeQuery = true)
    int upsert(@Param("userId") Long userId, @Param("token") String token,
            @Param("platform") String platform, @Param("now") LocalDateTime now);

    /**
     * 한 사용자의 모든 기기 토큰(발송 대상).
     *
     * @param userId 사용자 ID
     * @return 토큰 목록(없으면 빈 리스트)
     */
    List<DeviceToken> findAllByUser_Id(Long userId);

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
}
