package com.honjeong.file.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * LocalFileStorage 단위 테스트(목 저장소). 임시 디렉터리에 실제로 바이트를 쓰고, base-url 기반 URL을 돌려주는지 검증한다.
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("store: 파일을 디렉터리에 저장하고 base-url 기반 URL(확장자 유지)을 반환한다")
    void store_savesAndReturnsUrl() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080/files");
        MockMultipartFile file = new MockMultipartFile("file", "photo.JPG", "image/jpeg", "bytes".getBytes());

        String url = storage.store(file);

        assertThat(url).startsWith("http://localhost:8080/files/");
        assertThat(url).endsWith(".jpg"); // 확장자는 소문자로 보존
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
    }

    /**
     * 저장 파일명의 확장자가 그대로 공개 URL의 확장자가 되고, 스프링 정적 서빙은 그 확장자로 Content-Type을
     * 정한다. 그래서 원본 파일명을 믿으면 우리 도메인에 임의 HTML을 호스팅할 수 있게 된다 —
     * content-type 헤더는 클라이언트가 보내는 값이라 이걸 막지 못한다(FileService의 검사를 통과한다).
     */
    @Test
    @DisplayName("store: 이미지가 아닌 확장자(.html)는 저장 파일명에서 jpg로 대체된다")
    void store_replacesNonImageExtension() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080/files");
        MockMultipartFile file = new MockMultipartFile("file", "x.html", "image/png", "<script>alert(1)</script>".getBytes());

        String url = storage.store(file);

        assertThat(url).doesNotEndWith(".html");
        assertThat(url).endsWith(".jpg");
    }

    /** SVG는 확장자가 image/*로 매핑되지만 브라우저가 그 안의 스크립트를 실행하므로 허용 목록에서 뺀다. */
    @Test
    @DisplayName("store: .svg도 jpg로 대체된다")
    void store_replacesSvgExtension() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080/files");
        MockMultipartFile file = new MockMultipartFile("file", "x.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThat(storage.store(file)).endsWith(".jpg");
    }

    @Test
    @DisplayName("store: 허용 목록의 확장자(.png)는 그대로 보존한다")
    void store_keepsAllowedExtension() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080/files");
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", "bytes".getBytes());

        assertThat(storage.store(file)).endsWith(".png");
    }

    @Test
    @DisplayName("store: 확장자가 없으면 jpg를 붙인다")
    void store_addsExtensionWhenMissing() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080/files");
        MockMultipartFile file = new MockMultipartFile("file", "photo", "image/jpeg", "bytes".getBytes());

        assertThat(storage.store(file)).endsWith(".jpg");
    }

    @Test
    @DisplayName("store: 같은 이름을 올려도 UUID로 충돌 없이 별개 파일이 된다")
    void store_uniqueNames() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080/files");
        MockMultipartFile a = new MockMultipartFile("file", "p.jpg", "image/jpeg", "a".getBytes());
        MockMultipartFile b = new MockMultipartFile("file", "p.jpg", "image/jpeg", "b".getBytes());

        assertThat(storage.store(a)).isNotEqualTo(storage.store(b));
    }
}
