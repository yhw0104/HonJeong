package com.honjeong.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.honjeong.badge.service.BadgeService;
import com.honjeong.favorite.domain.Favorite;
import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.FavoriteStatusResponse;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;
import com.honjeong.user.domain.User;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteGroupRepository groupRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private PlaceService placeService;
    @Mock private BadgeService badgeService;
    @InjectMocks private FavoriteService service;

    @Test
    @DisplayName("getStatus: 담긴 그룹은 contains=true, 하나라도 있으면 saved=true")
    void getStatus() {
        FavoriteGroup g1 = group(1L, "그룹1", "#FF5A1F");
        FavoriteGroup g2 = group(2L, "그룹2", "#2F80ED");
        when(groupRepository.findByUser_IdOrderByCreatedAtAsc(7L)).thenReturn(List.of(g1, g2));
        when(favoriteRepository.findGroupIdsContaining(7L, 100L)).thenReturn(List.of(1L));

        FavoriteStatusResponse res = service.getStatus(7L, 100L);

        assertThat(res.saved()).isTrue();
        assertThat(res.groups()).extracting("groupId", "contains")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, true),
                        org.assertj.core.groups.Tuple.tuple(2L, false));
    }

    @Test
    @DisplayName("getStatus: 아무 그룹에도 없으면 saved=false")
    void getStatus_notSaved() {
        when(groupRepository.findByUser_IdOrderByCreatedAtAsc(7L)).thenReturn(List.of(group(1L, "그룹1", "#FF5A1F")));
        when(favoriteRepository.findGroupIdsContaining(7L, 100L)).thenReturn(List.of());

        assertThat(service.getStatus(7L, 100L).saved()).isFalse();
    }

    @Test
    @DisplayName("addPlace: 이미 담겨 있으면 저장하지 않음(멱등)")
    void addPlace_idempotent() {
        FavoriteGroup mine = group(5L, "내그룹", "#FF5A1F");
        setOwner(mine, 7L);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(mine));
        when(favoriteRepository.existsByGroup_IdAndPlace_Id(5L, 100L)).thenReturn(true);

        service.addPlace(7L, 5L, 100L);

        verify(favoriteRepository, never()).save(any());
        verify(badgeService, never()).checkAndAward(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("addPlace: 새로 담으면 place 검증 후 저장")
    void addPlace_saves() {
        FavoriteGroup mine = group(5L, "내그룹", "#FF5A1F");
        setOwner(mine, 7L);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(mine));
        when(favoriteRepository.existsByGroup_IdAndPlace_Id(5L, 100L)).thenReturn(false);
        when(placeService.getById(100L)).thenReturn(place(100L));

        service.addPlace(7L, 5L, 100L);

        verify(favoriteRepository).save(any(Favorite.class));
        verify(badgeService).checkAndAward(7L, true);
    }

    @Test
    @DisplayName("addPlace: 타인 그룹이면 FORBIDDEN")
    void addPlace_forbidden() {
        FavoriteGroup other = group(5L, "남의그룹", "#FF5A1F");
        setOwner(other, 99L);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.addPlace(7L, 5L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("removePlace: 본인 그룹이면 멱등 삭제 호출")
    void removePlace() {
        FavoriteGroup mine = group(5L, "내그룹", "#FF5A1F");
        setOwner(mine, 7L);
        when(groupRepository.findById(5L)).thenReturn(Optional.of(mine));

        service.removePlace(7L, 5L, 100L);

        verify(favoriteRepository).deleteByGroup_IdAndPlace_Id(5L, 100L);
    }

    private FavoriteGroup group(long id, String name, String color) {
        FavoriteGroup g = FavoriteGroup.create(User.pending("0100", null), name, null, color, false);
        setField(g, "id", id);
        return g;
    }

    private void setOwner(FavoriteGroup g, long userId) {
        User owner = User.pending("0100" + userId, null);
        setField(owner, "id", userId);
        setField(g, "user", owner);
    }

    private Place place(long id) {
        Place p = Place.ofPublicData("ext", "식당", "한식", "서울", "도로명", 37.5, 127.0, null, "영업");
        setField(p, "id", id);
        return p;
    }

    private void setField(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
