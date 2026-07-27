package com.honjeong.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageDeleteTest {

    private static final String BASE_URL = "http://localhost:8080/files";

    @Test
    @DisplayName("저장된 파일을 URL로 삭제한다")
    void deletesStoredFile(@TempDir Path dir) throws Exception {
        Path file = Files.createFile(dir.resolve("photo.jpg"));
        LocalFileStorage storage = new LocalFileStorage(dir.toString(), BASE_URL);

        storage.delete(BASE_URL + "/photo.jpg");

        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    @DisplayName("파일이 없어도 예외를 던지지 않는다 — 탈퇴가 파일 하나 때문에 실패하면 안 된다")
    void missingFileIsIgnored(@TempDir Path dir) {
        LocalFileStorage storage = new LocalFileStorage(dir.toString(), BASE_URL);

        assertThatCode(() -> storage.delete(BASE_URL + "/gone.jpg")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null·빈 값·우리 baseUrl이 아닌 URL은 무시한다")
    void foreignUrlIsIgnored(@TempDir Path dir) {
        LocalFileStorage storage = new LocalFileStorage(dir.toString(), BASE_URL);

        assertThatCode(() -> storage.delete(null)).doesNotThrowAnyException();
        assertThatCode(() -> storage.delete("")).doesNotThrowAnyException();
        assertThatCode(() -> storage.delete("https://evil.example.com/x.jpg")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("경로 조작이 섞인 URL은 저장 디렉터리 밖 파일을 지우지 않는다")
    void traversalIsRejected(@TempDir Path dir) throws Exception {
        Path outside = Files.createFile(dir.getParent().resolve("secret-" + dir.getFileName() + ".txt"));
        LocalFileStorage storage = new LocalFileStorage(dir.resolve("uploads").toString(), BASE_URL);

        storage.delete(BASE_URL + "/../" + outside.getFileName());

        assertThat(Files.exists(outside)).isTrue();
        Files.deleteIfExists(outside);
    }
}
