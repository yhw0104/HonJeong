package com.honjeong.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.honjeong.support.ActiveUserSliceSupport;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * {@link GlobalExceptionHandler} 슬라이스 테스트.
 * 임시 TestController가 던지는 예외가 advice를 통해 공통 에러 엔벨로프로 변환되는지 검증한다.
 * ({@code addFilters=false}로 보안 필터를 끄고 예외 변환만 따로 본다.)
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException은 ErrorCode의 상태·코드·메시지로 변환된다")
    void businessException_mappedToEnvelope() throws Exception {
        // given: NOT_FOUND + "없어요"로 BusinessException을 던지는 엔드포인트
        // when: 해당 엔드포인트를 호출하면
        // then: 404 상태와 success=false, error.code=NOT_FOUND, error.message=없어요로 변환된다
        mockMvc.perform(get("/__test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("없어요"));
    }

    @Test
    @DisplayName("@Valid 검증 실패는 400 INVALID_INPUT 엔벨로프로 변환된다")
    void validationError_mappedToBadRequest() throws Exception {
        // given: @NotBlank name이 빠진 빈 본문({})
        // when: @Valid가 걸린 엔드포인트에 POST하면
        // then: 400 상태와 success=false, error.code=INVALID_INPUT으로 변환된다
        mockMvc.perform(post("/__test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("PathVariable 타입 불일치는 500이 아니라 400 INVALID_INPUT이다")
    void typeMismatch_mappedToBadRequest() throws Exception {
        // given: Long id를 받는 엔드포인트에 숫자가 아닌 값
        // when/then: 클라이언트 잘못이므로 400이어야 한다(서버 오류 500이 아니다)
        mockMvc.perform(get("/__test/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("읽을 수 없는 본문(깨진 JSON)은 500이 아니라 400 INVALID_INPUT이다")
    void unreadableBody_mappedToBadRequest() throws Exception {
        mockMvc.perform(post("/__test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 500이 아니라 405 METHOD_NOT_ALLOWED다")
    void wrongMethod_mappedToMethodNotAllowed() throws Exception {
        // given: /__test/business는 GET만 매핑돼 있다
        mockMvc.perform(post("/__test/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("매핑이 없는 경로는 500이 아니라 404 NOT_FOUND다")
    void unmappedPath_mappedToNotFound() throws Exception {
        mockMvc.perform(get("/__test/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("업로드 용량 초과는 500이 아니라 413 FILE_TOO_LARGE다")
    void oversizedUpload_mappedToPayloadTooLarge() throws Exception {
        // given: 멀티파트 상한을 넘겨 스프링이 MaxUploadSizeExceededException을 던지는 상황
        // when/then: 사진이 큰 것은 클라이언트 사정이므로 4xx여야 한다. 500으로 나가면 앱은
        //            "서버 오류"라는 애매한 메시지만 띄우고, 운영자는 500 비율로 장애를 오판한다.
        mockMvc.perform(get("/__test/too-large"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"));
    }

    /** 핸들러 검증을 위해 일부러 예외를 던지는 테스트 전용 컨트롤러. */
    @RestController
    static class TestController {

        /** 업로드 용량 초과(MaxUploadSizeExceededException) 발생용 엔드포인트. */
        @GetMapping("/__test/too-large")
        void tooLarge() {
            throw new MaxUploadSizeExceededException(5 * 1024 * 1024);
        }

        /** BusinessException(NOT_FOUND) 발생용 엔드포인트. GET만 매핑해 405 케이스에도 쓴다. */
        @GetMapping("/__test/business")
        void business() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "없어요");
        }

        /** @Valid 검증 실패(MethodArgumentNotValidException) 발생용 엔드포인트. */
        @PostMapping("/__test/validate")
        void validate(@RequestBody @Valid Payload payload) {
        }

        /** PathVariable 타입 불일치(MethodArgumentTypeMismatchException) 발생용 엔드포인트. */
        @GetMapping("/__test/typed/{id}")
        void typed(@PathVariable Long id) {
        }

        /** name이 비어 있으면 검증 실패를 유발하는 요청 본문. */
        record Payload(@NotBlank String name) {
        }
    }
}
