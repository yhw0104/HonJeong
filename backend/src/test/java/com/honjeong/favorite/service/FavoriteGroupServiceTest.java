package com.honjeong.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.domain.Favorite;
import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.CreateGroupRequest;
import com.honjeong.favorite.dto.UpdateGroupRequest;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FavoriteGroupServiceTest {

    @Mock private FavoriteGroupRepository groupRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private CheckInRepository checkInRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private FavoriteGroupService service;

    @Test
    @DisplayName("createGroup: color 미지정이면 기본 브랜드색으로 저장")
    void createGroup_defaultColor() {
        when(userRepository.getReferenceById(1L)).thenReturn(User.pending("0100", null));
        when(groupRepository.save(any(FavoriteGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.createGroup(1L, new CreateGroupRequest("새 그룹", "메모", null));

        assertThat(res.color()).isEqualTo("#FF5A1F");
        assertThat(res.name()).isEqualTo("새 그룹");
        assertThat(res.placeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("deleteGroup: 기본 그룹이면 DEFAULT_GROUP_NOT_DELETABLE로 거부하고 삭제 안 함")
    void deleteGroup_defaultRejected() {
        FavoriteGroup def = FavoriteGroup.create(userWithId(1L), "즐겨찾기", null, "#FF5A1F", true);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(def));

        assertThatThrownBy(() -> service.deleteGroup(1L, 5L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEFAULT_GROUP_NOT_DELETABLE);
        verify(favoriteRepository, never()).deleteByGroup_Id(anyLong());
        verify(groupRepository, never()).delete(any(FavoriteGroup.class));
    }

    @Test
    @DisplayName("deleteGroup: 타인 그룹이면 FORBIDDEN")
    void deleteGroup_forbidden() {
        FavoriteGroup other = FavoriteGroup.create(userWithId(99L), "남의그룹", null, "#FF5A1F", false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.deleteGroup(1L, 5L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteGroup: 본인 일반 그룹이면 멤버십 삭제 후 그룹 삭제")
    void deleteGroup_ok() {
        FavoriteGroup mine = FavoriteGroup.create(userWithId(1L), "내그룹", null, "#FF5A1F", false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(mine));

        service.deleteGroup(1L, 5L);

        verify(favoriteRepository).deleteByGroup_Id(5L);
        verify(groupRepository).delete(mine);
    }

    @Test
    @DisplayName("createDefaultGroup: 이미 기본 그룹이 있으면 새로 만들지 않음(멱등)")
    void createDefault_idempotent() {
        when(groupRepository.existsByUser_IdAndIsDefaultTrue(1L)).thenReturn(true);

        service.createDefaultGroup(1L);

        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDefaultGroup: 기본 그룹이 없으면 '즐겨찾기' 생성")
    void createDefault_creates() {
        when(groupRepository.existsByUser_IdAndIsDefaultTrue(1L)).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(User.pending("0100", null));

        service.createDefaultGroup(1L);

        verify(groupRepository).save(any(FavoriteGroup.class));
    }

    @Test
    @DisplayName("getGroupDetail: 체크인 보유 식당은 visited=true, 아니면 false")
    void getGroupDetail_visitedMapping() {
        Place p1 = mock(Place.class);
        Place p2 = mock(Place.class);
        when(p1.getId()).thenReturn(10L);
        when(p2.getId()).thenReturn(20L);
        Favorite f1 = mock(Favorite.class);
        Favorite f2 = mock(Favorite.class);
        when(f1.getPlace()).thenReturn(p1);
        when(f2.getPlace()).thenReturn(p2);
        FavoriteGroup mine = FavoriteGroup.create(userWithId(1L), "내그룹", null, "#FF5A1F", false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(mine));
        when(favoriteRepository.findWithPlaceByGroupId(5L)).thenReturn(List.of(f1, f2));
        when(checkInRepository.findVisitedPlaceIds(1L, List.of(10L, 20L))).thenReturn(List.of(10L));

        var res = service.getGroupDetail(1L, 5L);

        assertThat(res.places()).hasSize(2);
        assertThat(res.places().get(0).visited()).isTrue();
        assertThat(res.places().get(1).visited()).isFalse();
    }

    @Test
    @DisplayName("getGroupDetail: favorites가 비면 checkIn 쿼리를 호출하지 않는다")
    void getGroupDetail_emptyFavorites_skipsCheckInQuery() {
        FavoriteGroup mine = FavoriteGroup.create(userWithId(1L), "내그룹", null, "#FF5A1F", false);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(mine));
        when(favoriteRepository.findWithPlaceByGroupId(5L)).thenReturn(List.of());

        service.getGroupDetail(1L, 5L);

        verify(checkInRepository, never()).findVisitedPlaceIds(anyLong(), any());
    }

    private User userWithId(long id) {
        User u = User.pending("0100" + id, null);
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return u;
    }
}
