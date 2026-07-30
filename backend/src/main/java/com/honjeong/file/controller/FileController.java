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
 * 파일(이미지) 업로드 컨트롤러.
 *
 * <p>기본 경로: /api/files
 *
 * <p>파일 업로드 REST 컨트롤러(/api/files). 얇게 유지 — 멀티파트 파일을 받아 {@link FileService}에 위임만 한다.
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
     * 이미지 파일을 업로드한다. multipart/form-data로 받고 part 이름은 {@code file}이다.
     *
     * <p>사용 화면: 프로필 설정(ProfileSetup)·프로필 수정(ProfileEdit)·혼밥 기록 작성(DiningLogWrite)의
     * 사진 업로드. 앱은 shared/upload/imageUpload.ts에서 expo-file-system uploadAsync로 호출한다
     * (RN fetch가 FormData 파일파트를 지원하지 않는다).
     *
     * <p>빈 파일이거나 이미지가 아니면 400({@code INVALID_INPUT})이다.
     *
     * @param file 업로드 파일
     * @return 접근 URL 응답
     */
    @PostMapping
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileService.upload(file));
    }
}
