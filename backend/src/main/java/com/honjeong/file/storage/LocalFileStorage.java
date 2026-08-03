package com.honjeong.file.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * mock-first 개발용 로컬 파일 저장소 — S3 대신 로컬 디렉터리에 저장하고 정적 서빙 경로 기반 URL을 반환(운영 시 S3 구현으로 교체 예정).
 *
 * <p>사용처: FileService(FileStorage 구현체로 주입 — honjeong.files.mode=mock 또는 미지정일 때 활성).
 * <p>개발용(mock) 파일 저장소. 실제 S3 대신 로컬 디렉터리에 파일을 저장하고, 정적 서빙 경로 기반 URL을 돌려준다.
 *
 * <p>{@code @ConditionalOnProperty(..., matchIfMissing = true)}: 설정 {@code honjeong.files.mode}가 "mock"이거나
 * 지정되지 않았을 때(기본) 이 빈이 등록된다. 운영은 {@code honjeong.files.mode=real}로 두고 S3 구현으로 교체한다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.files.mode", havingValue = "mock", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    /**
     * 저장 파일명에 허용하는 확장자. 여기 없는 값은 {@link #FALLBACK_EXTENSION}으로 바꾼다.
     *
     * <p>{@code svg}는 일부러 뺐다 — MIME은 {@code image/*}지만 브라우저가 SVG 안의 스크립트를 실행한다.
     */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp", "gif", "heic", "heif");

    /** 허용 목록에 없거나 확장자가 없는 파일에 붙일 확장자. */
    private static final String FALLBACK_EXTENSION = "jpg";

    private final String localDir;
    private final String baseUrl;

    public LocalFileStorage(@Value("${honjeong.files.local-dir:./uploads}") String localDir,
            @Value("${honjeong.files.base-url:http://localhost:8080/files}") String baseUrl) {
        this.localDir = localDir;
        this.baseUrl = baseUrl;
    }

    /** 기동 시 저장 위치와 공개 URL 접두사를 남긴다 — 사진이 안 열릴 때 원인을 즉시 좁힐 수 있다. */
    @PostConstruct
    void infoStorageMode() {
        log.info("[FILES] 로컬 디스크 저장으로 기동합니다 — dir={}, baseUrl={}", localDir, baseUrl);
    }

    /**
     * 파일을 로컬 디렉터리(localDir)에 UUID 파일명으로 저장하고 baseUrl 기반 접근 URL을 반환.
     *
     * @param file 업로드된 멀티파트 파일
     * @return baseUrl + "/" + 저장 파일명 (저장 실패 시 INTERNAL_ERROR)
     */
    @Override
    public String store(MultipartFile file) {
        String filename = UUID.randomUUID() + "." + extension(file.getOriginalFilename());
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
     * 저장된 파일을 baseUrl 기반 URL로 지운다(우리가 저장한 파일이 아니거나 이미 없으면 조용히 무시).
     *
     * @param url {@link #store}가 반환했던 접근 URL(null·빈 값 허용)
     */
    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(baseUrl + "/")) {
            return; // 우리가 저장한 파일이 아니면 건드리지 않는다(외부 URL·null 방어)
        }
        String filename = url.substring((baseUrl + "/").length());
        try {
            Path root = Path.of(localDir).toAbsolutePath().normalize();
            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                return; // 경로 조작(../)으로 저장 디렉터리 밖을 지우려는 시도 차단
            }
            Files.deleteIfExists(target);
        } catch (Exception e) {
            // 삭제 실패가 탈퇴 전체를 롤백시키면 안 된다(FileStorage.delete 계약) — IOException뿐 아니라
            // Path.resolve의 InvalidPathException, deleteIfExists의 SecurityException 같은 unchecked
            // 예외까지 전부 여기서 삼키고 흔적만 남긴 채 진행한다.
            log.warn("업로드 파일 삭제 실패: {}", filename, e);
        }
    }

    /**
     * 저장 파일명에 쓸 확장자를 정한다 — 원본 확장자가 {@link #ALLOWED_EXTENSIONS}에 있으면 그대로,
     * 아니면(확장자가 없는 경우 포함) {@link #FALLBACK_EXTENSION}.
     *
     * <p><b>★왜 원본 파일명을 그대로 믿으면 안 되는가.</b> 여기서 나온 값이 그대로 저장 파일명의 확장자가
     * 되고, 저장된 파일은 {@code GET /files/**}로 <b>누구나</b> 열 수 있게 공개 서빙된다(SecurityConfig).
     * 스프링 정적 서빙은 파일 <b>확장자</b>로 Content-Type을 정하므로, 원본 파일명을 믿으면
     * {@code Content-Type: image/png} 헤더를 붙인 채 파일명만 {@code x.html}로 보내는 것만으로 우리
     * 도메인에 임의의 HTML/JS를 호스팅할 수 있다. {@link com.honjeong.file.service.FileService}의
     * content-type 검사는 <b>클라이언트가 보낸 헤더</b>를 볼 뿐이라 이 경로를 막지 못한다.
     *
     * <p><b>왜 400으로 거부하지 않고 대체하는가.</b> 업로드 파일명은 기기·OS·이미지 피커가 만들어 주는
     * 값이라 예상 못 한 확장자가 올라올 수 있다. 그때 거부하면 사진 기능이 통째로 실패한다. 확장자를
     * jpg로 바꿔도 이미지 로더는 실제 바이트를 보고 렌더링하므로 정상 이미지는 그대로 보인다 —
     * 즉 대체는 안전한 쪽으로만 틀어진다.
     *
     * <p>저장 파일명의 앞부분은 UUID라 경로 조작(traversal) 위험은 별개로 없다.
     */
    private static String extension(String original) {
        String ext = rawExtension(original);
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : FALLBACK_EXTENSION;
    }

    /** 원본 파일명에서 마지막 점 뒤를 소문자로 추출한다(점이 없거나 뒤가 비면 빈 문자열). */
    private static String rawExtension(String original) {
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
