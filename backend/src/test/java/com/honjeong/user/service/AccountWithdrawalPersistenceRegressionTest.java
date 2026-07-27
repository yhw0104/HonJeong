package com.honjeong.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.place.domain.Place;
import com.honjeong.place.repository.PlaceRepository;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * Critical 회귀 방지 — {@code AccountWithdrawalService.withdraw}가 실제 Postgres에 flush 수준으로
 * 회원을 익명화하는지(모킹으로는 절대 잡을 수 없는 flush 경계 버그) 검증하는 단일 통합 테스트.
 *
 * <p><b>배경.</b> 이전 커밋이 탈퇴 정리용 bulk DELETE 11건에 {@code @Modifying(clearAutomatically = true)}를
 * 붙였다. {@code em.clear()}는 삭제 대상 테이블에 국한되지 않고 영속성 컨텍스트 전체를 비우므로,
 * {@code withdraw()}가 들고 있던 관리 중인 {@code User}가 중간에 detach되고 {@code endOngoing}이 만든
 * 체크인 취소·대화 종료 변경도 flush 전에 버려진다 — 그 결과 개인정보만 지워지고 {@code users.status}는
 * ACTIVE로, 진행 중 체크인은 SEEKING으로 그대로 남는다. {@code AccountWithdrawalServiceTest}는 모든
 * Repository를 Mockito로 모킹하므로 clearAutomatically가 실행조차 되지 않아 이 버그를 절대 잡지 못한다 —
 * 그래서 실제 EntityManager·실제 Postgres가 필요하다.
 *
 * <p><b>flush 경계에 대한 의도적 선택.</b> 이 클래스는 {@code ChatLifecycleTest}와 같은 관례로
 * {@code @Transactional}을 붙여 테스트 종료 후 자동 롤백되게 한다(공유 Testcontainers Postgres를
 * 다른 테스트와 함께 쓰므로 데이터를 남기지 않기 위함). 하지만 {@code @Transactional} 테스트는 서비스
 * 호출과 이후 조회가 같은 영속성 컨텍스트(세션)를 공유하기 때문에, 아무 조치 없이 바로 재조회하면
 * DB에 한 번도 반영되지 않은 "메모리 위 엔티티"를 그대로 돌려받아 <b>버그가 있어도 초록불이 뜬다</b>
 * (이 테스트가 잡으려는 바로 그 함정). 그래서 {@code withdraw()} 호출 직후 {@link EntityManager#flush()}로
 * 아직 관리 중인 변경을 강제로 SQL로 내보내고, {@link EntityManager#clear()}로 1차 캐시를 비워 이후 조회가
 * 반드시 실제 DB 행을 다시 읽게 만든다. (수정 전 코드에서는 User·체크인이 이미 detach된 상태라 이 flush가
 * 그 변경들을 되살리지 못한다 — 그래서 이 조합이 버그를 정확히 드러낸다.)
 */
@SpringBootTest
@Transactional
class AccountWithdrawalPersistenceRegressionTest extends AbstractPostgresTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private AccountWithdrawalService accountWithdrawalService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("탈퇴 후 flush+clear로 실제 DB를 다시 읽으면 users.status=WITHDRAWN, phone/nickname=null,"
            + " SEEKING이던 체크인이 더는 SEEKING이 아니다")
    void withdraw_actuallyPersistsAnonymizationAndEndsSeekingCheckIn() {
        // given: SEEKING 체크인을 가진 ACTIVE 회원
        User user = User.pending("01099990001", null);
        user.completeProfile("탈퇴통합테스트닉", null, null, null, null, null, null, null, null);
        user = userRepository.save(user);
        Long userId = user.getId();

        Place place = placeRepository.save(Place.ofPublicData(
                "WD-IT-001", "탈퇴통합테스트식당", "한식", "서울 어딘가", "서울 도로명",
                37.5665, 126.9780, "02-000-0000", "영업"));

        CheckInResponse created = checkInService.createCheckIn(userId, new CheckInRequest(place.getId()));
        assertThat(created.status()).isEqualTo("SEEKING");
        Long checkInId = created.checkInId();

        // when: 탈퇴 처리 후, 이 트랜잭션에 남아있을 수 있는 미반영 변경을 강제로 DB에 흘려보내고
        // 1차 캐시를 비운다(위 클래스 주석 참고 — 이 두 줄이 없으면 버그가 있어도 테스트가 통과해버린다).
        accountWithdrawalService.withdraw(userId);
        entityManager.flush();
        entityManager.clear();

        // then: 실제 DB 행 기준으로 회원이 익명화되고 체크인이 더는 SEEKING이 아니어야 한다.
        User reloaded = userRepository.findById(userId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(reloaded.getPhone()).isNull();
        assertThat(reloaded.getNickname()).isNull();

        CheckIn reloadedCheckIn = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(reloadedCheckIn.getStatus()).isEqualTo(CheckInStatus.CANCELLED);
    }
}
