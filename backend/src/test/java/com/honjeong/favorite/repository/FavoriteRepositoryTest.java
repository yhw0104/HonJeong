package com.honjeong.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.favorite.domain.Favorite;
import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class FavoriteRepositoryTest extends AbstractPostgresTest {

    @Autowired private FavoriteGroupRepository groupRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 12, 0);

    @Test
    @DisplayName("그룹+멤버십 저장 후 created_at 감사 컬럼이 채워진다")
    void save_auditing() {
        User user = persistUser("01000000001", "연남러");
        Place place = persistPlace("ext-1");
        FavoriteGroup group = em.persist(FavoriteGroup.create(user, "가보고 싶은 곳", "도전 리스트", "#FF5A1F", false));
        Favorite fav = em.persist(Favorite.of(group, place));
        em.flush();

        assertThat(group.getCreatedAt()).isNotNull();
        assertThat(fav.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 그룹에 같은 식당을 중복 저장하면 유니크 제약 위반")
    void uniqueGroupPlace() {
        User user = persistUser("01000000002", "중복러");
        Place place = persistPlace("ext-2");
        FavoriteGroup group = em.persist(FavoriteGroup.create(user, "단골", null, "#22A65A", false));
        em.persist(Favorite.of(group, place));
        em.flush();

        assertThatThrownBy(() -> favoriteRepository.saveAndFlush(Favorite.of(group, place)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("그룹 삭제 시 그 멤버십만 사라지고 다른 그룹의 같은 식당·식당 데이터는 유지")
    void deleteGroup_keepsOtherGroupMembershipAndPlace() {
        User user = persistUser("01000000003", "투그룹러");
        Place place = persistPlace("ext-3");
        FavoriteGroup g1 = em.persist(FavoriteGroup.create(user, "그룹1", null, "#FF5A1F", false));
        FavoriteGroup g2 = em.persist(FavoriteGroup.create(user, "그룹2", null, "#2F80ED", false));
        em.persist(Favorite.of(g1, place));
        em.persist(Favorite.of(g2, place));
        em.flush();

        favoriteRepository.deleteByGroup_Id(g1.getId());
        groupRepository.delete(g1);
        em.flush();
        em.clear();

        assertThat(groupRepository.findById(g1.getId())).isEmpty();
        assertThat(favoriteRepository.existsByGroup_IdAndPlace_Id(g2.getId(), place.getId())).isTrue();
        assertThat(em.find(Place.class, place.getId())).isNotNull();
    }

    @Test
    @DisplayName("기본 그룹 존재 여부·생성순 목록·그룹별 카운트 조회")
    void derivedQueries() {
        User user = persistUser("01000000004", "쿼리러");
        Place place = persistPlace("ext-4");
        FavoriteGroup def = em.persist(FavoriteGroup.create(user, "즐겨찾기", null, "#FF5A1F", true));
        FavoriteGroup g2 = em.persist(FavoriteGroup.create(user, "나중그룹", null, "#EB5757", false));
        em.persist(Favorite.of(def, place));
        em.flush();
        em.clear();

        assertThat(groupRepository.existsByUser_IdAndIsDefaultTrue(user.getId())).isTrue();
        List<FavoriteGroup> ordered = groupRepository.findByUser_IdOrderByCreatedAtAsc(user.getId());
        assertThat(ordered).extracting(FavoriteGroup::getName).containsExactly("즐겨찾기", "나중그룹");
        assertThat(favoriteRepository.countByGroup_Id(def.getId())).isEqualTo(1L);
        assertThat(favoriteRepository.countByGroup_Id(g2.getId())).isEqualTo(0L);
    }

    private User persistUser(String phone, String nickname) {
        User u = User.pending(phone, null);
        u.completeProfile(nickname, null, null, null, null, null, null, null, null);
        return em.persist(u);
    }

    private Place persistPlace(String sourceId) {
        return em.persist(Place.ofPublicData(sourceId, sourceId + "식당", "한식", "서울", "서울 도로명",
                37.5, 127.0, null, "영업"));
    }
}
