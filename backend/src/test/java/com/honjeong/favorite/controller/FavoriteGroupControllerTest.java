package com.honjeong.favorite.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.favorite.dto.FavoriteGroupSummaryResponse;
import com.honjeong.favorite.service.FavoriteGroupService;
import com.honjeong.favorite.service.FavoriteService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.support.ActiveUserSliceSupport;

@WebMvcTest(controllers = {FavoriteGroupController.class, FavoriteController.class})
@Import({SecurityConfig.class, WebConfig.class})
class FavoriteGroupControllerTest extends ActiveUserSliceSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private FavoriteGroupService groupService;
    @MockitoBean private FavoriteService favoriteService;

    private String token() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("GET /api/favorite-groups: 200 + 목록")
    void list_200() throws Exception {
        when(groupService.getGroups(1L)).thenReturn(List.of(
                new FavoriteGroupSummaryResponse(3L, "단골", "메모", "#FF5A1F", false, 2L)));

        mockMvc.perform(get("/api/favorite-groups").header("Authorization", token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupId").value(3))
                .andExpect(jsonPath("$.data[0].placeCount").value(2));
    }

    @Test
    @DisplayName("POST /api/favorite-groups: 이름 비면 400")
    void create_400_blankName() throws Exception {
        mockMvc.perform(post("/api/favorite-groups").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/favorite-groups/{id}: 기본 그룹이면 400")
    void delete_400_default() throws Exception {
        doThrow(new BusinessException(ErrorCode.DEFAULT_GROUP_NOT_DELETABLE))
                .when(groupService).deleteGroup(1L, 9L);

        mockMvc.perform(delete("/api/favorite-groups/9").header("Authorization", token()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/favorite-groups/{id}/places: placeId 누락이면 400")
    void add_400_noPlaceId() throws Exception {
        mockMvc.perform(post("/api/favorite-groups/5/places").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/favorite-groups/{id}/places: 200 + 담기 위임")
    void add_200() throws Exception {
        doNothing().when(favoriteService).addPlace(eq(1L), eq(5L), eq(100L));

        mockMvc.perform(post("/api/favorite-groups/5/places").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"placeId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/places/{placeId}/favorite-status: 토큰 없으면 401")
    void status_401() throws Exception {
        mockMvc.perform(get("/api/places/100/favorite-status"))
                .andExpect(status().isUnauthorized());
    }
}
