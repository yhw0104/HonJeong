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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
class GlobalExceptionHandlerTest {

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

    /** 핸들러 검증을 위해 일부러 예외를 던지는 테스트 전용 컨트롤러. */
    @RestController
    static class TestController {

        /** BusinessException(NOT_FOUND) 발생용 엔드포인트. */
        @GetMapping("/__test/business")
        void business() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "없어요");
        }

        /** @Valid 검증 실패(MethodArgumentNotValidException) 발생용 엔드포인트. */
        @PostMapping("/__test/validate")
        void validate(@RequestBody @Valid Payload payload) {
        }

        /** name이 비어 있으면 검증 실패를 유발하는 요청 본문. */
        record Payload(@NotBlank String name) {
        }
    }
}
