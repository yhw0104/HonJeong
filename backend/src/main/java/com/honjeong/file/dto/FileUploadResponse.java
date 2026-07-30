package com.honjeong.file.dto;

/**
 * 파일 업로드 결과(업로드된 파일의 접근 URL)를 나타내는 응답 DTO.
 *
 * <p>파일 업로드 응답 DTO. 업로드된 파일에 접근할 수 있는 URL을 담는다.
 *
 * <p>클라이언트는 이 {@code url}을 프로필 사진({@code profileImageUrl}) 등 후속 요청에 그대로 사용한다.
 *
 * @param url 업로드된 파일의 접근 URL
 */
public record FileUploadResponse(String url) {
}
