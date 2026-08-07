package com.honjeong.push.repository;

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
     * 토큰 문자열로 찾는다(등록 시 UPSERT 판단·해제 시 소유자 확인).
     *
     * @param token FCM 등록 토큰
     * @return 있으면 해당 행, 없으면 empty
     */
    Optional<DeviceToken> findByToken(String token);

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
}
