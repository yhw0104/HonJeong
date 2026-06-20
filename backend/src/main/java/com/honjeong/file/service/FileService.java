package com.honjeong.file.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.honjeong.file.dto.FileUploadResponse;
import com.honjeong.file.storage.FileStorage;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * 파일 업로드 도메인 서비스. 업로드 요청을 검증한 뒤 실제 저장은 {@link FileStorage}에 위임한다.
 *
 * <p>책임: ① 빈 파일·이미지가 아닌 content-type 거부(INVALID_INPUT) ② 저장 위임 ③ 접근 URL 응답 변환.
 */
@Service
public class FileService {

    private final FileStorage fileStorage;

    public FileService(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    /**
     * 업로드 파일을 검증·저장하고 접근 URL을 반환한다.
     *
     * @param file 업로드된 멀티파트 파일
     * @return 저장된 파일의 접근 URL을 담은 응답
     */
    public FileUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일이 비어 있습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미지 파일만 업로드할 수 있습니다.");
        }
        return new FileUploadResponse(fileStorage.store(file));
    }
}
