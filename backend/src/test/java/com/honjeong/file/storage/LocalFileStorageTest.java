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

    @Test
    @DisplayName("store: 같은 이름을 올려도 UUID로 충돌 없이 별개 파일이 된다")
    void store_uniqueNames() {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "http://localhost:8080/files");
        MockMultipartFile a = new MockMultipartFile("file", "p.jpg", "image/jpeg", "a".getBytes());
        MockMultipartFile b = new MockMultipartFile("file", "p.jpg", "image/jpeg", "b".getBytes());

        assertThat(storage.store(a)).isNotEqualTo(storage.store(b));
    }
}
