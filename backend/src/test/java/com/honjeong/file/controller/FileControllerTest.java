package com.honjeong.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.file.dto.FileUploadResponse;
import com.honjeong.file.service.FileService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.global.security.JwtProvider;

/**
 * {@link FileController}의 웹 계층 슬라이스 테스트. 멀티파트 업로드 + 인가(온보딩/USER 허용, 무토큰 401)·검증(400)을 본다.
 */
@WebMvcTest(controllers = FileController.class)
@Import({SecurityConfig.class, WebConfig.class})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private FileService fileService;

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "p.jpg", "image/jpeg", "x".getBytes());
    }

    @Test
    @DisplayName("POST /api/files: USER 토큰 + 이미지면 200 + url")
    void upload_user_200() throws Exception {
        when(fileService.upload(any())).thenReturn(new FileUploadResponse("http://localhost:8080/files/a.jpg"));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(multipart("/api/files").file(image())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("http://localhost:8080/files/a.jpg"));
    }

    @Test
    @DisplayName("POST /api/files: 온보딩 토큰도 허용(ProfileSetup 프로필 사진) 200")
    void upload_onboarding_200() throws Exception {
        when(fileService.upload(any())).thenReturn(new FileUploadResponse("http://localhost:8080/files/a.jpg"));
        String token = jwtProvider.createOnboardingToken(1L);

        mockMvc.perform(multipart("/api/files").file(image())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/files: 빈 파일이면 400 INVALID_INPUT")
    void upload_empty_400() throws Exception {
        when(fileService.upload(any())).thenThrow(new BusinessException(ErrorCode.INVALID_INPUT, "파일이 비어 있습니다."));
        String token = jwtProvider.createAccessToken(1L);

        mockMvc.perform(multipart("/api/files")
                        .file(new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[0]))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /api/files: 토큰 없으면 401")
    void upload_noToken_401() throws Exception {
        mockMvc.perform(multipart("/api/files").file(image()))
                .andExpect(status().isUnauthorized());
    }
}
