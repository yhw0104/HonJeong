package com.honjeong.file.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * 1. 기능: mock-first 개발용 로컬 파일 저장소 — S3 대신 로컬 디렉터리에 저장하고 정적 서빙 경로 기반 URL을 반환(운영 시 S3 구현으로 교체 예정)
 * 2. 사용처: FileService(FileStorage 구현체로 주입 — honjeong.files.mode=mock 또는 미지정일 때 활성)
 *
 * <p>[기존 주석] 개발용(mock) 파일 저장소. 실제 S3 대신 로컬 디렉터리에 파일을 저장하고, 정적 서빙 경로 기반 URL을 돌려준다.
 *
 * <p>{@code @ConditionalOnProperty(..., matchIfMissing = true)}: 설정 {@code honjeong.files.mode}가 "mock"이거나
 * 지정되지 않았을 때(기본) 이 빈이 등록된다. 운영은 {@code honjeong.files.mode=real}로 두고 S3 구현으로 교체한다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.files.mode", havingValue = "mock", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final String localDir;
    private final String baseUrl;

    public LocalFileStorage(@Value("${honjeong.files.local-dir:./uploads}") String localDir,
            @Value("${honjeong.files.base-url:http://localhost:8080/files}") String baseUrl) {
        this.localDir = localDir;
        this.baseUrl = baseUrl;
    }

    /**
     * 기능: 파일을 로컬 디렉터리(localDir)에 UUID 파일명으로 저장하고 baseUrl 기반 접근 URL을 반환
     * Request: file — 업로드된 멀티파트 파일
     * Response: String — baseUrl + "/" + 저장 파일명 (저장 실패 시 INTERNAL_ERROR)
     */
    @Override
    public String store(MultipartFile file) {
        String ext = extension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path target = Path.of(localDir).toAbsolutePath().resolve(filename);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "파일 저장에 실패했습니다.");
        }
        return baseUrl + "/" + filename;
    }

    /**
     * 기능: 원본 파일명에서 확장자를 소문자로 추출(없으면 빈 문자열)
     *
     * <p>[기존 주석] 원본 파일명에서 확장자를 소문자로 추출한다. 없으면 빈 문자열. 저장 파일명은 UUID라 경로 조작(traversal) 위험이 없다.
     */
    private static String extension(String original) {
        if (original == null) {
            return "";
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            return "";
        }
        return original.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
