package com.honjeong.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/**
 * User 엔티티 영속성 접근. JpaRepository<User, Long>을 상속해 기본 CRUD(save/findById/delete 등)를
 * 자동으로 제공받고, 아래 파생 쿼리(메서드 이름으로 SQL을 생성)만 추가로 선언한다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 휴대폰 번호로 회원 1명 조회: SELECT ... FROM users WHERE phone = ?. 없으면 Optional.empty(). */
    Optional<User> findByPhone(String phone);

    /** 닉네임으로 회원 1명 조회: WHERE nickname = ?. 없으면 Optional.empty(). */
    Optional<User> findByNickname(String nickname);

    /** 닉네임 중복 확인: 해당 닉네임 행 존재 여부를 boolean으로 반환(SELECT COUNT/EXISTS). */
    boolean existsByNickname(String nickname);

    /**
     * 닉네임 부분 검색(대소문자 무시) + 상태 필터. 최대 20건 반환.
     * 닉네임 검색 화면에서 활성 사용자만 대상으로 한다.
     *
     * @param nickname 검색어(포함 여부, 대소문자 무시)
     * @param status   필터할 회원 상태(ACTIVE)
     * @return 조건에 맞는 회원 목록(최대 20건)
     */
    List<User> findTop20ByNicknameContainingIgnoreCaseAndStatus(String nickname, UserStatus status);
}
