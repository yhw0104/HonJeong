package com.honjeong.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.honjeong.file.dto.FileUploadResponse;
import com.honjeong.file.storage.FileStorage;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/**
 * FileService 단위 테스트(순수 Mockito). 검증·위임 책임만 본다 — 실제 저장은 FileStorage 몫이라 모킹한다.
 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileStorage fileStorage;

    @InjectMocks
    FileService service;

    @Test
    @DisplayName("upload: 정상 이미지면 storage에 위임하고 반환 URL을 응답으로 감싼다")
    void upload_ok() {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", "x".getBytes());
        when(fileStorage.store(file)).thenReturn("http://localhost:8080/files/abc.jpg");

        FileUploadResponse res = service.upload(file);

        assertThat(res.url()).isEqualTo("http://localhost:8080/files/abc.jpg");
    }

    @Test
    @DisplayName("upload: 빈 파일이면 INVALID_INPUT(400) — storage 미호출")
    void upload_empty() {
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(fileStorage);
    }

    @Test
    @DisplayName("upload: 이미지가 아닌 content-type이면 INVALID_INPUT(400) — storage 미호출")
    void upload_notImage() {
        MockMultipartFile file = new MockMultipartFile("file", "p.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(fileStorage);
    }
}
