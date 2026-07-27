package com.honjeong.block.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.honjeong.block.dto.BlockedUserResponse;
import com.honjeong.block.service.BlockService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.support.ActiveUserSliceSupport;

@WebMvcTest(controllers = BlockController.class)
@Import({SecurityConfig.class, WebConfig.class})
class BlockControllerTest extends ActiveUserSliceSupport {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private BlockService blockService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("POST /api/blocks: 201 + 차단 서비스 호출")
    void block_201() throws Exception {
        mockMvc.perform(post("/api/blocks").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(blockService).block(1L, 2L);
    }

    @Test
    @DisplayName("GET /api/blocks: 200 + 차단 목록")
    void list_200() throws Exception {
        List<BlockedUserResponse> stub =
                List.of(new BlockedUserResponse(2L, "상대", null, LocalDateTime.now()));
        when(blockService.getMyBlocks(1L)).thenReturn(stub);

        mockMvc.perform(get("/api/blocks").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(2))
                .andExpect(jsonPath("$.data[0].nickname").value("상대"));
    }

    @Test
    @DisplayName("DELETE /api/blocks/2: 200 + 차단 해제 서비스 호출")
    void unblock_200() throws Exception {
        mockMvc.perform(delete("/api/blocks/2").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(blockService).unblock(1L, 2L);
    }

    @Test
    @DisplayName("POST /api/blocks: targetUserId 누락 → 400")
    void block_400_missingTargetUserId() throws Exception {
        mockMvc.perform(post("/api/blocks").header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(blockService);
    }
}
