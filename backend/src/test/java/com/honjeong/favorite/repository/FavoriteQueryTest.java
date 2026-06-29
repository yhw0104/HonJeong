package com.honjeong.favorite.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.domain.Favorite;
import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.FavoriteGroupSummaryResponse;
import com.honjeong.global.config.JpaConfig;
import com.honjeong.place.domain.Place;
import com.honjeong.support.AbstractPostgresTest;
import com.honjeong.user.domain.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
class FavoriteQueryTest extends AbstractPostgresTest {

    @Autowired private FavoriteGroupRepository groupRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private CheckInRepository checkInRepository;
    @Autowired private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 12, 0);

    @Test
    @DisplayName("findSummaries: 그룹별 식당 수 + 생성순 정렬")
    void summaries() {
        User user = persistUser("01000000010", "요약러");
        Place p1 = persistPlace("q-1");
        Place p2 = persistPlace("q-2");
        FavoriteGroup g1 = em.persist(FavoriteGroup.create(user, "그룹1", "메모1", "#FF5A1F", true));
        FavoriteGroup g2 = em.persist(FavoriteGroup.create(user, "그룹2", null, "#2F80ED", false));
        em.persist(Favorite.of(g1, p1));
        em.persist(Favorite.of(g1, p2));
        em.flush();
        em.clear();

        List<FavoriteGroupSummaryResponse> rows = groupRepository.findSummaries(user.getId());

        assertThat(rows).extracting(FavoriteGroupSummaryResponse::name).containsExactly("그룹1", "그룹2");
        assertThat(rows.get(0).placeCount()).isEqualTo(2L);
        assertThat(rows.get(0).isDefault()).isTrue();
        assertThat(rows.get(1).placeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findGroupIdsContaining: 해당 식당을 담은 내 그룹 id들")
    void groupIdsContaining() {
        User user = persistUser("01000000011", "컨테인러");
        Place place = persistPlace("q-3");
        FavoriteGroup g1 = em.persist(FavoriteGroup.create(user, "그룹1", null, "#FF5A1F", false));
        FavoriteGroup g2 = em.persist(FavoriteGroup.create(user, "그룹2", null, "#2F80ED", false));
        em.persist(Favorite.of(g1, place));
        em.flush();
        em.clear();

        List<Long> ids = favoriteRepository.findGroupIdsContaining(user.getId(), place.getId());

        assertThat(ids).containsExactly(g1.getId());
        assertThat(ids).doesNotContain(g2.getId());
    }

    @Test
    @DisplayName("findWithPlaceByGroupId: place 페치 + 최근 담은 순")
    void withPlace() {
        User user = persistUser("01000000012", "페치러");
        Place p1 = persistPlace("q-4");
        Place p2 = persistPlace("q-5");
        FavoriteGroup g = em.persist(FavoriteGroup.create(user, "그룹", null, "#FF5A1F", false));
        Favorite older = Favorite.of(g, p1);
        em.persist(older);
        em.flush();
        Favorite newer = Favorite.of(g, p2);
        em.persist(newer);
        em.flush();
        em.clear();

        List<Favorite> rows = favoriteRepository.findWithPlaceByGroupId(g.getId());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getPlace().getId()).isEqualTo(p2.getId()); // 최근 먼저
        assertThat(rows.get(1).getPlace().getId()).isEqualTo(p1.getId());
    }

    @Test
    @DisplayName("findVisitedPlaceIds: 체크인 보유 식당만")
    void visited() {
        User user = persistUser("01000000013", "방문러");
        Place visitedPlace = persistPlace("q-6");
        Place notVisited = persistPlace("q-7");
        em.persist(CheckIn.start(user, visitedPlace, NOW.minusHours(1)));
        em.flush();
        em.clear();

        List<Long> ids = checkInRepository.findVisitedPlaceIds(
                user.getId(), List.of(visitedPlace.getId(), notVisited.getId()));

        assertThat(ids).containsExactly(visitedPlace.getId());
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
