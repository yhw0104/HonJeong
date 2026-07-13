package com.honjeong.checkin.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

/**
 * V17 단일 활성 부분 유니크 인덱스({@code uq_check_ins_current_user})가 SEEKING까지 포함하는지 검증하는
 * 슬라이스 테스트. Task 1에서 인덱스·enum만 추가되고, {@code CheckIn.startSeeking}이 없어 컴파일이 안 돼
 * Task 2(엔티티 전이 메서드 추가)와 함께 커밋한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class CheckInSeekingConstraintTest extends AbstractPostgresTest {

    @Autowired
    private CheckInRepository checkInRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 15, 12, 0);

    /** 닉네임을 가진 ACTIVE 사용자 1명을 영속화해 반환한다. */
    private User persistUser(String phone, String nickname) {
        User user = User.pending(phone, null);
        // completeProfile(nickname, gender, ageGroup, introduction, region, regionLat, regionLng, diningStyle, profileImageUrl) — 9개
        user.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(user);
    }

    private Place persistPlace(String sourceId, double lat, double lng) {
        return em.persist(Place.ofPublicData(sourceId, sourceId + "식당", "한식", "서울 어딘가", "서울 도로명",
                lat, lng, null, "영업"));
    }

    @Test
    @DisplayName("한 사용자는 SEEKING/ACTIVE/TOGETHER 중 동시에 1건만 가질 수 있다")
    void 단일활성_인덱스가_SEEKING을_포함한다() {
        User user = persistUser("01000000001", "혼밥러A");
        Place place = persistPlace("ext-1", 37.5, 127.0);
        Place place2 = persistPlace("ext-2", 37.6, 127.1);

        checkInRepository.saveAndFlush(CheckIn.startSeeking(user, place, NOW)); // 1건째 SEEKING
        CheckIn second = CheckIn.start(user, place2, NOW);                       // 2건째 ACTIVE 시도

        assertThatThrownBy(() -> checkInRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);            // 부분 유니크 위반
    }
}
