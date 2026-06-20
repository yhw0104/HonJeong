package com.honjeong.file.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.honjeong.file.dto.FileUploadResponse;
import com.honjeong.file.service.FileService;
import com.honjeong.global.common.ApiResponse;

/**
 * 파일 업로드 REST 컨트롤러(/api/files). 얇게 유지 — 멀티파트 파일을 받아 {@link FileService}에 위임만 한다.
 *
 * <p><b>인가:</b> 프로필 사진은 온보딩(ProfileSetup) 단계에서도 업로드하므로 {@code ONBOARDING|USER} 모두 허용한다
 * (SecurityConfig에서 게이팅). 토큰이 없으면 401.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 이미지 파일 업로드.
     *
     * <p><b>요청:</b> {@code POST /api/files} (multipart/form-data, part 이름 {@code file}).
     *
     * <p><b>응답:</b> {@code ApiResponse<FileUploadResponse>} — 업로드된 파일의 접근 URL. 빈 파일·이미지가 아니면
     * 400({@code INVALID_INPUT}).
     *
     * @param file 업로드 파일
     * @return 접근 URL 응답
     */
    @PostMapping
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileService.upload(file));
    }
}
