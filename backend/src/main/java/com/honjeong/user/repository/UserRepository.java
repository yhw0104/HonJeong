package com.honjeong.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/**
 * 1. 기능: 회원 엔티티의 조회·저장 등 데이터 접근 (대상 테이블: users)
 *
 * <p>[기존 주석] User 엔티티 영속성 접근. JpaRepository&lt;User, Long&gt;을 상속해 기본 CRUD(save/findById/delete 등)를
 * 자동으로 제공받고, 아래 파생 쿼리(메서드 이름으로 SQL을 생성)만 추가로 선언한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 기능: 휴대폰 번호로 회원 1명 조회
     * 쿼리: SELECT * FROM users WHERE phone = :phone
     * Request: phone — 휴대폰 번호 / Response: Optional&lt;User&gt; — 회원(없으면 empty)
     *
     * <p>[기존 주석] 휴대폰 번호로 회원 1명 조회: SELECT ... FROM users WHERE phone = ?. 없으면 Optional.empty().
     */
    Optional<User> findByPhone(String phone);

    /**
     * 기능: 닉네임으로 회원 1명 조회
     * 쿼리: SELECT * FROM users WHERE nickname = :nickname
     * Request: nickname — 닉네임 / Response: Optional&lt;User&gt; — 회원(없으면 empty)
     *
     * <p>[기존 주석] 닉네임으로 회원 1명 조회: WHERE nickname = ?. 없으면 Optional.empty().
     */
    Optional<User> findByNickname(String nickname);

    /**
     * 기능: 해당 닉네임이 이미 사용 중인지 확인
     * 쿼리: SELECT COUNT(*) > 0 FROM users WHERE nickname = :nickname
     * Request: nickname — 확인할 닉네임 / Response: boolean — 존재 여부
     *
     * <p>[기존 주석] 닉네임 중복 확인: 해당 닉네임 행 존재 여부를 boolean으로 반환(SELECT COUNT/EXISTS).
     */
    boolean existsByNickname(String nickname);

    /**
     * 기능: 닉네임 부분 일치(대소문자 무시) + 상태 필터로 회원 검색(최대 20건)
     * 쿼리: SELECT * FROM users WHERE UPPER(nickname) LIKE UPPER('%' || :nickname || '%') AND status = :status LIMIT 20
     * Request: nickname — 검색어, status — 회원 상태(ACTIVE) / Response: List&lt;User&gt; — 조건에 맞는 회원(최대 20건)
     *
     * <p>[기존 주석] 닉네임 부분 검색(대소문자 무시) + 상태 필터. 최대 20건 반환.
     * 닉네임 검색 화면에서 활성 사용자만 대상으로 한다.
     *
     * @param nickname 검색어(포함 여부, 대소문자 무시)
     * @param status   필터할 회원 상태(ACTIVE)
     * @return 조건에 맞는 회원 목록(최대 20건)
     */
    List<User> findTop20ByNicknameContainingIgnoreCaseAndStatus(String nickname, UserStatus status);

    /**
     * 기능: 사용자 상태만 조회한다(요청마다 호출되므로 엔티티 전체를 로딩하지 않는다)
     * 쿼리: SELECT status FROM users WHERE id = :id
     */
    @Query("SELECT u.status FROM User u WHERE u.id = :id")
    Optional<UserStatus> findStatusById(@Param("id") Long id);
}
