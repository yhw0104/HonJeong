package com.honjeong.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/**
 * 회원 엔티티의 조회·저장 등 데이터 접근. (대상 테이블: users)
 *
 * <p>{@code JpaRepository<User, Long>}을 상속해 기본 CRUD(save/findById/delete 등)를 자동으로 제공받고,
 * 아래 파생 쿼리(메서드 이름으로 SQL을 생성)만 추가로 선언한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 휴대폰 번호로 회원 1명을 조회한다.
     *
     * @param phone 휴대폰 번호
     * @return 회원(없으면 빈 Optional)
     */
    Optional<User> findByPhone(String phone);

    /**
     * 닉네임으로 회원 1명을 조회한다.
     *
     * @param nickname 닉네임
     * @return 회원(없으면 빈 Optional)
     */
    Optional<User> findByNickname(String nickname);

    /**
     * 해당 닉네임이 이미 사용 중인지 확인한다.
     *
     * @param nickname 확인할 닉네임
     * @return 이미 쓰고 있으면 true
     */
    boolean existsByNickname(String nickname);

    /**
     * 닉네임 부분 일치(대소문자 무시)와 상태 필터로 회원을 검색한다. 최대 20건 반환.
     *
     * <p>닉네임 검색 화면에서 활성 사용자만 대상으로 한다.
     *
     * @param nickname 검색어(포함 여부, 대소문자 무시)
     * @param status   필터할 회원 상태(ACTIVE)
     * @return 조건에 맞는 회원 목록(최대 20건)
     */
    List<User> findTop20ByNicknameContainingIgnoreCaseAndStatus(String nickname, UserStatus status);

    /**
     * 사용자 상태만 조회한다 — 요청마다 호출되므로 엔티티 전체를 로딩하지 않는다.
     *
     * @param id 회원 id
     * @return 회원 상태(행이 없으면 빈 Optional)
     */
    @Query("SELECT u.status FROM User u WHERE u.id = :id")
    Optional<UserStatus> findStatusById(@Param("id") Long id);
}
